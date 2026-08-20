package com.example.smartstorage.domain.usecase

import com.example.smartstorage.domain.model.Item
import com.example.smartstorage.domain.repository.ItemRepository
import javax.inject.Inject

/**
 * 新增物品用例。
 */
class AddItemUseCase @Inject constructor(
    private val repository: ItemRepository,
) {
    /** @return 新记录的自增 ID */
    suspend operator fun invoke(item: Item): Long = repository.addItem(item)
}