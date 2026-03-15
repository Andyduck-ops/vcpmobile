package com.naigebao.network.websocket

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

class ReconnectingWebSocket(
    private val client: OkHttpClient,
    private val scope: CoroutineScope,
    private val listener: Listener
) {
    private val activeSocket = AtomicReference<WebSocket?>(null)
    private val reconnectJob = AtomicReference<Job?>(null)
    private val closedByUser = AtomicBoolean(false)
    private val seenFlakeIds = ConcurrentHashMap.newKeySet<String>()

    private var request: Request? = null
    private var reconnectAttempts: Int = 0

    fun connect(request: Request) {
        this.request = request
        closedByUser.set(false)
        reconnectAttempts = 0
        listener.onStateChanged(ConnectionState.Connecting(request.url.toString()))
        openSocket(request)
    }

    fun disconnect(code: Int = NORMAL_CLOSURE_STATUS, reason: String = "Client requested disconnect") {
        closedByUser.set(true)
        reconnectJob.getAndSet(null)?.cancel()
        activeSocket.getAndSet(null)?.close(code, reason)
        listener.onStateChanged(ConnectionState.Disconnected(code, reason))
    }

    fun send(text: String): Boolean {
        return activeSocket.get()?.send(text) == true
    }

    private fun openSocket(request: Request) {
        reconnectJob.getAndSet(null)?.cancel()
        activeSocket.set(client.newWebSocket(request, socketListener()))
    }

    private fun socketListener(): WebSocketListener {
        return object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                reconnectAttempts = 0
                activeSocket.set(webSocket)
                listener.onStateChanged(ConnectionState.Connected(response.request.url.toString()))
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                listener.onMessage(text, ::rememberFlakeId)
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                listener.onStateChanged(ConnectionState.Disconnected(code, reason))
                webSocket.close(code, reason)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                listener.onStateChanged(ConnectionState.Disconnected(code, reason))
                scheduleReconnectIfNeeded()
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                listener.onStateChanged(ConnectionState.Failed(t))
                listener.onFailure(t)
                scheduleReconnectIfNeeded()
            }
        }
    }

    private fun scheduleReconnectIfNeeded() {
        val currentRequest = request ?: return
        if (closedByUser.get()) {
            return
        }

        reconnectAttempts += 1
        val delayMillis = calculateBackoffDelay(reconnectAttempts)
        listener.onStateChanged(ConnectionState.Reconnecting(reconnectAttempts, delayMillis))
        reconnectJob.set(
            scope.launch {
                delay(delayMillis)
                if (!closedByUser.get()) {
                    listener.onReconnectScheduled(reconnectAttempts)
                    openSocket(currentRequest)
                }
            }
        )
    }

    private fun rememberFlakeId(flakeId: String?): Boolean {
        if (flakeId.isNullOrBlank()) {
            return true
        }
        val isNew = seenFlakeIds.add(flakeId)
        if (seenFlakeIds.size > MAX_TRACKED_FLAKE_IDS) {
            seenFlakeIds.clear()
            seenFlakeIds.add(flakeId)
        }
        return isNew
    }

    interface Listener {
        fun onStateChanged(state: ConnectionState)

        fun onMessage(rawMessage: String, dedupe: (String?) -> Boolean)

        fun onFailure(throwable: Throwable)

        fun onReconnectScheduled(attempt: Int)
    }

    companion object {
        const val NORMAL_CLOSURE_STATUS = 1000
        private const val MAX_TRACKED_FLAKE_IDS = 512

        fun calculateBackoffDelay(attempt: Int): Long {
            val exponent = (attempt - 1).coerceAtLeast(0)
            val delay = 1_000L * (1L shl exponent.coerceAtMost(5))
            return delay.coerceAtMost(30_000L)
        }
    }
}
