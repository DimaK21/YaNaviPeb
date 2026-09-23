// Standalone native test for nav_layout_compute(). Has no dependency on the Pebble SDK, so it
// is compiled and run directly with the host C compiler, not through `pebble build`:
//
//   cc -std=c99 -Wall -Wextra tests/nav_layout_test.c -lm -o /tmp/nav_layout_test && /tmp/nav_layout_test

#include "../src/c/nav_layout.h"

#include <math.h>
#include <stddef.h>
#include <stdio.h>

static int failures = 0;

static void expect_rect(const char *label, NavRect actual, NavRect expected) {
  if (actual.x != expected.x || actual.y != expected.y ||
      actual.w != expected.w || actual.h != expected.h) {
    fprintf(stderr, "FAIL: %s = {%d,%d,%d,%d}, expected {%d,%d,%d,%d}\n", label,
            actual.x, actual.y, actual.w, actual.h,
            expected.x, expected.y, expected.w, expected.h);
    failures++;
  }
}

// Distance from screen center to the farthest corner of `r`, on a screen_w x screen_h display.
static double nav_rect_max_corner_dist(NavRect r, int screen_w, int screen_h) {
  double cx = screen_w / 2.0, cy = screen_h / 2.0;
  double corners_x[2] = {r.x, r.x + r.w};
  double corners_y[2] = {r.y, r.y + r.h};
  double worst = 0;
  for (int i = 0; i < 2; i++) {
    for (int j = 0; j < 2; j++) {
      double d = hypot(corners_x[i] - cx, corners_y[j] - cy);
      if (d > worst) worst = d;
    }
  }
  return worst;
}

int main(void) {
  // emery (Pebble Time 2, 200x228 rectangular) must reproduce today's hand-tuned pixel layout
  // exactly, since the primary device's look must not change.
  NavLayout emery = nav_layout_compute(200, 228, 0);
  expect_rect("emery.message", emery.message, (NavRect){4, 70, 192, 90});
  expect_rect("emery.hint", emery.hint, (NavRect){4, 162, 192, 40});
  expect_rect("emery.icon", emery.icon, (NavRect){68, 4, 64, 64});
  expect_rect("emery.distance", emery.distance, (NavRect){4, 70, 192, 34});
  expect_rect("emery.maneuver", emery.maneuver, (NavRect){4, 106, 192, 58});
  expect_rect("emery.divider", emery.divider, (NavRect){4, 170, 192, 1});
  expect_rect("emery.eta", emery.eta, (NavRect){4, 173, 192, 26});
  expect_rect("emery.summary", emery.summary, (NavRect){4, 199, 192, 28});

  // flint (Pebble 2 Duo) and the legacy rectangular platforms (aplite, basalt, diorite) share
  // this exact 144x168 panel, so one scaled-down layout serves all four.
  NavLayout small_rect = nav_layout_compute(144, 168, 0);
  expect_rect("small_rect.message", small_rect.message, (NavRect){4, 52, 136, 66});
  expect_rect("small_rect.hint", small_rect.hint, (NavRect){4, 119, 136, 29});
  expect_rect("small_rect.icon", small_rect.icon, (NavRect){48, 3, 47, 47});
  expect_rect("small_rect.summary", small_rect.summary, (NavRect){4, 147, 136, 21});

  // gabbro (Pebble Round 2, 260x260 round) must not put anything under the bezel: every row
  // scales inside a centered square well clear of the circle (see NAV_LAYOUT_ROUND_FRACTION).
  NavLayout gabbro = nav_layout_compute(260, 260, 1);
  expect_rect("gabbro.message", gabbro.message, (NavRect){49, 97, 161, 67});
  expect_rect("gabbro.icon", gabbro.icon, (NavRect){106, 48, 47, 47});
  expect_rect("gabbro.summary", gabbro.summary, (NavRect){49, 193, 161, 21});

  // chalk (Pebble Time Round, 180x180 round) gets the same bezel-safe treatment as gabbro.
  NavLayout chalk = nav_layout_compute(180, 180, 1);
  expect_rect("chalk.message", chalk.message, (NavRect){35, 67, 109, 46});
  expect_rect("chalk.icon", chalk.icon, (NavRect){73, 33, 33, 33});
  expect_rect("chalk.summary", chalk.summary, (NavRect){35, 133, 109, 14});

  // Every row of every real target platform must stay fully on screen, the icon must never
  // overlap the row below it, and on round screens every corner must clear the true bezel with
  // real breathing room -- not just be mathematically inside the circle. This is a regression
  // test for a real bug: an earlier version let the icon (scaled by Y only, not by size) collide
  // with the message row, and let a too-generous inscribed square put row corners exactly on the
  // glass edge instead of clear of it.
  const int min_icon_message_gap = 1;
  const double min_bezel_clearance = 3.0;  // px of slack demanded beyond the exact circle edge
  struct { const char *name; int w, h, round; } platforms[] = {
      {"aplite", 144, 168, 0}, {"basalt", 144, 168, 0}, {"chalk", 180, 180, 1},
      {"diorite", 144, 168, 0}, {"flint", 144, 168, 0}, {"emery", 200, 228, 0},
      {"gabbro", 260, 260, 1},
  };
  for (size_t i = 0; i < sizeof(platforms) / sizeof(platforms[0]); i++) {
    NavLayout l = nav_layout_compute(platforms[i].w, platforms[i].h, platforms[i].round);
    NavRect rects[] = {l.message, l.hint, l.icon, l.distance, l.maneuver, l.divider, l.eta, l.summary};
    for (size_t j = 0; j < sizeof(rects) / sizeof(rects[0]); j++) {
      NavRect r = rects[j];
      if (r.x < 0 || r.y < 0 || r.w <= 0 || r.h <= 0 ||
          r.x + r.w > platforms[i].w || r.y + r.h > platforms[i].h) {
        fprintf(stderr, "FAIL: %s rect %zu = {%d,%d,%d,%d} escapes %dx%d screen\n",
                platforms[i].name, j, r.x, r.y, r.w, r.h, platforms[i].w, platforms[i].h);
        failures++;
      }
      if (platforms[i].round) {
        double dist = nav_rect_max_corner_dist(r, platforms[i].w, platforms[i].h);
        double radius = platforms[i].w / 2.0;
        if (dist > radius - min_bezel_clearance) {
          fprintf(stderr, "FAIL: %s rect %zu corner dist %.1f is within %.1fpx of the %.0fpx bezel\n",
                  platforms[i].name, j, dist, radius - dist, radius);
          failures++;
        }
      }
    }
    if (l.icon.y + l.icon.h + min_icon_message_gap > l.message.y) {
      fprintf(stderr, "FAIL: %s icon (bottom %d) overlaps message row (y %d)\n",
              platforms[i].name, l.icon.y + l.icon.h, l.message.y);
      failures++;
    }
  }

  if (failures == 0) {
    printf("all nav_layout tests passed\n");
    return 0;
  }
  fprintf(stderr, "%d nav_layout test(s) failed\n", failures);
  return 1;
}
