package com.example.smartstorage.data.repository

/**
 * 把去掉空白后的关键词转成逐字模糊 LIKE 模式，并转义 % _ \（配合 SQL 的 ESCAPE '\'）。
 *
 * 例如：“红色 口红”与“红色口红”都会归一化为 %红%色%口%红%，从而命中“红色的口红”。
 */
internal fun buildFuzzyLikePattern(normalized: String): String {
    val escaped = normalized.map { c ->
        when (c) {
            '\\' -> "\\\\"
            '%' -> "\\%"
            '_' -> "\\_"
            else -> c.toString()
        }
    }
    return "%" + escaped.joinToString("%") + "%"
}