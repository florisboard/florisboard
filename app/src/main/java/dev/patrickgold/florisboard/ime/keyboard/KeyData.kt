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

import dev.patrickgold.florisboard.ime.text.key.KeyVariation
import dev.patrickgold.florisboard.ime.text.keyboard.TextComputingEvaluator
import dev.patrickgold.florisboard.ime.text.keyboard.TextKeyData
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Basic interface for a key data object. Base for all key data objects across the IME, such as text, emojis and
 * selectors. The implementation is as abstract as possible, as different features require different implementations.
 */
interface KeyData {
    /**
     * Computes a [TextKeyData] object for this key data. Returns null if no computation is possible or if the key is
     * not relevant based on the result of [evaluator].
     *
     * @param evaluator The evaluator used to retrieve different states from the parent controller.
     *
     * @return A [TextKeyData] object or null if no computation is possible.
     */
    fun computeTextKeyData(evaluator: TextComputingEvaluator): TextKeyData?

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
 * Allows to select a [KeyData] based on the current caps state. Note that this type of selector only really makes
 * sense in a text context, though technically speaking it can be used anywhere, so this implementation allows for
 * any [KeyData] to be used here. The JSON class identifier for this selector is `case_selector`.
 *
 * Example usage in a layout JSON file:
 * ```
 * { "$": "case_selector",
 *   "lower": { "code":   59, "label": ";" },
 *   "upper": { "code":   58, "label": ":" }
 * }
 * ```
 *
 * @property lower The property to use if the current caps state is lowercase.
 * @property upper The property to use if the current caps state is uppercase.
 */
@Serializable
@SerialName("case_selector")
class CaseSelector(
    val lower: KeyData,
    val upper: KeyData,
) : KeyData {
    override fun computeTextKeyData(evaluator: TextComputingEvaluator): TextKeyData? {
        return (if (evaluator.evaluateCaps()) { upper } else { lower }).computeTextKeyData(evaluator)
    }

    override fun asString(isForDisplay: Boolean): String {
        return ""
    }
}

/**
 * Allows to select a [KeyData] based on the current key variation. Note that this type of selector only really makes
 * sense in a text context, though technically speaking it can be used anywhere, so this implementation allows for
 * any [KeyData] to be used here. The JSON class identifier for this selector is `variation_selector`.
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
 * @property default The default [KeyData] which should be used in case no key variation is known or for the current
 *  key variation no override key is defined.
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
    val default: KeyData,
    val email: KeyData? = null,
    val uri: KeyData? = null,
    val normal: KeyData? = null,
    val password: KeyData? = null,
) : KeyData {
    override fun computeTextKeyData(evaluator: TextComputingEvaluator): TextKeyData? {
        return when (evaluator.getKeyVariation()) {
            KeyVariation.ALL -> default
            KeyVariation.EMAIL_ADDRESS -> email ?: default
            KeyVariation.NORMAL -> normal ?: default
            KeyVariation.PASSWORD -> password ?: default
            KeyVariation.URI -> uri ?: default
        }.computeTextKeyData(evaluator)
    }

    override fun asString(isForDisplay: Boolean): String {
        return ""
    }
}
