package com.example.smartstorage.data.remote.llm

/**
 * 大模型请求错误的种类，用于给用户展示可理解、可行动的提示，并决定是否重试。
 */
enum class LlmErrorKind {
    /** 配置缺失（免费模式未内置 Key / 自定义配置不完整） */
    MISSING_CONFIG,

    /** 认证失败：API Key 无效或无权限 */
    AUTH,

    /** 额度不足 / 账户被限制 */
    QUOTA,

    /** 限流（429） */
    RATE_LIMIT,

    /** 模型不存在 / 已下线 / 请求参数错误（400/404 等） */
    MODEL_OR_REQUEST,

    /** 服务端错误（5xx） */
    SERVER,

    /** 网络错误（连接失败 / SSL / 超时） */
    NETWORK,

    /** 其它（含响应体解析失败） */
    UNKNOWN,
}

/** 携带错误种类与用户可读消息的类型化异常（消息已脱敏，绝不包含 Key）。 */
class LlmRequestException(
    val kind: LlmErrorKind,
    message: String,
    val httpCode: Int? = null,
    val detail: String? = null,
    val isFreeMode: Boolean? = null,
) : Exception(message)

/**
 * 纯函数错误分类器（无 Android 依赖，便于单元测试）：
 * 把 HTTP 状态码与响应体文本映射为类型化的 [LlmRequestException]。
 * 响应体仅用正则截取 message 摘要拼接，绝不输出 Key。
 */
object LlmErrorClassifier {

    /** 单次请求最多尝试次数（含首次）；只对可重试错误触发第 2 次。 */
    const val MAX_ATTEMPTS = 2

    /** 可重试的错误种类：限流 / 服务端错误 / 网络错误。 */
    fun isRetryable(kind: LlmErrorKind): Boolean =
        kind == LlmErrorKind.RATE_LIMIT || kind == LlmErrorKind.SERVER || kind == LlmErrorKind.NETWORK

    /** 根据 HTTP 状态码判断错误种类（2xx 不在此列，返回 null）。 */
    fun kindForHttp(code: Int): LlmErrorKind? = when (code) {
        in 200..299 -> null
        401, 403 -> LlmErrorKind.AUTH
        402 -> LlmErrorKind.QUOTA
        404 -> LlmErrorKind.MODEL_OR_REQUEST
        429 -> LlmErrorKind.RATE_LIMIT
        in 400..499 -> LlmErrorKind.MODEL_OR_REQUEST
        in 500..599 -> LlmErrorKind.SERVER
        else -> LlmErrorKind.UNKNOWN
    }

    /** 生成带原因的用户提示（不含服务地址与 Key）。 */
    fun userMessage(kind: LlmErrorKind, code: Int?, bodyMessage: String?): String {
        val base = when (kind) {
            LlmErrorKind.MISSING_CONFIG -> "当前安装包未内置免费模型 Key（构建时需配置 SILICONFLOW_API_KEY），请到设置页切换到自定义模式填写自己的 API Key"
            LlmErrorKind.AUTH -> "API Key 无效或无权限，请到设置页检查密钥或切换到自定义模式"
            LlmErrorKind.QUOTA -> "账户额度不足或已被限制，请检查服务商账户"
            LlmErrorKind.RATE_LIMIT -> "请求过于频繁，已自动重试，请稍后再试"
            LlmErrorKind.MODEL_OR_REQUEST -> "模型不存在、已下线或请求参数错误，请检查设置中的模型版本"
            LlmErrorKind.SERVER -> "识别服务暂时不可用，已自动重试，请稍后再试"
            LlmErrorKind.NETWORK -> "网络连接失败或超时，请检查网络后重试"
            LlmErrorKind.UNKNOWN -> "识别服务返回异常，请稍后重试"
        }
        val detail = summarizeBody(bodyMessage)
        return when {
            code != null && detail != null -> "$base（HTTP $code：$detail）"
            code != null -> "$base（HTTP $code）"
            detail != null -> "$base（$detail）"
            else -> base
        }
    }

    /** 从 HTTP 状态码 + 响应体文本构造类型化异常。 */
    fun exceptionFor(code: Int, body: String?): LlmRequestException {
        val kind = kindForHttp(code) ?: LlmErrorKind.UNKNOWN
        return LlmRequestException(
            kind = kind,
            message = userMessage(kind, code, body),
            httpCode = code,
            detail = summarizeBody(body),
        )
    }

    /** 从响应体里提取 error/message 字段摘要（正则，避免依赖 org.json；截断且单行化）。 */
    fun summarizeBody(body: String?): String? {
        if (body.isNullOrBlank()) return null
        val match = Regex("\"message\"\\s*:\\s*\"([^\"]*)\"")
            .find(body)
        val raw = match?.groupValues?.get(1)?.trim().takeIf { !it.isNullOrEmpty() }
            ?: return null
        val singleLine = raw.replace("\n", " ").replace("\r", " ").trim()
        return if (singleLine.length > 120) singleLine.take(120) + "…" else singleLine
    }
}
