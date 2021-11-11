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

package dev.patrickgold.florisboard.ime.text

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.viewinterop.AndroidView
import dev.patrickgold.florisboard.app.prefs.florisPreferenceModel
import dev.patrickgold.florisboard.common.observeAsNonNullState
import dev.patrickgold.florisboard.ime.core.Subtype
import dev.patrickgold.florisboard.ime.keyboard.ComputingEvaluator
import dev.patrickgold.florisboard.ime.keyboard.CurrencySet
import dev.patrickgold.florisboard.ime.keyboard.KeyData
import dev.patrickgold.florisboard.ime.keyboard.Keyboard
import dev.patrickgold.florisboard.ime.keyboard.LocalKeyboardRowBaseHeight
import dev.patrickgold.florisboard.ime.text.key.KeyCode
import dev.patrickgold.florisboard.ime.text.key.KeyVariation
import dev.patrickgold.florisboard.ime.text.key.UtilityKeyAction
import dev.patrickgold.florisboard.ime.text.keyboard.TextKeyboardIconSet
import dev.patrickgold.florisboard.ime.text.keyboard.TextKeyboardView
import dev.patrickgold.florisboard.ime.text.smartbar.Smartbar
import dev.patrickgold.florisboard.keyboardManager
import dev.patrickgold.florisboard.subtypeManager

@Composable
fun TextInputLayout() = Column {
    val context = LocalContext.current
    val prefs by florisPreferenceModel()
    val keyboardManager by context.keyboardManager()
    val subtypeManager by context.subtypeManager()

    val activeState by keyboardManager.activeState.observeAsNonNullState()
    val computingEvaluator = remember {
        object : ComputingEvaluator {
            override fun evaluateCaps(): Boolean {
                return activeState.caps || activeState.capsLock
            }

            override fun evaluateCaps(data: KeyData): Boolean {
                return evaluateCaps() && data.code >= KeyCode.SPACE
            }

            override fun evaluateCharHalfWidth(): Boolean = activeState.isCharHalfWidth

            override fun evaluateKanaKata(): Boolean = activeState.isKanaKata

            override fun evaluateKanaSmall(): Boolean = activeState.isKanaSmall

            override fun evaluateEnabled(data: KeyData): Boolean {
                return when (data.code) {
                    KeyCode.CLIPBOARD_COPY,
                    KeyCode.CLIPBOARD_CUT -> {
                        activeState.isSelectionMode && activeState.isRichInputEditor
                    }
                    KeyCode.CLIPBOARD_PASTE -> {
                        // such gore. checks
                        // 1. has a clipboard item
                        // 2. the clipboard item has any of the supported mime types of the editor OR is plain text.
                        //florisboard.florisClipboardManager?.canBePasted(
                        //    florisboard.florisClipboardManager?.primaryClip
                        //) == true
                        false
                    }
                    KeyCode.CLIPBOARD_SELECT_ALL -> {
                        activeState.isRichInputEditor
                    }
                    KeyCode.SWITCH_TO_CLIPBOARD_CONTEXT -> {
                        prefs.clipboard.enableHistory.get()
                    }
                    else -> true
                }
            }

            override fun evaluateVisible(data: KeyData): Boolean {
                return when (data.code) {
                    KeyCode.SWITCH_TO_TEXT_CONTEXT,
                    KeyCode.SWITCH_TO_MEDIA_CONTEXT -> {
                        val tempUtilityKeyAction = when {
                            prefs.keyboard.utilityKeyEnabled.get() -> prefs.keyboard.utilityKeyAction.get()
                            else -> UtilityKeyAction.DISABLED
                        }
                        when (tempUtilityKeyAction) {
                            UtilityKeyAction.DISABLED,
                            UtilityKeyAction.SWITCH_LANGUAGE,
                            UtilityKeyAction.SWITCH_KEYBOARD_APP -> false
                            UtilityKeyAction.SWITCH_TO_EMOJIS -> true
                            UtilityKeyAction.DYNAMIC_SWITCH_LANGUAGE_EMOJIS -> !keyboardManager.shouldShowLanguageSwitch()
                        }
                    }
                    KeyCode.SWITCH_TO_CLIPBOARD_CONTEXT -> prefs.clipboard.enableHistory.get()
                    KeyCode.LANGUAGE_SWITCH -> {
                        val tempUtilityKeyAction = when {
                            prefs.keyboard.utilityKeyEnabled.get() -> prefs.keyboard.utilityKeyAction.get()
                            else -> UtilityKeyAction.DISABLED
                        }
                        when (tempUtilityKeyAction) {
                            UtilityKeyAction.DISABLED,
                            UtilityKeyAction.SWITCH_TO_EMOJIS -> false
                            UtilityKeyAction.SWITCH_LANGUAGE,
                            UtilityKeyAction.SWITCH_KEYBOARD_APP -> true
                            UtilityKeyAction.DYNAMIC_SWITCH_LANGUAGE_EMOJIS -> keyboardManager.shouldShowLanguageSwitch()
                        }
                    }
                    else -> true
                }
            }

            override fun getActiveSubtype(): Subtype {
                return subtypeManager.activeSubtype.value!!
            }

            override fun getKeyVariation(): KeyVariation {
                return activeState.keyVariation
            }

            override fun getKeyboard(): Keyboard {
                throw NotImplementedError() // Correct value must be inserted by the TextKeyboardView
            }

            override fun isSlot(data: KeyData): Boolean {
                return CurrencySet.isCurrencySlot(data.code)
            }

            override fun getSlotData(data: KeyData): KeyData? {
                return subtypeManager.getCurrencySet(getActiveSubtype()).getSlot(data.code)
            }
        }
    }
    val computedKeyboard by keyboardManager.computedKeyboard.observeAsNonNullState()
    val keyboardRowBaseHeight = LocalKeyboardRowBaseHeight.current
    val keyboardRowBaseHeightPx = with(LocalDensity.current) { keyboardRowBaseHeight.toPx() }
    val textKeyboardIconSet = remember { TextKeyboardIconSet.new(context) }

    Smartbar()
    AndroidView(
        modifier = Modifier.fillMaxWidth().wrapContentHeight(),
        factory = { ctx -> TextKeyboardView(ctx).also { view ->
            view.setComputingEvaluator(computingEvaluator)
            view.setIconSet(textKeyboardIconSet)
        } },
        update = { view ->
            view.setKeyboardRowBaseHeight(keyboardRowBaseHeightPx)
            view.setComputedKeyboard(computedKeyboard)
            view.sync()
            view.requestLayout()
        },
    )
}
