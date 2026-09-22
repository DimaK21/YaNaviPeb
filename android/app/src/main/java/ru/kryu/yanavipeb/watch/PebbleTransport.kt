package ru.kryu.yanavipeb.watch

import android.content.Context
import android.util.Log
import io.rebble.pebblekit2.client.DefaultPebbleSender
import io.rebble.pebblekit2.common.model.PebbleDictionary
import io.rebble.pebblekit2.common.model.TransmissionResult
import io.rebble.pebblekit2.common.model.WatchIdentifier
import kotlinx.coroutines.flow.Flow

class PebbleTransport(context: Context) : WatchTransport {
    private val sender = DefaultPebbleSender(context)
    private val activeAppObserver = PebbleActiveAppObserver(context, Protocol.PEBBLE_APP_PACKAGE)

    override suspend fun send(data: PebbleDictionary): Boolean =
        report("send", sender.sendDataToPebble(Protocol.WATCHAPP_UUID, data, null))

    override suspend fun startApp(): Boolean =
        report("start", sender.startAppOnTheWatch(Protocol.WATCHAPP_UUID, null))

    override suspend fun stopApp(): Boolean =
        report("stop", sender.stopAppOnTheWatch(Protocol.WATCHAPP_UUID, null))

    override fun watchappOpen(): Flow<Boolean> = activeAppObserver.isActive(Protocol.WATCHAPP_UUID)

    private fun report(operation: String, results: Map<WatchIdentifier, TransmissionResult>?): Boolean {
        if (results == null) {
            Log.w(TAG, "$operation: Pebble app is unreachable")
            return false
        }
        if (results.isEmpty()) {
            Log.w(TAG, "$operation: no connected watch")
            return false
        }
        for ((watch, result) in results) {
            if (result != TransmissionResult.Success) {
                Log.w(TAG, "$operation -> $watch: $result")
            } else {
                Log.i(TAG, "$operation -> $watch: success")
            }
        }
        return results.values.all { it == TransmissionResult.Success }
    }

    private companion object {
        const val TAG = "PebbleTransport"
    }
}
