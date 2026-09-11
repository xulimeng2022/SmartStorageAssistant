package com.example.smartstorage.presentation.common

import android.content.Context
import androidx.annotation.StringRes

/**
 * 显示层消息：业务层只产生资源 ID 与参数，由界面按当前 App 语言解析。
 *
 * 语言切换时只有 Activity 会按新语言重建，Application Context 的 Resources 仍停留在
 * 进程启动时的语言；因此 ViewModel/数据层不得提前 getString() 缓存文案。
 */
sealed interface UiMessage {
    /** 单条资源消息；args 支持 Int/String 以及嵌套的 [UiMessage]（用于“标题：原因”组合）。 */
    data class Res(
        @StringRes val id: Int,
        val args: List<Any> = emptyList(),
    ) : UiMessage

    /** 多段消息按顺序拼接（例如“成功添加 2 件，更新 1 件”）。 */
    data class Group(val messages: List<Res>) : UiMessage
}

/** 在显示层用界面 Context（跟随当前 App 语言）解析为最终文本。 */
fun UiMessage.resolve(context: Context): String = when (this) {
    is UiMessage.Res -> {
        val resolved = args.map { if (it is UiMessage) it.resolve(context) else it }.toTypedArray()
        if (resolved.isEmpty()) context.getString(id) else context.getString(id, *resolved)
    }
    is UiMessage.Group -> messages.joinToString(separator = "") { it.resolve(context) }
}
