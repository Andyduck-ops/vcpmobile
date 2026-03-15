package com.naigebao.network.sync

import com.naigebao.common.AppDispatchers
import com.naigebao.model.message.SyncEvent
import com.naigebao.model.sync.SyncStateStore
import com.naigebao.network.websocket.ConnectionState
import com.naigebao.network.websocket.WebSocketManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class SyncManager(
    private val webSocketManager: WebSocketManager,
    private val syncStateStore: SyncStateStore,
    private val deltaSyncStrategy: DeltaSyncStrategy = DeltaSyncStrategy(syncStateStore),
    private val protocol: SyncProtocol = SyncProtocol(),
    private val dispatchers: AppDispatchers = AppDispatchers(),
    private val dedupe: (String?) -> Boolean = { true },
    private val handler: SyncEventHandler
) {
    private val scope = CoroutineScope(SupervisorJob() + dispatchers.io)

    fun start() {
        scope.launch {
            webSocketManager.connectionState.collectLatest { state ->
                syncStateStore.updateConnectionStatus(state.toSimpleStatus())
                if (state is ConnectionState.Connected) {
                    requestSync()
                }
            }
        }
        scope.launch {
            webSocketManager.events.collectLatest { event ->
                handleEvent(event)
            }
        }
    }

    fun stop() {
        // No-op for now; state updates are handled by collectors.
    }

    suspend fun requestSync() {
        val syncRequest = deltaSyncStrategy.buildSyncRequest()
        webSocketManager.send(syncRequest)
    }

    private suspend fun handleEvent(event: SyncEvent) {
        if (!dedupe(event.flakeId)) {
            return
        }
        when (val action = protocol.parse(event)) {
            is SyncAction.SessionAdded -> handler.onSessionAdded(action.session)
            is SyncAction.SessionUpdated -> handler.onSessionUpdated(action.session)
            is SyncAction.SessionRemoved -> handler.onSessionRemoved(action.sessionId)
            is SyncAction.MessageReceived -> handler.onMessageReceived(action.sessionId, action.message)
            is SyncAction.ConnectionChanged -> handler.onConnectionChanged(action.status)
            is SyncAction.Unknown -> handler.onUnknown(event)
        }
        syncStateStore.updateLastSync(event.receivedAt)
    }
}

interface SyncEventHandler {
    suspend fun onSessionAdded(session: com.naigebao.model.session.Session)

    suspend fun onSessionUpdated(session: com.naigebao.model.session.Session)

    suspend fun onSessionRemoved(sessionId: String)

    suspend fun onMessageReceived(sessionId: String, message: com.naigebao.model.message.Message)

    suspend fun onConnectionChanged(status: String?)

    suspend fun onUnknown(event: SyncEvent)
}

private fun ConnectionState.toSimpleStatus(): String {
    return when (this) {
        ConnectionState.Idle -> "idle"
        is ConnectionState.Connecting -> "connecting"
        is ConnectionState.Connected -> "connected"
        is ConnectionState.Reconnecting -> "reconnecting"
        is ConnectionState.Disconnected -> "disconnected"
        is ConnectionState.Failed -> "failed"
    }
}
