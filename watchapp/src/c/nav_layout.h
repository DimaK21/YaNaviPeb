#pragma once

// Pure geometry for nav_window.c's layout, kept free of pebble.h so it can be exercised by a
// native test (tests/nav_layout_test.c) without the Pebble SDK/emulator.

typedef struct {
  int x, y, w, h;
} NavRect;

typedef struct {
  NavRect message;
  NavRect hint;
  NavRect icon;
  NavRect distance;
  NavRect maneuver;
  NavRect divider;
  NavRect eta;
  NavRect summary;
} NavLayout;

#define NAV_LAYOUT_MARGIN 4
// The 64x64 icon (nav_state.h's ICON_SIZE) is scaled to this fraction of the reference size, so
// it shrinks together with everything else instead of eating a growing share of small screens.
#define NAV_LAYOUT_ICON_NATIVE 64
// Every row's Y position/height below was hand-tuned against emery's 200x228 rectangular
// display; other rectangular displays scale them against their own height, so screen_h=228
// reproduces the reference pixels exactly.
#define NAV_LAYOUT_REF_H 228
// On round displays, content is confined to a square this fraction of the diameter, centered on
// the screen. 1/sqrt(2) (~0.707) is the *largest* axis-aligned square that fits the circle, but
// its corners then sit exactly on the bezel; 0.65 leaves a real few-pixel margin at every
// corner of every row instead of just touching the glass edge.
#define NAV_LAYOUT_ROUND_FRACTION 0.65

static inline int nav_layout_scale(int v, int layout_h) {
  return (int)(((double)v * layout_h) / NAV_LAYOUT_REF_H + 0.5);
}

static inline NavRect nav_layout_row(int x, int w, int offset_y, int layout_h, int ref_y, int ref_h) {
  NavRect r = {x, offset_y + nav_layout_scale(ref_y, layout_h), w, nav_layout_scale(ref_h, layout_h)};
  return r;
}

// Computes label/icon placement for a screen_w x screen_h display. `is_round` shrinks the
// working area to a bezel-safe centered square (see NAV_LAYOUT_ROUND_FRACTION) so no row needs
// its own per-row bezel math.
static inline NavLayout nav_layout_compute(int screen_w, int screen_h, int is_round) {
  int layout_w = screen_w;
  int layout_h = screen_h;
  int offset_x = 0;
  int offset_y = 0;
  if (is_round) {
    int min_dim = (screen_w < screen_h) ? screen_w : screen_h;
    int side = (int)((double)min_dim * NAV_LAYOUT_ROUND_FRACTION + 0.5);
    offset_x = (screen_w - side) / 2;
    offset_y = (screen_h - side) / 2;
    layout_w = side;
    layout_h = side;
  }

  int x = offset_x + NAV_LAYOUT_MARGIN;
  int inner_w = layout_w - 2 * NAV_LAYOUT_MARGIN;

  NavLayout out;
  out.message = nav_layout_row(x, inner_w, offset_y, layout_h, 70, 90);
  out.hint = nav_layout_row(x, inner_w, offset_y, layout_h, 162, 40);
  out.distance = nav_layout_row(x, inner_w, offset_y, layout_h, 70, 34);
  out.maneuver = nav_layout_row(x, inner_w, offset_y, layout_h, 106, 58);
  out.divider = nav_layout_row(x, inner_w, offset_y, layout_h, 170, 1);
  out.eta = nav_layout_row(x, inner_w, offset_y, layout_h, 173, 26);
  out.summary = nav_layout_row(x, inner_w, offset_y, layout_h, 199, 28);

  int icon_size = nav_layout_scale(NAV_LAYOUT_ICON_NATIVE, layout_h);
  int icon_x = offset_x + (layout_w - icon_size) / 2;
  int icon_y = offset_y + nav_layout_scale(4, layout_h);
  out.icon = (NavRect){icon_x, icon_y, icon_size, icon_size};

  return out;
}
