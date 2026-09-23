package ru.kryu.yanavipeb.watch

import android.content.Context
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.retryWhen
import java.util.UUID

/**
 * Tells whether a given watchapp is the one currently open on the (single) connected watch, by
 * observing the Pebble app's own ContentProvider (see PebbleKitProviderContract) with a
 * ContentObserver. We use this instead of PebbleKit's bound listener service
 * (onAppOpened/onAppClosed): against this Pebble app build that service is never bound at all
 * (confirmed with `adb shell dumpsys activity services`), while this provider reflects the active
 * app correctly and promptly.
 */
@OptIn(ExperimentalCoroutinesApi::class) // flatMapLatest
class PebbleActiveAppObserver internal constructor(
    private val provider: PebbleProvider,
    private val retryIntervalMs: Long = 10_000,
) {
    constructor(context: Context, pebblePackage: String) :
        this(ContentResolverPebbleProvider(context, pebblePackage))

    /**
     * Emits false whenever no watch is connected or a different app is active. While the Pebble
     * app's provider is unavailable (the app is not installed, or refuses access) it emits nothing
     * and retries every [retryIntervalMs], so it starts reporting once the Pebble app shows up.
     */
    fun isActive(expectedApp: UUID): Flow<Boolean> =
        provider.connectedWatchChanges()
            .map { queryOrNull { provider.firstConnectedWatch() } }
            .distinctUntilChanged()
            .flatMapLatest { watch ->
                if (watch == null) {
                    flowOf(false)
                } else {
                    provider.activeAppChanges(watch).map { queryOrNull { provider.activeApp(watch) } == expectedApp }
                }
            }
            .retryWhen { cause, _ ->
                val unavailable = cause is SecurityException
                if (unavailable) delay(retryIntervalMs)
                unavailable
            }

    private fun <T> queryOrNull(query: () -> T?): T? = try {
        query()
    } catch (e: SecurityException) {
        null
    }
}
