#include "nav_window.h"

#include "nav_layout.h"

static Window *s_window;
static NavState *s_state;

static Layer *s_icon_layer;
static Layer *s_divider_layer;
static TextLayer *s_message_layer;  // "start navigation" / "navigation finished"
static TextLayer *s_hint_layer;  // caveat shown under "navigation finished" only
static TextLayer *s_distance_layer;
static TextLayer *s_maneuver_layer;
static TextLayer *s_eta_layer;
static TextLayer *s_summary_layer;  // remaining distance / duration
static char s_summary[40];
static NavLayout s_layout;

static bool icon_bit(int x, int y) {
  return s_state->icon[y * (ICON_SIZE / 8) + x / 8] & (0x80 >> (x % 8));
}

// The watch's own locale, not the phone's: distance/maneuver/eta/duration come from Yandex Maps
// as-is and are not translated here.
static bool is_russian_locale(void) {
  const char *locale = i18n_get_system_locale();
  return locale && strncmp(locale, "ru", 2) == 0;
}

// Draws the ICON_SIZE x ICON_SIZE source bitmap into the layer's own (possibly smaller) bounds,
// nearest-neighbor downsampled so it shrinks together with the rest of the layout instead of
// getting clipped. On emery, where the layer is still native size, dst==src and this is a
// pixel-for-pixel copy, same as before.
static void icon_update_proc(Layer *layer, GContext *ctx) {
  if (!s_state->has_icon) return;
  GSize dst = layer_get_bounds(layer).size;
  if (dst.w <= 0 || dst.h <= 0) return;
  graphics_context_set_fill_color(ctx, GColorBlack);
  for (int y = 0; y < dst.h; y++) {
    int sy = y * ICON_SIZE / dst.h;
    int x = 0;
    while (x < dst.w) {
      if (!icon_bit(x * ICON_SIZE / dst.w, sy)) {
        x++;
        continue;
      }
      int start = x;
      while (x < dst.w && icon_bit(x * ICON_SIZE / dst.w, sy)) x++;
      graphics_fill_rect(ctx, GRect(start, y, x - start, 1), 0, GCornerNone);
    }
  }
}

static void divider_update_proc(Layer *layer, GContext *ctx) {
  graphics_context_set_stroke_color(ctx, GColorDarkGray);
  graphics_draw_line(ctx, GPoint(0, 0), GPoint(layer_get_bounds(layer).size.w, 0));
}

// Every row picks its font from a ladder, largest first, that starts with the row's original
// fixed/preferred font -- so on emery's generously hand-tuned boxes the first tier always fits
// and every row renders exactly as before. Smaller screens (flint, the round platforms) fall
// through to the later, smaller tiers instead of clipping.

// Rows meant to stay on one line (distance, eta, summary): largest ladder font whose *unwrapped*
// text fits inside max_w x max_h. A single-line box that wraps anyway gets clipped by whatever
// sits below it, so this shrinks the font rather than letting that happen.
static GFont choose_font_single_line(const char *text, const char *const *ladder, int ladder_len,
                                      int max_w, int max_h) {
  for (int i = 0; i < ladder_len; i++) {
    GFont font = fonts_get_system_font(ladder[i]);
    GSize natural = graphics_text_layout_get_content_size(
        text, font, GRect(0, 0, 1000, 1000), GTextOverflowModeFill, GTextAlignmentLeft);
    if (natural.w <= max_w && natural.h <= max_h) return font;
  }
  return fonts_get_system_font(ladder[ladder_len - 1]);
}

// Rows meant to wrap across multiple lines (message, hint, maneuver): largest ladder font whose
// word-wrapped rendering (wrapped to max_w) fits within max_h.
static GFont choose_font_wrapped(const char *text, const char *const *ladder, int ladder_len,
                                  int max_w, int max_h) {
  for (int i = 0; i < ladder_len; i++) {
    GFont font = fonts_get_system_font(ladder[i]);
    GSize natural = graphics_text_layout_get_content_size(
        text, font, GRect(0, 0, max_w, 1000), GTextOverflowModeWordWrap, GTextAlignmentLeft);
    if (natural.h <= max_h) return font;
  }
  return fonts_get_system_font(ladder[ladder_len - 1]);
}

