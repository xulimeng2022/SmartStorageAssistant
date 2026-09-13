package com.example.smartstorage.data.remote.vision

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ImageIndexMissingFileRecoveryTest {

    @Test
    fun claimedMissingFileLeavesQueuedStateExactlyOnce() = runBlocking {
        var status = "PENDING"
        var claimCount = 0
        var markFailedCount = 0

        val handled = failMissingImageFile(
            claimProcessing = {
                claimCount++
                if (status == "PENDING" || status == "OUTDATED") {
                    status = "PROCESSING"
                    true
                } else {
                    false
                }
            },
            markFailed = {
                markFailedCount++
                if (status == "PROCESSING") {
                    status = "FAILED"
                    true
                } else {
                    false
                }
            },
        )

        assertTrue(handled)
        assertEquals("FAILED", status)
        assertFalse(status in setOf("PENDING", "OUTDATED"))
        assertEquals(1, claimCount)
        assertEquals(1, markFailedCount)
    }

    @Test
    fun concurrentClaimFailureExitsWithoutMarkingFailed() = runBlocking {
        var markFailedCount = 0

        val handled = failMissingImageFile(
            claimProcessing = { false },
            markFailed = { markFailedCount++; true },
        )

        assertFalse(handled)
        assertEquals(0, markFailedCount)
    }
}
