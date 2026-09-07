package com.example.smartstorage.domain.usecase

import com.example.smartstorage.domain.model.BatchDuplicateChoice
import com.example.smartstorage.domain.model.BatchDraftItem
import com.example.smartstorage.domain.model.Item
import com.example.smartstorage.domain.repository.ItemRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** BatchAddItemsUseCase 的单元测试：按 uid 逐件保存、照片逐条归属与“不共享文件”规则。 */
class BatchAddItemsUseCaseTest {

    /** 内存版假仓库：仅实现测试需要的方法。 */
    private class FakeItemRepository : ItemRepository {
        val items = mutableListOf<Item>()
        var nextId = 1L
        var failNextAdd = false

        override fun observeItems(): Flow<List<Item>> = flowOf(items.toList())
        override fun observeActiveCount(): Flow<Int> = flowOf(items.size)
        override fun observeItemById(id: Long): Flow<Item?> = flowOf(items.firstOrNull { it.id == id })
        override fun observeTrash(): Flow<List<Item>> = flowOf(emptyList())
        override fun searchItems(query: String): Flow<List<Item>> = flowOf(items.toList())
        override fun searchByFields(name: String, location: String, description: String): Flow<List<Item>> =
            flowOf(items.toList())

        override suspend fun getItemByName(name: String): Item? =
            items.lastOrNull { it.name.equals(name, ignoreCase = true) }

        override suspend fun addItem(item: Item): Long {
            if (failNextAdd) {
                // 只让第一次写入失败，验证单条失败不影响后续条目
                failNextAdd = false
                throw IllegalStateException("写入失败")
            }
            val copy = item.copy(id = nextId++)
            items += copy
            return copy.id
        }

        override suspend fun updateItem(item: Item) {
            val idx = items.indexOfFirst { it.id == item.id }
            if (idx >= 0) items[idx] = item else items += item
        }

        override suspend fun deleteItem(item: Item) = Unit
        override suspend fun restoreItem(item: Item) = Unit
        override suspend fun permanentDeleteItem(item: Item) = Unit
        override suspend fun emptyTrash() = Unit
    }

    /** 假 copyFile：每次调用生成不同的独立路径（模拟 ImageStorage 保存新文件）。 */
    private class CopyCounter {
        var count = 0
        private set
        fun copy(path: String): String {
            count++
            return "copy$count:$path"
        }
    }

    private fun useCase(repo: FakeItemRepository): BatchAddItemsUseCase =
        BatchAddItemsUseCase(
            AddItemUseCase(repo),
            UpdateItemUseCase(repo),
            GetItemByNameUseCase(repo),
        )

    private fun draft(
        uid: Long,
        name: String,
        location: String = "柜子里",
        description: String = "",
        photos: List<String> = emptyList(),
    ) = BatchDraftItem(uid = uid, name = name, location = location, description = description, photoPaths = photos)

    private fun noDuplicateResolver(
        existing: Item,
        draft: BatchDraftItem,
    ): BatchDuplicateChoice = throw AssertionError("不应触发重名弹窗：${existing.name}")

    @Test
    fun addsSelectedNewItemsWithOwnPhotos() = runBlocking {
        val repo = FakeItemRepository()
        val drafts = listOf(
            draft(uid = 1, name = "雨衣", photos = listOf("/p/a.jpg")),
            draft(uid = 2, name = "拖鞋", photos = listOf("/p/b.jpg")),
        )
        val copier = CopyCounter()
        val outcome = useCase(repo).execute(
            drafts = drafts,
            selected = setOf(1, 2),
            resolveDuplicate = ::noDuplicateResolver,
            copyFile = { copier.copy(it) },
        )
        assertEquals(2, outcome.added)
        assertEquals(0, outcome.updated)
        assertEquals(0, outcome.skipped)
        assertTrue(outcome.failedUids.isEmpty())
        assertEquals(listOf("雨衣", "拖鞋"), repo.items.map { it.name })
        // 各自照片只归属到自己
        assertEquals(listOf("/p/a.jpg"), repo.items[0].imagePaths)
        assertEquals(listOf("/p/b.jpg"), repo.items[1].imagePaths)
        // 单持有人：不触发复制，原路径直存并上报
        assertEquals(0, copier.count)
        assertEquals(listOf("/p/a.jpg", "/p/b.jpg"), outcome.consumedOriginals.sorted())
    }

