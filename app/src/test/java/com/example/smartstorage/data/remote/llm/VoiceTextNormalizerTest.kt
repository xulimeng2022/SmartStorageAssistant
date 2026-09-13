package com.example.smartstorage.data.remote.llm

import org.junit.Assert.assertEquals
import org.junit.Test

class VoiceTextNormalizerTest {

    @Test
    fun removesPauseWordsAndFoldsRepeatedPunctuation() {
        assertEquals(
            "一个充电器，两根数据线。",
            VoiceTextNormalizer.normalize("嗯，一个充电器，，那个，两根数据线。。"),
        )
    }

    @Test
    fun preservesCommaBoundaryBetweenMultipleItems() {
        assertEquals(
            "充电器，钥匙，数据线",
            VoiceTextNormalizer.normalize("充电器，钥匙，数据线"),
        )
    }

    @Test
    fun removesAdjacentDuplicateWithoutMergingNextItem() {
        assertEquals(
            "充电器，数据线",
            VoiceTextNormalizer.normalize("充电器，充电器，数据线"),
        )
    }
}
