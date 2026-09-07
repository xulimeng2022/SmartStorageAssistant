package com.example.smartstorage.domain.model

/**
 * 大模型或本地规则解析出的物品字段（识别结果，尚未入库）。
 *
 * @property name 物品名称（必填，可为空串表示未能识别）
 * @property location 存放地点（可为空串，表示等待用户补充）
 * @property description 备注/描述（可为空串）
 */
data class ParsedItem(
    val name: String,
    val location: String,
    val description: String,
)