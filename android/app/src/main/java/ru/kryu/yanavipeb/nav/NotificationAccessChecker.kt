package ru.kryu.yanavipeb.nav

/** Whether this app is currently granted notification-listener access. */
interface NotificationAccessChecker {
    fun isEnabled(): Boolean
}
