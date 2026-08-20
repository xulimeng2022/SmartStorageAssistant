package com.example.smartstorage.domain.usecase

import com.example.smartstorage.domain.model.Item
import com.example.smartstorage.domain.repository.ItemRepository
import javax.inject.Inject

/**
 * 恢复回收站物品用例：将物品移回主列表。
 */
class RestoreItemUseCase @Inject constructor(
    private val repository: ItemRepository,
) {
    suspend operator fun invoke(item: Item) {
        repository.restoreItem(item)
    }
}
