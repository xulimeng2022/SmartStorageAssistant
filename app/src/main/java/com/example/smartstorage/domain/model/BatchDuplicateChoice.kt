package com.example.smartstorage.domain.model

/** 批量添加时，遇到同名旧记录的用户选择。 */
enum class BatchDuplicateChoice {
    /** 更新旧记录：用本次解析内容覆盖旧记录（保留其照片，按需追加新照片） */
    UPDATE_EXISTING,

    /** 新建记录：创建一条同名新记录（可携带本次照片） */
    INSERT_NEW,

    /** 跳过此物品：不处理 */
    SKIP,
}