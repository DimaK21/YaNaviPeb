package ru.kryu.yanavipeb.watch

import io.rebble.pebblekit2.common.model.PebbleDictionary
import io.rebble.pebblekit2.common.model.PebbleDictionaryItem
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import ru.kryu.yanavipeb.nav.NavState

@OptIn(ExperimentalCoroutinesApi::class)
class NavSyncerTest {
    private class FakeTransport(private val clock: () -> Long) : WatchTransport {
        val sends = mutableListOf<Pair<Long, PebbleDictionary>>()
        var starts = 0
        var stops = 0
        var sendResult = true

        /** Stands in for PebbleActiveAppObserver: tests flip this instead of pushing open/close events. */
        val watchOpenFlow = MutableStateFlow(false)

        override suspend fun send(data: PebbleDictionary): Boolean {
            sends += clock() to data
            return sendResult
        }

        override suspend fun startApp(): Boolean {
            starts++
            return true
        }

        override suspend fun stopApp(): Boolean {
            stops++
            return true
        }

        override fun watchappOpen(): Flow<Boolean> = watchOpenFlow
    }

    private class Fixture(scope: TestScope) {
        val transport = FakeTransport { scope.testScheduler.currentTime }

        // NavSyncer's init block collects transport.watchappOpen() for its whole lifetime, so it
        // must run on backgroundScope: runTest fails if a coroutine on the test's own scope is
        // still active when the test body returns (kotlinx.coroutines.test.UncompletedCoroutinesError).
        val syncer = NavSyncer(transport, scope.backgroundScope, { scope.testScheduler.currentTime })
    }

    private fun nav(distance: String = "150 м") =
        NavState(true, distance, "Поверните направо", "1,61 км", "23:57", "15 мин", ByteArray(Protocol.ICON_BYTES) { 1 })

    private fun TestScope.fixture() = Fixture(this)

    @Test
    fun startsWatchappWhenNavigationBegins() = runTest {
        val f = fixture()
        f.syncer.onNavState(nav())
        runCurrent()
        assertEquals(1, f.transport.starts)
    }

    @Test
    fun startsWatchappOnlyOncePerNavigation() = runTest {
        val f = fixture()
        f.syncer.onNavState(nav())
        f.syncer.onNavState(nav())
        runCurrent()
        assertEquals(1, f.transport.starts)
    }

    @Test
    fun sendsNothingWhileWatchappIsClosed() = runTest {
        val f = fixture()
        f.syncer.onNavState(nav())
        runCurrent()
        assertEquals(0, f.transport.sends.size)
    }

    @Test
    fun sendsNothingWhenWatchappOpensWhileIdle() = runTest {
        val f = fixture()
        f.transport.watchOpenFlow.value = true
        runCurrent()
        assertEquals(0, f.transport.sends.size)
    }

    @Test
    fun sendsFullStateWhenWatchappOpens() = runTest {
        val f = fixture()
        f.syncer.onNavState(nav())
        f.transport.watchOpenFlow.value = true
        runCurrent()
        assertEquals(1, f.transport.sends.size)
        assertEquals(7, f.transport.sends[0].second.size)
    }

    @Test
    fun sendsOnlyChangedKeys() = runTest {
        val f = fixture()
        f.transport.watchOpenFlow.value = true
        f.syncer.onNavState(nav("150 м"))
        runCurrent()
        advanceTimeBy(1_000)
        f.syncer.onNavState(nav("100 м"))
        runCurrent()
        assertEquals(2, f.transport.sends.size)
        assertEquals(setOf(Protocol.KEY_DISTANCE), f.transport.sends[1].second.keys)
    }

    @Test
    fun identicalRepeatsAreNotSent() = runTest {
        val f = fixture()
        f.transport.watchOpenFlow.value = true
        f.syncer.onNavState(nav())
        runCurrent()
        repeat(5) {
            advanceTimeBy(1_000)
            f.syncer.onNavState(nav())
            runCurrent()
        }
        assertEquals(1, f.transport.sends.size)
    }

    @Test
    fun throttlesToOnePerSecondAndSendsTheLatest() = runTest {
        val f = fixture()
        f.transport.watchOpenFlow.value = true
        f.syncer.onNavState(nav("150 м"))
        runCurrent()
        f.syncer.onNavState(nav("100 м"))
        f.syncer.onNavState(nav("50 м"))
        runCurrent()
        assertEquals(1, f.transport.sends.size)

        advanceTimeBy(999)
        runCurrent()
        assertEquals(1, f.transport.sends.size)

        advanceTimeBy(1)
        runCurrent()
        assertEquals(2, f.transport.sends.size)
        assertEquals(1_000L, f.transport.sends[1].first)
        assertEquals(PebbleDictionaryItem.Text("50 м"), f.transport.sends[1].second[Protocol.KEY_DISTANCE])
    }

    @Test
    fun resendsEverythingWhenWatchappReopens() = runTest {
        val f = fixture()
        f.syncer.onNavState(nav())
        f.transport.watchOpenFlow.value = true
        runCurrent()
        f.transport.watchOpenFlow.value = false
        runCurrent()
        advanceTimeBy(1_000)
        f.transport.watchOpenFlow.value = true
        runCurrent()
        assertEquals(2, f.transport.sends.size)
        assertEquals(7, f.transport.sends[1].second.size)
    }

    @Test
    fun failedSendIsRetriedOnTheNextState() = runTest {
        val f = fixture()
        f.transport.sendResult = false
        f.transport.watchOpenFlow.value = true
        f.syncer.onNavState(nav())
        runCurrent()
        assertEquals(1, f.transport.sends.size)

        f.transport.sendResult = true
        advanceTimeBy(1_000)
        f.syncer.onNavState(nav())
        runCurrent()
        assertEquals(2, f.transport.sends.size)
        assertEquals(7, f.transport.sends[1].second.size)
    }

    @Test
    fun stopsWatchappFiveSecondsAfterNavigationEnds() = runTest {
        val f = fixture()
        f.transport.watchOpenFlow.value = true
        f.syncer.onNavState(nav())
        runCurrent()
        f.syncer.onNavState(NavState.IDLE)
        runCurrent()

        advanceTimeBy(4_999)
        runCurrent()
        assertEquals(0, f.transport.stops)

        advanceTimeBy(1)
        runCurrent()
        assertEquals(1, f.transport.stops)
        assertEquals(mapOf(Protocol.KEY_STATE to PebbleDictionaryItem.UInt8(0)), f.transport.sends.last().second)
    }

    @Test
    fun resumedNavigationCancelsPendingStop() = runTest {
        val f = fixture()
        f.syncer.onNavState(nav())
        runCurrent()
        f.syncer.onNavState(NavState.IDLE)
        runCurrent()
        advanceTimeBy(2_000)
        f.syncer.onNavState(nav())
        runCurrent()
        advanceTimeBy(10_000)
        runCurrent()
        assertEquals(0, f.transport.stops)
    }
}
