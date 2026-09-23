package ru.kryu.yanavipeb.demo

import android.content.Context
import kotlinx.coroutines.delay
import ru.kryu.yanavipeb.R
import ru.kryu.yanavipeb.nav.NavState
import ru.kryu.yanavipeb.watch.NavSyncer

/** A short made-up route with the same kinds of strings Yandex Maps produces. */
object DemoScript {
    /**
     * Which icon each demo step shows, in order. Deferred (not `ByteArray` directly) so a plain
     * JVM test can check its length without calling [DemoIcons], which draws through
     * `android.graphics` and needs a real Android runtime.
     */
    internal val DEMO_ICON_SEQUENCE: List<() -> ByteArray> = listOf(
        { DemoIcons.right() }, { DemoIcons.right() }, { DemoIcons.right() }, { DemoIcons.right() },
        { DemoIcons.left() }, { DemoIcons.left() }, { DemoIcons.left() }, { DemoIcons.right() },
    )

    fun steps(context: Context): List<NavState> {
        val resources = context.resources
        return buildSteps(
            maneuvers = resources.getStringArray(R.array.demo_maneuvers),
            distances = resources.getStringArray(R.array.demo_distances),
            remaining = resources.getStringArray(R.array.demo_remaining),
            eta = resources.getStringArray(R.array.demo_eta),
            durations = resources.getStringArray(R.array.demo_durations),
            icons = DEMO_ICON_SEQUENCE.map { it() },
        )
    }

    /** Zips the parallel arrays into steps, in order. All five arrays and [icons] must be the same length. */
    internal fun buildSteps(
        maneuvers: Array<String>,
        distances: Array<String>,
        remaining: Array<String>,
        eta: Array<String>,
        durations: Array<String>,
        icons: List<ByteArray>,
    ): List<NavState> {
        val size = maneuvers.size
        require(
            distances.size == size && remaining.size == size && eta.size == size &&
                durations.size == size && icons.size == size,
        ) {
            "Demo step arrays must have the same length, got maneuvers=$size distances=${distances.size} " +
                "remaining=${remaining.size} eta=${eta.size} durations=${durations.size} icons=${icons.size}"
        }
        return (0 until size).map { i ->
            NavState(
                navigating = true,
                distance = distances[i],
                maneuver = maneuvers[i],
                remaining = remaining[i],
                eta = eta[i],
                duration = durations[i],
                icon = icons[i],
            )
        }
    }
}

object DemoPlayer {
    /** Feeds [steps] to [syncer] one by one and always finishes with "navigation ended". */
    suspend fun play(syncer: NavSyncer, steps: List<NavState>, stepDelayMs: Long = 2_000) {
        try {
            for (step in steps) {
                syncer.onNavState(step)
                delay(stepDelayMs)
            }
        } finally {
            syncer.onNavState(NavState.IDLE)
        }
    }
}
