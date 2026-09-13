package com.example.smartstorage.data.local.backup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/** 导入图片清理与复制中断残片的纯逻辑回归测试。 */
class BackupImportCleanupTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun committedImageReferencedByDatabaseSurvivesParentCancellation() {
        val committedImage = File(temporaryFolder.root, "committed.jpg")

        // 父协程取消时，事务可能已经提交；数据库引用存在时不得把新文件当失败产物删除。
        val filesToDelete = importFilesToDelete(
            newFiles = listOf(committedImage),
            referencedPaths = setOf(committedImage.absolutePath),
        )

        assertTrue(filesToDelete.isEmpty())
    }

    @Test
    fun mergeDeletesSkippedImageButKeepsSharedImage() {
        val sharedImage = File(temporaryFolder.root, "shared.jpg")
        val skippedOnlyImage = File(temporaryFolder.root, "skipped-only.jpg")

        val filesToDelete = importFilesToDelete(
            newFiles = listOf(sharedImage, skippedOnlyImage),
            referencedPaths = setOf(sharedImage.absolutePath),
        )

        assertEquals(listOf(skippedOnlyImage), filesToDelete)
    }

    @Test
    fun unverifiableDatabaseReferencesAreKept() {
        val candidate = File(temporaryFolder.root, "unknown.jpg")

        val filesToDelete = importFilesToDelete(
            newFiles = listOf(candidate),
            referencedPaths = null,
        )

        assertTrue(filesToDelete.isEmpty())
    }

    @Test
    fun interruptedCopyTracksPartialFileAndPreservesOriginalError() {
        val destination = File(temporaryFolder.root, "partial.jpg")
        val trackedFiles = mutableListOf<File>()
        val originalError = IllegalStateException("copy interrupted")

        try {
            trackImportCopy(destination, trackedFiles) {
                destination.writeText("partial")
                throw originalError
            }
            fail("复制异常必须原样抛出")
        } catch (error: Exception) {
            assertSame(originalError, error)
        }

        assertTrue(destination in trackedFiles)
        assertTrue(importFilesToDelete(trackedFiles, emptySet()).contains(destination))
        assertTrue(destination.delete())
    }
}
