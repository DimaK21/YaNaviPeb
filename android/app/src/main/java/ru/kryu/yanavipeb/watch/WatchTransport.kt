package ru.kryu.yanavipeb.watch

import io.rebble.pebblekit2.common.model.PebbleDictionary

/** The part of PebbleKit that [NavSyncer] needs; each call returns true when the watch accepted it. */
interface WatchTransport {
    suspend fun send(data: PebbleDictionary): Boolean
    suspend fun startApp(): Boolean
    suspend fun stopApp(): Boolean
}
