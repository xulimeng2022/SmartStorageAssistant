package com.example.smartstorage.presentation.home

import com.example.smartstorage.data.local.prefs.ImageUnderstandingState
import com.example.smartstorage.data.local.prefs.VisionCapabilityStatus
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeVisualVerificationPolicyTest {

    @Test
    fun networkVerificationRequiresEnabledAndSupportedCapability() {
        assertTrue(
            canVerifyWithImageUnderstanding(
                ImageUnderstandingState(
                    enabled = true,
                    capability = VisionCapabilityStatus.SUPPORTED,
                ),
            ),
        )
        assertFalse(
            canVerifyWithImageUnderstanding(
                ImageUnderstandingState(
                    enabled = false,
                    capability = VisionCapabilityStatus.SUPPORTED,
                ),
            ),
        )
        assertFalse(
            canVerifyWithImageUnderstanding(
                ImageUnderstandingState(
                    enabled = true,
                    capability = VisionCapabilityStatus.UNSUPPORTED,
                ),
            ),
        )
    }
}
