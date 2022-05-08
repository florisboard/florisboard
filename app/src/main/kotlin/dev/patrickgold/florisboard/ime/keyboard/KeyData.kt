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

package dev.patrickgold.florisboard.ime.keyboard

import androidx.compose.ui.unit.LayoutDirection
import dev.patrickgold.florisboard.ime.input.InputShiftState
import dev.patrickgold.florisboard.ime.popup.PopupSet
import dev.patrickgold.florisboard.ime.text.key.KeyCode
import dev.patrickgold.florisboard.ime.text.key.KeyType
import dev.patrickgold.florisboard.ime.text.key.KeyVariation
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Basic interface for a key data object. Base for all key data objects across the IME, such as text, emojis and
 * selectors. The implementation is as abstract as possible, as different features require different implementations.
 */
interface AbstractKeyData {
    /**
     * Computes a [KeyData] object for this key data. Returns null if no computation is possible or if the key is
     * not relevant based on the result of [evaluator].
     *
     * @param evaluator The evaluator used to retrieve different states from the parent controller.
     *
     * @return A [KeyData] object or null if no computation is possible.
     */
    fun compute(evaluator: ComputingEvaluator): KeyData?

    /**
     * Returns the data described by this key as a string.
     *
     * @param isForDisplay Specifies if the returned string is intended to be displayed in a UI label (=true) or if
     *  it should be computed to be sent to an input connection (=false).
     *
     * @return The computed string for the key data object. Note: some objects may return an empty string here, meaning
     *  it is always required to check for the string's length before attempting to directly retrieve the first char.
     */
    fun asString(isForDisplay: Boolean): String
}

/**
 * Interface describing a basic key which can carry a character, an emoji, a special function etc. while being as
 * abstract as possible.
 *
 * @property type The type of the key.
 * @property code The Unicode code point of this key, or a special code from [KeyCode].
 * @property label The label of the key. This should always be a representative string for [code].
 * @property groupId The group which this key belongs to.
 * @property popup The popups for ths key. Can also dynamically be provided via popup extensions.
 */
interface KeyData : AbstractKeyData {
    val type: KeyType
    val code: Int
    val label: String
    val groupId: Int
    val popup: PopupSet<AbstractKeyData>?

    companion object {
        /**
         * Constant for the default group. If not otherwise specified, any key is automatically
         * assigned to this group.
         */
        const val GROUP_DEFAULT: Int = 0

        /**
         * Constant for the Left modifier key group. Any key belonging to this group will get the
         * popups specified for "~left" in the popup mapping.
         */
        const val GROUP_LEFT: Int = 1

        /**
         * Constant for the right modifier key group. Any key belonging to this group will get the
         * popups specified for "~right" in the popup mapping.
         */
        const val GROUP_RIGHT: Int = 2

        /**
         * Constant for the enter modifier key group. Any key belonging to this group will get the
         * popups specified for "~enter" in the popup mapping.
         */
        const val GROUP_ENTER: Int = 3

        /**
         * Constant for the enter modifier key group. Any key belonging to this group will get the
         * popups specified for "~kana" in the popup mapping.
         */
        const val GROUP_KANA: Int = 97
    }

    fun isSpaceKey(): Boolean {
        return type == KeyType.CHARACTER && (code == KeyCode.SPACE || code == KeyCode.CJK_SPACE
            || code == KeyCode.HALF_SPACE || code == KeyCode.KESHIDA)
    }
}

/**
 * Allows to select an [AbstractKeyData] based on the current caps state. Note that this type of selector only really
 * makes sense in a text context, though technically speaking it can be used anywhere, so this implementation allows
 * for any [AbstractKeyData] to be used here. The JSON class identifier for this selector is `case_selector`.
 *
 * Example usage in a layout JSON file:
 * ```
 * { "$": "case_selector",
 *   "lower": { "code":   59, "label": ";" },
 *   "upper": { "code":   58, "label": ":" }
 * }
 * ```
 *
 * @property lower The key data to use if the current caps state is lowercase.
 * @property upper The key data to use if the current caps state is uppercase.
 */
