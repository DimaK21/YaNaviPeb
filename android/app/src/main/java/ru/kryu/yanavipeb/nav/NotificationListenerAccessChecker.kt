package ru.kryu.yanavipeb.nav

import android.content.Context
import androidx.core.app.NotificationManagerCompat

/** Checks [NotificationAccessChecker] via the system's enabled-listeners list. */
class NotificationListenerAccessChecker(private val context: Context) : NotificationAccessChecker {
    override fun isEnabled(): Boolean =
        NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)
}
