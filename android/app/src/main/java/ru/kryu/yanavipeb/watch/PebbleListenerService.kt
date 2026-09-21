package ru.kryu.yanavipeb.watch

import io.rebble.pebblekit2.client.BasePebbleListenerService
import io.rebble.pebblekit2.common.model.WatchIdentifier
import ru.kryu.yanavipeb.NavRuntime
import java.util.UUID

/** Bound by the Pebble app; tells [NavSyncer] when our watchapp opens or closes on the watch. */
class PebbleListenerService : BasePebbleListenerService() {
    override fun onAppOpened(watchappUUID: UUID, watch: WatchIdentifier) {
        if (watchappUUID == Protocol.WATCHAPP_UUID) NavRuntime.syncer(this).onWatchOpened()
    }

    override fun onAppClosed(watchappUUID: UUID, watch: WatchIdentifier) {
        if (watchappUUID == Protocol.WATCHAPP_UUID) NavRuntime.syncer(this).onWatchClosed()
    }
}
