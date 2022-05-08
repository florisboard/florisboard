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

package dev.patrickgold.florisboard.lib.snygg

import androidx.compose.runtime.saveable.Saver
import dev.patrickgold.florisboard.ime.input.InputShiftState
import dev.patrickgold.florisboard.ime.text.key.KeyCode
import dev.patrickgold.florisboard.ime.theme.FlorisImeUi
import dev.patrickgold.florisboard.lib.kotlin.curlyFormat
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable(with = SnyggRule.Serializer::class)
data class SnyggRule(
    val isAnnotation: Boolean = false,
    val element: String,
    val codes: List<Int> = emptyList(),
    val groups: List<Int> = emptyList(),
    val shiftStates: List<Int> = emptyList(),
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
        const val SHIFT_STATES_KEY = "shiftstate"

        const val SELECTOR_COLON = ':'
        const val PRESSED_SELECTOR = "pressed"
        const val FOCUS_SELECTOR = "focus"
        const val DISABLED_SELECTOR = "disabled"

        @Suppress("RegExpRedundantEscape", "RegExpSingleCharAlternation")
        private val RuleValidator =
            """^(@?)[a-zA-Z0-9-]+(\[(code|group|shiftstate)=(\+|-)?([0-9]+)(\|(\+|-)?([0-9]+))*\])*(:(pressed|focus|disabled))*${'$'}""".toRegex()

        val Placeholders = mapOf(
            "c:delete" to KeyCode.DELETE,
            "c:enter" to KeyCode.ENTER,
            "c:shift" to KeyCode.SHIFT,
            "c:space" to KeyCode.SPACE,
            "sh:unshifted" to InputShiftState.UNSHIFTED.value,
            "sh:shifted_manual" to InputShiftState.SHIFTED_MANUAL.value,
            "sh:shifted_automatic" to InputShiftState.SHIFTED_AUTOMATIC.value,
            "sh:caps_lock" to InputShiftState.CAPS_LOCK.value,
        )

        private val PreferredElementSorting = listOf(
            FlorisImeUi.Keyboard,
            FlorisImeUi.Key,
            FlorisImeUi.KeyHint,
            FlorisImeUi.KeyPopup,
            FlorisImeUi.SmartbarPrimaryRow,
            FlorisImeUi.SmartbarSecondaryRow,
            FlorisImeUi.SmartbarPrimaryActionsToggle,
            FlorisImeUi.SmartbarSecondaryActionsToggle,
            FlorisImeUi.SmartbarQuickAction,
            FlorisImeUi.SmartbarKey,
            FlorisImeUi.SmartbarCandidateWord,
            FlorisImeUi.SmartbarCandidateClip,
            FlorisImeUi.SmartbarCandidateSpacer,
        )

        val Saver = Saver<SnyggRule?, String>(
            save = { it?.toString() ?: "" },
            restore = { from(it) },
        )

        fun from(raw: String): SnyggRule? {
            val str = raw.trim().curlyFormat { Placeholders[it]?.toString() }
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
                val shiftStates = mutableListOf<Int>()
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
                            SHIFT_STATES_KEY -> shiftStates
                            else -> null
                        }?.addAll(values.map { it.toInt(10) })
                    }
                }
                return SnyggRule(
                    isAnnotation, element, codes.toList(), groups.toList(), shiftStates.toList(),
                    pressedSelector, focusSelector, disabledSelector,
                )
            } catch (e: Exception) {
                return null
            }
        }
    }

    override fun toString() = buildString {
        if (isAnnotation) {
            append(ANNOTATION_MARKER)
        }
        append(element)
        appendAttribute(CODES_KEY, codes)
        appendAttribute(GROUPS_KEY, groups)
        appendAttribute(SHIFT_STATES_KEY, shiftStates)
        appendSelector(PRESSED_SELECTOR, pressedSelector)
        appendSelector(FOCUS_SELECTOR, focusSelector)
        appendSelector(DISABLED_SELECTOR, disabledSelector)
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

    override fun compareTo(other: SnyggRule): Int {
        return when {
            this.isAnnotation && !other.isAnnotation -> -1
            !this.isAnnotation && other.isAnnotation -> 1
            else -> when (val elem = this.element.compareTo(other.element)) {
                0 -> when (val diff = this.comparatorWeight() - other.comparatorWeight()) {
                    0 -> when {
                        this.codes.size != other.codes.size -> this.codes.size.compareTo(other.codes.size)
                        this.groups.size != other.groups.size -> this.groups.size.compareTo(other.groups.size)
                        this.shiftStates.size != other.shiftStates.size -> this.shiftStates.size.compareTo(other.shiftStates.size)
                        else -> {
                            this.codes.indices.firstNotNullOfOrNull { n ->
                                (this.codes[n].compareTo(other.codes[n])).takeIf { it != 0 }
                            } ?: this.groups.indices.firstNotNullOfOrNull { n ->
                                (this.groups[n].compareTo(other.groups[n])).takeIf { it != 0 }
                            } ?: this.shiftStates.indices.firstNotNullOfOrNull { n ->
                                (this.shiftStates[n].compareTo(other.shiftStates[n])).takeIf { it != 0 }
                            } ?: 0
                        }
                    }
                    else -> diff
                }
                else -> {
                    val a = PreferredElementSorting.indexOf(this.element).let { if (it < 0) Int.MAX_VALUE else it }
                    val b = PreferredElementSorting.indexOf(other.element).let { if (it < 0) Int.MAX_VALUE else it }
                    a.compareTo(b).let { if (it == 0) elem else it }
                }
            }
        }
    }

    private fun comparatorWeight(): Int {
        return (if (codes.isNotEmpty()) 0x01 else 0) +
            (if (groups.isNotEmpty()) 0x02 else 0) +
            (if (shiftStates.isNotEmpty()) 0x04 else 0) +
            (if (pressedSelector) 0x08 else 0) +
            (if (focusSelector) 0x10 else 0) +
            (if (disabledSelector) 0x20 else 0)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SnyggRule

        if (isAnnotation != other.isAnnotation) return false
        if (element != other.element) return false
        if (!codes.containsAll(other.codes) || !other.codes.containsAll(codes)) return false
        if (!groups.containsAll(other.groups) || !other.groups.containsAll(groups)) return false
        if (!shiftStates.containsAll(other.shiftStates) || !other.shiftStates.containsAll(shiftStates)) return false
        if (pressedSelector != other.pressedSelector) return false
        if (focusSelector != other.focusSelector) return false
        if (disabledSelector != other.disabledSelector) return false

        return true
    }

    override fun hashCode(): Int {
        var result = isAnnotation.hashCode()
        result = 31 * result + element.hashCode()
        for (code in codes.sorted()) {
            result = 31 * result + code.hashCode()
        }
        for (group in groups.sorted()) {
            result = 31 * result + group.hashCode()
        }
        for (mode in shiftStates.sorted()) {
            result = 31 * result + mode.hashCode()
        }
        result = 31 * result + pressedSelector.hashCode()
        result = 31 * result + focusSelector.hashCode()
        result = 31 * result + disabledSelector.hashCode()
        return result
    }

    object Serializer : KSerializer<SnyggRule> {
        override val descriptor = PrimitiveSerialDescriptor("SnyggRule", PrimitiveKind.STRING)

        override fun serialize(encoder: Encoder, value: SnyggRule) {
            encoder.encodeString(value.toString())
        }

        override fun deserialize(decoder: Decoder): SnyggRule {
            return from(decoder.decodeString()) ?: SnyggRule(element = "invalid")
        }
    }
}

fun SnyggRule.Companion.definedVariablesRule(): SnyggRule {
    return SnyggRule(isAnnotation = true, element = "defines")
}

fun SnyggRule.isDefinedVariablesRule(): Boolean {
    return this.isAnnotation && this.element == "defines"
}
