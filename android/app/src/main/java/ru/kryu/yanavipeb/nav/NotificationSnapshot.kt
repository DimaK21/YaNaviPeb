package ru.kryu.yanavipeb.nav

/** What we read from one Yandex Maps / Yandex Navigator notification, free of Android types so it can be parsed in tests. */
class NotificationSnapshot(
    val title: String?,
    val text: String?,
    /** `android.contains.customView` extra: true only while a route is being followed. */
    val hasCustomView: Boolean,
    /** Text of each TextView in the notification layout, keyed by its resource entry name. */
    val viewTexts: Map<String, String>,
    /** Maneuver icon drawn as ICON_SIZE x ICON_SIZE ARGB pixels, or null when absent. */
    val iconArgb: IntArray?,
)
