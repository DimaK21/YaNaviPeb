package ru.kryu.yanavipeb.watch

import java.util.UUID

/** AppMessage contract with the watchapp. Key numbers follow `messageKeys` in watchapp/package.json. */
object Protocol {
    val WATCHAPP_UUID: UUID = UUID.fromString("2fa3743c-bafc-4d0c-b784-188e1bee90b8")

    /** Package of the official Pebble companion app (coredevices.coreapp). */
    const val PEBBLE_APP_PACKAGE = "coredevices.coreapp"

    const val KEY_STATE = 10000u
    const val KEY_DISTANCE = 10001u
    const val KEY_MANEUVER = 10002u
    const val KEY_REMAINING = 10003u
    const val KEY_ETA = 10004u
    const val KEY_DURATION = 10005u
    const val KEY_ICON = 10006u

    /** Same order as `pebble.messageKeys` in watchapp/package.json. */
    val KEYS: Map<String, UInt> = mapOf(
        "STATE" to KEY_STATE,
        "DISTANCE" to KEY_DISTANCE,
        "MANEUVER" to KEY_MANEUVER,
        "REMAINING" to KEY_REMAINING,
        "ETA" to KEY_ETA,
        "DURATION" to KEY_DURATION,
        "ICON" to KEY_ICON,
    )

    /** Maximum UTF-8 length of each string, without the terminating NUL. */
    const val MAX_DISTANCE_BYTES = 15
    const val MAX_MANEUVER_BYTES = 48
    const val MAX_REMAINING_BYTES = 15
    const val MAX_ETA_BYTES = 7
    const val MAX_DURATION_BYTES = 19

    const val ICON_SIZE = 64
    const val ICON_BYTES = ICON_SIZE * ICON_SIZE / 8
}
