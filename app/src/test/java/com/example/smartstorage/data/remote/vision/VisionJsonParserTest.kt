package com.example.smartstorage.data.remote.vision

import com.example.smartstorage.domain.model.VisionMatchLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class VisionJsonParserTest {
    @Test
    fun parsesTriLanguageAnalysis() {
        val content = "{\"objects\":{\"zh-Hans\":[\"充电器\"],\"zh-Hant\":[\"充電器\"],\"en\":[\"charger\"]},\"attributes\":{\"zh-Hans\":[\"黑色\"],\"zh-Hant\":[\"黑色\"],\"en\":[\"black\"]},\"visibleText\":{\"zh-Hans\":[\"65W\"],\"zh-Hant\":[\"65W\"],\"en\":[\"65W\"]},\"description\":{\"zh-Hans\":\"黑色充电器\",\"zh-Hant\":\"黑色充電器\",\"en\":\"black charger\"},\"confidence\":0.91}"
        val analysis = assertNotNull(VisionJsonParser.parseAnalysis(content))
        val parsed = requireNotNull(VisionJsonParser.parseAnalysis(content))
        assertEquals("充电器", parsed.objectTags.zhHans)
        assertEquals("black charger", parsed.description.en)
        assertEquals(0.91, parsed.confidence, 0.001)
    }

    @Test
    fun parsesVerificationLevel() {
        val verification = VisionJsonParser.parseVerification(
            "{\"candidateId\":\"1:/a.jpg\",\"match\":\"HIGH\",\"reason\":{\"zh-Hans\":\"匹配\",\"zh-Hant\":\"匹配\",\"en\":\"match\"},\"confidence\":0.8}",
        )
        requireNotNull(verification)
        assertEquals(VisionMatchLevel.HIGH, verification.level)
    }

    @Test
    fun parsesFencedJsonWithSurroundingText() {
        val content = "分析结果如下：\n```json\n{\"objects\":{\"zh-Hans\":[\"水杯\"],\"zh-Hant\":[\"水杯\"],\"en\":[\"cup\"]},\"attributes\":{\"zh-Hans\":[],\"zh-Hant\":[],\"en\":[]},\"visibleText\":{\"zh-Hans\":[],\"zh-Hant\":[],\"en\":[]},\"description\":{\"zh-Hans\":\"一个水杯\",\"zh-Hant\":\"一個水杯\",\"en\":\"a cup\"},\"confidence\":0.7}\n```\n希望有帮助。"
        val parsed = requireNotNull(VisionJsonParser.parseAnalysis(content))
        assertEquals("水杯", parsed.objectTags.zhHans)
        assertEquals("a cup", parsed.description.en)
    }

    @Test
    fun parsesDeepSeekStyleResponseWithReasoningPrefix() {
        val content = "让我先观察图片……\n{\"match\":\"LOW\",\"reason\":{\"zh-Hans\":\"不确定\",\"zh-Hant\":\"不確定\",\"en\":\"unsure\"},\"confidence\":0.2}\n（以上为复核结果）"
        val parsed = requireNotNull(VisionJsonParser.parseVerification(content))
        assertEquals(VisionMatchLevel.LOW, parsed.level)
    }

    @Test
    fun emptyOrNonJsonContentReturnsNull() {
        assertNull(VisionJsonParser.parseAnalysis(""))
        assertNull(VisionJsonParser.parseAnalysis("   "))
        assertNull(VisionJsonParser.parseAnalysis("模型拒绝了本次请求"))
        assertNull(VisionJsonParser.parseVerification(""))
    }
}
