package com.example.smartstorage.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

/** BatchDraftOps 纯函数操作的单元测试：按 uid 编辑/删除、照片归属切换与路径剥离。 */
class BatchDraftOpsTest {

    private fun d(
        uid: Long,
        name: String,
        location: String = "柜子里",
        photos: List<String> = emptyList(),
    ) = BatchDraftItem(uid = uid, name = name, location = location, description = "", photoPaths = photos)

    @Test
    fun updateOnlyAffectsTargetUid() {
        val items = listOf(d(1, "雨衣"), d(2, "拖鞋"))
        val result = BatchDraftOps.update(items, 2, "毛拖鞋", "抽屉里", "新备注")
        assertEquals("雨衣", result[0].name)
        assertEquals("毛拖鞋", result[1].name)
        assertEquals("抽屉里", result[1].location)
        assertEquals("新备注", result[1].description)
        // 未命中 uid 时原样返回
        assertEquals(items, BatchDraftOps.update(items, 99, "x", "y", "z"))
    }

    @Test
    fun removeKeepsOthersAndTheirPhotos() {
        val items = listOf(
            d(1, "雨衣", photos = listOf("/p/a.jpg")),
            d(2, "拖鞋", photos = listOf("/p/a.jpg", "/p/b.jpg")),
            d(3, "夹子", photos = listOf("/p/c.jpg")),
        )
        val result = BatchDraftOps.remove(items, 2)
        assertEquals(listOf(1L, 3L), result.map { it.uid })
        // 删除条目 2 不影响 1/3 的照片归属（照片仍在池中，可再分配给其它条目）
        assertEquals(listOf("/p/a.jpg"), result[0].photoPaths)
        assertEquals(listOf("/p/c.jpg"), result[1].photoPaths)
        // 删除不存在的 uid 返回原列表
        assertEquals(items, BatchDraftOps.remove(items, 99))
    }

    @Test
    fun setPhotoAddsAndRemovesForSingleUid() {
        val items = listOf(d(1, "雨衣"), d(2, "拖鞋", photos = listOf("/p/a.jpg")))
        // 添加
        val added = BatchDraftOps.setPhoto(items, 1, "/p/a.jpg", true)
        assertEquals(listOf("/p/a.jpg"), added[0].photoPaths)
        // 重复添加幂等
        val again = BatchDraftOps.setPhoto(added, 1, "/p/a.jpg", true)
        assertEquals(listOf("/p/a.jpg"), again[0].photoPaths)
        // 不影响其它条目
        assertEquals(listOf("/p/a.jpg"), again[1].photoPaths)
        // 取消分配只影响目标条目
        val removed = BatchDraftOps.setPhoto(again, 1, "/p/a.jpg", false)
        assertEquals(emptyList<String>(), removed[0].photoPaths)
        assertEquals(listOf("/p/a.jpg"), removed[1].photoPaths)
    }

    @Test
    fun detachPathStripsAllDraftsReferencingThatPath() {
        val items = listOf(
            d(1, "雨衣", photos = listOf("/p/a.jpg", "/p/b.jpg")),
            d(2, "拖鞋", photos = listOf("/p/a.jpg")),
            d(3, "夹子", photos = listOf("/p/c.jpg")),
        )
        val result = BatchDraftOps.detachPath(items, "/p/a.jpg")
        assertEquals(listOf("/p/b.jpg"), result[0].photoPaths)
        assertEquals(emptyList<String>(), result[1].photoPaths)
        assertEquals(listOf("/p/c.jpg"), result[2].photoPaths)
    }
}