package ru.kryu.yanavipeb.watch

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

class ProtocolTest {
    @Test
    fun keysMatchWatchappPackageJson() {
        // The Pebble SDK numbers messageKeys from 10000 in array order.
        val expected = watchappMessageKeys().mapIndexed { index, name -> name to 10000u + index.toUInt() }.toMap()
        assertEquals(expected, Protocol.KEYS)
    }

    /** Unit tests run with the module directory (android/app) as working directory. */
    private fun watchappMessageKeys(): List<String> {
        val json = File("../../watchapp/package.json").readText()
        val array = Regex("\"messageKeys\"\\s*:\\s*\\[(.*?)]", RegexOption.DOT_MATCHES_ALL)
            .find(json)!!.groupValues[1]
        return Regex("\"([A-Z_]+)\"").findAll(array).map { it.groupValues[1] }.toList()
    }
}
