package com.naigebao.storage.deduplicate

import java.util.LinkedHashMap

class MessageDeduplicator(
    private val capacity: Int = DEFAULT_CAPACITY
) {
    private val seen = object : LinkedHashMap<String, Unit>(capacity, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Unit>?): Boolean {
            return size > capacity
        }
    }

    @Synchronized
    fun shouldProcess(flakeId: String?): Boolean {
        if (flakeId.isNullOrBlank()) {
            return true
        }
        val exists = seen.containsKey(flakeId)
        seen[flakeId] = Unit
        return !exists
    }

    @Synchronized
    fun reset() {
        seen.clear()
    }

    companion object {
        const val DEFAULT_CAPACITY = 512
    }
}
