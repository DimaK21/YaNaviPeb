#include "nav_state.h"

static bool copy_string(char *dst, size_t dst_size, const Tuple *tuple) {
  if (!tuple || tuple->type != TUPLE_CSTRING) return false;
  strncpy(dst, tuple->value->cstring, dst_size - 1);
  dst[dst_size - 1] = '\0';
  return true;
}

bool nav_state_apply(NavState *state, DictionaryIterator *iter) {
  bool changed = false;

  Tuple *state_tuple = dict_find(iter, MESSAGE_KEY_STATE);
  if (state_tuple) {
    bool was_navigating = state->navigating;
    bool navigating = state_tuple->value->uint8 != 0;
    memset(state, 0, sizeof(*state));
    state->navigating = navigating;
    state->finished = was_navigating && !navigating;
    changed = true;
  }

  changed |= copy_string(state->distance, sizeof(state->distance), dict_find(iter, MESSAGE_KEY_DISTANCE));
  changed |= copy_string(state->maneuver, sizeof(state->maneuver), dict_find(iter, MESSAGE_KEY_MANEUVER));
  changed |= copy_string(state->remaining, sizeof(state->remaining), dict_find(iter, MESSAGE_KEY_REMAINING));
  changed |= copy_string(state->eta, sizeof(state->eta), dict_find(iter, MESSAGE_KEY_ETA));
  changed |= copy_string(state->duration, sizeof(state->duration), dict_find(iter, MESSAGE_KEY_DURATION));

  Tuple *icon = dict_find(iter, MESSAGE_KEY_ICON);
  if (icon && icon->type == TUPLE_BYTE_ARRAY && icon->length == ICON_BYTES) {
    memcpy(state->icon, icon->value->data, ICON_BYTES);
    state->has_icon = true;
    changed = true;
  }
  return changed;
}
