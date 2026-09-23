package ru.kryu.yanavipeb.watch

import android.content.Context
import android.content.pm.PackageManager

/** Checks [PebbleAppChecker] via [PackageManager.getPackageInfo] on [Protocol.PEBBLE_APP_PACKAGE]. */
class PackageManagerPebbleAppChecker(private val context: Context) : PebbleAppChecker {
    @Suppress("DEPRECATION")
    override fun isInstalled(): Boolean = try {
        context.packageManager.getPackageInfo(Protocol.PEBBLE_APP_PACKAGE, 0)
        true
    } catch (e: PackageManager.NameNotFoundException) {
        false
    }
}
