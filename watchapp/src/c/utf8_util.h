#pragma once
#include <stddef.h>

// Returns how many leading bytes of the NUL-terminated UTF-8 string `src` fit within
// `max_bytes` without splitting a multi-byte codepoint in half. Mirrors the semantics of
// Kotlin's String.truncateUtf8() (android/.../watch/NavMessage.kt): walk codepoint by
// codepoint and stop before a codepoint that would push the total past `max_bytes`, rather
// than cutting at a raw byte offset.
static inline size_t utf8_safe_truncate_len(const char *src, size_t max_bytes) {
  size_t i = 0;
  while (src[i] != '\0') {
    unsigned char lead = (unsigned char)src[i];
    size_t seq_len;
    if ((lead & 0x80) == 0x00) {
      seq_len = 1;
    } else if ((lead & 0xE0) == 0xC0) {
      seq_len = 2;
    } else if ((lead & 0xF0) == 0xE0) {
      seq_len = 3;
    } else if ((lead & 0xF8) == 0xF0) {
      seq_len = 4;
    } else {
      // Not a valid UTF-8 lead byte; consume it alone so we still make progress.
      seq_len = 1;
    }
    if (i + seq_len > max_bytes) break;
    i += seq_len;
  }
  return i;
}
