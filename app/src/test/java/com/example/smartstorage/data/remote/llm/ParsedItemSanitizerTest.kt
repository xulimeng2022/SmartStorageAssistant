package com.example.smartstorage.data.remote.llm

import com.example.smartstorage.domain.model.ParsedItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** ParsedItemSanitizer 的单元测试：清洗字段、剥离尾缀、过滤空名称。 */
class ParsedItemSanitizerTest {

    @Test
    fun trailingConnectorStripped() {
        // “…都”来自“都在”句式残留，应被剥离
        assertEquals(
            "学校发的雨衣和自己的毛拖鞋",
            ParsedItemSanitizer.sanitize(ParsedItem("学校发的雨衣和自己的毛拖鞋都", "", ""))?.name,
        )
    }

    @Test
    fun trailingAndStripped() {
        assertEquals("雨衣", ParsedItemSanitizer.sanitize(ParsedItem("雨衣和", "", ""))?.name)
    }

    @Test
    fun quotesAndPunctuationStripped() {
        assertEquals(
            "充电器",
            ParsedItemSanitizer.sanitize(ParsedItem("“充电器”、", "", ""))?.name,
        )
    }

    @Test
    fun fieldsTrimmed() {
        val out = ParsedItemSanitizer.sanitize(
            ParsedItem(" 红色的笔 ", " 柜子里 ", " 备注 "),
        )
        assertEquals(ParsedItem("红色的笔", "柜子里", "备注"), out)
    }

    @Test
    fun blankNameDropped() {
        assertNull(ParsedItemSanitizer.sanitize(ParsedItem("  ", "柜子里", "")))
        assertNull(ParsedItemSanitizer.sanitize(ParsedItem("", "", "")))
    }

    @Test
    fun sanitizeAllFiltersBlanks() {
        val out = ParsedItemSanitizer.sanitizeAll(
            listOf(
                ParsedItem("雨衣", "柜子里", ""),
                ParsedItem("", "抽屉里", ""),
                ParsedItem("拖鞋都", "", ""),
            ),
        )
        assertEquals(
            listOf(
                ParsedItem("雨衣", "柜子里", ""),
                ParsedItem("拖鞋", "", ""),
            ),
            out,
        )
    }
}