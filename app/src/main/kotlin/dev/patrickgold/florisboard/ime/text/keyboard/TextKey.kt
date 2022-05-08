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

package dev.patrickgold.florisboard.ime.text.keyboard

import dev.patrickgold.florisboard.app.florisPreferenceModel
import dev.patrickgold.florisboard.ime.keyboard.AbstractKeyData
import dev.patrickgold.florisboard.ime.keyboard.ComputingEvaluator
import dev.patrickgold.florisboard.ime.keyboard.Key
import dev.patrickgold.florisboard.ime.keyboard.KeyData
import dev.patrickgold.florisboard.ime.keyboard.KeyboardMode
import dev.patrickgold.florisboard.ime.keyboard.computeIconResId
import dev.patrickgold.florisboard.ime.keyboard.computeLabel
import dev.patrickgold.florisboard.ime.popup.MutablePopupSet
import dev.patrickgold.florisboard.ime.popup.PopupMapping
import dev.patrickgold.florisboard.ime.popup.PopupSet
import dev.patrickgold.florisboard.ime.text.key.KeyCode
import dev.patrickgold.florisboard.ime.text.key.KeyType
import dev.patrickgold.florisboard.ime.text.key.KeyVariation
import dev.patrickgold.florisboard.lib.kotlin.lowercase

class TextKey(override val data: AbstractKeyData) : Key(data) {
    var computedData: KeyData = TextKeyData.UNSPECIFIED
        private set
    val computedPopups: MutablePopupSet<KeyData> = MutablePopupSet()
    var computedSymbolHint: KeyData? = null
    var computedNumberHint: KeyData? = null
    var computedHintData: KeyData = TextKeyData.UNSPECIFIED

    // This should exclusively be set and used by the TextKeyboardLayout
    var computedDataOnDown: KeyData = TextKeyData.UNSPECIFIED

    fun compute(evaluator: ComputingEvaluator) {
        val keyboard = evaluator.keyboard() as? TextKeyboard ?: return
        val keyboardMode = keyboard.mode
        val computed = data.compute(evaluator)

        if (computed == null || !evaluator.evaluateVisible(computed)) {
            computedData = TextKeyData.UNSPECIFIED
            computedPopups.clear()
            isEnabled = false
            isVisible = false

            flayShrink = 0.0f
            flayGrow = 0.0f
            flayWidthFactor = 0.0f
        } else {
            computedData = computed
            computedPopups.clear()
            mergePopups(computed, evaluator, computedPopups::merge)
            if (keyboardMode == KeyboardMode.CHARACTERS || keyboardMode == KeyboardMode.NUMERIC_ADVANCED ||
                keyboardMode == KeyboardMode.SYMBOLS || keyboardMode == KeyboardMode.SYMBOLS2) {
                val computedLabel = computed.label.lowercase(evaluator.activeSubtype().primaryLocale)
                val extLabel = when (computed.groupId) {
                    KeyData.GROUP_ENTER -> {
                        "~enter"
                    }
                    KeyData.GROUP_LEFT -> {
                        "~left"
                    }
                    KeyData.GROUP_RIGHT -> {
                        "~right"
                    }
                    KeyData.GROUP_KANA -> {
                        "~kana"
                    }
                    else -> {
                        computedLabel
                    }
                }
                val extendedPopupsDefault = keyboard.extendedPopupMappingDefault
                val extendedPopups = keyboard.extendedPopupMapping
                var popupSet: PopupSet<AbstractKeyData>? = null
                val kv = evaluator.activeState().keyVariation
                if (popupSet == null && kv == KeyVariation.PASSWORD) {
                    popupSet = extendedPopups?.get(KeyVariation.PASSWORD)?.get(extLabel) ?:
                        extendedPopupsDefault?.get(KeyVariation.PASSWORD)?.get(extLabel)
                }
                if (popupSet == null && (kv == KeyVariation.NORMAL || kv == KeyVariation.PASSWORD)) {
                    popupSet = extendedPopups?.get(KeyVariation.NORMAL)?.get(extLabel) ?:
                        extendedPopupsDefault?.get(KeyVariation.NORMAL)?.get(extLabel)
                }
                if (popupSet == null && kv == KeyVariation.EMAIL_ADDRESS) {
                    popupSet = extendedPopups?.get(KeyVariation.EMAIL_ADDRESS)?.get(extLabel) ?:
                        extendedPopupsDefault?.get(KeyVariation.EMAIL_ADDRESS)?.get(extLabel)
                }
                if (popupSet == null && (kv == KeyVariation.EMAIL_ADDRESS || kv == KeyVariation.URI)) {
                    popupSet = extendedPopups?.get(KeyVariation.URI)?.get(extLabel) ?:
                        extendedPopupsDefault?.get(KeyVariation.URI)?.get(extLabel)
                }
                if (popupSet == null) {
                    popupSet = extendedPopups?.get(KeyVariation.ALL)?.get(extLabel) ?:
                        extendedPopupsDefault?.get(KeyVariation.ALL)?.get(extLabel)
                }
                var keySpecificPopupSet: PopupSet<AbstractKeyData>? = null
                if (extLabel != computedLabel) {
                    keySpecificPopupSet = extendedPopups?.get(KeyVariation.ALL)?.get(computedLabel) ?:
                        extendedPopupsDefault?.get(KeyVariation.ALL)?.get(computedLabel)
                }
                computedPopups.apply {
                    keySpecificPopupSet?.let { merge(it, evaluator) }
                    popupSet?.let { merge(it, evaluator) }
                }
                if (computed.type == KeyType.CHARACTER) {
                    addComputedHints(computed.code, evaluator, extendedPopups, extendedPopupsDefault)
                }
            }
            isEnabled = evaluator.evaluateEnabled(computed)
            isVisible = true

            flayShrink = when (keyboardMode) {
                KeyboardMode.NUMERIC,
                KeyboardMode.NUMERIC_ADVANCED,
                KeyboardMode.PHONE,
                KeyboardMode.PHONE2 -> 1.0f
                else -> when (computed.code) {
                    KeyCode.SHIFT,
                    KeyCode.DELETE -> 1.5f
                    KeyCode.VIEW_CHARACTERS,
                    KeyCode.VIEW_SYMBOLS,
                    KeyCode.VIEW_SYMBOLS2,
                    KeyCode.ENTER -> 0.0f
                    else -> 1.0f
                }
            }
            flayGrow = when (keyboardMode) {
                KeyboardMode.NUMERIC,
                KeyboardMode.PHONE,
                KeyboardMode.PHONE2 -> 0.0f
                KeyboardMode.NUMERIC_ADVANCED -> when (computed.type) {
                    KeyType.NUMERIC -> 1.0f
                    else -> 0.0f
                }
                else -> when (computed.code) {
                    KeyCode.SPACE, KeyCode.CJK_SPACE -> 1.0f
                    else -> 0.0f
                }
            }
            flayWidthFactor = when (keyboardMode) {
                KeyboardMode.NUMERIC,
                KeyboardMode.PHONE,
                KeyboardMode.PHONE2 -> 2.68f
                KeyboardMode.NUMERIC_ADVANCED -> when (computed.code) {
                    44, 46 -> 1.00f
                    KeyCode.VIEW_SYMBOLS, 61 -> 1.26f
                    else -> 1.56f
                }
                else -> when (computed.code) {
                    KeyCode.SHIFT,
                    KeyCode.DELETE -> 1.56f
                    KeyCode.VIEW_CHARACTERS,
                    KeyCode.VIEW_SYMBOLS,
                    KeyCode.VIEW_SYMBOLS2,
                    KeyCode.ENTER -> 1.56f
                    else -> 1.00f
                }
            }
        }
    }

