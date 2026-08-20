package com.example.smartstorage.domain.usecase

import com.example.smartstorage.domain.model.Item
import com.example.smartstorage.domain.repository.ItemRepository
import javax.inject.Inject

/**
 * 更新物品用例。
 */
class UpdateItemUseCase @Inject constructor(
    private val repository: ItemRepository,
) {
    suspend operator fun invoke(item: Item) {
        repository.updateItem(item)
    }
}