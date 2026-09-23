package ru.kryu.yanavipeb.watch

/** Whether the official Pebble companion app is installed on this phone. */
interface PebbleAppChecker {
    fun isInstalled(): Boolean
}
