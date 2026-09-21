package ru.kryu.yanavipeb.nav

import android.app.Notification
import android.content.ComponentName
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import ru.kryu.yanavipeb.NavRuntime

/** Feeds every Yandex Maps navigation notification into [NavRuntime]'s syncer. */
class MapsNotificationListener : NotificationListenerService() {
    private val reader by lazy { NotificationSnapshotReader(applicationContext) }
    private var lastLogged: NavState? = null

    override fun onListenerConnected() {
        activeNotifications?.forEach { handle(it) }
    }

    override fun onListenerDisconnected() {
        requestRebind(ComponentName(this, MapsNotificationListener::class.java))
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        handle(sbn)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        if (isNavigation(sbn)) NavRuntime.syncer(applicationContext).onNavState(NavState.IDLE)
    }

    private fun handle(sbn: StatusBarNotification) {
        if (!isNavigation(sbn)) return
        val state = NavStateParser.parse(reader.read(sbn))
        if (state != lastLogged) {
            Log.i(TAG, state.toString())
            lastLogged = state
        }
        NavRuntime.syncer(applicationContext).onNavState(state)
    }

    private fun isNavigation(sbn: StatusBarNotification): Boolean =
        sbn.packageName == MAPS_PACKAGE &&
            sbn.id == NOTIFICATION_ID &&
            sbn.notification.category == Notification.CATEGORY_NAVIGATION

    private companion object {
        const val TAG = "NavListener"
        const val MAPS_PACKAGE = "ru.yandex.yandexmaps"
        const val NOTIFICATION_ID = 2
    }
}
