package com.naigebao.network.websocket

import com.naigebao.common.AppDispatchers
import com.naigebao.model.message.SyncEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.ConcurrentHashMap

class WebSocketManager(
    client: OkHttpClient,
    dispatchers: AppDispatchers = AppDispatchers(),
    private val protocol: MessageProtocol = MessageProtocol()
) {
    private val scope = CoroutineScope(SupervisorJob() + dispatchers.io)
    private val heartbeatManager = HeartbeatManager(scope)
    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Idle)
    private val _events = MutableSharedFlow<SyncEvent>(extraBufferCapacity = 32)

    // Active requests tracking for cancellation
    private val activeRequests = ConcurrentHashMap<String, Job>()

    private val reconnectingWebSocket = ReconnectingWebSocket(
        client = client,
        scope = scope,
        listener = object : ReconnectingWebSocket.Listener {
            override fun onStateChanged(state: ConnectionState) {
                _connectionState.value = state
                when (state) {
                    is ConnectionState.Connected -> startHeartbeat()
                    is ConnectionState.Disconnected, is ConnectionState.Failed -> heartbeatManager.stop()
                    else -> Unit
                }
            }

            override fun onMessage(rawMessage: String, dedupe: (String?) -> Boolean) {
                val event = protocol.decode(rawMessage)
                if (dedupe(event.flakeId)) {
                    _events.tryEmit(event)
                }
            }

            override fun onFailure(throwable: Throwable) {
                _connectionState.value = ConnectionState.Failed(throwable)
            }

            override fun onReconnectScheduled(attempt: Int) = Unit
        }
    )

    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()
    val events: SharedFlow<SyncEvent> = _events.asSharedFlow()

    fun connect(url: String, headers: Map<String, String> = emptyMap()) {
        val builder = Request.Builder().url(url)
        headers.forEach { (key, value) -> builder.header(key, value) }
        reconnectingWebSocket.connect(builder.build())
    }

    fun disconnect() {
        heartbeatManager.stop()
        reconnectingWebSocket.disconnect()
    }

    fun send(event: SyncEvent): Boolean {
        return reconnectingWebSocket.send(protocol.encode(event))
    }

    /**
     * Register an active request for tracking
     * @param requestId Unique identifier for the request
     * @param job The coroutine job to track
     */
    fun registerRequest(requestId: String, job: Job) {
        activeRequests[requestId] = job
    }

    /**
     * Cancel an active request by requestId
     * @param requestId The request ID to cancel
     * @return true if request was found and cancelled, false otherwise
     */
    fun cancelRequest(requestId: String): Boolean {
        val job = activeRequests.remove(requestId)
        return if (job != null) {
            job.cancel()
            true
        } else {
            false
        }
    }

    /**
     * Remove a request from tracking (for completed requests)
     * @param requestId The request ID to remove
     */
    fun removeRequest(requestId: String) {
        activeRequests.remove(requestId)
    }

    /**
     * Check if a request is still active
     * @param requestId The request ID to check
     * @return true if request is active
     */
    fun isRequestActive(requestId: String): Boolean {
        val job = activeRequests[requestId]
        return job != null && job.isActive
    }

    /**
     * Generate a unique request ID
     * @return A unique request ID string
     */
    fun generateRequestId(): String {
        return java.util.UUID.randomUUID().toString()
    }

    /**
     * Clean up completed or cancelled requests
     */
    fun cleanupInactiveRequests() {
        activeRequests.entries.removeIf { !it.value.isActive }
    }

    private fun startHeartbeat() {
        heartbeatManager.start {
            send(
                SyncEvent(
                    type = "heartbeat",
                    payload = kotlinx.serialization.json.buildJsonObject {
                        put("timestamp", JsonPrimitive(System.currentTimeMillis()))
                    }
                )
            )
        }
    }
}
