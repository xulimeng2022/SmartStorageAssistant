package com.example.smartstorage.domain.usecase

import com.example.smartstorage.domain.repository.ItemRepository
import javax.inject.Inject

/**
 * 清空回收站用例：批量物理删除所有已删除记录及其照片文件。
 */
class EmptyTrashUseCase @Inject constructor(
    private val repository: ItemRepository,
) {
    suspend operator fun invoke() {
        repository.emptyTrash()
    }
}
