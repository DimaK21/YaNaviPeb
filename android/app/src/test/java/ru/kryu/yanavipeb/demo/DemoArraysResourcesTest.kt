package ru.kryu.yanavipeb.demo

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

/** Unit tests run with the module directory (android/app) as working directory. */
class DemoArraysResourcesTest {
    @Test
    fun `English and Russian demo arrays declare the same arrays with the same lengths`() {
        val english = arrayLengths("src/main/res/values/demo_arrays.xml")
        val russian = arrayLengths("src/main/res/values-ru/demo_arrays.xml")

        assertEquals(english.keys, russian.keys)
        assertEquals(english, russian)
    }

    @Test
    fun `every demo array is as long as the hardcoded icon sequence`() {
        val english = arrayLengths("src/main/res/values/demo_arrays.xml")

        for ((name, length) in english) {
            assertEquals("$name length must match DemoScript.DEMO_ICON_SEQUENCE", DemoScript.DEMO_ICON_SEQUENCE.size, length)
        }
    }

    private fun arrayLengths(path: String): Map<String, Int> {
        val xml = File(path).readText()
        return Regex("<string-array name=\"([a-z_]+)\">(.*?)</string-array>", RegexOption.DOT_MATCHES_ALL)
            .findAll(xml)
            .associate { it.groupValues[1] to Regex("<item>").findAll(it.groupValues[2]).count() }
    }
}
