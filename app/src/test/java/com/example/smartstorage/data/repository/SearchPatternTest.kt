package com.example.smartstorage.data.repository

import org.junit.Assert.assertEquals
import org.junit.Test

/** buildFuzzyLikePattern 的单元测试：验证逐字模糊模式生成与通配符转义。 */
class SearchPatternTest {

    @Test
    fun testChineseKeywordCreatesCharPattern() {
        // “红色口红” → %红%色%口%红%
        assertEquals("%红%色%口%红%", buildFuzzyLikePattern("红色口红"))
    }

    @Test
    fun testWhitespaceRemovedBeforePattern() {
        // 调用方（ItemRepositoryImpl.searchItems）先去空白再生成模式；
        // 这里模拟“红色 口红” → 去掉空格 → 与“红色口红”生成相同模式
        val normalized = "红色 口红".filterNot { it.isWhitespace() }
        assertEquals("%红%色%口%红%", buildFuzzyLikePattern(normalized))
    }

    @Test
    fun testLikeWildcardsEscaped() {
        // 输入 a%b_c\d → 输出 %a%\%%b%\_%c%\\%d%（% _ \ 均被转义，且每个原始字符间保留通配符 %）
        assertEquals("%a%\\%%b%\\_%c%\\\\%d%", buildFuzzyLikePattern("a%b_c\\d"))
    }

    @Test
    fun testEnglishCaseDoesNotAffectPattern() {
        // 模式保留原始大小写；SQLite LIKE 对 ASCII 默认忽略大小写，因此 “Lip”/“lip” 匹配结果一致
        assertEquals("%L%i%p%", buildFuzzyLikePattern("Lip"))
    }
}