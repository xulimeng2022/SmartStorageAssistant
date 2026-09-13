package com.example.smartstorage.data.repository

import com.example.smartstorage.data.local.entity.ImageAiIndexEntity
import com.example.smartstorage.domain.model.ImageAnalysisStatus
import com.example.smartstorage.domain.model.Item
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** 视觉搜索必须拒绝已不属于当前物品的 stale 图片路径。 */
class VisualSearchEngineTest {

    @Test
    fun staleIndexPathIsNotReturned() {
        val item = item(paths = listOf("/images/current.jpg"))
        val staleIndex = index(path = "/images/removed.jpg")

        val matches = VisualSearchEngine.match(
            query = "杯子",
            indices = listOf(staleIndex),
            items = listOf(item),
            languageCode = "zh",
        )

        assertTrue(matches.isEmpty())
    }

    @Test
    fun currentIndexPathStillMatches() {
        val item = item(paths = listOf("/images/current.jpg"))
        val currentIndex = index(path = "/images/current.jpg")

        val matches = VisualSearchEngine.match(
            query = "杯子",
            indices = listOf(currentIndex),
            items = listOf(item),
            languageCode = "zh",
        )

        assertEquals(1, matches.size)
        assertEquals(currentIndex.imagePath, matches.single().imagePath)
    }

    private fun item(paths: List<String>) = Item(
        id = 1L,
        name = "杯子",
        imagePaths = paths,
    )

    private fun index(path: String) = ImageAiIndexEntity(
        imagePath = path,
        itemId = 1L,
        searchText = "杯子",
        status = ImageAnalysisStatus.SUCCESS.name,
    )
}
