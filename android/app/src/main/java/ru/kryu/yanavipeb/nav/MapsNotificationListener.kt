package ru.kryu.yanavipeb.nav

import android.app.Notification
import android.content.ComponentName
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import ru.kryu.yanavipeb.BuildConfig
import ru.kryu.yanavipeb.NavRuntime

/** Feeds every Yandex Maps / Yandex Navigator navigation notification into [NavRuntime]'s syncer. */
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
        // state.toString() includes the route (maneuver text, ETA); keep it out of release logs.
        if (BuildConfig.DEBUG && state != lastLogged) {
            Log.i(TAG, state.toString())
            lastLogged = state
        }
        NavRuntime.syncer(applicationContext).onNavState(state)
    }

    private fun isNavigation(sbn: StatusBarNotification): Boolean =
        sbn.packageName in SUPPORTED_PACKAGES &&
            sbn.id == NOTIFICATION_ID &&
            sbn.notification.category == Notification.CATEGORY_NAVIGATION

    private companion object {
        const val TAG = "NavListener"

        // Yandex Navigator only posts this notification while its "Фоновая навигация"
        // (background navigation) setting is enabled; otherwise it has no maneuver data.
        val SUPPORTED_PACKAGES = setOf("ru.yandex.yandexmaps", "ru.yandex.yandexnavi")
        const val NOTIFICATION_ID = 2
    }
}
