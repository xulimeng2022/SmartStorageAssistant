package com.example.smartstorage.data.local.image

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/** 托管图片目录清理的 JVM 回归测试。 */
class ImageStorageCleanupTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun deletesFileTreeInsideManagedDirectory() {
        val managedDir = temporaryFolder.newFolder("item_images")
        val nestedDir = File(managedDir, "nested").apply { mkdirs() }
        val image = File(nestedDir, "item.jpg").apply { writeText("image") }

        val cleared = deleteDirectoryContents(managedDir)

        assertTrue(cleared)
        assertTrue(managedDir.exists())
        assertFalse(nestedDir.exists())
        assertFalse(image.exists())
    }

    @Test
    fun missingManagedDirectoryIsAlreadyClear() {
        assertTrue(deleteDirectoryContents(File(temporaryFolder.root, "missing")))
    }
}
