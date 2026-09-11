package com.example.smartstorage.presentation.about

import android.content.Context
import androidx.annotation.StringRes
import com.example.smartstorage.R
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Public
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.ViewModel
import com.example.smartstorage.presentation.common.UiMessage
import com.example.smartstorage.presentation.common.copyTextToClipboard
import com.example.smartstorage.presentation.common.openUrlWithChooser
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import javax.inject.Inject

/**
 * 单个联系方式条目（数据驱动：新增联系方式只需在 [contacts] 列表追加一项）。
 *
 * @param icon 左侧图标
 * @param labelRes 名称资源（如 “QQ”）
 * @param value 显示值（如 “2913895771”）
 * @param copyable true 表示点击为“复制”（QQ/微信），false 表示点击为“跳转”（GitHub/个人网站）
 * @param onClick 点击行为
 */
data class ContactItem(
    val icon: ImageVector,
    @StringRes val labelRes: Int,
    val value: String,
    val copyable: Boolean = false,
    val onClick: () -> Unit,
)

/**
 * 关于页 ViewModel：封装联系方式点击行为。
 * - QQ / 微信：点击直接复制号码（QQ 无法稳定拉起 App，与微信一致）
 * - GitHub / 个人网站：点击弹出应用选择器打开浏览器（失败时复制链接兜底）
 */
@HiltViewModel
class AboutViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
) : ViewModel() {

    // 一次性 UI 消息（复制成功提示）：由关于页用界面 Context 按当前语言解析显示
    private val _uiMessages = Channel<UiMessage>(Channel.BUFFERED)
    val uiMessages: Flow<UiMessage> = _uiMessages.receiveAsFlow()

    /** 联系方式列表：QQ / 微信 / GitHub / 个人网站；未来新增邮箱等在此追加即可。 */
    val contacts: List<ContactItem> = listOf(
        ContactItem(
            icon = Icons.Outlined.Chat,
            labelRes = R.string.about_qq_label,
            value = "2913895771",
            copyable = true, // 部分设备无法拉起 QQ，点击=复制号码
            onClick = { copyContact("2913895771", R.string.about_qq_copied) },
        ),
        ContactItem(
            icon = Icons.Outlined.Chat,
            labelRes = R.string.about_wechat,
            value = "xulimeng2021",
            copyable = true, // 微信无法直接跳转加好友，点击=复制
            onClick = { copyContact("xulimeng2021", R.string.about_wechat_copied) },
        ),
        ContactItem(
            icon = Icons.Outlined.Code,
            labelRes = R.string.about_github_label,
            value = "github.com/xulimeng2022",
            onClick = { openUrlWithChooser(context, "https://github.com/xulimeng2022") },
        ),
        ContactItem(
            icon = Icons.Outlined.Public,
            labelRes = R.string.about_website,
            value = "xulimeng2026.netlify.app",
            onClick = { openUrlWithChooser(context, "https://xulimeng2026.netlify.app") },
        ),
        // 预留扩展位示例（取消注释即可启用）：
        // ContactItem(
        //     icon = Icons.Outlined.Email,
        //     labelRes = R.string.about_email,
        //     value = "your_email@example.com",
        //     onClick = { openUrlWithChooser(context, "mailto:your_email@example.com") },
        // ),
    )

    /** 复制联系方式并发一次性提示（QQ/微信共用）；文案由关于页按当前语言解析 */
    private fun copyContact(value: String, @StringRes messageRes: Int) {
        copyTextToClipboard(context, value)
        _uiMessages.trySend(UiMessage.Res(messageRes))
    }
}
