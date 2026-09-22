package ru.kryu.yanavipeb.watch

import android.content.ContentResolver
import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import io.rebble.pebblekit2.PebbleKitProviderContract
import io.rebble.pebblekit2.common.model.WatchIdentifier
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
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
class PebbleActiveAppObserver(context: Context, private val pebblePackage: String) {
    private val resolver = context.applicationContext.contentResolver

    /** Emits false whenever no watch is connected or a different app is active. */
    fun isActive(expectedApp: UUID): Flow<Boolean> =
        resolver.observe(PebbleKitProviderContract.ConnectedWatch.getContentUri(pebblePackage))
            .map { queryFirstConnectedWatchId() }
            .distinctUntilChanged()
            .flatMapLatest { watchId ->
                if (watchId == null) {
                    flowOf(false)
                } else {
                    val uri = PebbleKitProviderContract.ActiveApp.getContentUri(pebblePackage, watchId)
                    resolver.observe(uri).map { queryActiveAppId(watchId) == expectedApp }
                }
            }

    private fun queryFirstConnectedWatchId(): WatchIdentifier? {
        val uri = PebbleKitProviderContract.ConnectedWatch.getContentUri(pebblePackage)
        val column = PebbleKitProviderContract.ConnectedWatch.ID
        return resolver.query(uri, arrayOf(column), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) WatchIdentifier(cursor.getString(0)) else null
        }
    }

    private fun queryActiveAppId(watchId: WatchIdentifier): UUID? {
        val uri = PebbleKitProviderContract.ActiveApp.getContentUri(pebblePackage, watchId)
        val column = PebbleKitProviderContract.ActiveApp.ID
        return resolver.query(uri, arrayOf(column), null, null, null)?.use { cursor ->
            if (!cursor.moveToFirst()) return@use null
            runCatching { UUID.fromString(cursor.getString(0)) }.getOrNull()
        }
    }

    /** Emits once immediately (the current state) and again every time [uri] changes. */
    private fun ContentResolver.observe(uri: Uri): Flow<Unit> = callbackFlow {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                trySend(Unit)
            }
        }
        registerContentObserver(uri, false, observer)
        trySend(Unit)
        awaitClose { unregisterContentObserver(observer) }
    }
}
