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

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import dev.patrickgold.florisboard.app.prefs.florisPreferenceModel
import dev.patrickgold.florisboard.extensionManager
import dev.patrickgold.florisboard.ime.core.InputEventDispatcher
import dev.patrickgold.florisboard.ime.core.InputKeyEvent
import dev.patrickgold.florisboard.ime.core.InputKeyEventReceiver
import dev.patrickgold.florisboard.ime.core.Subtype
import dev.patrickgold.florisboard.ime.core.SubtypePreset
import dev.patrickgold.florisboard.ime.onehanded.OneHandedMode
import dev.patrickgold.florisboard.ime.popup.PopupMapping
import dev.patrickgold.florisboard.ime.popup.PopupMappingComponent
import dev.patrickgold.florisboard.ime.text.key.KeyCode
import dev.patrickgold.florisboard.ime.text.key.KeyVariation
import dev.patrickgold.florisboard.ime.text.key.UtilityKeyAction
import dev.patrickgold.florisboard.ime.text.keyboard.KeyboardMode
import dev.patrickgold.florisboard.ime.text.keyboard.TextKeyboard
import dev.patrickgold.florisboard.res.ext.ExtensionComponentName
import dev.patrickgold.florisboard.subtypeManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex

typealias DeferredResult<T> = Deferred<Result<T>>

class KeyboardManager(context: Context) : InputKeyEventReceiver {
    private val prefs by florisPreferenceModel()
    private val extensionManager by context.extensionManager()
    private val subtypeManager by context.subtypeManager()

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    val resources = KeyboardManagerResources()

    val activeState = KeyboardState.new()
    val computedKeyboard: LiveData<TextKeyboard> = MutableLiveData(PlaceholderLoadingKeyboard)
    val computingEvaluator: ComputingEvaluator = KeyboardManagerComputingEvaluator()
    val inputEventDispatcher = InputEventDispatcher.new(
        repeatableKeyCodes = intArrayOf(
            KeyCode.ARROW_DOWN,
            KeyCode.ARROW_LEFT,
            KeyCode.ARROW_RIGHT,
            KeyCode.ARROW_UP,
            KeyCode.DELETE,
            KeyCode.FORWARD_DELETE
        )
    ).also { it.keyEventReceiver = this }

    fun computeKeyboardAsync(
        keyboardMode: KeyboardMode,
        subtype: Subtype
    ): Deferred<TextKeyboard> = scope.async { TextKeyboard(emptyArray(), KeyboardMode.CHARACTERS, null, null) }

    /**
     * @return If the language switch should be shown.
     */
    fun shouldShowLanguageSwitch(): Boolean {
        return subtypeManager.subtypes().size > 1
    }

    fun toggleOneHandedMode(isRight: Boolean) {
        prefs.keyboard.oneHandedMode.set(when (prefs.keyboard.oneHandedMode.get()) {
            OneHandedMode.OFF -> if (isRight) { OneHandedMode.END } else { OneHandedMode.START }
            else -> OneHandedMode.OFF
        })
    }

    override fun onInputKeyDown(ev: InputKeyEvent) {
        TODO("Not yet implemented")
    }

    override fun onInputKeyUp(ev: InputKeyEvent) {
        TODO("Not yet implemented")
    }

    override fun onInputKeyCancel(ev: InputKeyEvent) {
        TODO("Not yet implemented")
    }

    override fun onInputKeyRepeat(ev: InputKeyEvent) {
        TODO("Not yet implemented")
    }

    inner class KeyboardManagerResources {
        private val cachedLayouts = mutableMapOf<ExtensionComponentName, DeferredResult<LayoutArrangement>>()
        private val cachedLayoutsGuard = Mutex()

        private val cachedPopupMappings = mutableMapOf<ExtensionComponentName, DeferredResult<PopupMapping>>()
        private val cachedPopupMappingsGuard = Mutex()

        val currencySets = MutableLiveData<Map<ExtensionComponentName, CurrencySet>>()
        val layouts = MutableLiveData<Map<LayoutType, Map<ExtensionComponentName, LayoutArrangementComponent>>>()
        val popupMappings = MutableLiveData<Map<ExtensionComponentName, PopupMappingComponent>>()
        val subtypePresets = MutableLiveData<List<SubtypePreset>>()

        init {
            extensionManager.keyboardExtensions.observeForever { keyboardExtensions ->
                parseKeyboardExtensions(keyboardExtensions)
            }
        }

        private fun parseKeyboardExtensions(keyboardExtensions: List<KeyboardExtension>) = scope.launch {
            val localCurrencySets = mutableMapOf<ExtensionComponentName, CurrencySet>()
            val localLayouts = mutableMapOf<LayoutType, MutableMap<ExtensionComponentName, LayoutArrangementComponent>>()
            val localPopupMappings = mutableMapOf<ExtensionComponentName, PopupMappingComponent>()
            val localSubtypePresets = mutableListOf<SubtypePreset>()
            for (layoutType in LayoutType.values()) {
                localLayouts[layoutType] = mutableMapOf()
            }
            for (keyboardExtension in keyboardExtensions) {
                keyboardExtension.currencySets.forEach { currencySet ->
                    localCurrencySets[ExtensionComponentName(keyboardExtension.meta.id, currencySet.id)] = currencySet
                }
                keyboardExtension.layouts.forEach { (type, layoutComponents) ->
                    for (layoutComponent in layoutComponents) {
                        localLayouts[type]!![ExtensionComponentName(keyboardExtension.meta.id, layoutComponent.id)] = layoutComponent
                    }
                }
                keyboardExtension.popupMappings.forEach { popupMapping ->
                    localPopupMappings[ExtensionComponentName(keyboardExtension.meta.id, popupMapping.id)] = popupMapping
                }
                localSubtypePresets.addAll(keyboardExtension.subtypePresets)
            }
            localSubtypePresets.sortBy { it.locale.languageTag() }
            for (languageCode in listOf("en-CA", "en-AU", "en-UK", "en-US")) {
                val index: Int = localSubtypePresets.indexOfFirst { it.locale.languageTag() == languageCode }
                if (index > 0) {
                    localSubtypePresets.add(0, localSubtypePresets.removeAt(index))
                }
            }
            subtypePresets.postValue(localSubtypePresets)
            currencySets.postValue(localCurrencySets)
            layouts.postValue(localLayouts)
            popupMappings.postValue(localPopupMappings)
        }
    }

    private inner class KeyboardManagerComputingEvaluator : ComputingEvaluator {
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
                        UtilityKeyAction.DYNAMIC_SWITCH_LANGUAGE_EMOJIS -> !shouldShowLanguageSwitch()
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
                        UtilityKeyAction.DYNAMIC_SWITCH_LANGUAGE_EMOJIS -> shouldShowLanguageSwitch()
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
            return computedKeyboard.value!!
        }

        override fun isSlot(data: KeyData): Boolean {
            return CurrencySet.isCurrencySlot(data.code)
        }

        override fun getSlotData(data: KeyData): KeyData? {
            return subtypeManager.getCurrencySet(getActiveSubtype()).getSlot(data.code)
        }
    }
}
