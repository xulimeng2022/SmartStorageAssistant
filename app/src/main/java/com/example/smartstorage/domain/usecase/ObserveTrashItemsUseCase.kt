package com.example.smartstorage.domain.usecase

import com.example.smartstorage.domain.model.Item
import com.example.smartstorage.domain.repository.ItemRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * 观察回收站物品（已软删除）用例。
 */
class ObserveTrashItemsUseCase @Inject constructor(
    private val repository: ItemRepository,
) {
    operator fun invoke(): Flow<List<Item>> = repository.observeTrash()
}
