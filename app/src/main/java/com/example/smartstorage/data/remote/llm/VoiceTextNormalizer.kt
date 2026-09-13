package com.example.smartstorage.data.remote.llm

/**
 * 语音转文字文本规范化：仅处理停顿词、重复标点和相邻重复短语。
 * 保留用户原始输入，不消除数量、地点、否定词或有效修饰。
 */
internal object VoiceTextNormalizer {

    /** 可作为独立停顿成分删除的词，只有被标点或空白包围时才处理。 */
    private val FILLER = Regex(
        "(?<=^|[，,。．；;！？!?\\s])(嗯+|呃+|啊+|那个|这个|就是|然后|这么说吧|怎么说呢)(?=[，,。．；;！？!?\\s]|$)",
    )

    /** 连续中文/英文标点折叠为一个。 */
    private val REPEATED_COMMA = Regex("[，,]{2,}")
    private val REPEATED_PERIOD = Regex("[。．.]{2,}")
    private val REPEATED_SEMICOLON = Regex("[；;]{2,}")
    private val REPEATED_PAUSE = Regex("[、]{2,}")

    /** 相邻重复短语，如“充电器充电器”“数据线，数据线”。 */
    private val ADJACENT_DUPLICATE = Regex("([\\p{L}\\p{N}]{2,})([，,、\\s]*)\\1")

    fun normalize(raw: String): String {
        var text = raw.trim()
        if (text.isEmpty()) return text
        text = text.replace(FILLER, "")
        text = text.replace(REPEATED_COMMA, "，")
        text = text.replace(REPEATED_PERIOD, "。")
        text = text.replace(REPEATED_SEMICOLON, "；")
        text = text.replace(REPEATED_PAUSE, "、")
        text = text.replace(Regex("[ \\t\\u3000]+"), " ")
        text = text.replace(ADJACENT_DUPLICATE, "$1")
        return text.trim().trimStart('，', ',', '。', '.', '；', ';', '、')
    }
}