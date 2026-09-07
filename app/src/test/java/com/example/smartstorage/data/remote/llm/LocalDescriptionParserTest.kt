package com.example.smartstorage.data.remote.llm

import org.junit.Assert.assertEquals
import org.junit.Test

/** LocalDescriptionParser 的单元测试：覆盖验收表与边界输入。 */
class LocalDescriptionParserTest {

    /** 把解析结果转成 (名称, 地点) 列表，便于断言。 */
    private fun pairs(input: String): List<Pair<String, String>> =
        LocalDescriptionParser.parse(input).map { it.name to it.location }

    @Test
    fun sharedLocationTwoItems_raincoatSlippers() {
        // 截图问题句：共享地点且不得把“都”混入名称
        assertEquals(
            listOf(
                "学校发的雨衣" to "左边最下面开门柜子里",
                "自己的毛拖鞋" to "左边最下面开门柜子里",
            ),
            pairs("学校发的雨衣和自己的毛拖鞋都在左边最下面开门柜子里"),
        )
    }

    @Test
    fun sharedLocationTwoItems_clipHook() {
        // 截图问题句
        assertEquals(
            listOf(
                "夹子" to "左下抽屉里",
                "墙壁挂钩" to "左下抽屉里",
            ),
            pairs("夹子和墙壁挂钩都在左下抽屉里"),
        )
    }

    @Test
    fun quantityKeptAsSingleItem() {
        // “一堆”只是数量描述，保留在名称，不虚构数量或多条
        assertEquals(
            listOf("一堆消毒液试用装" to "左下抽屉里"),
            pairs("一堆消毒液试用装在左下抽屉里"),
        )
    }

    @Test
    fun differentLocationsEachMatched() {
        assertEquals(
            listOf(
                "红色的笔" to "柜子里",
                "蓝色的笔" to "抽屉里",
            ),
            pairs("红色的笔在柜子里，蓝色的笔在抽屉里"),
        )
    }

    @Test
    fun threeItemsSharedLocation() {
        assertEquals(
            listOf(
                "雨衣" to "柜子里",
                "拖鞋" to "柜子里",
                "夹子" to "柜子里",
            ),
            pairs("雨衣、拖鞋和夹子都在柜子里"),
        )
    }

    @Test
    fun brandNameWithHeNotSplit() {
        // 品牌复读（强生 / 强生牌…）不得按“和”误拆
        assertEquals(
            listOf("强生和强生牌的创可贴" to "抽屉里"),
            pairs("强生和强生牌的创可贴在抽屉里"),
        )
    }

    @Test
    fun setItemNotSplit() {
        assertEquals(
            listOf("一套桌椅" to "阳台"),
            pairs("一套桌椅在阳台"),
        )
    }

    @Test
    fun twoItemsWithoutLocation() {
        // 无地点：拆成两条，地点留空等待补充
        assertEquals(
            listOf("雨衣" to "", "拖鞋" to ""),
            pairs("雨衣和拖鞋"),
        )
    }

    @Test
    fun singleItemWithLocation() {
        assertEquals(listOf("鼠标" to "桌子上"), pairs("鼠标在桌子上"))
    }

    @Test
    fun nameContainsLocationWordButHasMultiVerb() {
        // 名称里的“在”（上次在淘宝买的）不应被误当作地点分隔符
        assertEquals(
            listOf("上次在淘宝买的杯子" to "柜子里"),
            pairs("上次在淘宝买的杯子放在柜子里"),
        )
    }

    @Test
    fun baObjectFrontingStripped() {
        assertEquals(listOf("雨衣" to "柜子里"), pairs("把雨衣放在柜子里"))
    }

    @Test
    fun emptyInputReturnsEmpty() {
        assertEquals(emptyList<Pair<String, String>>(), pairs("   "))
    }
}