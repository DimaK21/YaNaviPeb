package ru.kryu.yanavipeb.watch

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import ru.kryu.yanavipeb.nav.NavState

/**
 * Keeps the watch in step with the latest [NavState]: starts and stops the watchapp, sends only
 * what changed, at most once per [minIntervalMs], and resends everything when the watchapp reopens.
 * [now] is a monotonic clock in milliseconds. Whether the watchapp is open comes from
 * [WatchTransport.watchappOpen], collected for the lifetime of this instance.
 */
class NavSyncer(
    private val transport: WatchTransport,
    private val scope: CoroutineScope,
    private val now: () -> Long,
    private val minIntervalMs: Long = 1_000,
    private val stopDelayMs: Long = 5_000,
) {
    private val lock = Mutex()
    private var current = NavState.IDLE
    private var sent: NavState? = null // what the watch shows; null = unknown (watchapp closed)
    private var watchOpen = false
    private var lastSendAt: Long? = null
    private var flushJob: Job? = null
    private var stopJob: Job? = null

    init {
        scope.launch {
            transport.watchappOpen().collect { open ->
                lock.withLock { applyWatchOpen(open) }
            }
        }
    }

    fun onNavState(state: NavState) {
        scope.launch { lock.withLock { handleState(state) } }
    }

    private suspend fun applyWatchOpen(open: Boolean) {
        watchOpen = open
        if (open) {
            sent = NavState.IDLE // a freshly opened watchapp starts with an empty state
            flush()
        } else {
            sent = null
            flushJob?.cancel()
            flushJob = null
        }
    }

    private suspend fun handleState(state: NavState) {
        val wasNavigating = current.navigating
        current = state
        if (state.navigating) {
            stopJob?.cancel()
            stopJob = null
            if (!wasNavigating) transport.startApp()
            flush()
        } else if (wasNavigating) {
            flush()
            scheduleStop()
        }
    }

    private suspend fun flush() {
        if (!watchOpen) return
        val message = NavMessage.diff(sent, current)
        if (message.isEmpty()) return
        val last = lastSendAt
        if (last != null) {
            val wait = minIntervalMs - (now() - last)
            if (wait > 0) {
                scheduleFlush(wait)
                return
            }
        }
        val target = current
        if (transport.send(message)) sent = target
        lastSendAt = now()
    }

    private fun scheduleFlush(delayMs: Long) {
        if (flushJob?.isActive == true) return
        flushJob = scope.launch {
            delay(delayMs)
            lock.withLock {
                flushJob = null
                flush()
            }
        }
    }

    private fun scheduleStop() {
        stopJob?.cancel()
        stopJob = scope.launch {
            delay(stopDelayMs)
            lock.withLock { if (!current.navigating) transport.stopApp() }
        }
    }
}