    @Test
    fun sharedPhotoAcrossTwoItemsKeepsIndependentFiles() = runBlocking {
        val repo = FakeItemRepository()
        val drafts = listOf(
            draft(uid = 1, name = "雨衣", photos = listOf("/p/a.jpg")),
            draft(uid = 2, name = "拖鞋", photos = listOf("/p/a.jpg")),
        )
        val copier = CopyCounter()
        val outcome = useCase(repo).execute(
            drafts = drafts,
            selected = setOf(1, 2),
            resolveDuplicate = ::noDuplicateResolver,
            copyFile = { copier.copy(it) },
        )
        assertEquals(2, outcome.added)
        // 第一个持有原路径，第二个必须复制出独立文件
        assertEquals(listOf("/p/a.jpg"), repo.items[0].imagePaths)
        assertEquals(listOf("copy1:/p/a.jpg"), repo.items[1].imagePaths)
        assertEquals(1, copier.count)
        assertEquals(listOf("/p/a.jpg"), outcome.consumedOriginals)
        // 保证库中不存在被两条记录共享的绝对路径
        val allPaths = repo.items.flatMap { it.imagePaths }
        assertEquals(allPaths.size, allPaths.toSet().size)
    }

    @Test
    fun alreadyClaimedPathIsCopiedNotReused() = runBlocking {
        val repo = FakeItemRepository()
        val drafts = listOf(draft(uid = 7, name = "拖鞋", photos = listOf("/p/a.jpg")))
        val copier = CopyCounter()
        val outcome = useCase(repo).execute(
            drafts = drafts,
            selected = setOf(7),
            alreadyClaimedPaths = setOf("/p/a.jpg"),
            resolveDuplicate = ::noDuplicateResolver,
            copyFile = { copier.copy(it) },
        )
        assertEquals(1, outcome.added)
        assertEquals(listOf("copy1:/p/a.jpg"), repo.items.single().imagePaths)
        // 命中占用集合时不会再次“原路径直存”
        assertTrue(outcome.consumedOriginals.isEmpty())
        assertEquals(1, copier.count)
    }

    @Test
    fun partialFailureRetryKeepsFilesDistinct() = runBlocking {
        val repo = FakeItemRepository().apply { failNextAdd = true }
        val all = listOf(
            draft(uid = 1, name = "雨衣", photos = listOf("/p/a.jpg")),
            draft(uid = 2, name = "拖鞋", photos = listOf("/p/a.jpg")),
        )
        val copier = CopyCounter()
        // 第一次：雨衣写入失败（但先占用 a），拖鞋改为复制 a 成功
        val first = useCase(repo).execute(
            drafts = all,
            selected = setOf(1, 2),
            resolveDuplicate = ::noDuplicateResolver,
            copyFile = { copier.copy(it) },
        )
        assertEquals(1, first.added)
        assertEquals(listOf(1L), first.failedUids)
        // 重试：只剩雨衣，且带上第一次已占用/已直存的路径集合
        val retryDrafts = all.filter { it.uid in first.failedUids }
        val second = useCase(repo).execute(
            drafts = retryDrafts,
            selected = setOf(1),
            alreadyClaimedPaths = first.consumedOriginals.toSet(),
            resolveDuplicate = ::noDuplicateResolver,
            copyFile = { copier.copy(it) },
        )
        assertEquals(1, second.added)
        assertTrue(second.failedUids.isEmpty())
        assertEquals(2, repo.items.size)
        // 两条记录的照片绝对路径互不相同（雨衣 copy2、拖鞋 copy1）
        val paths = repo.items.flatMap { it.imagePaths }
        assertEquals(paths.size, paths.toSet().size)
    }

    @Test
    fun duplicateUpdateKeepsOldPhotosAndAppendsNewOnes() = runBlocking {
        val repo = FakeItemRepository()
        repo.items += Item(
            id = 1L,
            name = "笔",
            location = "旧位置",
            description = "旧备注",
            imagePaths = listOf("/old/1.jpg"),
        )
        val copier = CopyCounter()
        val outcome = useCase(repo).execute(
            drafts = listOf(draft(uid = 9, name = "笔", location = "新抽屉里", description = "新备注", photos = listOf("/new/a.jpg"))),
            selected = setOf(9),
            resolveDuplicate = { _, _ -> BatchDuplicateChoice.UPDATE_EXISTING },
            copyFile = { copier.copy(it) },
        )
        assertEquals(1, outcome.updated)
        val saved = repo.items.single()
        assertEquals("新抽屉里", saved.location)
        assertEquals("新备注", saved.description)
        assertEquals(listOf("/old/1.jpg", "/new/a.jpg"), saved.imagePaths)
        // 新照片首次占用：原路径直存，不上报复制
        assertEquals(listOf("/new/a.jpg"), outcome.consumedOriginals)
        assertEquals(0, copier.count)
    }

