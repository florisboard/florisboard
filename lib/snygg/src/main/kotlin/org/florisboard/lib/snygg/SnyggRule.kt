/*
 * Copyright (C) 2025 The FlorisBoard Contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.florisboard.lib.snygg

import kotlin.math.min

sealed interface SnyggRule : Comparable<SnyggRule> {
    companion object {
        fun fromOrNull(str: String): SnyggRule? {
            return SnyggAnnotationRule.Defines.fromOrNull(str)
                ?: SnyggAnnotationRule.Font.fromOrNull(str)
                ?: SnyggElementRule.fromOrNull(str)
        }
    }
}

sealed interface SnyggAnnotationRule : SnyggRule {
    val annotationName: String

    data object Defines : SnyggAnnotationRule {
        override val annotationName: String = "defines"

        internal val REGEX = """@defines""".toRegex()

        fun fromOrNull(str: String): SnyggRule? {
            REGEX.matchEntire(str) ?: return null
            return Defines
        }

        override fun compareTo(other: SnyggRule): Int {
            return when (other) {
                is Defines -> 0 // same
                is SnyggAnnotationRule -> annotationName.compareTo(other.annotationName)
                is SnyggElementRule -> -1 // annotations always come first
            }
        }
    }

    data class Font(val fontName: String) : SnyggAnnotationRule {
        override val annotationName: String = "font"

        companion object {
            internal val REGEX = """@font `(?<fontName>[a-zA-Z0-9\s-]+)`""".toRegex()

            fun fromOrNull(str: String): SnyggRule? {
                val match = REGEX.matchEntire(str) ?: return null
                return Font(match.groups["fontName"]!!.value)
            }
        }

        override fun compareTo(other: SnyggRule): Int {
            return when (other) {
                is Font -> fontName.compareTo(other.fontName)
                is SnyggAnnotationRule -> annotationName.compareTo(other.annotationName)
                is SnyggElementRule -> -1 // annotations always come first
            }
        }
    }
}

data class SnyggElementRule(
    val elementName: String,
    val attributes: Attributes = Attributes(),
    val selector: SnyggSelector = SnyggSelector.NONE,
) : SnyggRule {
    init {
        requireNotNull(ELEMENT_NAME_REGEX.matchEntire(elementName)) { "element name is invalid" }
    }

    override fun compareTo(other: SnyggRule): Int {
        if (other !is SnyggElementRule) {
            return 1 // annotations always come first
        }
        val elemDiff = elementName.compareTo(other.elementName)
        if (elemDiff != 0) {
            return elemDiff
        }
        val attrDiff = attributes.compareTo(other.attributes)
        if (attrDiff != 0) {
            return attrDiff
        }
        if (selector == SnyggSelector.NONE && other.selector == SnyggSelector.NONE) {
            return 0
        }
        if (selector == SnyggSelector.NONE) {
            return -1
        }
        if (other.selector == SnyggSelector.NONE) {
            return 1
        }
        return selector.compareTo(other.selector)
    }

    override fun toString(): String {
        return buildString {
            append(elementName)
            append(attributes)
            append(selector)
        }
    }

    companion object {
        private const val ATTRIBUTE_OPEN = "["
        private const val ATTRIBUTE_CLOSE = "]"
        private const val ATTRIBUTE_ASSIGN = "="
        private const val ATTRIBUTE_VALUES_SEPARATOR = ","

        private val ELEMENT_NAME_REGEX = """(?<elementName>[a-zA-Z0-9-]+)""".toRegex()
        private val ATTRIBUTE_VALUE_REGEX = """[+-]?(?:0|[1-9][0-9]*)(?:[.]{2}[+-]?(?:0|[1-9][0-9]*))?""".toRegex()
        private val ATTRIBUTE_REGEX = """\[[a-zA-Z0-9-]+=$ATTRIBUTE_VALUE_REGEX(?:,$ATTRIBUTE_VALUE_REGEX)*]""".toRegex()
        private val ATTRIBUTES_REGEX = """(?<attributesRaw>(?:$ATTRIBUTE_REGEX)+)?""".toRegex()
        private val SELECTOR_REGEX = """(?<selectorRaw>:pressed|:focus|:hover|:disabled)?""".toRegex()
        internal val REGEX = """^$ELEMENT_NAME_REGEX$ATTRIBUTES_REGEX$SELECTOR_REGEX${'$'}""".toRegex()

        fun fromOrNull(str: String): SnyggRule? {
            val result = REGEX.matchEntire(str) ?: return null
            val elementName = result.groups["elementName"]!!.value // can not be null logically
            val attributesRaw = result.groups["attributesRaw"]?.value
            val selectorRaw = result.groups["selectorRaw"]?.value

            return SnyggElementRule(
                elementName = elementName,
                attributes = Attributes.from(attributesRaw ?: ""),
                selector = SnyggSelector.from(selectorRaw ?: ""),
            )
        }
    }

    data class Attributes internal constructor(
        private val attributes: Map<String, List<Int>> = emptyMap(),
    ) : Map<String, List<Int>> by attributes, Comparable<Attributes> {
        override fun compareTo(other: Attributes): Int {
            if (attributes.isEmpty() && other.attributes.isEmpty()) {
                return 0
            }
            val sizeDiff = attributes.size.compareTo(other.attributes.size)
            if (sizeDiff != 0) {
                return sizeDiff
            }
            // both have attributes at this point and size matches
            val attrs = attributes.entries.toList().sortedBy { it.key }
            val otherAttrs = other.attributes.entries.toList().sortedBy { it.key }
            for (attrIndex in 0..<min(attrs.size, otherAttrs.size)) {
                val attr = attrs[attrIndex]
                val otherAttr = otherAttrs[attrIndex]
                val keyDiff = attr.key.compareTo(otherAttr.key)
                if (keyDiff != 0) {
                    return keyDiff
                }
                for (valueIndex in 0..<min(attr.value.size, otherAttr.value.size)) {
                    val value = attr.value[valueIndex]
                    val otherValue = otherAttr.value[valueIndex]
                    val valueDiff = value.compareTo(otherValue)
                    if (valueDiff != 0) {
                        return valueDiff
                    }
                }
                val valueSizeDiff = attr.value.size.compareTo(otherAttr.value.size)
                if (valueSizeDiff != 0) {
                    return valueSizeDiff
                }
            }
            return 0
        }

        override fun toString(): String {
            return buildString {
                for ((key, values) in attributes) {
                    append(ATTRIBUTE_OPEN)
                    append(key)
                    append(ATTRIBUTE_ASSIGN)
                    val valueStrings = mutableListOf<String>()
                    values.fold(mutableListOf<IntRange>()) { acc, value ->
                        if (acc.isEmpty()) {
                            acc.add(value..value)
                        } else {
                            val lastRange = acc.last()
                            if (lastRange.last + 1 == value) {
                                acc[acc.size - 1] = lastRange.first..value
                            } else {
                                acc.add(value..value)
                            }
                        }
                        acc
                    }.forEach { range ->
                        when (range.first) {
                            range.last -> {
                                valueStrings.add(range.first.toString())
                            }
                            range.last - 1 -> {
                                valueStrings.add(range.first.toString())
                                valueStrings.add((range.first + 1).toString())
                            }
                            else -> {
                                valueStrings.add(range.toString())
                            }
                        }
                    }
                    append(valueStrings.joinToString(ATTRIBUTE_VALUES_SEPARATOR))
                    append(ATTRIBUTE_CLOSE)
                }
            }
        }

        companion object {
            internal fun from(str: String): Attributes {
                val attributes = mutableMapOf<String, List<Int>>()
                ATTRIBUTE_REGEX.findAll(str).forEach { attrRawMatch ->
                    val attrRaw = attrRawMatch.value.substring(1, attrRawMatch.value.length - 1)
                    val (key, valuesRaw) = attrRaw.split(ATTRIBUTE_ASSIGN)
                    val values = mutableSetOf<Int>()
                    ATTRIBUTE_VALUE_REGEX.findAll(valuesRaw).forEach { valueRawMatch ->
                        val valueRaw = valueRawMatch.value
                        if (valueRaw.contains("..")) {
                            val (start, end) = valueRaw.split("..")
                            values.addAll(start.toInt()..end.toInt())
                        } else {
                            values.add(valueRaw.toInt())
                        }
                    }
                    attributes[key] = values.toList().sorted()
                }
                return Attributes(attributes.toMap())
            }

            internal fun of(vararg pairs: Pair<String, List<Int>>) = Attributes(mapOf(*pairs))
        }
    }
}

enum class SnyggSelector(val id: String) {
    NONE("none"),
    PRESSED("pressed"),
    FOCUS("focus"),
    HOVER("hover"),
    DISABLED("disabled");

    override fun toString(): String {
        if (this == NONE) {
            return ""
        }
        return buildString {
            append(SELECTOR_COLON)
            append(id)
        }
    }

    companion object {
        private const val SELECTOR_COLON = ":"

        internal fun from(str: String): SnyggSelector {
            if (str.isNotEmpty()) {
                val selector = str.substring(1)
                return entries.first { it.id == selector }
            }
            return SnyggSelector.NONE
        }
    }
}
