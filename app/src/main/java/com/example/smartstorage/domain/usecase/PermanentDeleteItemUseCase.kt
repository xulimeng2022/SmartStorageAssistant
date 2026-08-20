package com.example.smartstorage.domain.usecase

import com.example.smartstorage.domain.model.Item
import com.example.smartstorage.domain.repository.ItemRepository
import javax.inject.Inject

/**
 * 永久删除物品用例：物理删除记录并清理关联照片文件。
 */
class PermanentDeleteItemUseCase @Inject constructor(
    private val repository: ItemRepository,
) {
    suspend operator fun invoke(item: Item) {
        repository.permanentDeleteItem(item)
    }
}
