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

import dev.patrickgold.florisboard.common.curlyFormat
import dev.patrickgold.florisboard.common.stringBuilder
import dev.patrickgold.florisboard.ime.text.key.KeyCode
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.Serializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

private const val ATTRIBUTE_OPEN = '['
private const val ATTRIBUTE_CLOSE = ']'
private const val ATTRIBUTE_ASSIGN = '='
private const val ATTRIBUTE_OR = '|'
private const val CODES_KEY = "code"
private const val GROUPS_KEY = "group"
private const val MODES_KEY = "mode"

private const val SELECTOR_COLON = ':'
private const val HOVER_SELECTOR = "hover"
private const val FOCUS_SELECTOR = "focus"
private const val PRESSED_SELECTOR = "pressed"

@Serializable(with = SnyggRuleSerializer::class)
data class SnyggRule(
    val element: String,
    val codes: List<Int> = listOf(),
    val groups: List<Int> = listOf(),
    val modes: List<Int> = listOf(),
    val hoverSelector: Boolean = false,
    val focusSelector: Boolean = false,
    val pressedSelector: Boolean = false,
) {
    companion object {
        private val RuleValidator =
            """^[a-zA-Z0-9-]+(\[(code|group|normal)=(\+|-)?([0-9]+)(\|(\+|-)?([0-9]+))*\])*(:(hover|focus|pressed))*${'$'}""".toRegex()
        private val placeholders = mapOf(
            "c:delete" to KeyCode.DELETE,
            "c:shift" to KeyCode.SHIFT,
            "c:space" to KeyCode.SPACE,
            "m:normal" to 1,
            "m:shiftlock" to 2,
            "m:capslock" to 3,
        )

        fun from(raw: String): SnyggRule? {
            val str = raw.trim().curlyFormat { placeholders[it]?.toString() }
            if (!RuleValidator.matches(str) || str.isBlank()) {
                return null
            }
            try {
                val selectors = str.split(SELECTOR_COLON)
                val elementAndAttributes = selectors[0]
                var hoverSelector = false
                var focusSelector = false
                var pressedSelector = false
                if (selectors.size > 1) {
                    for (selector in selectors.listIterator(1)) {
                        when (selector) {
                            HOVER_SELECTOR -> hoverSelector = true
                            FOCUS_SELECTOR -> focusSelector = true
                            PRESSED_SELECTOR -> pressedSelector = true
                        }
                    }
                }
                val codes = mutableListOf<Int>()
                val groups = mutableListOf<Int>()
                val modes = mutableListOf<Int>()
                val attributes = elementAndAttributes.split(ATTRIBUTE_OPEN, ATTRIBUTE_CLOSE)
                val element = attributes[0]
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
                    element, codes.toList(), groups.toList(), modes.toList(),
                    hoverSelector, focusSelector, pressedSelector
                )
            } catch (e: Exception) {
                return null
            }
        }
    }

    override fun toString() = stringBuilder {
        append(element)
        appendAttribute(CODES_KEY, codes)
        appendAttribute(GROUPS_KEY, groups)
        appendAttribute(MODES_KEY, modes)
        appendSelector(HOVER_SELECTOR, hoverSelector)
        appendSelector(FOCUS_SELECTOR, focusSelector)
        appendSelector(PRESSED_SELECTOR, pressedSelector)
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
        return SnyggRule.from(decoder.decodeString()) ?: SnyggRule("invalid")
    }
}
