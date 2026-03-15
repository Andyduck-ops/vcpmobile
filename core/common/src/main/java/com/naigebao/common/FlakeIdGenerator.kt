package com.naigebao.common

import java.util.UUID

object FlakeIdGenerator {
    fun nextId(): String {
        return "${System.currentTimeMillis()}-${UUID.randomUUID()}"
    }
}
