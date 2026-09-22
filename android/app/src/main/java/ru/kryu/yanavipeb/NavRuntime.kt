package ru.kryu.yanavipeb

import android.content.Context
import android.os.SystemClock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import ru.kryu.yanavipeb.watch.NavSyncer
import ru.kryu.yanavipeb.watch.PebbleTransport

/** Process-wide [NavSyncer] shared by the notification listener, the Pebble service and the UI. */
object NavRuntime {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Volatile
    private var instance: NavSyncer? = null

    fun syncer(context: Context): NavSyncer =
        instance ?: synchronized(this) {
            instance ?: NavSyncer(
                transport = PebbleTransport(context.applicationContext),
                scope = scope,
                now = SystemClock::elapsedRealtime,
            ).also { instance = it }
        }
}
