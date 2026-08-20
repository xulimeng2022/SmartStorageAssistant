package com.example.smartstorage.data.local.converters

import androidx.room.TypeConverter
import org.json.JSONArray

/**
 * Room 类型转换：List<String>（多照片路径）↔ JSON 数组字符串。
 * 兼容旧版单路径存储：非 JSON 字符串按单元素列表解析。
 */
class Converters {

    @TypeConverter
    fun fromImagePaths(paths: List<String>?): String =
        if (paths.isNullOrEmpty()) "[]" else JSONArray(paths).toString()

    @TypeConverter
    fun toImagePaths(json: String?): List<String> {
        if (json.isNullOrBlank()) return emptyList()
        return runCatching {
            val arr = JSONArray(json)
            (0 until arr.length()).map { arr.getString(it) }
        }.getOrElse {
            // 旧数据：单个路径字符串 → 单元素列表
            listOf(json)
        }
    }
}
