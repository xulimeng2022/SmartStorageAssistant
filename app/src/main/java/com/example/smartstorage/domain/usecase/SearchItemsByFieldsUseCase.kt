package com.example.smartstorage.domain.usecase

import com.example.smartstorage.domain.model.Item
import com.example.smartstorage.domain.repository.ItemRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * 按名称/地点/描述三字段精确筛选物品用例（空字段不限制，AND 组合）。
 */
class SearchItemsByFieldsUseCase @Inject constructor(
    private val repository: ItemRepository,
) {
    operator fun invoke(name: String, location: String, description: String): Flow<List<Item>> =
        repository.searchByFields(name, location, description)
}