@Serializable
@SerialName("case_selector")
class CaseSelector(
    val lower: AbstractKeyData,
    val upper: AbstractKeyData,
) : AbstractKeyData {
    override fun compute(evaluator: ComputingEvaluator): KeyData? {
        return (if (evaluator.activeState().isUppercase) { upper } else { lower }).compute(evaluator)
    }

    override fun asString(isForDisplay: Boolean): String {
        return ""
    }
}

/**
 * Allows to select an [AbstractKeyData] based on the current shift state. Note that this type of selector only really
 * makes sense in a text context, though technically speaking it can be used anywhere, so this implementation allows
 * for any [AbstractKeyData] to be used here. The JSON class identifier for this selector is `shift_state_selector`.
 *
 * Example usage in a layout JSON file:
 * ```
 * { "$": "shift_state_selector",
 *   "shiftedManual": { "code":   59, "label": ";" },
 *   "default": { "code":   58, "label": ":" }
 * }
 * ```
 *
 * @property unshifted The key data to use if the current shift state is [InputShiftState.UNSHIFTED], falling back to
 *  [default] if unspecified.
 * @property shifted The key data to use if the current shift state is either [InputShiftState.SHIFTED_MANUAL] or
 *  [InputShiftState.SHIFTED_AUTOMATIC]. Is overridden if [shiftedManual] or [shiftedAutomatic] is specified.
 * @property shiftedManual The key data to use if the current shift state is [InputShiftState.SHIFTED_MANUAL],
 *  falling back to [shifted] or [default] if unspecified.
 * @property shiftedAutomatic The key data to use if the current shift state is [InputShiftState.SHIFTED_AUTOMATIC],
 *  falling back to [shifted] or [default] if unspecified.
 * @property capsLock The key data to use if the current shift state is [InputShiftState.CAPS_LOCK], falling back to
 *  [default] if unspecified.
 * @property default The key data to use if the current shift state is set to a value not specified by this selector.
 *  If a key data is provided for all shift states possible this key data will never be used.
 */
@Serializable
@SerialName("shift_state_selector")
class ShiftStateSelector(
    val unshifted: AbstractKeyData? = null,
    val shifted: AbstractKeyData? = null,
    val shiftedManual: AbstractKeyData? = null,
    val shiftedAutomatic: AbstractKeyData? = null,
    val capsLock: AbstractKeyData? = null,
    val default: AbstractKeyData? = null,
) : AbstractKeyData {
    override fun compute(evaluator: ComputingEvaluator): KeyData? {
        return when (evaluator.activeState().inputShiftState) {
            InputShiftState.UNSHIFTED -> unshifted ?: default
            InputShiftState.SHIFTED_MANUAL -> shiftedManual ?: shifted ?: default
            InputShiftState.SHIFTED_AUTOMATIC -> shiftedAutomatic ?: shifted ?: default
            InputShiftState.CAPS_LOCK -> capsLock ?: default
        }?.compute(evaluator)
    }

    override fun asString(isForDisplay: Boolean): String {
        return ""
    }
}

/**
 * Allows to select an [AbstractKeyData] based on the current variation. Note that this type of selector only really
 * makes sense in a text context, though technically speaking it can be used anywhere, so this implementation allows
 * for any [AbstractKeyData] to be used here. The JSON class identifier for this selector is `variation_selector`.
 *
 * Example usage in a layout JSON file:
 * ```
 * { "$": "variation_selector",
 *   "default":  { "code":   44, "label": "," },
 *   "email":    { "code":   64, "label": "@" },
 *   "uri":      { "code":   47, "label": "/" }
 * }
 * ```
 *
 * @property default The default key data which should be used in case no key variation is known or for the current
 *  key variation no override key is defined. Can be null, in this case this may mean the variation selector hides
 *  the key if no direct match is present.
 * @property email The key data to use if [KeyVariation.EMAIL_ADDRESS] is the active key variation. If this value is
 *  null, [default] will be used instead.
 * @property uri The key data to use if [KeyVariation.URI] is the active key variation. If this value is null,
 *  [default] will be used instead.
 * @property normal The key data to use if [KeyVariation.NORMAL] is the active key variation. If this value is null,
 *  [default] will be used instead.
 * @property password The key data to use if [KeyVariation.PASSWORD] is the active key variation. If this value is
 *  null, [default] will be used instead.
 */
