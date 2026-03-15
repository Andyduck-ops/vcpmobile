package com.naigebao.network.sync

import com.naigebao.model.message.SyncEvent
import com.naigebao.model.sync.SyncStateStore

class DeltaSyncStrategy(
    private val syncStateStore: SyncStateStore,
    private val protocol: SyncProtocol = SyncProtocol()
) {
    suspend fun buildSyncRequest(): SyncEvent {
        val snapshot = syncStateStore.readState()
        return protocol.buildSyncRequest(snapshot.lastSyncAt)
    }
}
