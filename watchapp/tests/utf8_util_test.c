// Standalone native test for utf8_safe_truncate_len(). Has no dependency on the Pebble SDK,
// so it is compiled and run directly with the host C compiler, not through `pebble build`:
//
//   cc -std=c99 -Wall -Wextra tests/utf8_util_test.c -o /tmp/utf8_util_test && /tmp/utf8_util_test
//
// It checks the same truncation semantics as Kotlin's String.truncateUtf8() in
// android/app/src/main/java/ru/kryu/yanavipeb/watch/NavMessage.kt: cut at a byte limit without
// ever splitting a multi-byte UTF-8 codepoint.

#include "../src/c/utf8_util.h"

#include <stdio.h>
#include <stdlib.h>
#include <string.h>

static int failures = 0;

static void expect_truncate(const char *src, size_t max_bytes, const char *expected) {
  size_t len = utf8_safe_truncate_len(src, max_bytes);
  char buf[256];
  memcpy(buf, src, len);
  buf[len] = '\0';
  if (strcmp(buf, expected) != 0) {
    fprintf(stderr, "FAIL: utf8_safe_truncate_len(\"%s\", %zu) = \"%s\", expected \"%s\"\n",
            src, max_bytes, buf, expected);
    failures++;
  }
}

int main(void) {
  // Plain ASCII (1-byte codepoints) is never split, so plain byte truncation is correct here.
  expect_truncate("hello", 10, "hello");
  expect_truncate("hello", 5, "hello");
  expect_truncate("hello world", 5, "hello");

  // 2-byte Cyrillic codepoint "м" (0xD0 0xBC): must not be split.
  expect_truncate("\xD0\xBC", 1, "");
  expect_truncate("\xD0\xBC", 2, "\xD0\xBC");
  expect_truncate("a\xD0\xBC", 2, "a");
  expect_truncate("a\xD0\xBC", 3, "a\xD0\xBC");

  // 3-byte codepoint "你" (0xE4 0xBD 0xA0): must not be split.
  expect_truncate("\xE4\xBD\xA0", 1, "");
  expect_truncate("\xE4\xBD\xA0", 2, "");
  expect_truncate("\xE4\xBD\xA0", 3, "\xE4\xBD\xA0");

  // Edge cases.
  expect_truncate("", 5, "");
  expect_truncate("\xD0\xBC", 0, "");

  if (failures == 0) {
    printf("all utf8_safe_truncate_len tests passed\n");
    return 0;
  }
  fprintf(stderr, "%d utf8_safe_truncate_len test(s) failed\n", failures);
  return 1;
}
