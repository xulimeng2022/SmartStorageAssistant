package com.example.smartstorage.data.local.reset

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** 完全删除结果的失败汇总测试。 */
class ResetClearResultTest {

    @Test
    fun failedClearResultIsReported() {
        val errors = mutableListOf<String>()

        val cleared = recordClearResults(
            results = listOf(true, false),
            label = "files",
            errors = errors,
        )

        assertFalse(cleared)
        assertEquals(listOf("files:false"), errors)
    }

    @Test
    fun allSuccessfulClearResultsAreComplete() {
        val errors = mutableListOf<String>()

        val cleared = recordClearResults(
            results = listOf(true, true),
            label = "preferences",
            errors = errors,
        )

        assertTrue(cleared)
        assertTrue(errors.isEmpty())
    }
}
