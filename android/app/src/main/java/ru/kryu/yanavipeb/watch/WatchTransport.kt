package ru.kryu.yanavipeb.watch

import io.rebble.pebblekit2.common.model.PebbleDictionary
import kotlinx.coroutines.flow.Flow

/** The part of PebbleKit that [NavSyncer] needs; each send-like call returns true when the watch accepted it. */
interface WatchTransport {
    suspend fun send(data: PebbleDictionary): Boolean
    suspend fun startApp(): Boolean
    suspend fun stopApp(): Boolean

    /**
     * Whether our watchapp is currently the one open on the connected watch. PebbleKit's bound
     * listener service (onAppOpened/onAppClosed) never fired against this Pebble app build, so
     * this is derived by observing the Pebble app's own ContentProvider instead of waiting for a
     * push callback.
     */
    fun watchappOpen(): Flow<Boolean>
}
