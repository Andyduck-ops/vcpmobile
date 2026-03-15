package com.naigebao.network.websocket

import org.junit.Assert.assertEquals
import org.junit.Test

class ReconnectingWebSocketTest {
    @Test
    fun backoffDelayCapsAtThirtySeconds() {
        val delay = ReconnectingWebSocket.calculateBackoffDelay(10)
        assertEquals(30_000L, delay)
    }

    @Test
    fun backoffStartsAtOneSecond() {
        val delay = ReconnectingWebSocket.calculateBackoffDelay(1)
        assertEquals(1_000L, delay)
    }
}
