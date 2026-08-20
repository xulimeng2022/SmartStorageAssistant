package com.example.smartstorage.domain.usecase

import com.example.smartstorage.domain.model.Item
import com.example.smartstorage.domain.repository.ItemRepository
import javax.inject.Inject

/**
 * 按名称精确查询物品（忽略大小写）用例：用于保存前的重复检测。
 */
class GetItemByNameUseCase @Inject constructor(
    private val repository: ItemRepository,
) {
    suspend operator fun invoke(name: String): Item? = repository.getItemByName(name)
}
