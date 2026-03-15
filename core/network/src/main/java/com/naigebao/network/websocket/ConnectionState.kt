package com.naigebao.network.websocket

sealed interface ConnectionState {
    data object Idle : ConnectionState

    data class Connecting(val url: String) : ConnectionState

    data class Connected(val url: String) : ConnectionState

    data class Reconnecting(val attempt: Int, val delayMillis: Long) : ConnectionState

    data class Disconnected(val code: Int? = null, val reason: String? = null) : ConnectionState

    data class Failed(val throwable: Throwable) : ConnectionState
}
