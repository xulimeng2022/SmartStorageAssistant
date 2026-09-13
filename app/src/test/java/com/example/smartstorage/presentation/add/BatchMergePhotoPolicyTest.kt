package com.example.smartstorage.presentation.add

import com.example.smartstorage.domain.model.BatchDraftItem
import org.junit.Assert.assertEquals
import org.junit.Test

/** 批量合并候选照片策略的单元测试：候选范围、默认选择与路径去重规则。 */
class BatchMergePhotoPolicyTest {

    private fun draft(uid: Long, vararg photos: String) = BatchDraftItem(
        uid = uid,
        name = "物品$uid",
        location = "",
        description = "",
        photoPaths = photos.toList(),
    )

    private fun candidates(
        drafts: List<BatchDraftItem>,
        selectedUids: Set<Long>,
        currentImagePaths: List<String>,
    ) = resolveBatchMergePhotoCandidates(drafts, selectedUids, currentImagePaths)

    @Test
    fun allUnassignedPhotosAreAvailableButNotSelectedByDefault() {
        val pool = listOf("/data/user/0/app/files/a.jpg", "/data/user/0/app/files/b.jpg")
        val result = candidates(
            drafts = listOf(draft(1), draft(2)),
            selectedUids = setOf(1L, 2L),
            currentImagePaths = pool,
        )

        assertEquals(pool, result.availablePhotoPaths)
        assertEquals(emptyList<String>(), result.selectedPhotoPaths)
    }

    @Test
    fun selectedDraftPhotosAndUnassignedPoolPhotosAreAvailable() {
        val a = "/data/user/0/app/files/a.jpg"
        val b = "/data/user/0/app/files/b.jpg"
        val unassigned = "/data/user/0/app/files/c.jpg"
        val result = candidates(
            drafts = listOf(draft(1, a), draft(2, b)),
            selectedUids = setOf(1L, 2L),
            currentImagePaths = listOf(a, b, unassigned),
        )

        assertEquals(listOf(a, b, unassigned), result.availablePhotoPaths)
        assertEquals(listOf(a, b), result.selectedPhotoPaths)
    }

    @Test
    fun photosAssignedOnlyToUnselectedDraftsAreExcluded() {
        val selectedPhoto = "/data/user/0/app/files/a.jpg"
        val unselectedPhoto = "/data/user/0/app/files/b.jpg"
        val unassignedPhoto = "/data/user/0/app/files/c.jpg"
        val result = candidates(
            drafts = listOf(draft(1, selectedPhoto), draft(2, unselectedPhoto)),
            selectedUids = setOf(1L),
            currentImagePaths = listOf(selectedPhoto, unselectedPhoto, unassignedPhoto),
        )

        assertEquals(listOf(selectedPhoto, unassignedPhoto), result.availablePhotoPaths)
        assertEquals(listOf(selectedPhoto), result.selectedPhotoPaths)
    }

    @Test
    fun duplicateAbsolutePathsAreDeduplicated() {
        val a = "/data/user/0/app/files/a.jpg"
        val b = "/data/user/0/app/files/b.jpg"
        val result = candidates(
            drafts = listOf(draft(1, a), draft(2, a, b)),
            selectedUids = setOf(1L, 2L),
            currentImagePaths = listOf(a, a, b),
        )

        assertEquals(listOf(a, b), result.availablePhotoPaths)
        assertEquals(listOf(a, b), result.selectedPhotoPaths)
    }

    @Test
    fun sameFilenameInDifferentDirectoriesIsKept() {
        val left = "/data/user/0/app/files/left/photo.jpg"
        val right = "/data/user/0/app/files/right/photo.jpg"
        val result = candidates(
            drafts = listOf(draft(1, left), draft(2, right)),
            selectedUids = setOf(1L, 2L),
            currentImagePaths = listOf(left, right),
        )

        assertEquals(listOf(left, right), result.availablePhotoPaths)
        assertEquals(listOf(left, right), result.selectedPhotoPaths)
    }

    @Test
    fun noPhotosProducesEmptyCandidates() {
        val result = candidates(
            drafts = listOf(draft(1), draft(2)),
            selectedUids = setOf(1L, 2L),
            currentImagePaths = emptyList(),
        )

        assertEquals(emptyList<String>(), result.availablePhotoPaths)
        assertEquals(emptyList<String>(), result.selectedPhotoPaths)
    }

    @Test
    fun moreThanNineAvailablePhotosAreNotTruncated() {
        val pool = (1..12).map { "/data/user/0/app/files/$it.jpg" }
        val result = candidates(
            drafts = listOf(draft(1), draft(2)),
            selectedUids = setOf(1L, 2L),
            currentImagePaths = pool,
        )

        assertEquals(pool, result.availablePhotoPaths)
        assertEquals(emptyList<String>(), result.selectedPhotoPaths)
    }

    @Test
    fun reopeningReturnsSameCandidatesWithoutMutatingInputs() {
        val selectedPhoto = "/data/user/0/app/files/a.jpg"
        val unassignedPhoto = "/data/user/0/app/files/b.jpg"
        val drafts = listOf(draft(1, selectedPhoto), draft(2))
        val originalDrafts = drafts.toList()
        val pool = listOf(selectedPhoto, unassignedPhoto)

        val first = candidates(drafts, setOf(1L, 2L), pool)
        val second = candidates(drafts, setOf(1L, 2L), pool)

        assertEquals(first, second)
        assertEquals(originalDrafts, drafts)
        assertEquals(listOf(selectedPhoto, unassignedPhoto), pool)
    }
}
