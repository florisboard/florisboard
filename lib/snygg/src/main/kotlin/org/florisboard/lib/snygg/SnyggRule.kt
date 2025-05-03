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

/**
 * Base interface for all Snygg stylesheet rules. A rule in a stylesheet is a core component, acting as the key for a
 * property set map. There are two main rule categories, annotation and element rules.
 *
 * - **Annotation rules**: Represent meta-rules, describing globally valid properties. This includes global style
 *   variables, global font faces, etc.
 * - **Element rules**: These rules target a specific element with a specific attribute/selector set. Used to describe
 *   the style of a specific element.
 *
 * @since 0.5.0-alpha01
 * @see [SnyggAnnotationRule.Defines]
 * @see [SnyggAnnotationRule.Font]
 * @see [SnyggElementRule]
 */
sealed interface SnyggRule : Comparable<SnyggRule> {
    /**
     * Returns the associated declaration of this rule.
     *
     * @since 0.5.0-alpha01
     */
    fun decl(): SnyggSpecDecl.RuleDecl

    /**
     * Compares this Snygg rule with [other]. The ordering is defined as follows:
     * - @defines
     * - @font (multiple fonts => sort by fontName)
     * - elements (first by element name, then by attributes/selectors)
     *
     * @since 0.5.0-alpha01
     */
    override fun compareTo(other: SnyggRule): Int

    /**
     * Serializes the Snygg rule to a string. This method never fails.
     *
     * @return The serialized representation of the Snygg rule instance.
     * @since 0.5.0-alpha01
     */
    override fun toString(): String

    companion object {
        /**
         * Attempts to parse the given string into a `SnyggRule` instance or `null` if no match is found.
         *
         * @param str The string to parse into a `SnyggRule`.
         * @return A `SnyggRule` instance if the string matches any supported rule type, or `null` if no match is found.
         * @since 0.5.0-alpha01
         */
        fun fromOrNull(str: String): SnyggRule? {
            return SnyggAnnotationRule.Defines.fromOrNull(str)
                ?: SnyggAnnotationRule.Font.fromOrNull(str)
                ?: SnyggElementRule.fromOrNull(str)
        }
    }
}

/**
 * Annotation rule base interface. See the specific implementations for details.
 *
 * @since 0.5.0-alpha01
 *
 * @see [SnyggAnnotationRule.Defines]
 * @see [SnyggAnnotationRule.Font]
 */
sealed interface SnyggAnnotationRule : SnyggRule {
    data object Defines : SnyggAnnotationRule, SnyggSpecDecl.RuleDecl {
        override val name: String = "defines"

        override val pattern = """^@$name$""".toRegex()

        override fun decl() = this

        override fun compareTo(other: SnyggRule): Int {
            return when (other) {
                is Defines -> 0 // same
                is SnyggAnnotationRule -> decl().name.compareTo(other.decl().name)
                is SnyggElementRule -> -1 // annotations always come first
            }
        }

        override fun toString(): String {
            return "@defines"
        }

        /**
         * Attempts to parse the given string into a `defines` annotation rule instance, or `null` if the given string
         * does not represent a `defines` annotation rule.
         *
         * @param str The string to parse into a `defines` annotation rule instance.
         * @return A `defines` annotation rule instance or `null`.
         *
         * @since 0.5.0-alpha01
         */
        fun fromOrNull(str: String): Defines? {
            pattern.matchEntire(str) ?: return null
            return Defines
        }
    }

    data class Font(val fontName: String) : SnyggAnnotationRule {
        override fun decl() = Companion

        override fun compareTo(other: SnyggRule): Int {
            return when (other) {
                is Font -> fontName.compareTo(other.fontName)
                is SnyggAnnotationRule -> decl().name.compareTo(other.decl().name)
                is SnyggElementRule -> -1 // annotations always come first
            }
        }

        override fun toString(): String {
            return "@font `$fontName`"
        }

        companion object : SnyggSpecDecl.RuleDecl {
            override val name = "font"
            override val pattern = """^@$name `(?<fontName>[a-zA-Z0-9\s-]+)`$""".toRegex()

            /**
             * Attempts to parse the given string into a `font` annotation rule instance, or `null` if the given string
             * does not represent a `font` annotation rule.
             *
             * @param str The string to parse into a `font` annotation rule instance.
             * @return A `font` annotation rule instance or `null`.
             *
             * @since 0.5.0-alpha01
             */
            fun fromOrNull(str: String): Font? {
                val match = pattern.matchEntire(str) ?: return null
                return Font(match.groups["fontName"]!!.value)
            }
        }
    }
}

/**
 * A core element in the Snygg styling system, this rule allows targeting specific elements with specific attributes
 * and selectors.
 *
 * @property elementName The element name this rule targets, it can be seen similarly to a CSS class.
 * @property attributes The attributes this rule targets.
 * @property selector The selector this rule targets, or [SnyggSelector.NONE] for not specified.
 *
 * @since 0.5.0-alpha01
 */
