package com.example.smartstorage.data.local.backup

import com.example.smartstorage.data.local.prefs.GradientDirection
import com.example.smartstorage.data.local.prefs.TextColorConfig
import com.example.smartstorage.data.local.prefs.TextColorType
import org.json.JSONObject

/** 备份文件当前版本号（与 data.json 格式对应，后续兼容性判断用） */
const val BACKUP_VERSION = "1.0"

/** 备份数据：data.json 根对象 */
data class BackupData(
    val version: String,
    val exportTime: Long,
    val items: List<BackupItem>,
    /** 全局文字颜色配置（JSON 字符串），用于换机 / 重装后恢复外观 */
    val textColorConfigJson: String? = null,
)

/** 备份中的单条物品数据 */
data class BackupItem(
    val id: Long,
    val name: String,
    val location: String,
    val description: String,
    /** 图片在 ZIP 包内的相对路径（images/xxx.jpg） */
    val imagePaths: List<String>,
    val createTime: Long,
    val updateTime: Long,
    /** 非 null 表示导出时在回收站 */
    val deletedAt: Long?,
)

/** 备份文件预览信息（导入确认对话框展示） */
data class BackupInfo(
    val version: String,
    val exportTime: Long,
    val itemCount: Int,
)

/** 导入模式：覆盖（清空后导入）/ 合并（按名称去重） */
enum class ImportMode {
    OVERWRITE,
    MERGE,
}

/** 导入结果统计 */
data class ImportResult(
    val totalItems: Int,
    val importedItems: Int,
    val skippedItems: Int,
    val failedItems: Int,
)

/** 导出结果 */
data class ExportResult(val itemCount: Int)

/** 全局文字颜色配置 → JSON 字符串（写入备份 data.json） */
fun textColorConfigToJson(config: TextColorConfig): String = JSONObject().apply {
    put("type", config.type.name)
    put("solidColor", config.solidColor)
    put("gradientStart", config.gradientStart)
    put("gradientEnd", config.gradientEnd)
    put("direction", config.direction.name)
}.toString()

/** 从 JSON 字符串恢复全局文字颜色配置（解析失败返回 null，导入时忽略外观恢复） */
fun textColorConfigFromJson(json: String?): TextColorConfig? {
    if (json.isNullOrBlank()) return null
    return runCatching {
        val obj = JSONObject(json)
        TextColorConfig(
            type = TextColorType.entries.firstOrNull { it.name == obj.optString("type") }
                ?: TextColorType.DEFAULT,
            solidColor = obj.optLong("solidColor", 0xFF111111L),
            gradientStart = obj.optLong("gradientStart", 0xFF111111L),
            gradientEnd = obj.optLong("gradientEnd", 0xFF1976D2L),
            direction = GradientDirection.entries.firstOrNull { it.name == obj.optString("direction") }
                ?: GradientDirection.HORIZONTAL,
        )
    }.getOrNull()
}
