package com.example.smartstorage.data.remote.update

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateVersionComparatorTest {
    @Test
    fun numericSegmentsComparedNotAsString() {
        assertTrue(UpdateVersionComparator.isNewer("1.10.0", "1.9.9") == true)
        assertFalse(UpdateVersionComparator.isNewer("1.2.0", "1.2.0") == true)
    }

    @Test
    fun vPrefixAcceptedAndPrereleaseRejected() {
        assertTrue(UpdateVersionComparator.parse("v2.0.1") != null)
        assertNull(UpdateVersionComparator.parse("v2.0.1-rc1"))
    }
}