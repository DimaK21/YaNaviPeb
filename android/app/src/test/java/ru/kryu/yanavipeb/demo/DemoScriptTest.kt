package ru.kryu.yanavipeb.demo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class DemoScriptTest {
    @Test
    fun `builds one step per array entry in order`() {
        val icons = listOf(byteArrayOf(1), byteArrayOf(2))

        val steps = DemoScript.buildSteps(
            maneuvers = arrayOf("Turn right", "Turn left"),
            distances = arrayOf("300 m", "1.2 km"),
            remaining = arrayOf("5 km", "3.5 km"),
            eta = arrayOf("23:53", "23:52"),
            durations = arrayOf("11 min", "9 min"),
            icons = icons,
        )

        assertEquals(2, steps.size)
        assertEquals(true, steps[0].navigating)
        assertEquals("300 m", steps[0].distance)
        assertEquals("Turn right", steps[0].maneuver)
        assertEquals("5 km", steps[0].remaining)
        assertEquals("23:53", steps[0].eta)
        assertEquals("11 min", steps[0].duration)
        assertEquals("Turn left", steps[1].maneuver)
    }

    @Test
    fun `rejects mismatched array lengths`() {
        assertThrows(IllegalArgumentException::class.java) {
            DemoScript.buildSteps(
                maneuvers = arrayOf("Turn right", "Turn left"),
                distances = arrayOf("300 m"),
                remaining = arrayOf("5 km", "3.5 km"),
                eta = arrayOf("23:53", "23:52"),
                durations = arrayOf("11 min", "9 min"),
                icons = listOf(byteArrayOf(1), byteArrayOf(2)),
            )
        }
    }
}
