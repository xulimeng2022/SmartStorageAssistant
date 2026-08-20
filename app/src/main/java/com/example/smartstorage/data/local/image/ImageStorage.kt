package com.example.smartstorage.data.local.image

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 物品照片存储：负责压缩保存（1080px 宽 / JPEG 80%）、EXIF 方向修正与删除。
 *
 * 图片保存在应用私有目录 filesDir/item_images/，命名 item_时间戳.jpg。
 */
@Singleton
class ImageStorage @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    private val imageDir: File by lazy {
        File(context.filesDir, "item_images").apply { mkdirs() }
    }

    /** 从 content Uri 保存图片，返回本地绝对路径。 */
    fun saveFromUri(uri: Uri): String {
        val tmp = File(context.cacheDir, "tmp_${System.currentTimeMillis()}.jpg")
        context.contentResolver.openInputStream(uri).use { input ->
            checkNotNull(input) { "无法读取所选图片" }
            FileOutputStream(tmp).use { out -> input.copyTo(out) }
        }
        return saveFromFile(tmp)
    }

    /** 从文件（如拍照临时文件）压缩保存，返回本地绝对路径。 */
    fun saveFromFile(srcFile: File): String {
        val rotation = readRotation(srcFile)
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(srcFile.absolutePath, bounds)
        val sample = calculateInSampleSize(bounds.outWidth, bounds.outHeight)
        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        val bitmap = BitmapFactory.decodeFile(srcFile.absolutePath, options)
            ?: throw IllegalStateException("图片解码失败")

        // 缩放至目标宽度
        val scaled = scaleToWidth(bitmap)
        if (scaled !== bitmap) bitmap.recycle()

        // 按 EXIF 方向旋转
        val oriented = if (rotation != 0) rotate(scaled, rotation) else scaled
        if (oriented !== scaled) scaled.recycle()

        val dest = File(imageDir, "item_${System.currentTimeMillis()}.jpg")
        FileOutputStream(dest).use { out ->
            oriented.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
        }
        oriented.recycle()

        // 清理缓存中的临时文件
        if (srcFile.absolutePath.startsWith(context.cacheDir.absolutePath)) {
            srcFile.delete()
        }
        return dest.absolutePath
    }

    /** 删除图片文件（路径为空时忽略）。 */
    fun deleteImage(path: String?) {
        if (path.isNullOrBlank()) return
        runCatching { File(path).delete() }
    }

    /** 读取 EXIF 旋转角度（拍照图片常见）。 */
    private fun readRotation(file: File): Int = runCatching {
        val exif = ExifInterface(file.absolutePath)
        when (exif.getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL,
        )) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90
            ExifInterface.ORIENTATION_ROTATE_180 -> 180
            ExifInterface.ORIENTATION_ROTATE_270 -> 270
            else -> 0
        }
    }.getOrDefault(0)

    /** 按目标宽度计算采样率，避免内存峰值。 */
    private fun calculateInSampleSize(width: Int, height: Int): Int {
        var sample = 1
        if (width > TARGET_WIDTH) {
            var half = width / 2
            while (half / sample > TARGET_WIDTH) sample *= 2
        }
        return sample
    }

    private fun scaleToWidth(bitmap: Bitmap): Bitmap {
        if (bitmap.width <= TARGET_WIDTH) return bitmap
        val scale = TARGET_WIDTH.toFloat() / bitmap.width
        val newHeight = (bitmap.height * scale).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, TARGET_WIDTH, newHeight, true)
    }

    private fun rotate(bitmap: Bitmap, degrees: Int): Bitmap {
        val matrix = Matrix().apply { postRotate(degrees.toFloat()) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    companion object {
        private const val TARGET_WIDTH = 1080
        private const val JPEG_QUALITY = 80
    }
}