@Serializable
@SerialName("variation_selector")
data class VariationSelector(
    val default: AbstractKeyData? = null,
    val email: AbstractKeyData? = null,
    val uri: AbstractKeyData? = null,
    val normal: AbstractKeyData? = null,
    val password: AbstractKeyData? = null,
) : AbstractKeyData {
    override fun compute(evaluator: ComputingEvaluator): KeyData? {
        return when (evaluator.activeState().keyVariation) {
            KeyVariation.ALL -> default
            KeyVariation.EMAIL_ADDRESS -> email ?: default
            KeyVariation.NORMAL -> normal ?: default
            KeyVariation.PASSWORD -> password ?: default
            KeyVariation.URI -> uri ?: default
        }?.compute(evaluator)
    }

    override fun asString(isForDisplay: Boolean): String {
        return ""
    }
}

/**
 * Allows to select an [AbstractKeyData] based on the current layout direction. Note that this type of selector only
 * really makes sense in a text context, though technically speaking it can be used anywhere, so this implementation
 * allows for any [AbstractKeyData] to be used here. The JSON class identifier for this selector is
 * `layout_direction_selector`.
 *
 * Example usage in a layout JSON file:
 * ```
 * { "$": "layout_direction_selector",
 *   "ltr": { "code":   59, "label": ";" },
 *   "rtl": { "code":   58, "label": ":" }
 * }
 * ```
 *
 * @property ltr The key data to use if the current layout direction is LTR.
 * @property rtl The key data to use if the current layout direction is RTL.
 */
@Serializable
@SerialName("layout_direction_selector")
class LayoutDirectionSelector(
    val ltr: AbstractKeyData,
    val rtl: AbstractKeyData,
) : AbstractKeyData {
    override fun compute(evaluator: ComputingEvaluator): KeyData? {
        val isRtl = evaluator.activeState().layoutDirection == LayoutDirection.Rtl
        return (if (isRtl) { rtl } else { ltr }).compute(evaluator)
    }

    override fun asString(isForDisplay: Boolean): String {
        return ""
    }
}

/**
 * Allows to select an [AbstractKeyData] based on the character's width. Note that this type of selector only really
 * makes sense in a text context, though technically speaking it can be used anywhere, so this implementation allows
 * for any [AbstractKeyData] to be used here. The JSON class identifier for this selector is `char_width_selector`.
 *
 * Example usage in a layout JSON file:
 * ```
 * { "$": "char_width_selector",
 *   "full": { "code": 12450, "label": "ア" },
 *   "half": { "code": 65393, "label": "ｱ" }
 * }
 * ```
 *
 * @property full The key data to use if the current character width is full.
 * @property half The key data to use if the current character width is half.
 */
@Serializable
@SerialName("char_width_selector")
class CharWidthSelector(
    val full: AbstractKeyData?,
    val half: AbstractKeyData?,
) : AbstractKeyData {
    override fun compute(evaluator: ComputingEvaluator): KeyData? {
        val data = if (evaluator.activeState().isCharHalfWidth) { half } else { full }
        return data?.compute(evaluator)
    }

    override fun asString(isForDisplay: Boolean): String {
        return ""
    }
}

/**
 * Allows to select an [AbstractKeyData] based on the kana state. Note that this type of selector only really
 * makes sense in a text context, though technically speaking it can be used anywhere, so this implementation allows
 * for any [AbstractKeyData] to be used here. The JSON class identifier for this selector is `kana_selector`.
 *
 * Example usage in a layout JSON file:
 * ```
 * { "$": "kana_selector",
 *   "hira": { "code": 12354, "label": "あ" },
 *   "kata": { "code": 12450, "label": "ア" }
 * }
 * ```
 *
 * @property hira The key data to use if the current kana state is hiragana.
 * @property kata The key data to use if the current kana state is katakana.
 */
@Serializable
@SerialName("kana_selector")
class KanaSelector(
    val hira: AbstractKeyData,
    val kata: AbstractKeyData,
) : AbstractKeyData {
    override fun compute(evaluator: ComputingEvaluator): KeyData? {
        val data = if (evaluator.activeState().isKanaKata) { kata } else { hira }
        return data.compute(evaluator)
    }

    override fun asString(isForDisplay: Boolean): String {
        return ""
    }
}