static const char *const MESSAGE_FONTS[] = {
    FONT_KEY_GOTHIC_28_BOLD, FONT_KEY_GOTHIC_24_BOLD, FONT_KEY_GOTHIC_18_BOLD, FONT_KEY_GOTHIC_14_BOLD};
static const char *const HINT_FONTS[] = {FONT_KEY_GOTHIC_18, FONT_KEY_GOTHIC_14};
static const char *const DISTANCE_FONTS[] = {
    FONT_KEY_GOTHIC_28_BOLD, FONT_KEY_GOTHIC_18_BOLD, FONT_KEY_GOTHIC_14_BOLD};
static const char *const MANEUVER_FONTS[] = {
    FONT_KEY_GOTHIC_24_BOLD, FONT_KEY_GOTHIC_18_BOLD, FONT_KEY_GOTHIC_14_BOLD};
static const char *const ETA_FONTS[] = {
    FONT_KEY_GOTHIC_24_BOLD, FONT_KEY_GOTHIC_18_BOLD, FONT_KEY_GOTHIC_14_BOLD};
static const char *const SUMMARY_FONTS[] = {
    FONT_KEY_GOTHIC_24_BOLD, FONT_KEY_GOTHIC_18_BOLD, FONT_KEY_GOTHIC_14_BOLD};

#define FONT_LADDER_LEN(arr) (int)(sizeof(arr) / sizeof((arr)[0]))

static TextLayer *make_text(Layer *root, GRect frame, const char *font_key, GTextAlignment align) {
  TextLayer *layer = text_layer_create(frame);
  text_layer_set_background_color(layer, GColorClear);
  text_layer_set_text_color(layer, GColorBlack);
  text_layer_set_font(layer, fonts_get_system_font(font_key));
  text_layer_set_text_alignment(layer, align);
  layer_add_child(root, text_layer_get_layer(layer));
  return layer;
}

static GRect to_grect(NavRect r) {
  return GRect(r.x, r.y, r.w, r.h);
}

static void window_load(Window *window) {
  Layer *root = window_get_root_layer(window);
  GSize size = layer_get_bounds(root).size;
  // ICON_SIZE (nav_state.h) and NAV_LAYOUT_ICON_SIZE (nav_layout.h) must both track the 64x64
  // icon the phone sends; nav_layout.h stays free of pebble.h so it can't just reuse ICON_SIZE.
  s_layout = nav_layout_compute(size.w, size.h, PBL_IF_ROUND_ELSE(true, false));

  s_message_layer = make_text(root, to_grect(s_layout.message), FONT_KEY_GOTHIC_28_BOLD, GTextAlignmentCenter);
  s_hint_layer = make_text(root, to_grect(s_layout.hint), FONT_KEY_GOTHIC_18, GTextAlignmentCenter);

  s_icon_layer = layer_create(to_grect(s_layout.icon));
  layer_set_update_proc(s_icon_layer, icon_update_proc);
  layer_add_child(root, s_icon_layer);

  s_distance_layer = make_text(root, to_grect(s_layout.distance), FONT_KEY_GOTHIC_28_BOLD, GTextAlignmentCenter);
  s_maneuver_layer = make_text(root, to_grect(s_layout.maneuver), FONT_KEY_GOTHIC_24_BOLD, GTextAlignmentCenter);

  s_divider_layer = layer_create(to_grect(s_layout.divider));
  layer_set_update_proc(s_divider_layer, divider_update_proc);
  layer_add_child(root, s_divider_layer);

  s_eta_layer = make_text(root, to_grect(s_layout.eta), FONT_KEY_GOTHIC_24_BOLD, GTextAlignmentCenter);
  s_summary_layer = make_text(root, to_grect(s_layout.summary), FONT_KEY_GOTHIC_24, GTextAlignmentCenter);

  nav_window_refresh();
}

static void window_unload(Window *window) {
  text_layer_destroy(s_message_layer);
  text_layer_destroy(s_hint_layer);
  text_layer_destroy(s_distance_layer);
  text_layer_destroy(s_maneuver_layer);
  text_layer_destroy(s_eta_layer);
  text_layer_destroy(s_summary_layer);
  layer_destroy(s_icon_layer);
  layer_destroy(s_divider_layer);
}