data class SnyggElementRule(
    val elementName: String,
    val attributes: SnyggAttributes = SnyggAttributes(),
    val selector: SnyggSelector = SnyggSelector.NONE,
) : SnyggRule {
    init {
        requireNotNull(ELEMENT_NAME_REGEX.matchEntire(elementName)) { "element name is invalid" }
    }

    override fun decl() = Companion

    override fun compareTo(other: SnyggRule): Int {
        if (other !is SnyggElementRule) {
            return 1 // annotations always come first
        }
        val elemDiff = elementName.compareTo(other.elementName)
        if (elemDiff != 0) {
            return elemDiff
        }
        if (selector != SnyggSelector.NONE || other.selector != SnyggSelector.NONE) {
            if (selector == SnyggSelector.NONE) {
                return -1
            }
            if (other.selector == SnyggSelector.NONE) {
                return 1
            }
            val selectorDiff = selector.compareTo(other.selector)
            if (selectorDiff != 0) {
                return selectorDiff
            }
        }
        val attrDiff = attributes.compareTo(other.attributes)
        return attrDiff
    }

    override fun toString() = buildString {
        append(elementName)
        append(attributes)
        append(selector)
    }

    companion object : SnyggSpecDecl.RuleDecl {
        override val name = "element"
        private val ELEMENT_NAME_REGEX = """(?<elementName>[a-zA-Z0-9-]+)""".toRegex()

        private val ATTRIBUTES_REGEX = """(?<attributesRaw>(?:${SnyggAttributes.ATTRIBUTE_REGEX})+)?""".toRegex()
        private val SELECTOR_REGEX = """(?<selectorRaw>:pressed|:focus|:hover|:disabled)?""".toRegex()
        override val pattern = """^$ELEMENT_NAME_REGEX$ATTRIBUTES_REGEX$SELECTOR_REGEX$""".toRegex()

        /**
         * Attempts to parse the given string into an element rule instance, or `null` if the given string
         * does not represent an element rule instance.
         *
         * @param str the string to parse into an element rule instance
         * @return an element rule instance or `null`
         *
         * @since 0.5.0-alpha01
         */
        fun fromOrNull(str: String): SnyggElementRule? {
            val result = pattern.matchEntire(str) ?: return null
            val elementName = result.groups["elementName"]!!.value // cannot be null logically
            val attributesRaw = result.groups["attributesRaw"]?.value
            val selectorRaw = result.groups["selectorRaw"]?.value

            return SnyggElementRule(
                elementName = elementName,
                attributes = SnyggAttributes.from(attributesRaw ?: ""),
                selector = SnyggSelector.from(selectorRaw ?: ""),
            )
        }
    }
}

