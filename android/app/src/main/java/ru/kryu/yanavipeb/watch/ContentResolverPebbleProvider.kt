package ru.kryu.yanavipeb.watch

import android.content.ContentResolver
import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import io.rebble.pebblekit2.PebbleKitProviderContract
import io.rebble.pebblekit2.common.model.WatchIdentifier
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.util.UUID

/** Reads the Pebble app's PebbleKit ContentProvider through a [ContentResolver]. */
class ContentResolverPebbleProvider(context: Context, private val pebblePackage: String) : PebbleProvider {
    private val resolver = context.applicationContext.contentResolver

    override fun connectedWatchChanges(): Flow<Unit> =
        resolver.observe(PebbleKitProviderContract.ConnectedWatch.getContentUri(pebblePackage))

    override fun activeAppChanges(watch: WatchIdentifier): Flow<Unit> =
        resolver.observe(PebbleKitProviderContract.ActiveApp.getContentUri(pebblePackage, watch))

    override fun firstConnectedWatch(): WatchIdentifier? {
        val uri = PebbleKitProviderContract.ConnectedWatch.getContentUri(pebblePackage)
        val column = PebbleKitProviderContract.ConnectedWatch.ID
        return resolver.query(uri, arrayOf(column), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) WatchIdentifier(cursor.getString(0)) else null
        }
    }

    override fun activeApp(watch: WatchIdentifier): UUID? {
        val uri = PebbleKitProviderContract.ActiveApp.getContentUri(pebblePackage, watch)
        val column = PebbleKitProviderContract.ActiveApp.ID
        return resolver.query(uri, arrayOf(column), null, null, null)?.use { cursor ->
            if (!cursor.moveToFirst()) return@use null
            runCatching { UUID.fromString(cursor.getString(0)) }.getOrNull()
        }
    }

    /**
     * Emits once immediately (the current state) and again every time [uri] changes. Fails with
     * [SecurityException] when the provider behind [uri] cannot be found or accessed.
     */
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
