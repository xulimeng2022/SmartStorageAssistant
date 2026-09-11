package com.example.smartstorage.data.repository

import com.example.smartstorage.data.local.entity.ImageAiIndexEntity
import com.example.smartstorage.domain.model.Item
import com.example.smartstorage.domain.model.LocalizedVisionFields
import com.example.smartstorage.domain.model.VisualMatch

/** 纯本地视觉索引检索与排序，不发起网络请求。 */
object VisualSearchEngine {
    fun match(
        query: String,
        indices: List<ImageAiIndexEntity>,
        items: List<Item>,
        languageCode: String,
    ): List<VisualMatch> {
        val tokens = tokenize(query)
        if (tokens.isEmpty()) return emptyList()
        val itemById = items.associateBy { it.id }
        return indices.mapNotNull { index ->
            val searchable = index.searchText.ifBlank { buildSearchText(index) }
            val hits = tokens.count { searchable.contains(it) }
            if (hits != tokens.size) return@mapNotNull null
            val item = itemById[index.itemId] ?: return@mapNotNull null
            val analysis = index.toDomain()
            VisualMatch(
                item = item,
                imagePath = index.imagePath,
                evidence = evidenceFor(analysis.objectTags, analysis.attributes, analysis.visibleText, languageCode),
                confidence = analysis.confidence,
                score = hits.toDouble() * 10.0 + analysis.confidence,
            )
        }.sortedWith(compareByDescending<VisualMatch> { it.score }.thenByDescending { it.item.updatedAt })
    }

    fun tokenize(query: String): List<String> = query
        .trim()
        .lowercase()
        .split(Regex("[\\s,，。.!！?？、;；:：/\\\\|()（）\\[\\]【】]+"))
        .filter { it.isNotBlank() }

    fun localized(fields: LocalizedVisionFields, languageCode: String): String = when {
        languageCode.startsWith("en") -> fields.en
        languageCode == "zh-rTW" -> fields.zhHant
        else -> fields.zhHans
    }

    private fun evidenceFor(
        objects: LocalizedVisionFields,
        attributes: LocalizedVisionFields,
        visibleText: LocalizedVisionFields,
        languageCode: String,
    ): String = listOf(
        localized(objects, languageCode),
        localized(attributes, languageCode),
        localized(visibleText, languageCode),
    ).filter { it.isNotBlank() }.joinToString(" · ")

    private fun buildSearchText(index: ImageAiIndexEntity): String = listOf(
        index.objectTagsJson,
        index.attributesJson,
        index.visibleTextJson,
        index.descriptionJson,
    ).joinToString(" ").lowercase()
}
