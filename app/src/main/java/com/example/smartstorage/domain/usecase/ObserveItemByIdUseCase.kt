package com.example.smartstorage.domain.usecase

import com.example.smartstorage.domain.model.Item
import com.example.smartstorage.domain.repository.ItemRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * 按 ID 观察单个物品用例。
 */
class ObserveItemByIdUseCase @Inject constructor(
    private val repository: ItemRepository,
) {
    operator fun invoke(id: Long): Flow<Item?> = repository.observeItemById(id)
}