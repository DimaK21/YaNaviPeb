package ru.kryu.yanavipeb.watch

import io.rebble.pebblekit2.common.model.WatchIdentifier
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
class PebbleActiveAppObserverTest {
    /** Fails the way the real provider does while the Pebble app is missing: with [SecurityException]. */
    private class FakeProvider : PebbleProvider {
        var connectedWatch: WatchIdentifier? = null
        var openApp: UUID? = null
        var subscribeFails = false
        var watchQueryFails = false
        var activeAppQueryFails = false
        var connectedWatchSubscriptions = 0

        private val connectedWatchTicks = MutableStateFlow(0)
        private val activeAppTicks = MutableStateFlow(0)

        fun notifyConnectedWatchChanged() {
            connectedWatchTicks.value++
        }

        fun notifyActiveAppChanged() {
            activeAppTicks.value++
        }

        override fun connectedWatchChanges(): Flow<Unit> = changes(connectedWatchTicks) { connectedWatchSubscriptions++ }

        override fun activeAppChanges(watch: WatchIdentifier): Flow<Unit> = changes(activeAppTicks)

        override fun firstConnectedWatch(): WatchIdentifier? {
            if (watchQueryFails) throw SecurityException("Permission Denial")
            return connectedWatch
        }

        override fun activeApp(watch: WatchIdentifier): UUID? {
            if (activeAppQueryFails) throw SecurityException("Permission Denial")
            return openApp
        }

        private fun changes(ticks: Flow<Int>, onSubscribe: () -> Unit = {}): Flow<Unit> = flow {
            onSubscribe()
            if (subscribeFails) throw SecurityException("Failed to find provider for user 0")
            ticks.collect { emit(Unit) }
        }
    }

    private class Fixture(scope: TestScope) {
        val provider = FakeProvider()
        val states = mutableListOf<Boolean>()

        // isActive() never completes, so it is collected on backgroundScope, like NavSyncer's
        // init block in NavSyncerTest; collection starts on the first runCurrent()/advanceTimeBy().
        val collection = scope.backgroundScope.launch {
            PebbleActiveAppObserver(provider, RETRY_INTERVAL_MS).isActive(Protocol.WATCHAPP_UUID).toList(states)
        }
    }

    private fun TestScope.fixture() = Fixture(this)

    @Test
    fun reportsWatchappOpenOnlyWhileItIsActiveOnTheConnectedWatch() = runTest {
        val f = fixture()
        f.provider.connectedWatch = WATCH
        f.provider.openApp = Protocol.WATCHAPP_UUID
        runCurrent()
        assertEquals(listOf(true), f.states)

        f.provider.openApp = OTHER_APP
        f.provider.notifyActiveAppChanged()
        runCurrent()
        assertEquals(listOf(true, false), f.states)

        f.provider.openApp = Protocol.WATCHAPP_UUID
        f.provider.notifyActiveAppChanged()
        runCurrent()
        assertEquals(listOf(true, false, true), f.states)

        f.provider.connectedWatch = null
        f.provider.notifyConnectedWatchChanged()
        runCurrent()
        assertEquals(listOf(true, false, true, false), f.states)
    }

    @Test
    fun reportsClosedWhenNoWatchIsConnected() = runTest {
        val f = fixture()
        runCurrent()
        assertEquals(listOf(false), f.states)
    }

    @Test
    fun staysAliveAndKeepsRetryingWhileProviderIsMissing() = runTest {
        val f = fixture()
        f.provider.subscribeFails = true
        runCurrent()
        assertTrue(f.collection.isActive)
        assertEquals(1, f.provider.connectedWatchSubscriptions)

        advanceTimeBy(RETRY_INTERVAL_MS - 1)
        runCurrent()
        assertEquals(1, f.provider.connectedWatchSubscriptions)

        advanceTimeBy(1)
        runCurrent()
        assertEquals(2, f.provider.connectedWatchSubscriptions)
        assertTrue(f.collection.isActive)
        assertEquals(emptyList<Boolean>(), f.states)
    }

    @Test
    fun picksUpWatchappOnceProviderAppears() = runTest {
        val f = fixture()
        f.provider.subscribeFails = true
        runCurrent()

        f.provider.subscribeFails = false
        f.provider.connectedWatch = WATCH
        f.provider.openApp = Protocol.WATCHAPP_UUID
        advanceTimeBy(RETRY_INTERVAL_MS)
        runCurrent()

        assertEquals(listOf(true), f.states)
    }

    @Test
    fun treatsFailingWatchQueryAsNoWatchAndRecoversOnNextChange() = runTest {
        val f = fixture()
        f.provider.watchQueryFails = true
        runCurrent()
        assertEquals(listOf(false), f.states)

        f.provider.watchQueryFails = false
        f.provider.connectedWatch = WATCH
        f.provider.openApp = Protocol.WATCHAPP_UUID
        f.provider.notifyConnectedWatchChanged()
        runCurrent()
        assertEquals(listOf(false, true), f.states)
    }

    @Test
    fun treatsFailingActiveAppQueryAsWatchappNotOpen() = runTest {
        val f = fixture()
        f.provider.connectedWatch = WATCH
        f.provider.openApp = Protocol.WATCHAPP_UUID
        f.provider.activeAppQueryFails = true
        runCurrent()
        assertEquals(listOf(false), f.states)
        assertTrue(f.collection.isActive)

        f.provider.activeAppQueryFails = false
        f.provider.notifyActiveAppChanged()
        runCurrent()
        assertEquals(listOf(false, true), f.states)
    }

    private companion object {
        const val RETRY_INTERVAL_MS = 10_000L
        val WATCH = WatchIdentifier("watch-1")
        val OTHER_APP: UUID = UUID.fromString("00000000-0000-0000-0000-000000000001")
    }
}
