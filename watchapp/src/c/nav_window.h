#pragma once
#include "nav_state.h"

// The window reads `state` on every refresh, so the caller keeps the pointer valid.
void nav_window_push(NavState *state);
void nav_window_refresh(void);
void nav_window_destroy(void);
