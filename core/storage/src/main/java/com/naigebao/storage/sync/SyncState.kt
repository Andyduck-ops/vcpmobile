package com.naigebao.storage.sync

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.naigebao.model.sync.SyncStateSnapshot
import com.naigebao.model.sync.SyncStateStore
import kotlinx.coroutines.flow.first

data class SyncState(
    val lastSyncAt: Long? = null,
    val connectionStatus: String? = null
)

class DataStoreSyncStateStore(private val context: Context) : SyncStateStore {
    override suspend fun readState(): SyncStateSnapshot {
        val prefs = context.syncStateDataStore.data.first()
        return SyncStateSnapshot(
            lastSyncAt = prefs[KEY_LAST_SYNC],
            connectionStatus = prefs[KEY_CONNECTION_STATUS]
        )
    }

    override suspend fun updateLastSync(timestamp: Long) {
        context.syncStateDataStore.edit { prefs ->
            prefs[KEY_LAST_SYNC] = timestamp
        }
    }

    override suspend fun updateConnectionStatus(status: String?) {
        context.syncStateDataStore.edit { prefs ->
            if (status == null) {
                prefs.remove(KEY_CONNECTION_STATUS)
            } else {
                prefs[KEY_CONNECTION_STATUS] = status
            }
        }
    }

    companion object {
        private val KEY_LAST_SYNC = longPreferencesKey("last_sync_at")
        private val KEY_CONNECTION_STATUS = stringPreferencesKey("connection_status")
    }
}

private val Context.syncStateDataStore by preferencesDataStore(name = "sync_state")
