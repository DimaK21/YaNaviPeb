#include "nav_window.h"

#define MARGIN 4

static Window *s_window;
static NavState *s_state;

static Layer *s_icon_layer;
static Layer *s_divider_layer;
static TextLayer *s_message_layer;  // "start navigation" / "navigation finished"
static TextLayer *s_distance_layer;
static TextLayer *s_maneuver_layer;
static TextLayer *s_eta_layer;
static TextLayer *s_summary_layer;  // remaining distance / duration
static char s_summary[40];

static bool icon_bit(int x, int y) {
  return s_state->icon[y * (ICON_SIZE / 8) + x / 8] & (0x80 >> (x % 8));
}

// The watch's own locale, not the phone's: distance/maneuver/eta/duration come from Yandex Maps
// as-is and are not translated here.
static bool is_russian_locale(void) {
  const char *locale = i18n_get_system_locale();
  return locale && strncmp(locale, "ru", 2) == 0;
}

static void icon_update_proc(Layer *layer, GContext *ctx) {
  if (!s_state->has_icon) return;
  graphics_context_set_fill_color(ctx, GColorWhite);
  for (int y = 0; y < ICON_SIZE; y++) {
    int x = 0;
    while (x < ICON_SIZE) {
      if (!icon_bit(x, y)) {
        x++;
        continue;
      }
      int start = x;
      while (x < ICON_SIZE && icon_bit(x, y)) x++;
      graphics_fill_rect(ctx, GRect(start, y, x - start, 1), 0, GCornerNone);
    }
  }
}

static void divider_update_proc(Layer *layer, GContext *ctx) {
  graphics_context_set_stroke_color(ctx, GColorDarkGray);
  graphics_draw_line(ctx, GPoint(0, 0), GPoint(layer_get_bounds(layer).size.w, 0));
}

static TextLayer *make_text(Layer *root, GRect frame, const char *font_key, GTextAlignment align) {
  TextLayer *layer = text_layer_create(frame);
  text_layer_set_background_color(layer, GColorClear);
  text_layer_set_text_color(layer, GColorWhite);
  text_layer_set_font(layer, fonts_get_system_font(font_key));
  text_layer_set_text_alignment(layer, align);
  layer_add_child(root, text_layer_get_layer(layer));
  return layer;
}

static void window_load(Window *window) {
  Layer *root = window_get_root_layer(window);
  int w = layer_get_bounds(root).size.w;
  int inner = w - 2 * MARGIN;

  s_message_layer = make_text(root, GRect(MARGIN, 70, inner, 90), FONT_KEY_GOTHIC_28_BOLD, GTextAlignmentCenter);

  s_icon_layer = layer_create(GRect((w - ICON_SIZE) / 2, 4, ICON_SIZE, ICON_SIZE));
  layer_set_update_proc(s_icon_layer, icon_update_proc);
  layer_add_child(root, s_icon_layer);

  s_distance_layer = make_text(root, GRect(MARGIN, 70, inner, 34), FONT_KEY_GOTHIC_28_BOLD, GTextAlignmentCenter);
  s_maneuver_layer = make_text(root, GRect(MARGIN, 106, inner, 58), FONT_KEY_GOTHIC_24_BOLD, GTextAlignmentCenter);

  s_divider_layer = layer_create(GRect(MARGIN, 170, inner, 1));
  layer_set_update_proc(s_divider_layer, divider_update_proc);
  layer_add_child(root, s_divider_layer);

  s_eta_layer = make_text(root, GRect(MARGIN, 174, inner, 28), FONT_KEY_GOTHIC_24_BOLD, GTextAlignmentCenter);
  s_summary_layer = make_text(root, GRect(MARGIN, 202, inner, 22), FONT_KEY_GOTHIC_18, GTextAlignmentCenter);

  nav_window_refresh();
}

static void window_unload(Window *window) {
  text_layer_destroy(s_message_layer);
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
  window_set_background_color(s_window, GColorBlack);
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
    text_layer_set_text(s_message_layer, s_state->finished
        ? (ru ? "Навигация завершена" : "Navigation finished")
        : (ru ? "Ожидание навигации" : "Waiting for navigation"));
    return;
  }

  if (s_state->remaining[0] && s_state->duration[0]) {
    snprintf(s_summary, sizeof(s_summary), "%s / %s", s_state->remaining, s_state->duration);
  } else {
    snprintf(s_summary, sizeof(s_summary), "%s%s", s_state->remaining, s_state->duration);
  }
  text_layer_set_text(s_distance_layer, s_state->distance);
  text_layer_set_text(s_maneuver_layer, s_state->maneuver);
  text_layer_set_text(s_eta_layer, s_state->eta);
  text_layer_set_text(s_summary_layer, s_summary);
  layer_mark_dirty(s_icon_layer);
}

void nav_window_destroy(void) {
  if (s_window) window_destroy(s_window);
  s_window = NULL;
}
