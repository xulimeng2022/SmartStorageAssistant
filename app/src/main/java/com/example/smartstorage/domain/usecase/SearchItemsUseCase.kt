package com.example.smartstorage.domain.usecase

import com.example.smartstorage.domain.model.Item
import com.example.smartstorage.domain.repository.ItemRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * 按名称或存放地点模糊搜索物品用例。
 */
class SearchItemsUseCase @Inject constructor(
    private val repository: ItemRepository,
) {
    operator fun invoke(query: String): Flow<List<Item>> = repository.searchItems(query)
}