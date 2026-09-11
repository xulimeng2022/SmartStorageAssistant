package com.example.smartstorage.data.remote.vision

import com.example.smartstorage.domain.model.LocalizedVisionFields
import com.example.smartstorage.domain.model.VisionAnalysis
import com.example.smartstorage.domain.model.VisionMatchLevel
import com.example.smartstorage.domain.model.VisionVerification

/** 视觉模型输出解析：使用轻量纯 Kotlin JSON 解析器，运行时和 JVM 测试行为一致。 */
object VisionJsonParser {
    fun parseAnalysis(content: String): VisionAnalysis? {
        val root = parseRoot(content)?.asObject() ?: return null
        return runCatching {
            VisionAnalysis(
                objectTags = parseField(root["objects"]),
                attributes = parseField(root["attributes"]),
                visibleText = parseField(root["visibleText"]),
                description = parseDescription(root["description"]),
                confidence = root["confidence"].asDouble().coerceIn(0.0, 1.0),
            )
        }.getOrNull()
    }

    fun parseVerification(content: String): VisionVerification? {
        val root = parseRoot(content)?.asObject() ?: return null
        val level = runCatching { VisionMatchLevel.valueOf(root["match"].asString().uppercase()) }
            .getOrDefault(VisionMatchLevel.UNKNOWN)
        return runCatching {
            VisionVerification(
                candidateKey = root["candidateId"].asString(),
                level = level,
                reason = parseField(root["reason"]).zhHans,
                confidence = root["confidence"].asDouble().coerceIn(0.0, 1.0),
            )
        }.getOrNull()
    }

    private fun parseRoot(content: String): Node? {
        val start = content.indexOf('{')
        val end = content.lastIndexOf('}')
        if (start < 0 || end <= start) return null
        return runCatching { Reader(content.substring(start, end + 1)).parse() }.getOrNull()
    }

    private fun parseField(node: Node?): LocalizedVisionFields {
        val obj = node.asObject() ?: return LocalizedVisionFields()
        return LocalizedVisionFields(
            zhHans = readValues(obj["zh-Hans"]),
            zhHant = readValues(obj["zh-Hant"]),
            en = readValues(obj["en"]),
        )
    }

    private fun parseDescription(node: Node?): LocalizedVisionFields {
        val obj = node.asObject() ?: return LocalizedVisionFields()
        return LocalizedVisionFields(
            zhHans = obj["zh-Hans"].asString(),
            zhHant = obj["zh-Hant"].asString(),
            en = obj["en"].asString(),
        )
    }

    private fun readValues(node: Node?): String = when (node) {
        is Node.Arr -> node.values.mapNotNull { it.asString().ifBlank { null } }.joinToString("、")
        is Node.Str -> node.value
        else -> ""
    }

    private sealed interface Node {
        data class Obj(val values: Map<String, Node>) : Node
        data class Arr(val values: List<Node>) : Node
        data class Str(val value: String) : Node
        data class Num(val value: Double) : Node
        data class Bool(val value: Boolean) : Node
        data object Null : Node
    }

    private fun Node?.asObject(): Map<String, Node>? = (this as? Node.Obj)?.values

    private fun Node?.asString(): String = when (this) {
        is Node.Str -> value
        is Node.Num -> value.toString()
        else -> ""
    }

    private fun Node?.asDouble(): Double = (this as? Node.Num)?.value ?: 0.0

    private class Reader(private val text: String) {
        private var index = 0

        fun parse(): Node {
            val value = readValue()
            skipWhitespace()
            check(index == text.length)
            return value
        }

        private fun readValue(): Node {
            skipWhitespace()
            return when (peek()) {
                '{' -> readObject()
                '[' -> readArray()
                '"' -> Node.Str(readString())
                't' -> readLiteral("true", Node.Bool(true))
                'f' -> readLiteral("false", Node.Bool(false))
                'n' -> readLiteral("null", Node.Null)
                else -> Node.Num(readNumber())
            }
        }

        private fun readObject(): Node.Obj {
            expect('{')
            val values = linkedMapOf<String, Node>()
            skipWhitespace()
            if (peek() == '}') {
                index++
                return Node.Obj(values)
            }
            while (true) {
                val key = readString()
                skipWhitespace()
                expect(':')
                values[key] = readValue()
                skipWhitespace()
                when (peek()) {
                    ',' -> index++
                    '}' -> {
                        index++
                        return Node.Obj(values)
                    }
                    else -> error("对象缺少逗号或右括号")
                }
            }
        }

        private fun readArray(): Node.Arr {
            expect('[')
            val values = mutableListOf<Node>()
            skipWhitespace()
            if (peek() == ']') {
                index++
                return Node.Arr(values)
            }
            while (true) {
                values += readValue()
                skipWhitespace()
                when (peek()) {
                    ',' -> index++
                    ']' -> {
                        index++
                        return Node.Arr(values)
                    }
                    else -> error("数组缺少逗号或右括号")
                }
            }
        }

        private fun readString(): String {
            expect('"')
            val result = StringBuilder()
            while (index < text.length) {
                when (val char = text[index++]) {
                    '"' -> return result.toString()
                    '\\' -> {
                        if (index >= text.length) error("字符串转义不完整")
                        when (val escaped = text[index++]) {
                            '"', '\\', '/' -> result.append(escaped)
                            'b' -> result.append('\b')
                            'f' -> result.append('\u000C')
                            'n' -> result.append('\n')
                            'r' -> result.append('\r')
                            't' -> result.append('\t')
                            'u' -> {
                                val hex = text.substring(index, index + 4)
                                result.append(hex.toInt(16).toChar())
                                index += 4
                            }
                            else -> result.append(escaped)
                        }
                    }
                    else -> result.append(char)
                }
            }
            error("字符串未闭合")
        }

        private fun readNumber(): Double {
            val start = index
            while (index < text.length && text[index] in "-+0123456789.eE") index++
            return text.substring(start, index).toDouble()
        }

        private fun readLiteral(literal: String, value: Node): Node {
            check(text.startsWith(literal, index))
            index += literal.length
            return value
        }

        private fun expect(char: Char) {
            skipWhitespace()
            check(peek() == char)
            index++
        }

        private fun peek(): Char = text[index]

        private fun skipWhitespace() {
            while (index < text.length && text[index].isWhitespace()) index++
        }
    }
}