void nav_window_push(NavState *state) {
  s_state = state;
  s_window = window_create();
  window_set_background_color(s_window, GColorWhite);
  window_set_window_handlers(s_window, (WindowHandlers){.load = window_load, .unload = window_unload});
  window_stack_push(s_window, true);
}

void nav_window_refresh(void) {
  if (!s_window) return;
  bool navigating = s_state->navigating;

  layer_set_hidden(text_layer_get_layer(s_message_layer), navigating);
  layer_set_hidden(s_icon_layer, !navigating);
  layer_set_hidden(text_layer_get_layer(s_distance_layer), !navigating);
  layer_set_hidden(text_layer_get_layer(s_maneuver_layer), !navigating);
  layer_set_hidden(s_divider_layer, !navigating);
  layer_set_hidden(text_layer_get_layer(s_eta_layer), !navigating);
  layer_set_hidden(text_layer_get_layer(s_summary_layer), !navigating);

  if (!navigating) {
    bool ru = is_russian_locale();
    bool finished = s_state->finished;
    // Yandex Maps cancels its notification both when a route actually ends and whenever its own
    // app is opened in the foreground for more than NavSyncer's debounce window, so "finished"
    // cannot be told apart from "the phone app is open" — the hint says so instead of asserting
    // an end that may not have happened.
    const char *message = finished
        ? (ru ? "Навигация завершена" : "Navigation finished")
        : (ru ? "Ожидание фоновой навигации" : "Waiting for background navigation");
    text_layer_set_font(s_message_layer, choose_font_wrapped(message, MESSAGE_FONTS, FONT_LADDER_LEN(MESSAGE_FONTS),
                                                              s_layout.message.w, s_layout.message.h));
    text_layer_set_text(s_message_layer, message);
    layer_set_hidden(text_layer_get_layer(s_hint_layer), !finished);
    if (finished) {
      const char *hint = ru ? "или открыты Карты" : "or Maps app is open";
      text_layer_set_font(s_hint_layer, choose_font_wrapped(hint, HINT_FONTS, FONT_LADDER_LEN(HINT_FONTS),
                                                             s_layout.hint.w, s_layout.hint.h));
      text_layer_set_text(s_hint_layer, hint);
    }
    return;
  }
  layer_set_hidden(text_layer_get_layer(s_hint_layer), true);

  if (s_state->remaining[0] && s_state->duration[0]) {
    snprintf(s_summary, sizeof(s_summary), "%s / %s", s_state->remaining, s_state->duration);
  } else {
    snprintf(s_summary, sizeof(s_summary), "%s%s", s_state->remaining, s_state->duration);
  }
  text_layer_set_font(s_distance_layer, choose_font_single_line(s_state->distance, DISTANCE_FONTS,
                                                                 FONT_LADDER_LEN(DISTANCE_FONTS),
                                                                 s_layout.distance.w, s_layout.distance.h));
  text_layer_set_text(s_distance_layer, s_state->distance);
  text_layer_set_font(s_maneuver_layer, choose_font_wrapped(s_state->maneuver, MANEUVER_FONTS,
                                                             FONT_LADDER_LEN(MANEUVER_FONTS),
                                                             s_layout.maneuver.w, s_layout.maneuver.h));
  text_layer_set_text(s_maneuver_layer, s_state->maneuver);
  text_layer_set_font(s_eta_layer, choose_font_single_line(s_state->eta, ETA_FONTS, FONT_LADDER_LEN(ETA_FONTS),
                                                            s_layout.eta.w, s_layout.eta.h));
  text_layer_set_text(s_eta_layer, s_state->eta);
  text_layer_set_font(s_summary_layer, choose_font_single_line(s_summary, SUMMARY_FONTS, FONT_LADDER_LEN(SUMMARY_FONTS),
                                                                s_layout.summary.w, s_layout.summary.h));
  text_layer_set_text(s_summary_layer, s_summary);
  layer_mark_dirty(s_icon_layer);
}

void nav_window_destroy(void) {
  if (s_window) window_destroy(s_window);
  s_window = NULL;
}
