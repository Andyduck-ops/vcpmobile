package com.naigebao.storage.deduplicate

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MessageDeduplicatorTest {
    @Test
    fun shouldProcessReturnsFalseForDuplicate() {
        val deduplicator = MessageDeduplicator(capacity = 2)
        assertTrue(deduplicator.shouldProcess("flake-1"))
        assertFalse(deduplicator.shouldProcess("flake-1"))
    }

    @Test
    fun shouldProcessReturnsTrueForNull() {
        val deduplicator = MessageDeduplicator()
        assertTrue(deduplicator.shouldProcess(null))
    }
}
