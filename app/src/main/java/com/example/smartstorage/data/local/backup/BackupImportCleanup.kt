package com.example.smartstorage.data.local.backup

import java.io.File

/**
 * 计算本次导入后可以安全删除的新图片。
 *
 * [referencedPaths] 为 null 表示数据库引用集合无法确认，此时保守保留全部文件。
 */
internal fun importFilesToDelete(
    newFiles: List<File>,
    referencedPaths: Set<String>?,
): List<File> {
    if (referencedPaths == null) return emptyList()
    return newFiles.filterNot { it.absolutePath in referencedPaths }
}

/** 复制前先登记目标文件，确保半写残片也会进入最终清理范围。 */
internal fun trackImportCopy(
    destination: File,
    trackedFiles: MutableCollection<File>,
    copy: () -> Unit,
) {
    trackedFiles.add(destination)
    copy()
}
