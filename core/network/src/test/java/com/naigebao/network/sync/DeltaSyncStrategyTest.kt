package com.naigebao.network.sync

import com.naigebao.model.sync.SyncStateSnapshot
import com.naigebao.model.sync.SyncStateStore
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class DeltaSyncStrategyTest {
    @Test
    fun buildSyncRequestUsesStoredLastSync() = runTest {
        val store = FakeStore(lastSyncAt = 1234L)
        val strategy = DeltaSyncStrategy(store, SyncProtocol())

        val request = strategy.buildSyncRequest()

        assertEquals("sync-request", request.type)
        assertEquals("1234", request.payload["lastSyncAt"]?.toString()?.trim('"'))
    }

    private class FakeStore(private val lastSyncAt: Long?) : SyncStateStore {
        override suspend fun readState(): SyncStateSnapshot {
            return SyncStateSnapshot(lastSyncAt = lastSyncAt)
        }

        override suspend fun updateLastSync(timestamp: Long) = Unit

        override suspend fun updateConnectionStatus(status: String?) = Unit
    }
}
