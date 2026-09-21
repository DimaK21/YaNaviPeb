#include <pebble.h>

#include "nav_state.h"
#include "nav_window.h"

// Largest incoming message: the 512-byte icon plus the strings and dictionary overhead.
#define INBOX_SIZE 1024
#define OUTBOX_SIZE 64

static NavState s_state;

static void inbox_received(DictionaryIterator *iter, void *context) {
  if (nav_state_apply(&s_state, iter)) nav_window_refresh();
}

static void inbox_dropped(AppMessageResult reason, void *context) {
  APP_LOG(APP_LOG_LEVEL_WARNING, "inbox dropped: %d", (int)reason);
}

int main(void) {
  app_message_register_inbox_received(inbox_received);
  app_message_register_inbox_dropped(inbox_dropped);
  app_message_open(INBOX_SIZE, OUTBOX_SIZE);
  nav_window_push(&s_state);
  app_event_loop();
  nav_window_destroy();
}