    inline fun setPressed(state: Boolean, blockIfChanged: () -> Unit) {
        if (isPressed != state) {
            isPressed = state
            blockIfChanged()
        }
    }

    private fun addComputedHints(
        keyCode: Int,
        evaluator: ComputingEvaluator,
        extendedPopups: PopupMapping?,
        extendedPopupsDefault: PopupMapping?
    ) {
        val symbolHint = computedSymbolHint
        if (symbolHint != null) {
            val evaluatedSymbolHint = symbolHint.compute(evaluator)
            if (symbolHint.code != keyCode) {
                computedPopups.symbolHint = evaluatedSymbolHint
                mergePopups(evaluatedSymbolHint, evaluator, computedPopups::mergeSymbolHint)
                val hintSpecificPopupSet =
                    extendedPopups?.get(KeyVariation.ALL)?.get(symbolHint.label) ?: extendedPopupsDefault?.get(
                        KeyVariation.ALL
                    )?.get(symbolHint.label)
                hintSpecificPopupSet?.let { computedPopups.mergeSymbolHint(it, evaluator) }
            }
        }
        val numericHint = computedNumberHint
        if (numericHint != null) {
            val evaluatedNumberHint = numericHint.compute(evaluator)
            if (numericHint.code != keyCode) {
                computedPopups.numberHint = evaluatedNumberHint
                mergePopups(evaluatedNumberHint, evaluator, computedPopups::mergeNumberHint)
                val hintSpecificPopupSet =
                    extendedPopups?.get(KeyVariation.ALL)?.get(numericHint.label) ?: extendedPopupsDefault?.get(
                        KeyVariation.ALL
                    )?.get(numericHint.label)
                hintSpecificPopupSet?.let { computedPopups.mergeNumberHint(it, evaluator) }
            }
        }
    }

    private fun mergePopups(
        keyData: KeyData?,
        evaluator: ComputingEvaluator,
        merge: (popups: PopupSet<AbstractKeyData>, evaluator: ComputingEvaluator) -> Unit,
    ) {
        if (keyData?.popup != null) {
            merge(keyData.popup!!, evaluator)
        }
    }

    /**
     * Computes the label, hintedLabel and iconResId for [computedData] based on given [evaluator].
     */
    fun computeLabelsAndDrawables(evaluator: ComputingEvaluator) {
        label = evaluator.computeLabel(computedData)
        hintedLabel = null
        foregroundDrawableId = evaluator.computeIconResId(computedData)

        val data = computedData
        if (data.type == KeyType.NUMERIC && evaluator.keyboard().mode == KeyboardMode.PHONE) {
            hintedLabel = when (data.code) {
                48 /* 0 */ -> "+"
                49 /* 1 */ -> ""
                50 /* 2 */ -> "ABC"
                51 /* 3 */ -> "DEF"
                52 /* 4 */ -> "GHI"
                53 /* 5 */ -> "JKL"
                54 /* 6 */ -> "MNO"
                55 /* 7 */ -> "PQRS"
                56 /* 8 */ -> "TUV"
                57 /* 9 */ -> "WXYZ"
                else -> null
            }
        } else if (!data.isSpaceKey() || data.type == KeyType.NUMERIC) {
            val prefs by florisPreferenceModel()
            computedPopups.getPopupKeys(prefs.keyboard.keyHintConfiguration()).hint.let { hintData ->
                if (hintData?.isSpaceKey() == false) {
                    hintedLabel = hintData.asString(isForDisplay = true)
                    computedHintData = hintData
                } else {
                    hintedLabel = null
                    computedHintData = TextKeyData.UNSPECIFIED
                }
            }
        }
    }

    override fun toString(): String {
        return computedData.toString()
    }
}
