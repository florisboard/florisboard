/*
 * Copyright (C) 2021 Patrick Goldinger
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

package dev.patrickgold.florisboard.snygg

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import dev.patrickgold.florisboard.common.kotlin.curlyFormat
import dev.patrickgold.florisboard.ime.text.key.InputMode
import dev.patrickgold.florisboard.ime.text.key.KeyCode
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.Serializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlin.Comparator

@Serializable(with = SnyggRuleSerializer::class)
data class SnyggRule(
    val isAnnotation: Boolean = false,
    val element: String,
    val codes: List<Int> = listOf(),
    val groups: List<Int> = listOf(),
    val modes: List<Int> = listOf(),
    val pressedSelector: Boolean = false,
    val focusSelector: Boolean = false,
    val disabledSelector: Boolean = false,
) : Comparable<SnyggRule> {

    companion object {
        const val ANNOTATION_MARKER = '@'

        const val ATTRIBUTE_OPEN = '['
        const val ATTRIBUTE_CLOSE = ']'
        const val ATTRIBUTE_ASSIGN = '='
        const val ATTRIBUTE_OR = '|'
        const val CODES_KEY = "code"
        const val GROUPS_KEY = "group"
        const val MODES_KEY = "mode"

        const val SELECTOR_COLON = ':'
        const val PRESSED_SELECTOR = "pressed"
        const val FOCUS_SELECTOR = "focus"
        const val DISABLED_SELECTOR = "disabled"

        @Suppress("RegExpRedundantEscape", "RegExpSingleCharAlternation")
        private val RuleValidator =
            """^(@?)[a-zA-Z0-9-]+(\[(code|group|mode)=(\+|-)?([0-9]+)(\|(\+|-)?([0-9]+))*\])*(:(pressed|focus|disabled))*${'$'}""".toRegex()
        private val placeholders = mapOf(
            "c:delete" to KeyCode.DELETE,
            "c:enter" to KeyCode.ENTER,
            "c:shift" to KeyCode.SHIFT,
            "c:space" to KeyCode.SPACE,
            "m:normal" to InputMode.NORMAL.value,
            "m:shiftlock" to InputMode.SHIFT_LOCK.value,
            "m:capslock" to InputMode.CAPS_LOCK.value,
        )

        val Comparator = Comparator<SnyggRule> { a, b ->
            when {
                a.isAnnotation && !b.isAnnotation -> -1
                !a.isAnnotation && b.isAnnotation -> 1
                else /* a.isAnnotation == b.isAnnotation */ -> {
                    when (val elem = a.element.compareTo(b.element)) {
                        0 -> a.comparatorWeight() - b.comparatorWeight()
                        else -> elem
                    }
                }
            }
        }

        val Saver = Saver<MutableState<SnyggRule?>, String>(
            save = { it.toString() },
            restore = { mutableStateOf(from(it)) },
        )

        fun from(raw: String): SnyggRule? {
            val str = raw.trim().curlyFormat { placeholders[it]?.toString() }
            if (!RuleValidator.matches(str) || str.isBlank()) {
                return null
            }
            try {
                val selectors = str.split(SELECTOR_COLON)
                val elementAndAttributes = selectors[0]
                var pressedSelector = false
                var focusSelector = false
                var disabledSelector = false
                if (selectors.size > 1) {
                    for (selector in selectors.listIterator(1)) {
                        when (selector) {
                            PRESSED_SELECTOR -> pressedSelector = true
                            FOCUS_SELECTOR -> focusSelector = true
                            DISABLED_SELECTOR -> disabledSelector = true
                        }
                    }
                }
                val codes = mutableListOf<Int>()
                val groups = mutableListOf<Int>()
                val modes = mutableListOf<Int>()
                val attributes = elementAndAttributes.split(ATTRIBUTE_OPEN, ATTRIBUTE_CLOSE)
                val isAnnotation = attributes[0].startsWith(ANNOTATION_MARKER)
                val element = if (isAnnotation) attributes[0].substring(1) else attributes[0]
                if (attributes.size > 1) {
                    for (attribute in attributes.listIterator(1)) {
                        if (attribute.isBlank()) continue
                        val (key, valueArray) = attribute.split(ATTRIBUTE_ASSIGN)
                        val values = valueArray.split(ATTRIBUTE_OR)
                        when (key) {
                            CODES_KEY -> codes
                            GROUPS_KEY -> groups
                            MODES_KEY -> modes
                            else -> null
                        }?.addAll(values.map { it.toInt(10) })
                    }
                }
                return SnyggRule(
                    isAnnotation, element, codes.toList(), groups.toList(), modes.toList(),
                    pressedSelector, focusSelector, disabledSelector,
                )
            } catch (e: Exception) {
                return null
            }
        }
    }

    override fun compareTo(other: SnyggRule): Int {
        return Comparator.compare(this, other)
    }

    override fun toString() = buildString {
        if (isAnnotation) {
            append(ANNOTATION_MARKER)
        }
        append(element)
        appendAttribute(CODES_KEY, codes)
        appendAttribute(GROUPS_KEY, groups)
        appendAttribute(MODES_KEY, modes)
        appendSelector(PRESSED_SELECTOR, pressedSelector)
        appendSelector(FOCUS_SELECTOR, focusSelector)
        appendSelector(DISABLED_SELECTOR, disabledSelector)
    }

    private fun comparatorWeight(): Int {
        return (if (codes.isNotEmpty()) 0x01 else 0) +
            (if (groups.isNotEmpty()) 0x02 else 0) +
            (if (modes.isNotEmpty()) 0x04 else 0) +
            (if (pressedSelector) 0x08 else 0) +
            (if (focusSelector) 0x10 else 0) +
            (if (disabledSelector) 0x20 else 0)
    }

    private fun StringBuilder.appendAttribute(key: String, entries: List<Int>) {
        if (entries.isNotEmpty()) {
            append(ATTRIBUTE_OPEN)
            append(key)
            append(ATTRIBUTE_ASSIGN)
            for ((n, entry) in entries.withIndex()) {
                if (n != 0) {
                    append(ATTRIBUTE_OR)
                }
                append(entry)
            }
            append(ATTRIBUTE_CLOSE)
        }
    }

    private fun StringBuilder.appendSelector(key: String, selector: Boolean) {
        if (selector) {
            append(SELECTOR_COLON)
            append(key)
        }
    }
}

@OptIn(ExperimentalSerializationApi::class)
@Serializer(forClass = SnyggRule::class)
class SnyggRuleSerializer : KSerializer<SnyggRule> {
    override val descriptor = PrimitiveSerialDescriptor("SnyggRule", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: SnyggRule) {
        encoder.encodeString(value.toString())
    }

    override fun deserialize(decoder: Decoder): SnyggRule {
        return SnyggRule.from(decoder.decodeString()) ?: SnyggRule(element = "invalid")
    }
}
