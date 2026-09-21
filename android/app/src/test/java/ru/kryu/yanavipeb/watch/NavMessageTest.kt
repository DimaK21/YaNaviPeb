package ru.kryu.yanavipeb.watch

import io.rebble.pebblekit2.common.model.PebbleDictionaryItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.kryu.yanavipeb.nav.NavState

class NavMessageTest {
    private val icon = ByteArray(Protocol.ICON_BYTES) { 0x0F }

    private fun nav(
        distance: String = "150 м",
        maneuver: String = "Поверните направо",
        icon: ByteArray? = this.icon,
    ) = NavState(true, distance, maneuver, "1,61 км", "23:57", "15 мин", icon)

    @Test
    fun firstMessageCarriesEveryField() {
        val message = NavMessage.diff(null, nav())
        assertEquals(
            setOf(
                Protocol.KEY_STATE, Protocol.KEY_DISTANCE, Protocol.KEY_MANEUVER, Protocol.KEY_REMAINING,
                Protocol.KEY_ETA, Protocol.KEY_DURATION, Protocol.KEY_ICON,
            ),
            message.keys,
        )
        assertEquals(PebbleDictionaryItem.UInt8(1), message[Protocol.KEY_STATE])
        assertEquals(PebbleDictionaryItem.Text("150 м"), message[Protocol.KEY_DISTANCE])
        assertEquals(PebbleDictionaryItem.Text("Поверните направо"), message[Protocol.KEY_MANEUVER])
        assertEquals(PebbleDictionaryItem.Bytes(icon), message[Protocol.KEY_ICON])
    }

    @Test
    fun unchangedStateProducesEmptyMessage() {
        assertTrue(NavMessage.diff(nav(), nav()).isEmpty())
    }

    @Test
    fun onlyChangedFieldIsSent() {
        val message = NavMessage.diff(nav(distance = "150 м"), nav(distance = "100 м"))
        assertEquals(setOf(Protocol.KEY_DISTANCE), message.keys)
        assertEquals(PebbleDictionaryItem.Text("100 м"), message[Protocol.KEY_DISTANCE])
    }

    @Test
    fun changedIconIsSentAlone() {
        val other = ByteArray(Protocol.ICON_BYTES) { 0x33 }
        val message = NavMessage.diff(nav(), nav(icon = other))
        assertEquals(setOf(Protocol.KEY_ICON), message.keys)
    }

    @Test
    fun goingIdleSendsOnlyState() {
        val message = NavMessage.diff(nav(), NavState.IDLE)
        assertEquals(mapOf(Protocol.KEY_STATE to PebbleDictionaryItem.UInt8(0)), message)
    }

    @Test
    fun startingNavigationResendsAllFieldsBecauseWatchClearsOnState() {
        val message = NavMessage.diff(NavState.IDLE, nav())
        assertEquals(7, message.size)
    }

    @Test
    fun clearedFieldIsSentAsEmptyString() {
        val message = NavMessage.diff(nav(maneuver = "Поверните направо"), nav(maneuver = ""))
        assertEquals(mapOf(Protocol.KEY_MANEUVER to PebbleDictionaryItem.Text("")), message)
    }

    @Test
    fun longTextIsTruncatedAtCharacterBoundary() {
        val message = NavMessage.diff(null, nav(maneuver = "ab" + "я".repeat(30)))
        assertEquals(PebbleDictionaryItem.Text("ab" + "я".repeat(23)), message[Protocol.KEY_MANEUVER])
    }

    @Test
    fun truncateUtf8NeverSplitsCharacters() {
        assertEquals("пр", "привет".truncateUtf8(5))
        assertEquals("привет", "привет".truncateUtf8(12))
        assertEquals("", "привет".truncateUtf8(1))
    }
}
