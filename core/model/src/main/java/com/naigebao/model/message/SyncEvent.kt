package com.naigebao.model.message

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class SyncEvent(
    val type: String,
    val payload: JsonObject = JsonObject(emptyMap()),
    val flakeId: String? = null,
    val receivedAt: Long = System.currentTimeMillis()
)
