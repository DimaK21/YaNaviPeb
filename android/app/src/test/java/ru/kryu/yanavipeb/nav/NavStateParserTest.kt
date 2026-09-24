package ru.kryu.yanavipeb.nav

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.kryu.yanavipeb.watch.Protocol

/** Values below come from the notification dump recorded in spike-notification-dump. */
class NavStateParserTest {
    @Test
    fun notificationWithoutRouteIsIdle() {
        val state = NavStateParser.parse(snapshot(title = "Навигатор запущен", customView = false))
        assertEquals(NavState.IDLE, state)
    }

    @Test
    fun rightTurn() {
        val state = NavStateParser.parse(
            snapshot(
                title = "35 м",
                text = "Поверните направо",
                views = mapOf(
                    "titleView" to "35 м",
                    "descriptionView" to "Поверните направо",
                    "remainingDistanceView" to "1,61 км",
                    "timeOfArrivalView" to "23:57",
                    "remainingTimeView" to "15 мин",
                ),
            ),
        )
        assertEquals(
            NavState(true, "35 м", "Поверните направо", "1,61 км", "23:57", "15 мин", null),
            state,
        )
    }

    @Test
    fun longTripWithHoursAndCrossingMidnight() {
        val state = NavStateParser.parse(
            snapshot(
                title = "30 м",
                text = "Поверните налево",
                views = mapOf(
                    "titleView" to "30 м",
                    "descriptionView" to "Поверните налево",
                    "remainingDistanceView" to "6,2 км",
                    "timeOfArrivalView" to "01:09",
                    "remainingTimeView" to "1 ч 17 мин",
                ),
            ),
        )
        assertEquals("6,2 км", state.remaining)
        assertEquals("01:09", state.eta)
        assertEquals("1 ч 17 мин", state.duration)
    }

    @Test
    fun roundabout() {
        val state = NavStateParser.parse(
            snapshot(
                title = "150 м",
                text = "Кольцевое движение",
                views = mapOf("titleView" to "150 м", "descriptionView" to "Кольцевое движение"),
            ),
        )
        assertEquals("150 м", state.distance)
        assertEquals("Кольцевое движение", state.maneuver)
    }

    @Test
    fun missingViewsFallBackToNotificationTitleAndText() {
        val state = NavStateParser.parse(snapshot(title = "150 м", text = "Кольцевое движение"))
        assertTrue(state.navigating)
        assertEquals("150 м", state.distance)
        assertEquals("Кольцевое движение", state.maneuver)
        assertEquals("", state.remaining)
        assertEquals("", state.eta)
        assertEquals("", state.duration)
    }

    @Test
    fun valuesAreTrimmed() {
        val state = NavStateParser.parse(snapshot(views = mapOf("titleView" to "  35 м ")))
        assertEquals("35 м", state.distance)
    }

    @Test
    fun iconPixelsAreEncoded() {
        val opaque = IntArray(Protocol.ICON_SIZE * Protocol.ICON_SIZE) { 0xFFFFFFFF.toInt() }
        val state = NavStateParser.parse(snapshot(icon = opaque))
        assertEquals(Protocol.ICON_BYTES, state.icon!!.size)
        assertTrue(state.icon!!.all { it == 0xFF.toByte() })
    }

    @Test
    fun noIconPixelsMeansNoIcon() {
        assertNull(NavStateParser.parse(snapshot()).icon)
    }

    private fun snapshot(
        title: String? = null,
        text: String? = null,
        customView: Boolean = true,
        views: Map<String, String> = emptyMap(),
        icon: IntArray? = null,
    ) = NotificationSnapshot(title, text, customView, views, icon)
}
