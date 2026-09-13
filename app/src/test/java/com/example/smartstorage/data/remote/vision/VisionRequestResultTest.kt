package com.example.smartstorage.data.remote.vision

import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class VisionRequestResultTest {

    @Test
    fun cancellationExceptionIsRethrown() {
        val cancellation = CancellationException("cancelled")

        val thrown = try {
            runBlocking {
                runVisionRequest<Unit> { throw cancellation }
            }
            null
        } catch (error: CancellationException) {
            error
        }

        assertSame(cancellation, thrown)
    }

    @Test
    fun ordinaryExceptionBecomesFailure() {
        val error = IOException("network error")

        val result = runBlocking {
            runVisionRequest<Unit> { throw error }
        }

        assertTrue(result.isFailure)
        assertSame(error, result.exceptionOrNull())
    }
}
