package com.example.smartstorage.presentation.detail

import org.junit.Assert.assertEquals
import org.junit.Test

class PhotoIndexUiStateTest {

    @Test
    fun everyPhotoIndexStatusMapsToSafeAction() {
        val expected = mapOf(
            PhotoIndexStatus.NOT_CREATED to PhotoIndexAction.CREATE_OR_RETRY,
            PhotoIndexStatus.PENDING to PhotoIndexAction.NONE,
            PhotoIndexStatus.PROCESSING to PhotoIndexAction.NONE,
            PhotoIndexStatus.SUCCESS to PhotoIndexAction.DELETE,
            PhotoIndexStatus.FAILED to PhotoIndexAction.CREATE_OR_RETRY,
            PhotoIndexStatus.OUTDATED to PhotoIndexAction.REFRESH,
        )

        PhotoIndexStatus.entries.forEach { status ->
            assertEquals(status.name, expected.getValue(status), status.toPhotoIndexAction())
        }
    }
}
