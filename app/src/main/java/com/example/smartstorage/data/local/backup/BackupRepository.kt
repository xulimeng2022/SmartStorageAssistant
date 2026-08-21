package com.example.smartstorage.data.local.backup

import android.content.Context
import android.net.Uri
import com.example.smartstorage.data.local.dao.ItemDao
import com.example.smartstorage.data.local.entity.ItemEntity
import com.example.smartstorage.data.local.prefs.ThemeRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 数据备份与恢复仓库：
 * - 导出：把全部物品（含回收站）与图片打包为 ZIP（data.json + images/），写入用户选择的 Uri
 * - 导入：解析 ZIP，支持覆盖（清空后导入）与合并（按名称去重）两种模式
 * - 同时备份 / 恢复全局文字颜色配置
 *
 * 所有操作在 Dispatchers.IO 后台线程执行，UI 层通过 Result 回调展示结果。
 */
@Singleton
class BackupRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val itemDao: ItemDao,
    private val themeRepository: ThemeRepository,
) {

    /** 导出全部数据到指定 Uri（ZIP 包：data.json + images/） */
    suspend fun exportData(uri: Uri): Result<ExportResult> = withContext(Dispatchers.IO) {
        runCatching {
            val items = itemDao.getAllItems()
            // 原图片绝对路径 → ZIP 内相对路径（重命名避免冲突）
            val imageEntryNames = buildImageEntryNames(items)
            val json = buildDataJson(items, imageEntryNames)

            val outputStream = context.contentResolver.openOutputStream(uri)
                ?: throw IllegalStateException("无法打开保存位置")
            outputStream.use { os ->
                ZipOutputStream(BufferedOutputStream(os)).use { zip ->
                    // 1. data.json
                    zip.putNextEntry(ZipEntry("data.json"))
                    zip.write(json.toByteArray(Charsets.UTF_8))
                    zip.closeEntry()
                    // 2. images/ 目录下的图片文件
                    imageEntryNames.forEach { (originalPath, entryName) ->
                        val file = File(originalPath)
                        if (file.exists()) {
                            zip.putNextEntry(ZipEntry(entryName))
                            file.inputStream().use { it.copyTo(zip) }
                            zip.closeEntry()
                        }
                    }
                }
            }
            ExportResult(items.size)
        }
    }

    /** 解析备份文件信息（用于导入确认对话框展示导出时间与物品数量） */
    suspend fun parseBackupInfo(uri: Uri): Result<BackupInfo> = withContext(Dispatchers.IO) {
        runCatching {
            val root = JSONObject(readDataJson(uri))
            val version = root.optString("version", "")
            // 版本兼容性校验：备份版本不能高于当前支持版本
            if (version != BACKUP_VERSION) {
                throw IllegalArgumentException("备份文件版本不兼容（当前支持 $BACKUP_VERSION）")
            }
            val items = root.optJSONArray("items") ?: JSONArray()
            BackupInfo(
                version = version,
                exportTime = root.optLong("exportTime", 0L),
                itemCount = items.length(),
            )
        }
    }

    /** 导入备份数据（覆盖 / 合并） */
    suspend fun importData(uri: Uri, mode: ImportMode): Result<ImportResult> =
        withContext(Dispatchers.IO) {
            runCatching {
                // 1. 读取 ZIP：data.json 文本 + images 条目字节
                val (json, imageEntries) = readZipEntries(uri)
                val root = JSONObject(json)
                val version = root.optString("version", "")
                if (version != BACKUP_VERSION) {
                    throw IllegalArgumentException("备份文件版本不兼容（当前支持 $BACKUP_VERSION）")
                }
                val itemsJson = root.optJSONArray("items") ?: JSONArray()
                val backupItems = (0 until itemsJson.length()).map { parseBackupItem(itemsJson.getJSONObject(it)) }

                // 2. 覆盖模式：清空数据库并删除全部图片文件
                if (mode == ImportMode.OVERWRITE) {
                    itemDao.getAllItems().forEach { item ->
                        item.imagePaths.forEach { path ->
                            runCatching { File(path).delete() }
                        }
                    }
                    itemDao.deleteAllItems()
                }

                // 3. 解压 images/ 到应用私有目录，得到 ZIP 相对路径 → 新本地绝对路径 映射
                val localImageDir = File(context.filesDir, "item_images").apply { mkdirs() }
                val imagePathMap = mutableMapOf<String, String>()
                imageEntries.forEach { (entryName, bytes) ->
                    val dest = uniqueImageFile(localImageDir, entryName.substringAfterLast('/'))
                    dest.writeBytes(bytes)
                    imagePathMap[entryName] = dest.absolutePath
                }

                // 4. 逐条导入：合并模式按名称去重（忽略大小写，含回收站）
                var imported = 0
                var skipped = 0
                var failed = 0
                backupItems.forEach { backupItem ->
                    try {
                        if (mode == ImportMode.MERGE &&
                            itemDao.findByNameIgnoreCase(backupItem.name) != null
                        ) {
                            skipped++
                            return@forEach
                        }
                        val newPaths = backupItem.imagePaths.mapNotNull { imagePathMap[it] }
                        itemDao.insert(
                            ItemEntity(
                                // 覆盖模式保留原 ID；合并模式自增，避免与现有记录冲突
                                id = if (mode == ImportMode.OVERWRITE) backupItem.id else 0L,
                                name = backupItem.name,
                                location = backupItem.location,
                                description = backupItem.description,
                                imagePaths = newPaths,
                                createdAt = backupItem.createTime,
                                updatedAt = backupItem.updateTime,
                                deletedAt = backupItem.deletedAt,
                            ),
                        )
                        imported++
                    } catch (e: Exception) {
                        failed++
                    }
                }

                // 5. 恢复全局文字颜色配置（备份中包含时）
                textColorConfigFromJson(root.optString("textColorConfig", ""))?.let { config ->
                    themeRepository.saveTextColorConfig(config)
                }

                ImportResult(
                    totalItems = backupItems.size,
                    importedItems = imported,
                    skippedItems = skipped,
                    failedItems = failed,
                )
            }
        }

    /** 生成 原图片路径 → ZIP 内相对路径 的映射（images/item_<id>_<序号>_<哈希>.扩展名） */
    private fun buildImageEntryNames(items: List<ItemEntity>): Map<String, String> {
        val map = LinkedHashMap<String, String>()
        items.forEach { item ->
            item.imagePaths.forEachIndexed { index, path ->
                val ext = File(path).extension.ifBlank { "jpg" }
                // hashCode 可能为负，取绝对值保证文件名合法
                val hash = (path.hashCode() and Int.MAX_VALUE)
                map[path] = "images/item_${item.id}_${index}_$hash.$ext"
            }
        }
        return map
    }

    /** 构建 data.json 文本 */
    private fun buildDataJson(
        items: List<ItemEntity>,
        imageEntryNames: Map<String, String>,
    ): String {
        val itemsJson = JSONArray()
        items.forEach { item ->
            val imageJson = JSONArray()
            item.imagePaths.forEach { path ->
                imageEntryNames[path]?.let { imageJson.put(it) }
            }
            itemsJson.put(
                JSONObject().apply {
                    put("id", item.id)
                    put("name", item.name)
                    put("location", item.location)
                    put("description", item.description)
                    put("imagePaths", imageJson)
                    put("createTime", item.createdAt)
                    put("updateTime", item.updatedAt)
                    // JSONObject 对 null 会移除键，用 JSONObject.NULL 保留空值语义
                    put("deletedAt", item.deletedAt ?: JSONObject.NULL)
                },
            )
        }
        return JSONObject().apply {
            put("version", BACKUP_VERSION)
            put("exportTime", System.currentTimeMillis())
            // 备份当前全局文字颜色配置，便于导入后恢复外观
            put("textColorConfig", textColorConfigToJson(themeRepository.textColorConfig.value))
            put("items", itemsJson)
        }.toString()
    }

    /** 解析单条备份物品 */
    private fun parseBackupItem(obj: JSONObject): BackupItem {
        val imageJson = obj.optJSONArray("imagePaths") ?: JSONArray()
        val paths = (0 until imageJson.length()).map { imageJson.getString(it) }
        return BackupItem(
            id = obj.optLong("id", 0L),
            name = obj.optString("name", ""),
            location = obj.optString("location", ""),
            description = obj.optString("description", ""),
            imagePaths = paths,
            createTime = obj.optLong("createTime", System.currentTimeMillis()),
            updateTime = obj.optLong("updateTime", System.currentTimeMillis()),
            deletedAt = if (obj.isNull("deletedAt")) null else obj.optLong("deletedAt"),
        )
    }

    /** 只读取 ZIP 中的 data.json 文本（预览用，避免加载全部图片） */
    private fun readDataJson(uri: Uri): String {
        val inputStream = context.contentResolver.openInputStream(uri)
            ?: throw IllegalStateException("无法读取备份文件")
        inputStream.use { is0 ->
            ZipInputStream(BufferedInputStream(is0)).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    if (entry.name == "data.json") {
                        return zip.readBytes().toString(Charsets.UTF_8)
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }
        }
        throw IllegalArgumentException("备份文件缺少 data.json")
    }

    /** 读取 ZIP 全部内容：data.json 文本 + images/ 条目字节 */
    private fun readZipEntries(uri: Uri): Pair<String, Map<String, ByteArray>> {
        val inputStream = context.contentResolver.openInputStream(uri)
            ?: throw IllegalStateException("无法读取备份文件")
        var dataJson: String? = null
        val images = LinkedHashMap<String, ByteArray>()
        inputStream.use { is0 ->
            ZipInputStream(BufferedInputStream(is0)).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    when {
                        entry.name == "data.json" ->
                            dataJson = zip.readBytes().toString(Charsets.UTF_8)
                        entry.name.startsWith("images/") && !entry.isDirectory ->
                            images[entry.name] = zip.readBytes()
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }
        }
        val json = dataJson ?: throw IllegalArgumentException("备份文件缺少 data.json")
        return json to images
    }

    /** 生成不冲突的目标图片文件（同名时追加序号） */
    private fun uniqueImageFile(dir: File, fileName: String): File {
        val safeName = fileName.substringAfterLast('/').ifBlank { "item.jpg" }
        var candidate = File(dir, safeName)
        if (!candidate.exists()) return candidate
        val base = safeName.substringBeforeLast('.')
        val ext = safeName.substringAfterLast('.', "").ifBlank { "jpg" }
        var i = 1
        while (candidate.exists()) {
            candidate = File(dir, "${base}_$i.$ext")
            i++
        }
        return candidate
    }
}
