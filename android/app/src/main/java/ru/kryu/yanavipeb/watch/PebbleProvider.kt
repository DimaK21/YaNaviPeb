package ru.kryu.yanavipeb.watch

import io.rebble.pebblekit2.common.model.WatchIdentifier
import kotlinx.coroutines.flow.Flow
import java.util.UUID

/**
 * The part of the Pebble app's ContentProvider (see PebbleKitProviderContract) that
 * [PebbleActiveAppObserver] reads. While the provider is unavailable (the Pebble app is not
 * installed, or refuses access) the change flows fail and the queries may fail, both with
 * [SecurityException].
 */
interface PebbleProvider {
    /** Emits once immediately (the current state) and again every time the connected watches change. */
    fun connectedWatchChanges(): Flow<Unit>

    /** Emits once immediately (the current state) and again every time the app active on [watch] changes. */
    fun activeAppChanges(watch: WatchIdentifier): Flow<Unit>

    fun firstConnectedWatch(): WatchIdentifier?

    fun activeApp(watch: WatchIdentifier): UUID?
}
