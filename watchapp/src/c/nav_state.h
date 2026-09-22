#pragma once
#include <pebble.h>

#define ICON_SIZE 64
#define ICON_BYTES (ICON_SIZE * ICON_SIZE / 8)

// Buffer sizes are the maximum UTF-8 lengths from the protocol plus one byte for the NUL.
typedef struct {
  bool navigating;  // STATE == 1
  bool finished;    // navigation ended while the watchapp was open
  char distance[16];
  char maneuver[49];
  char remaining[16];
  char eta[8];
  char duration[20];
  bool has_icon;
  uint8_t icon[ICON_BYTES];  // ICON_SIZE rows of ICON_SIZE / 8 bytes, MSB first, 1 = white
} NavState;

// Applies every known key found in `iter`. A STATE key first clears everything, because the
// phone resends all non-empty fields together with STATE. Returns true if anything changed.
bool nav_state_apply(NavState *state, DictionaryIterator *iter);
