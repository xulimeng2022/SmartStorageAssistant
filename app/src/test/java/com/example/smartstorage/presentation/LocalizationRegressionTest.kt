package com.example.smartstorage.presentation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/** 本地化回归测试：三语言资源必须完整对齐，显示层不得残留中文字符串。 */
class LocalizationRegressionTest {

    private val projectRoot: File
        get() = File(
            requireNotNull(System.getProperty("localizationProjectRoot")) {
                "缺少 localizationProjectRoot 测试参数"
            },
        )

    @Test
    fun allStringResourcesHaveMatchingKeysAndPlaceholders() {
        val base = parseStrings("app/src/main/res/values/strings.xml")
        val traditional = parseStrings("app/src/main/res/values-zh-rTW/strings.xml")
        val english = parseStrings("app/src/main/res/values-en/strings.xml")

        assertEquals(base.keys, traditional.keys)
        assertEquals(base.keys, english.keys)

        base.forEach { (name, value) ->
            assertEquals("繁體占位符不一致: $name", placeholders(value), placeholders(traditional.getValue(name)))
            assertEquals("English 占位符不一致: $name", placeholders(value), placeholders(english.getValue(name)))
        }
    }

    @Test
    fun englishResourcesDoNotLeakChineseUiText() {
        val english = parseStrings("app/src/main/res/values-en/strings.xml")
        val allowedChineseKeys = setOf("lang_zh", "lang_tw", "about_copyright")
        val leaked = english.filter { (name, value) ->
            name !in allowedChineseKeys && value.any { it.code in 0x4E00..0x9FFF }
        }
        assertTrue("English 资源残留中文: ${leaked.keys}", leaked.isEmpty())
    }

    @Test
    fun presentationHasNoQuotedChineseLiterals() {
        val presentationDir = projectRoot.resolve("app/src/main/java/com/example/smartstorage/presentation")
        val offenders = mutableListOf<String>()
        presentationDir.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .forEach { file ->
                var inBlockComment = false
                file.readLines().forEachIndexed { index, line ->
                    val (code, nextInBlock) = codeBeforeLineComment(line, inBlockComment)
                    inBlockComment = nextInBlock
                    if (code.any { it.code in 0x4E00..0x9FFF } && code.contains('"')) {
                        offenders += "${file.relativeTo(projectRoot)}:${index + 1} ${code.trim()}"
                    }
                }
            }
        assertTrue("presentation 残留中文硬编码:\n${offenders.joinToString("\n")}", offenders.isEmpty())
    }

    private fun parseStrings(relativePath: String): Map<String, String> {
        val file = projectRoot.resolve(relativePath)
        val document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)
        val nodes = document.getElementsByTagName("string")
        return buildMap {
            repeat(nodes.length) { index ->
                val element = nodes.item(index) as Element
                put(element.getAttribute("name"), element.textContent)
            }
        }
    }

    private fun placeholders(text: String): List<String> =
        Regex("%(?:\\d+\\$)?[a-zA-Z]").findAll(text).map { it.value }.sorted().toList()

    private fun codeBeforeLineComment(line: String, inBlockComment: Boolean): Pair<String, Boolean> {
        var index = 0
        var inBlock = inBlockComment
        val code = StringBuilder()
        while (index < line.length) {
            if (inBlock) {
                val end = line.indexOf("*/", index)
                if (end < 0) return code.toString() to true
                inBlock = false
                index = end + 2
            } else {
                val block = line.indexOf("/*", index)
                val lineComment = line.indexOf("//", index)
                when {
                    lineComment >= 0 && (block < 0 || lineComment < block) -> {
                        code.append(line, index, lineComment)
                        return code.toString() to false
                    }
                    block >= 0 -> {
                        code.append(line, index, block)
                        inBlock = true
                        index = block + 2
                    }
                    else -> {
                        code.append(line, index, line.length)
                        return code.toString() to false
                    }
                }
            }
        }
        return code.toString() to inBlock
    }

}
