package com.example.smartstorage.presentation.about

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Public
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * 单个联系方式条目（数据驱动：新增联系方式只需在 [contacts] 列表追加一项）。
 *
 * @param icon 左侧图标
 * @param label 名称（如 “QQ”）
 * @param value 显示值（如 “2913895771”）
 * @param copyable true 表示点击为“复制”（微信），false 表示点击为“跳转”（QQ/GitHub/个人网站）
 * @param onClick 点击行为
 */
data class ContactItem(
    val icon: ImageVector,
    val label: String,
    val value: String,
    val copyable: Boolean = false,
    val onClick: () -> Unit,
)

/**
 * 关于页 ViewModel：封装联系方式点击行为（QQ 跳转、微信复制、浏览器打开）。
 */
@HiltViewModel
class AboutViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
) : ViewModel() {

    /** 联系方式列表：QQ / 微信 / GitHub / 个人网站；未来新增邮箱等在此追加即可。 */
    val contacts: List<ContactItem> = listOf(
        ContactItem(
            icon = Icons.Outlined.Chat,
            label = "QQ",
            value = "2913895771",
            onClick = { openQQ("2913895771") },
        ),
        ContactItem(
            icon = Icons.Outlined.Chat,
            label = "微信",
            value = "xulimeng2021",
            copyable = true, // 微信无法直接跳转加好友，点击=复制
            onClick = { copyWeChat("xulimeng2021") },
        ),
        ContactItem(
            icon = Icons.Outlined.Code,
            label = "GitHub",
            value = "github.com/xulimeng2022",
            onClick = { openBrowser("https://github.com/xulimeng2022") },
        ),
        ContactItem(
            icon = Icons.Outlined.Public,
            label = "个人网站",
            value = "xulimeng2026.netlify.app",
            onClick = { openBrowser("https://xulimeng2026.netlify.app") },
        ),
        // 预留扩展位示例（取消注释即可启用）：
        // ContactItem(
        //     icon = Icons.Outlined.Email,
        //     label = "邮箱",
        //     value = "your_email@example.com",
        //     onClick = { openBrowser("mailto:your_email@example.com") },
        // ),
    )

    /** 唤起 QQ 并跳转到该用户；QQ 未安装时复制号码并提示。 */
    fun openQQ(qq: String) {
        try {
            val intent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("mqqwpa://im/chat?chat_type=wpa&uin=$qq"),
            )
            context.startActivity(intent)
        } catch (e: Exception) {
            // QQ 未安装：复制 QQ 号并提示
            copyToClipboard(qq)
            Toast.makeText(context, "QQ 未安装，QQ号已复制", Toast.LENGTH_SHORT).show()
        }
    }

    /** 复制微信号并提示手动打开微信搜索添加（微信限制无法直接跳转加好友）。 */
    fun copyWeChat(wechatId: String) {
        copyToClipboard(wechatId)
        Toast.makeText(context, "微信号已复制，请打开微信搜索添加", Toast.LENGTH_LONG).show()
    }

    /** 打开链接：弹出应用选择器让用户选择浏览器；无可用应用时复制链接兜底。 */
    fun openBrowser(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            val chooser = Intent.createChooser(intent, "选择浏览器打开")
            context.startActivity(chooser)
        } catch (e: Exception) {
            // 兜底：复制链接到剪贴板
            copyToClipboard(url)
            Toast.makeText(context, "无法打开链接，已复制到剪贴板", Toast.LENGTH_SHORT).show()
        }
    }

    /** 复制文本到系统剪贴板。 */
    private fun copyToClipboard(text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("contact", text))
    }
}