    @Test
    fun duplicateUpdateSkipsPhotoAlreadyOnOldRecord() = runBlocking {
        val repo = FakeItemRepository()
        repo.items += Item(
            id = 1L,
            name = "笔",
            location = "旧位置",
            imagePaths = listOf("/pool/a.jpg"),
        )
        val outcome = useCase(repo).execute(
            drafts = listOf(draft(uid = 9, name = "笔", photos = listOf("/pool/a.jpg"))),
            selected = setOf(9),
            resolveDuplicate = { _, _ -> BatchDuplicateChoice.UPDATE_EXISTING },
            copyFile = { "/copy/${it.substringAfterLast('/')}" },
        )
        assertEquals(1, outcome.updated)
        // 旧记录已含该路径：不重复追加，也不上报“原路径直存”
        assertEquals(listOf("/pool/a.jpg"), repo.items.single().imagePaths)
        assertTrue(outcome.consumedOriginals.isEmpty())
    }
    @Test
    fun duplicateUpdateRespectsNinePhotoLimit() = runBlocking {
        val repo = FakeItemRepository()
        val oldNine = (1..9).map { "/old/$it.jpg" }
        repo.items += Item(id = 1L, name = "笔", location = "旧位置", imagePaths = oldNine)
        val outcome = useCase(repo).execute(
            drafts = listOf(draft(uid = 9, name = "笔", photos = listOf("/new/a.jpg"))),
            selected = setOf(9),
            resolveDuplicate = { _, _ -> BatchDuplicateChoice.UPDATE_EXISTING },
            copyFile = { "/copy/${it.substringAfterLast('/')}" },
        )
        assertEquals(1, outcome.updated)
        // 已满 9 张时不再追加，避免超出上限
        assertEquals(oldNine, repo.items.single().imagePaths)
    }

    @Test
    fun duplicateInsertNewCreatesRecordWithPhotos() = runBlocking {
        val repo = FakeItemRepository()
        repo.items += Item(id = 1L, name = "笔", location = "旧位置")
        val copier = CopyCounter()
        val outcome = useCase(repo).execute(
            drafts = listOf(draft(uid = 5, name = "笔", location = "新抽屉里", photos = listOf("/new/a.jpg"))),
            selected = setOf(5),
            resolveDuplicate = { _, _ -> BatchDuplicateChoice.INSERT_NEW },
            copyFile = { copier.copy(it) },
        )
        assertEquals(1, outcome.added)
        assertEquals(2, repo.items.size)
        val newRecord = repo.items.last()
        assertEquals(listOf("/new/a.jpg"), newRecord.imagePaths)
        assertEquals("新抽屉里", newRecord.location)
        // 旧记录不受影响
        assertEquals(emptyList<String>(), repo.items.first().imagePaths)
    }

    @Test
    fun duplicateSkipCountsSkipped() = runBlocking {
        val repo = FakeItemRepository()
        repo.items += Item(id = 1L, name = "笔", location = "旧位置")
        val outcome = useCase(repo).execute(
            drafts = listOf(draft(uid = 5, name = "笔", location = "新抽屉里")),
            selected = setOf(5),
            resolveDuplicate = { _, _ -> BatchDuplicateChoice.SKIP },
            copyFile = { it },
        )
        assertEquals(1, outcome.skipped)
        assertEquals(1, repo.items.size)
        assertEquals("旧位置", repo.items.single().location)
    }

    @Test
    fun blankNameGoesToFailed() = runBlocking {
        val repo = FakeItemRepository()
        val drafts = listOf(
            draft(uid = 1, name = "", photos = listOf("/p/a.jpg")),
            draft(uid = 2, name = "雨衣", photos = listOf("/p/b.jpg")),
        )
        val outcome = useCase(repo).execute(
            drafts = drafts,
            selected = setOf(1, 2),
            resolveDuplicate = ::noDuplicateResolver,
            copyFile = { it },
        )
        assertEquals(1, outcome.added)
        assertEquals(listOf(1L), outcome.failedUids)
        assertEquals(listOf("雨衣"), repo.items.map { it.name })
        // 空名称条目不写库，其照片也不落库
        assertEquals(listOf("/p/b.jpg"), repo.items.single().imagePaths)
    }

    @Test
    fun addFailureIsIsolatedAndRecorded() = runBlocking {
        val repo = FakeItemRepository().apply { failNextAdd = true }
        val drafts = listOf(
            draft(uid = 1, name = "雨衣"),
            draft(uid = 2, name = "拖鞋"),
        )
        val outcome = useCase(repo).execute(
            drafts = drafts,
            selected = setOf(1, 2),
            resolveDuplicate = ::noDuplicateResolver,
            copyFile = { it },
        )
        assertEquals(1, outcome.added)
        assertEquals(listOf(1L), outcome.failedUids)
        // 第 1 条失败不影响第 2 条保存
        assertEquals(listOf("拖鞋"), repo.items.map { it.name })
    }

    @Test
    fun unselectedDraftIsNotSaved() = runBlocking {
        val repo = FakeItemRepository()
        val drafts = listOf(
            draft(uid = 1, name = "雨衣", photos = listOf("/p/a.jpg")),
            draft(uid = 2, name = "拖鞋", photos = listOf("/p/b.jpg")),
        )
        val outcome = useCase(repo).execute(
            drafts = drafts,
            selected = setOf(1),
            resolveDuplicate = ::noDuplicateResolver,
            copyFile = { it },
        )
        assertEquals(1, outcome.added)
        assertEquals(listOf("雨衣"), repo.items.map { it.name })
        assertEquals(listOf("/p/a.jpg"), outcome.consumedOriginals)
    }
}
