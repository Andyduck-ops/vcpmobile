package com.naigebao.network.websocket

import com.naigebao.model.message.SyncEvent
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class MessageProtocol(
    private val json: Json = Json {
        ignoreUnknownKeys = true
    }
) {
    fun decode(rawMessage: String): SyncEvent {
        val root = json.parseToJsonElement(rawMessage).jsonObject
        val type = root["type"]?.jsonPrimitive?.content ?: "unknown"
        val flakeId = root["flakeId"]?.jsonPrimitive?.content
        val payload = root["payload"]?.jsonObject
            ?: root["data"]?.jsonObject
            ?: JsonObject(root.filterKeys { it != "type" && it != "flakeId" })
        return SyncEvent(
            type = type,
            payload = payload,
            flakeId = flakeId
        )
    }

    fun encode(event: SyncEvent): String {
        return json.encodeToString(
            WireEnvelope(
                type = event.type,
                data = event.payload,
                flakeId = event.flakeId
            )
        )
    }

    @Serializable
    private data class WireEnvelope(
        val type: String,
        val data: JsonObject? = null,
        @SerialName("payload")
        val payload: JsonObject? = null,
        val flakeId: String? = null
    )
}
