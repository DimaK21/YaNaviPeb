package ru.kryu.yanavipeb.nav

object NavStateParser {
    const val VIEW_DISTANCE = "titleView"
    const val VIEW_MANEUVER = "descriptionView"
    const val VIEW_REMAINING = "remainingDistanceView"
    const val VIEW_ETA = "timeOfArrivalView"
    const val VIEW_DURATION = "remainingTimeView"

    fun parse(snapshot: NotificationSnapshot): NavState {
        if (!snapshot.hasCustomView) return NavState.IDLE
        val views = snapshot.viewTexts
        return NavState(
            navigating = true,
            distance = firstNotEmpty(views[VIEW_DISTANCE], snapshot.title),
            maneuver = firstNotEmpty(views[VIEW_MANEUVER], snapshot.text),
            remaining = clean(views[VIEW_REMAINING]),
            eta = clean(views[VIEW_ETA]),
            duration = clean(views[VIEW_DURATION]),
            icon = snapshot.iconArgb?.let(IconEncoder::encode),
        )
    }

    private fun clean(value: String?): String = value?.trim().orEmpty()

    private fun firstNotEmpty(primary: String?, fallback: String?): String =
        clean(primary).ifEmpty { clean(fallback) }
}
