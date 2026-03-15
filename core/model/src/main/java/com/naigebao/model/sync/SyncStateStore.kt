package com.naigebao.model.sync

data class SyncStateSnapshot(
    val lastSyncAt: Long? = null,
    val connectionStatus: String? = null
)

interface SyncStateStore {
    suspend fun readState(): SyncStateSnapshot

    suspend fun updateLastSync(timestamp: Long)

    suspend fun updateConnectionStatus(status: String?)
}
