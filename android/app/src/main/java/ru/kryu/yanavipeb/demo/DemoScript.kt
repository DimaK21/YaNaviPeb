package ru.kryu.yanavipeb.demo

import kotlinx.coroutines.delay
import ru.kryu.yanavipeb.nav.NavState
import ru.kryu.yanavipeb.watch.NavSyncer

/** A short made-up route with the same kinds of strings Yandex Maps produces. */
object DemoScript {
    fun steps(): List<NavState> {
        val right = DemoIcons.right()
        val left = DemoIcons.left()
        fun step(distance: String, maneuver: String, remaining: String, eta: String, duration: String, icon: ByteArray) =
            NavState(true, distance, maneuver, remaining, eta, duration, icon)
        return listOf(
            step("300 м", "Поверните направо", "5 км", "23:53", "11 мин", right),
            step("200 м", "Поверните направо", "4,9 км", "23:53", "11 мин", right),
            step("100 м", "Поверните направо", "4,8 км", "23:53", "11 мин", right),
            step("30 м", "Поверните направо", "4,7 км", "23:52", "10 мин", right),
            step("1,2 км", "Поверните налево", "3,5 км", "23:52", "9 мин", left),
            step("400 м", "Поверните налево", "2,7 км", "23:51", "8 мин", left),
            step("50 м", "Поверните налево", "2,3 км", "23:51", "7 мин", left),
            step("800 м", "Поверните направо", "6,2 км", "01:09", "1 ч 17 мин", right),
        )
    }
}

object DemoPlayer {
    /** Feeds [steps] to [syncer] one by one and always finishes with "navigation ended". */
    suspend fun play(syncer: NavSyncer, steps: List<NavState> = DemoScript.steps(), stepDelayMs: Long = 2_000) {
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
