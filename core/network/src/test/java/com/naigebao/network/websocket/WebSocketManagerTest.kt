package com.naigebao.network.websocket

import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.WebSocketListener
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class WebSocketManagerTest {
    private lateinit var server: MockWebServer

    @Before
    fun setUp() {
        server = MockWebServer()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun receivesMessageFromWebSocket() = runTest {
        server.enqueue(
            MockResponse().withWebSocketUpgrade(object : WebSocketListener() {
                override fun onOpen(webSocket: okhttp3.WebSocket, response: okhttp3.Response) {
                    webSocket.send(
                        """{"type":"message-received","payload":{"sessionId":"s1","content":"hi"}}"""
                    )
                }
            })
        )
        server.start()

        val client = OkHttpClient.Builder().build()
        val manager = WebSocketManager(client)
        val url = server.url("/VCPlog/VCP_Key=test").toString().replace("http", "ws")

        manager.events.test {
            manager.connect(url)
            val event = awaitItem()
            assertEquals("message-received", event.type)
            cancelAndIgnoreRemainingEvents()
        }
    }

}
