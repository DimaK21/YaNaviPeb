package ru.kryu.yanavipeb.nav

data class NavState(
    val navigating: Boolean,
    val distance: String = "",
    val maneuver: String = "",
    val remaining: String = "",
    val eta: String = "",
    val duration: String = "",
    val icon: ByteArray? = null,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is NavState) return false
        return navigating == other.navigating &&
            distance == other.distance &&
            maneuver == other.maneuver &&
            remaining == other.remaining &&
            eta == other.eta &&
            duration == other.duration &&
            icon.contentEquals(other.icon)
    }

    override fun hashCode(): Int {
        var result = navigating.hashCode()
        result = 31 * result + distance.hashCode()
        result = 31 * result + maneuver.hashCode()
        result = 31 * result + remaining.hashCode()
        result = 31 * result + eta.hashCode()
        result = 31 * result + duration.hashCode()
        result = 31 * result + icon.contentHashCode()
        return result
    }

    override fun toString(): String =
        "NavState(navigating=$navigating, distance='$distance', maneuver='$maneuver', " +
            "remaining='$remaining', eta='$eta', duration='$duration', icon=${icon?.size ?: 0}B)"

    companion object {
        val IDLE = NavState(navigating = false)
    }
}
