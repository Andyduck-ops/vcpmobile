package com.naigebao.network.websocket

import com.naigebao.model.message.SyncEvent
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MessageProtocolTest {
    private val protocol = MessageProtocol()

    @Test
    fun `decode accepts vcptoolbox data envelope`() {
        val event = protocol.decode("""{"type":"sync","data":{"sessionId":"s-1"},"flakeId":"f-1"}""")

        assertEquals("sync", event.type)
        assertEquals("s-1", event.payload["sessionId"]?.toString()?.trim('"'))
        assertEquals("f-1", event.flakeId)
    }

    @Test
    fun `encode writes data envelope`() {
        val raw = protocol.encode(
            SyncEvent(
                type = "message",
                payload = buildJsonObject { put("content", "hello") },
                flakeId = "flake"
            )
        )

        assertTrue(raw.contains("\"type\":\"message\""))
        assertTrue(raw.contains("\"data\":{\"content\":\"hello\"}"))
        assertTrue(raw.contains("\"flakeId\":\"flake\""))
    }

    @Test
    fun `backoff delay uses capped exponential growth`() {
        assertEquals(1_000L, ReconnectingWebSocket.calculateBackoffDelay(1))
        assertEquals(2_000L, ReconnectingWebSocket.calculateBackoffDelay(2))
        assertEquals(30_000L, ReconnectingWebSocket.calculateBackoffDelay(10))
    }
}
