package ru.kryu.yanavipeb.watch

import io.rebble.pebblekit2.common.model.PebbleDictionary
import io.rebble.pebblekit2.common.model.PebbleDictionaryItem
import ru.kryu.yanavipeb.nav.NavState

object NavMessage {
    /**
     * The keys to send so that a watch currently showing [previous] (null = unknown) shows [current].
     * The watchapp clears everything when it receives STATE, so after a STATE change every
     * non-empty field is sent again. An icon that disappears is not cleared on the watch.
     */
    fun diff(previous: NavState?, current: NavState): PebbleDictionary {
        val out = LinkedHashMap<UInt, PebbleDictionaryItem>()
        val stateChanged = previous == null || previous.navigating != current.navigating
        if (stateChanged) {
            out[Protocol.KEY_STATE] = PebbleDictionaryItem.UInt8(if (current.navigating) 1 else 0)
        }
        val base = previous.takeUnless { stateChanged } ?: NavState.IDLE

        putText(out, Protocol.KEY_DISTANCE, Protocol.MAX_DISTANCE_BYTES, base.distance, current.distance)
        putText(out, Protocol.KEY_MANEUVER, Protocol.MAX_MANEUVER_BYTES, base.maneuver, current.maneuver)
        putText(out, Protocol.KEY_REMAINING, Protocol.MAX_REMAINING_BYTES, base.remaining, current.remaining)
        putText(out, Protocol.KEY_ETA, Protocol.MAX_ETA_BYTES, base.eta, current.eta)
        putText(out, Protocol.KEY_DURATION, Protocol.MAX_DURATION_BYTES, base.duration, current.duration)

        val icon = current.icon
        if (icon != null && !icon.contentEquals(base.icon)) {
            out[Protocol.KEY_ICON] = PebbleDictionaryItem.Bytes(icon)
        }
        return out
    }

    private fun putText(
        out: MutableMap<UInt, PebbleDictionaryItem>,
        key: UInt,
        maxBytes: Int,
        old: String,
        new: String,
    ) {
        val newText = new.truncateUtf8(maxBytes)
        if (newText != old.truncateUtf8(maxBytes)) out[key] = PebbleDictionaryItem.Text(newText)
    }
}

/** Cuts the string to at most [maxBytes] UTF-8 bytes without splitting a character. */
internal fun String.truncateUtf8(maxBytes: Int): String {
    var bytes = 0
    var end = 0
    while (end < length) {
        val codePoint = codePointAt(end)
        val size = when {
            codePoint < 0x80 -> 1
            codePoint < 0x800 -> 2
            codePoint < 0x10000 -> 3
            else -> 4
        }
        if (bytes + size > maxBytes) break
        bytes += size
        end += Character.charCount(codePoint)
    }
    return substring(0, end)
}
