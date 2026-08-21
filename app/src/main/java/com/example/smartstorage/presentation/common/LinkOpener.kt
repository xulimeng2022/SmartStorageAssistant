package com.example.smartstorage.presentation.common

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

/**
 * 打开外部链接（浏览器 / 应用选择器），失败时复制链接兜底。
 *
 * 复用点：关于页网站跳转、GitHub Star 提醒跳转。
 * 注意：从 Application 上下文启动 Activity 必须携带 FLAG_ACTIVITY_NEW_TASK，否则会抛异常。
 */
fun openUrlWithChooser(context: Context, url: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        val chooser = Intent.createChooser(intent, "选择浏览器打开")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    } catch (e: Exception) {
        // 兜底：复制链接到剪贴板
        copyTextToClipboard(context, url)
        Toast.makeText(context, "无法打开链接，已复制到剪贴板", Toast.LENGTH_SHORT).show()
    }
}

/** 复制文本到系统剪贴板 */
fun copyTextToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("text", text))
}
