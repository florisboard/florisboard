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

import androidx.compose.runtime.saveable.Saver

data class SnyggRule internal constructor(
    val elementName: String,
    val attributes: Attributes = Attributes(),
    val selectors: Selectors = Selectors(),
): Comparable<SnyggRule> {
    val isAnnotation
        get() = elementName.startsWith("@")

    init {
        requireNotNull(
            ANNOTATION_NAME_REGEX.matchEntire(elementName) ?:
                ELEMENT_NAME_REGEX.matchEntire(elementName)
        ) { "annotation/element name is invalid" }
    }

    override fun compareTo(other: SnyggRule): Int {
        if (isAnnotation && !other.isAnnotation) {
            return -1
        }
        if (!isAnnotation && other.isAnnotation) {
            return 1
        }
        val elemDiff = elementName.compareTo(other.elementName)
        if (elemDiff != 0) {
            return elemDiff
        }
        val attrDiff = attributes.size.compareTo(other.attributes.size)
        if (attrDiff != 0) {
            return attrDiff
        }
        return selectors.compareTo(other.selectors)
    }

    override fun toString(): String {
        return buildString {
            append(elementName)
            append(attributes.toString())
            append(selectors.toString())
        }
    }

    companion object {
        private const val ATTRIBUTE_OPEN = "["
        private const val ATTRIBUTE_CLOSE = "]"
        private const val ATTRIBUTE_ASSIGN = "="
        private const val ATTRIBUTE_VALUES_SEPARATOR = ","

        private val ANNOTATION_NAME_REGEX = """(?<annotationName>@[a-zA-Z0-9-]+)""".toRegex()
        private val ELEMENT_NAME_REGEX = """(?<elementName>[a-zA-Z0-9-]+)""".toRegex()
        private val ATTRIBUTE_VALUE_REGEX = """[+-]?(?:0|[1-9][0-9]*)(?:[.]{2}[+-]?(?:0|[1-9][0-9]*))?""".toRegex()
        private val ATTRIBUTE_REGEX = """\[[a-zA-Z0-9-]+=$ATTRIBUTE_VALUE_REGEX(?:,$ATTRIBUTE_VALUE_REGEX)*]""".toRegex()
        private val ATTRIBUTES_REGEX = """(?<attributesRaw>(?:$ATTRIBUTE_REGEX)+)?""".toRegex()
        private val SELECTOR_REGEX = """:pressed|:focus|:disabled""".toRegex()
        private val SELECTORS_REGEX = """(?<selectorsRaw>(?:$SELECTOR_REGEX)+)?""".toRegex()
        private val REGEX = """^$ANNOTATION_NAME_REGEX|$ELEMENT_NAME_REGEX$ATTRIBUTES_REGEX$SELECTORS_REGEX${'$'}""".toRegex()

        val Saver = Saver<SnyggRule?, String>(
            save = { it?.toString() ?: "" },
            restore = { fromOrNull(it) },
        )

        fun fromOrNull(str: String): SnyggRule? {
            val result = REGEX.matchEntire(str) ?: return null
            val annotationName = result.groups["annotationName"]?.value
            if (annotationName != null) {
                return SnyggRule(elementName = annotationName)
            }
            val elementName = result.groups["elementName"]?.value ?: return null
            val attributesRaw = result.groups["attributesRaw"]?.value
            val selectorsRaw = result.groups["selectorsRaw"]?.value

            return SnyggRule(
                elementName = elementName,
                attributes = Attributes.from(attributesRaw ?: ""),
                selectors = Selectors.from(selectorsRaw ?: ""),
            )
        }
    }

    data class Attributes internal constructor(
        private val attributes: Map<String, List<Int>> = emptyMap(),
    ) : Map<String, List<Int>> by attributes {
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

    data class Selectors internal constructor(
        val pressed: Boolean = false,
        val focus: Boolean = false,
        val disabled: Boolean = false,
    ): Comparable<Selectors> {
        override fun compareTo(other: Selectors): Int {
            return comparatorWeight().compareTo(other.comparatorWeight())
        }

        private fun comparatorWeight(): Int {
            return (if (pressed) 0x1 else 0) +
                (if (focus) 0x2 else 0) +
                (if (disabled) 0x4 else 0)
        }

        override fun toString(): String {
            return buildString {
                if (pressed) {
                    append(SELECTOR_COLON)
                    append(PRESSED)
                }
                if (focus) {
                    append(SELECTOR_COLON)
                    append(FOCUS)
                }
                if (disabled) {
                    append(SELECTOR_COLON)
                    append(DISABLED)
                }
            }
        }

        companion object {
            private const val SELECTOR_COLON = ":"
            private const val PRESSED = "pressed"
            private const val FOCUS = "focus"
            private const val DISABLED = "disabled"

            internal fun from(str: String): Selectors {
                var pressed = false
                var focus = false
                var disabled = false
                SELECTOR_REGEX.findAll(str).forEach { match ->
                    val selector = match.value.substring(1)
                    when (selector) {
                        PRESSED -> pressed = true
                        FOCUS -> focus = true
                        DISABLED -> disabled = true
                    }
                }
                return Selectors(pressed, focus, disabled)
            }
        }
    }
}

fun SnyggRule.isDefinedVariablesRule(): Boolean {
    return this.elementName == "@defines"
}

fun SnyggRule.Companion.definedVariablesRule(): SnyggRule {
    return SnyggRule("@defines")
}