data class SnyggAttributes internal constructor(
    private val attributes: Map<String, List<String>> = emptyMap(),
) : Map<String, List<String>> by attributes, Comparable<SnyggAttributes> {
    override fun compareTo(other: SnyggAttributes): Int {
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

    /**
     * Serializes the attributes to a string.
     *
     * @return The serialized representation of the attributes.
     *
     * @since 0.5.0-alpha01
     */
    override fun toString() = buildString {
        for ((key, values) in attributes.entries.sortedBy { it.key }) {
            if (values.isEmpty()) {
                continue
            }
            append(ATTRIBUTE_OPEN)
            append(key)
            append(ATTRIBUTE_ASSIGN)
            val (ints, strings) = values.partition { value ->
                value.toIntOrNull() != null
            }
            val serializedValues = buildList {
                ints.sorted().fold(mutableListOf()) { acc, valueStr ->
                    val value = valueStr.toInt()
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
                            add(range.first.toString())
                        }
                        range.last - 1 -> {
                            add(range.first.toString())
                            add((range.first + 1).toString())
                        }
                        else -> {
                            add(range.toString())
                        }
                    }
                }
                addAll(strings.map { string ->
                    buildString {
                        append('`')
                        append(string)
                        append('`')
                    }
                }.sorted())
            }
            append(serializedValues.joinToString(ATTRIBUTE_VALUES_SEPARATOR))
            append(ATTRIBUTE_CLOSE)
        }
    }

    /**
     * Copies the attributes, including the given key-value pairs. Duplicate key-value pairs are ignored.
     *
     * @param pairs The list of key-value pairs to include in the copy.
     * @return A new copy of the attribute mapping.
     *
     * @since 0.5.0-alpha01
     */
    fun including(vararg pairs: Pair<String, Any>): SnyggAttributes {
        val copy = attributes.toMutableMap()
        pairs.forEach { (key, anyValue) ->
            val value = anyValue.toString()
            copy[key] = buildSet {
                addAll(copy[key].orEmpty())
                add(value)
            }.toList().sorted()
        }
        return SnyggAttributes(copy.toMap())
    }

    /**
     * Copies the attributes, excluding the given key-value pairs. Ignores non-present pairs.
     *
     * @param pairs The list of key-value pairs to exclude in the copy.
     * @return A new copy of the attribute mapping.
     *
     * @since 0.5.0-alpha01
     */
    fun excluding(vararg pairs: Pair<String, Any>): SnyggAttributes {
        val copy = attributes.toMutableMap()
        pairs.forEach { (key, anyValue) ->
            val value = anyValue.toString()
            val list = buildSet {
                addAll(copy[key].orEmpty())
                remove(value)
            }.toList().sorted()
            if (list.isNotEmpty()) {
                copy[key] = list
            } else {
                copy.remove(key)
            }
        }
        return SnyggAttributes(copy.toMap())
    }

    /**
     * Copies the attributes, toggling the given key-value pairs. Existing key-value pairs will be removed,
     * non-existing will be added.
     *
     * @param pairs The list of key-value pairs to toggle in the copy.
     * @return A new copy of the attribute mapping.
     *
     * @since 0.5.0-alpha01
     */
    fun toggling(vararg pairs: Pair<String, Any>): SnyggAttributes {
        val copy = attributes.toMutableMap()
        pairs.forEach { (key, anyValue) ->
            val value = anyValue.toString()
            val list = buildSet {
                addAll(copy[key].orEmpty())
                if (contains(value)) {
                    remove(value)
                } else {
                    add(value)
                }
            }.toList().sorted()
            if (list.isNotEmpty()) {
                copy[key] = list
            } else {
                copy.remove(key)
            }
        }
        return SnyggAttributes(copy.toMap())
    }

    @Suppress("RegExpUnnecessaryNonCapturingGroup")
    companion object {
        private const val ATTRIBUTE_OPEN = "["
        private const val ATTRIBUTE_CLOSE = "]"
        private const val ATTRIBUTE_ASSIGN = "="
        private const val ATTRIBUTE_VALUES_SEPARATOR = ","

        private val INT_PATTERN = """(?:0|-?[1-9][0-9]*)""".toRegex()
        private val INT_RANGE_PATTERN = """$INT_PATTERN[.]{2}$INT_PATTERN""".toRegex()
        private val STRING_PATTERN = """`[^`]+`""".toRegex()
        private val ATTR_VALUE_PATTERN = """(?:$STRING_PATTERN|$INT_RANGE_PATTERN|$INT_PATTERN)""".toRegex()
        internal val ATTRIBUTE_REGEX = """\[(?<attrKey>[a-zA-Z0-9-]+)=(?<attrRawValues>$ATTR_VALUE_PATTERN(?:,$ATTR_VALUE_PATTERN)*)]""".toRegex()

        internal fun from(str: String): SnyggAttributes {
            val attributes = mutableMapOf<String, List<String>>()
            for (attrMatch in ATTRIBUTE_REGEX.findAll(str)) {
                val key = attrMatch.groups["attrKey"]!!.value
                val rawValues = attrMatch.groups["attrRawValues"]!!.value
                attributes[key] = buildList {
                    attributes[key]?.let { addAll(it) }
                    for (attrValueMatch in ATTR_VALUE_PATTERN.findAll(rawValues)) {
                        val rawValue = attrValueMatch.value
                        if (STRING_PATTERN.matches(rawValue)) {
                            add(rawValue.substring(1, rawValue.length - 1))
                            continue
                        }
                        if (INT_RANGE_PATTERN.matches(rawValue)) {
                            val (start, end) = rawValue.split("..").map { it.toInt() }
                            addAll((start..end).map { it.toString() })
                            continue
                        }
                        if (INT_PATTERN.matches(rawValue)) {
                            add(rawValue)
                            continue
                        }
                    }
                    sort()
                }.distinct()
            }
            return SnyggAttributes(attributes.toMap())
        }

        internal fun of(vararg pairs: Pair<String, List<Any>>) = SnyggAttributes(
            mapOf(
                *pairs.map { (key, values) -> key to values.map { it.toString() }.distinct() }.toTypedArray()
            )
        )
    }
}

/**
 * A Snygg selector describes the interaction state of a component. Within stylesheets, this can be used in element
 * rules to target specific interaction states of elements for styling. Within the UI implementation this is used to
 * pass the current interaction state to Snygg to allow for correct style resolving.
 *
 * @property id The id of the selector.
 *
 * @since 0.5.0-alpha01
 */
enum class SnyggSelector(val id: String) {
    /**
     * No interaction is active. Only used within UI implementation, is not serialized.
     *
     * @since 0.5.0-alpha01
     */
    NONE("none"),

    /**
     * Pressed interaction.
     *
     * @since 0.5.0-alpha01
     */
    PRESSED("pressed"),

    /**
     * Focus interaction.
     *
     * @since 0.5.0-alpha01
     */
    FOCUS("focus"),

    /**
     * Hover interaction.
     *
     * @since 0.5.0-alpha01
     */
    HOVER("hover"),

    /**
     * Disabled state. Used for inputs and buttons.
     *
     * @since 0.5.0-alpha01
     */
    DISABLED("disabled");

    /**
     * Serializes the selector to a string. If [NONE], an empty string is returned.
     *
     * @return The serialized representation of this selector.
     *
     * @since 0.5.0-alpha01
     */
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
            return NONE
        }
    }
}
