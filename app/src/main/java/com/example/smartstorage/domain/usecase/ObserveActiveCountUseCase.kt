package com.example.smartstorage.domain.usecase

import com.example.smartstorage.domain.repository.ItemRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * 观察正常物品总数用例（用于首页区分“从未添加”与“搜索无结果”）。
 */
class ObserveActiveCountUseCase @Inject constructor(
    private val repository: ItemRepository,
) {
    operator fun invoke(): Flow<Int> = repository.observeActiveCount()
}