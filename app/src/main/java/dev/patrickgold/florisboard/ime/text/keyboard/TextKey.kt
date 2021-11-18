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

import android.content.Context
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.app.prefs.florisPreferenceModel
import dev.patrickgold.florisboard.common.lowercase
import dev.patrickgold.florisboard.ime.keyboard.AbstractKeyData
import dev.patrickgold.florisboard.ime.keyboard.ComputingEvaluator
import dev.patrickgold.florisboard.ime.keyboard.ImeOptions
import dev.patrickgold.florisboard.ime.keyboard.Key
import dev.patrickgold.florisboard.ime.keyboard.KeyData
import dev.patrickgold.florisboard.ime.popup.MutablePopupSet
import dev.patrickgold.florisboard.ime.popup.PopupMapping
import dev.patrickgold.florisboard.ime.popup.PopupSet
import dev.patrickgold.florisboard.ime.text.key.*

class TextKey(override val data: AbstractKeyData) : Key(data) {
    var computedData: KeyData = TextKeyData.UNSPECIFIED
        private set
    val computedPopups: MutablePopupSet<KeyData> = MutablePopupSet()
    var computedSymbolHint: KeyData? = null
    var computedNumberHint: KeyData? = null

    fun compute(keyboard: TextKeyboard, evaluator: ComputingEvaluator) {
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
                val computedLabel = computed.label.lowercase(evaluator.getActiveSubtype().primaryLocale)
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
                val kv = evaluator.getKeyVariation()
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
     * Computes the labels and drawables needed to draw the
     */
    fun computeLabelsAndDrawables(
        context: Context,
        keyboard: TextKeyboard,
        evaluator: ComputingEvaluator,
    ) {
        // Reset attributes first to avoid invalid states if not updated
        label = null
        hintedLabel = null
        foregroundDrawableId = null

        val data = computedData
        if (data.type == KeyType.CHARACTER && data.code != KeyCode.SPACE && data.code != KeyCode.CJK_SPACE
            && data.code != KeyCode.HALF_SPACE && data.code != KeyCode.KESHIDA || data.type == KeyType.NUMERIC
        ) {
            val prefs by florisPreferenceModel()
            label = data.asString(isForDisplay = true)
            computedPopups.getPopupKeys(prefs.keyboard.keyHintConfiguration()).hint?.asString(isForDisplay = true).let {
                hintedLabel = it
            }
        } else {
            when (data.code) {
                KeyCode.ARROW_LEFT -> {
                    foregroundDrawableId = R.drawable.ic_keyboard_arrow_left
                }
                KeyCode.ARROW_RIGHT -> {
                    foregroundDrawableId = R.drawable.ic_keyboard_arrow_right
                }
                KeyCode.CLIPBOARD_COPY -> {
                    foregroundDrawableId = R.drawable.ic_content_copy
                }
                KeyCode.CLIPBOARD_CUT -> {
                    foregroundDrawableId = R.drawable.ic_content_cut
                }
                KeyCode.CLIPBOARD_PASTE -> {
                    foregroundDrawableId = R.drawable.ic_content_paste
                }
                KeyCode.CLIPBOARD_SELECT_ALL -> {
                    foregroundDrawableId = R.drawable.ic_select_all
                }
                KeyCode.DELETE -> {
                    foregroundDrawableId = R.drawable.ic_backspace
                }
                KeyCode.ENTER -> {
                    val imeOptions = evaluator.getActiveState().imeOptions
                    foregroundDrawableId = when (imeOptions.enterAction) {
                        ImeOptions.EnterAction.DONE -> R.drawable.ic_done
                        ImeOptions.EnterAction.GO -> R.drawable.ic_arrow_right_alt
                        ImeOptions.EnterAction.NEXT -> R.drawable.ic_arrow_right_alt
                        ImeOptions.EnterAction.NONE -> R.drawable.ic_keyboard_return
                        ImeOptions.EnterAction.PREVIOUS -> R.drawable.ic_arrow_right_alt
                        ImeOptions.EnterAction.SEARCH -> R.drawable.ic_search
                        ImeOptions.EnterAction.SEND -> R.drawable.ic_send
                        ImeOptions.EnterAction.UNSPECIFIED -> R.drawable.ic_keyboard_return
                    }
                    if (imeOptions.flagNoEnterAction) {
                        foregroundDrawableId = R.drawable.ic_keyboard_return
                    }
                }
                KeyCode.LANGUAGE_SWITCH -> {
                    foregroundDrawableId = R.drawable.ic_language
                }
                KeyCode.PHONE_PAUSE -> label = context.getString(R.string.key__phone_pause)
                KeyCode.PHONE_WAIT -> label = context.getString(R.string.key__phone_wait)
                KeyCode.SHIFT -> {
                    foregroundDrawableId = when (evaluator.evaluateCaps()) {
                        true -> R.drawable.ic_keyboard_capslock
                        else -> R.drawable.ic_keyboard_arrow_up
                    }
                }
                KeyCode.SPACE, KeyCode.CJK_SPACE -> {
                    when (keyboard.mode) {
                        KeyboardMode.NUMERIC,
                        KeyboardMode.NUMERIC_ADVANCED,
                        KeyboardMode.PHONE,
                        KeyboardMode.PHONE2 -> {
                            foregroundDrawableId = R.drawable.ic_space_bar
                        }
                        KeyboardMode.CHARACTERS -> {
                            label = evaluator.getActiveSubtype().primaryLocale.let { it.displayName() }
                        }
                        else -> {
                        }
                    }
                }
                KeyCode.SWITCH_TO_MEDIA_CONTEXT -> {
                    foregroundDrawableId = R.drawable.ic_sentiment_satisfied
                }
                KeyCode.SWITCH_TO_CLIPBOARD_CONTEXT -> {
                    foregroundDrawableId = R.drawable.ic_assignment
                }
                KeyCode.KANA_SWITCHER -> {
                    foregroundDrawableId = if (evaluator.evaluateKanaKata()) {
                        R.drawable.ic_keyboard_kana_switcher_kata
                    } else {
                        R.drawable.ic_keyboard_kana_switcher_hira
                    }
                }
                KeyCode.CHAR_WIDTH_SWITCHER -> {
                    foregroundDrawableId = if (evaluator.evaluateCharHalfWidth()) {
                        R.drawable.ic_keyboard_char_width_switcher_full
                    } else {
                        R.drawable.ic_keyboard_char_width_switcher_half
                    }
                }
                KeyCode.CHAR_WIDTH_FULL -> {
                    foregroundDrawableId = R.drawable.ic_keyboard_char_width_switcher_full
                }
                KeyCode.CHAR_WIDTH_HALF -> {
                    foregroundDrawableId = R.drawable.ic_keyboard_char_width_switcher_half
                }
                KeyCode.SWITCH_TO_TEXT_CONTEXT,
                KeyCode.VIEW_CHARACTERS -> {
                    label = context.getString(R.string.key__view_characters)
                }
                KeyCode.VIEW_NUMERIC,
                KeyCode.VIEW_NUMERIC_ADVANCED -> {
                    label = context.getString(R.string.key__view_numeric)
                }
                KeyCode.VIEW_PHONE -> {
                    label = context.getString(R.string.key__view_phone)
                }
                KeyCode.VIEW_PHONE2 -> {
                    label = context.getString(R.string.key__view_phone2)
                }
                KeyCode.VIEW_SYMBOLS -> {
                    label = context.getString(R.string.key__view_symbols)
                }
                KeyCode.VIEW_SYMBOLS2 -> {
                    label = context.getString(R.string.key__view_symbols2)
                }
                KeyCode.HALF_SPACE -> {
                    label = context.getString(R.string.key__view_half_space)
                }
                KeyCode.KESHIDA -> {
                    label = context.getString(R.string.key__view_keshida)
                }
            }
        }
    }
}
