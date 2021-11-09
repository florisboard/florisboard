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
import dev.patrickgold.florisboard.extensionManager
import dev.patrickgold.florisboard.ime.core.Subtype
import dev.patrickgold.florisboard.ime.core.SubtypePreset
import dev.patrickgold.florisboard.ime.popup.PopupMapping
import dev.patrickgold.florisboard.ime.popup.PopupMappingComponent
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

class KeyboardManager(context: Context) {
    private val extensionManager by context.extensionManager()
    private val subtypeManager by context.subtypeManager()

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    val resources = KeyboardManagerResources()

    val activeState: KeyboardState = KeyboardState.new()
    val computedKeyboard: LiveData<TextKeyboard> = MutableLiveData(PlaceholderLoadingKeyboard)

    fun computeKeyboardAsync(
        keyboardMode: KeyboardMode,
        subtype: Subtype
    ): Deferred<TextKeyboard> = scope.async { TextKeyboard(emptyArray(), KeyboardMode.CHARACTERS, null, null) }

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
}
