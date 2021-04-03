/*
 * Copyright (C) 2020 Patrick Goldinger
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

package dev.patrickgold.florisboard.ime.text.layout

import dev.patrickgold.florisboard.ime.core.PrefHelper
import dev.patrickgold.florisboard.ime.core.Subtype
import dev.patrickgold.florisboard.ime.extension.AssetManager
import dev.patrickgold.florisboard.ime.extension.AssetRef
import dev.patrickgold.florisboard.ime.extension.AssetSource
import dev.patrickgold.florisboard.ime.popup.PopupExtension
import dev.patrickgold.florisboard.ime.popup.PopupManager
import dev.patrickgold.florisboard.ime.popup.PopupSet
import dev.patrickgold.florisboard.ime.text.key.*
import dev.patrickgold.florisboard.ime.text.keyboard.KeyboardMode
import kotlinx.coroutines.*
import java.util.*
import kotlin.collections.HashMap

private data class LTN(
    val type: LayoutType,
    val name: String
)

private data class KMS(
    val keyboardMode: KeyboardMode,
    val subtype: Subtype
)

/**
 * Class which manages layout loading and caching.
 */
class LayoutManager(parentScope: CoroutineScope) {
    private val assetManager: AssetManager
        get() = AssetManager.default()

    private val computedLayoutCache: HashMap<KMS, Deferred<LayoutData>> = hashMapOf()
    private val scope = CoroutineScope(parentScope.coroutineContext)

    private val indexedLayoutRefs: EnumMap<LayoutType, MutableList<Pair<AssetRef, LayoutMetaOnly>>> = EnumMap(LayoutType::class.java)

    init {
        for (type in LayoutType.values()) {
            indexedLayoutRefs[type] = mutableListOf()
        }
        indexLayoutRefs()
    }

    /**
     * Loads the layout for the specified type and name.
     *
     * @return the [Layout] or null.
     */
    private fun loadLayout(ltn: LTN?): Result<Layout> {
        if (ltn == null) {
            return Result.failure(Exception("Invalid arguments passed: 'ltn' is null!"))
        }
        return assetManager.loadAsset(
            ref = AssetRef(source = AssetSource.Assets, path = "ime/text/${ltn.type}/${ltn.name}.json"),
            assetClass = Layout::class
        )
    }

    private fun loadExtendedPopups(subtype: Subtype? = null): PopupExtension {
        val langTagRef = AssetRef(
            source = AssetSource.Assets,
            path = PopupManager.POPUP_EXTENSION_PATH_REL + "/" + (subtype?.locale?.toLanguageTag() ?: "\$default") + ".json"
        )
        val langRef = AssetRef(
            source = AssetSource.Assets,
            path = PopupManager.POPUP_EXTENSION_PATH_REL + "/" + (subtype?.locale?.language ?: "\$default") + ".json"
        )
        assetManager.loadAsset(langTagRef, PopupExtension::class).onSuccess {
            return it
        }
        assetManager.loadAsset(langRef, PopupExtension::class).onSuccess {
            return it
        }
        return PopupExtension.empty()
    }

    /**
     * Merges the specified layouts (LTNs) and returns the computed layout.
     * The computed layout may looks like this:
     *   e e e e e e e e e e      e = extension
     *   c c c c c c c c c c      c = main
     *    c c c c c c c c c       m = mod
     *   m c c c c c c c c m
     *   m m m m m m m m m m
     *
     * @param keyboardMode The keyboard mode for the returning [LayoutData].
     * @param subtype The subtype used for populating the extended popups.
     * @param main The main layout type and name.
     * @param modifier The modifier (mod) layout type and name.
     * @param extension The extension layout type and name.
     * @return a [LayoutData] object, regardless of the specified LTNs or errors.
     */
    private suspend fun mergeLayoutsAsync(
        keyboardMode: KeyboardMode,
        subtype: Subtype,
        main: LTN? = null,
        modifier: LTN? = null,
        extension: LTN? = null,
        prefs: PrefHelper,
        currencySet: CurrencySet
    ): LayoutData {
        val computedArrangement: ComputedLayoutArrangement = mutableListOf()

        val mainLayout = loadLayout(main).getOrNull()?.copy()
        val modifierToLoad = if (mainLayout?.modifier != null) {
            LTN(LayoutType.CHARACTERS_MOD, mainLayout.modifier)
        } else {
            modifier
        }
        val modifierLayout = loadLayout(modifierToLoad).getOrNull()
        val extensionLayout = loadLayout(extension).getOrNull()

        if (extensionLayout != null) {
            val row = extensionLayout.arrangement.firstOrNull()
            if (row != null) {
                computedArrangement.add(row.toMutableList())
            }
        }

        if (mainLayout != null && modifierLayout != null) {
            for (mainRowI in mainLayout.arrangement.indices) {
                val mainRow = mainLayout.arrangement[mainRowI]
                if (mainRowI + 1 < mainLayout.arrangement.size) {
                    computedArrangement.add(mainRow.toMutableList())
                } else {
                    // merge main and mod here
                    val mergedRow = mutableListOf<FlorisKeyData>()
                    val firstModRow = modifierLayout.arrangement.firstOrNull()
                    for (modKey in (firstModRow ?: listOf())) {
                        if (modKey.code == 0) {
                            mergedRow.addAll(mainRow)
                        } else {
                            mergedRow.add(modKey)
                        }
                    }
                    computedArrangement.add(mergedRow)
                }
            }
            for (modRowI in 1 until modifierLayout.arrangement.size) {
                val modRow = modifierLayout.arrangement[modRowI]
                computedArrangement.add(modRow.toMutableList())
            }
        } else if (mainLayout != null && modifierLayout == null) {
            for (mainRow in mainLayout.arrangement) {
                computedArrangement.add(mainRow.toMutableList())
            }
        } else if (mainLayout == null && modifierLayout != null) {
            for (modRow in modifierLayout.arrangement) {
                computedArrangement.add(modRow.toMutableList())
            }
        }

        // Add popup to keys
        if (keyboardMode == KeyboardMode.CHARACTERS || keyboardMode == KeyboardMode.NUMERIC_ADVANCED ||
            keyboardMode == KeyboardMode.SYMBOLS || keyboardMode == KeyboardMode.SYMBOLS2) {
            val extendedPopupsDefault = loadExtendedPopups()
            val extendedPopups = loadExtendedPopups(subtype)
            for (row in computedArrangement) {
                var kOffset = 0
                for ((k, key) in row.withIndex()) {
                    if (CurrencySet.isCurrencySlot(key.code)) {
                        val newKey = currencySet.getSlot(key.code) ?: KeyData.UNSPECIFIED
                        key.type = newKey.type
                        key.code = newKey.code
                        key.label = newKey.label
                    }
                    val lastKey = row.getOrNull(k - 1)
                    if (lastKey != null && lastKey.groupId == key.groupId && key.groupId != FlorisKeyData.GROUP_DEFAULT) {
                        kOffset++
                    }
                    val label = when (key.groupId) {
                        FlorisKeyData.GROUP_ENTER -> {
                            "~enter"
                        }
                        FlorisKeyData.GROUP_LEFT -> {
                            "~left"
                        }
                        FlorisKeyData.GROUP_RIGHT -> {
                            "~right"
                        }
                        else -> {
                            key.label
                        }
                    }
                    var popupSet: PopupSet<KeyData>? = null
                    val kv = key.variation
                    if (popupSet == null && kv == KeyVariation.PASSWORD) {
                        popupSet = extendedPopups.mapping[KeyVariation.PASSWORD]?.get(label) ?:
                                extendedPopupsDefault.mapping[KeyVariation.PASSWORD]?.get(label)
                    }
                    if (popupSet == null && (kv == KeyVariation.NORMAL || kv == KeyVariation.PASSWORD)) {
                        popupSet = extendedPopups.mapping[KeyVariation.NORMAL]?.get(label) ?:
                                extendedPopupsDefault.mapping[KeyVariation.NORMAL]?.get(label)
                    }
                    if (popupSet == null && kv == KeyVariation.EMAIL_ADDRESS) {
                        popupSet = extendedPopups.mapping[KeyVariation.EMAIL_ADDRESS]?.get(label) ?:
                                extendedPopupsDefault.mapping[KeyVariation.EMAIL_ADDRESS]?.get(label)
                    }
                    if (popupSet == null && (kv == KeyVariation.EMAIL_ADDRESS || kv == KeyVariation.URI)) {
                        popupSet = extendedPopups.mapping[KeyVariation.URI]?.get(label) ?:
                                extendedPopupsDefault.mapping[KeyVariation.URI]?.get(label)
                    }
                    if (popupSet == null) {
                        popupSet = extendedPopups.mapping[KeyVariation.ALL]?.get(label) ?:
                                extendedPopupsDefault.mapping[KeyVariation.ALL]?.get(label)
                    }
                    var keySpecificPopupSet: PopupSet<KeyData>? = null
                    if (label != key.label) {
                        keySpecificPopupSet = extendedPopups.mapping[KeyVariation.ALL]?.get(key.label) ?:
                            extendedPopupsDefault.mapping[KeyVariation.ALL]?.get(key.label)
                    }
                    key.popup.apply {
                        keySpecificPopupSet?.let { merge(it) }
                        popupSet?.let { merge(it) }
                    }
                    key.popup.forEach { popupKey ->
                        if (CurrencySet.isCurrencySlot(popupKey.code)) {
                            val newKey = currencySet.getSlot(popupKey.code) ?: KeyData.UNSPECIFIED
                            popupKey.type = newKey.type
                            popupKey.code = newKey.code
                            popupKey.label = newKey.label
                        }
                    }
                }
            }
        }

        // Add hints to keys
        if (keyboardMode == KeyboardMode.CHARACTERS) {
            val symbolsComputedArrangement = fetchComputedLayoutAsync(KeyboardMode.SYMBOLS, subtype, prefs, currencySet).await().arrangement
            val minRow = if (prefs.keyboard.numberRow) { 1 } else { 0 }
            for ((r, row) in computedArrangement.withIndex()) {
                if (r >= (3 + minRow) || r < minRow) {
                    continue
                }
                var kOffset = 0
                val symbolRow = symbolsComputedArrangement.getOrNull(r - minRow)
                if (symbolRow != null) {
                    for ((k, key) in row.withIndex()) {
                        val lastKey = row.getOrNull(k - 1)
                        if (lastKey != null && lastKey.groupId == key.groupId && key.groupId != FlorisKeyData.GROUP_DEFAULT) {
                            kOffset++
                        }
                        val symbol = symbolRow.getOrNull(k - kOffset)
                        if (r == minRow && key.type == KeyType.CHARACTER && symbol?.type == KeyType.NUMERIC) {
                            key.popup.hint = symbol
                        } else if (r > minRow && key.type == KeyType.CHARACTER && symbol?.type == KeyType.CHARACTER) {
                            key.popup.hint = symbol
                        }
                    }
                }
            }
        }

        return LayoutData(
            keyboardMode,
            "computed",
            mainLayout?.direction ?: "ltr",
            computedArrangement
        )
    }

    /**
     * Computes a layout for [keyboardMode] based on the given [subtype] and returns it.
     *
     * @param keyboardMode The keyboard mode for which the layout should be computed.
     * @param subtype The subtype which localizes the computed layout.
     */
    private suspend fun computeLayoutFor(
        keyboardMode: KeyboardMode,
        subtype: Subtype,
        prefs: PrefHelper,
        currencySet: CurrencySet
    ): LayoutData {
        var main: LTN? = null
        var modifier: LTN? = null
        var extension: LTN? = null

        when (keyboardMode) {
            KeyboardMode.CHARACTERS -> {
                if (prefs.keyboard.numberRow) {
                    extension = LTN(LayoutType.EXTENSION, subtype.layoutMap.numericRow)
                }
                main = LTN(LayoutType.CHARACTERS, subtype.layoutMap.characters)
                modifier = LTN(LayoutType.CHARACTERS_MOD, "\$default")
            }
            KeyboardMode.EDITING -> {
                // Layout for this mode is defined in custom layout xml file.
            }
            KeyboardMode.NUMERIC -> {
                main = LTN(LayoutType.NUMERIC, subtype.layoutMap.numeric)
            }
            KeyboardMode.NUMERIC_ADVANCED -> {
                main = LTN(LayoutType.NUMERIC_ADVANCED, subtype.layoutMap.numericAdvanced)
            }
            KeyboardMode.PHONE -> {
                main = LTN(LayoutType.PHONE, subtype.layoutMap.phone)
            }
            KeyboardMode.PHONE2 -> {
                main = LTN(LayoutType.PHONE2, subtype.layoutMap.phone2)
            }
            KeyboardMode.SYMBOLS -> {
                extension = LTN(LayoutType.NUMERIC_ROW, subtype.layoutMap.numericRow)
                main = LTN(LayoutType.SYMBOLS, subtype.layoutMap.symbols)
                modifier = LTN(LayoutType.SYMBOLS_MOD, "\$default")
            }
            KeyboardMode.SYMBOLS2 -> {
                main = LTN(LayoutType.SYMBOLS2, subtype.layoutMap.symbols2)
                modifier = LTN(LayoutType.SYMBOLS2_MOD, "\$default")
            }
            KeyboardMode.SMARTBAR_CLIPBOARD_CURSOR_ROW -> {
                extension = LTN(LayoutType.EXTENSION, "clipboard_cursor_row")
            }
            KeyboardMode.SMARTBAR_NUMBER_ROW -> {
                extension = LTN(LayoutType.EXTENSION, subtype.layoutMap.numericRow)
            }
        }

        return mergeLayoutsAsync(keyboardMode, subtype, main, modifier, extension, prefs, currencySet)
    }

    /**
     * Clears the layout cache for the specified [keyboardMode].
     *
     * @param keyboardMode The keyboard mode for which the layout cache should be cleared. If null
     *  is passed, the entire cache will be cleared. Defaults to null.
     */
    fun clearLayoutCache(keyboardMode: KeyboardMode? = null) {
        if (keyboardMode == null) {
            computedLayoutCache.clear()
        } else {
            val it = computedLayoutCache.iterator()
            while (it.hasNext()) {
                val kms = it.next().key
                if (kms.keyboardMode == keyboardMode) {
                    it.remove()
                }
            }
        }
    }

    /**
     * Preloads the layout for the given [keyboardMode]/[subtype] combo or returns the cached entry,
     * if it exists. The returned value is a deferred computed layout data. This function returns
     * immediately and won't block. To retrieve the actual computed layout await the returned value.
     *
     * @param keyboardMode The keyboard mode for which the layout should be computed.
     * @param subtype The subtype which localizes the computed layout.
     */
    @Synchronized
    fun fetchComputedLayoutAsync(
        keyboardMode: KeyboardMode,
        subtype: Subtype,
        prefs: PrefHelper,
        currencySet: CurrencySet
    ): Deferred<LayoutData> {
        val kms = KMS(keyboardMode, subtype)
        val cachedComputedLayout = computedLayoutCache[kms]
        return if (cachedComputedLayout != null) {
            cachedComputedLayout
        } else {
            val computedLayout = scope.async(Dispatchers.IO) {
                computeLayoutFor(keyboardMode, subtype, prefs, currencySet)
            }
            computedLayoutCache[kms] = computedLayout
            computedLayout
        }
    }

    /**
     * Asynchronously preloads the layout for the given [keyboardMode]/[subtype] combo. Adds the
     * deferred async result for the layout to the cache and returns immediately. To retrieve the
     * layout use [fetchComputedLayoutAsync].
     *
     * @param keyboardMode The keyboard mode for which the layout should be computed.
     * @param subtype The subtype which localizes the computed layout.
     */
    @Synchronized
    fun preloadComputedLayout(
        keyboardMode: KeyboardMode,
        subtype: Subtype,
        prefs: PrefHelper,
        currencySet: CurrencySet
    ) {
        val kms = KMS(keyboardMode, subtype)
        if (computedLayoutCache[kms] == null) {
            computedLayoutCache[kms] = scope.async(Dispatchers.IO) {
                computeLayoutFor(keyboardMode, subtype, prefs, currencySet)
            }
        }
    }

    /**
     * Called when the application is destroyed. Used to cancel any pending coroutines.
     */
    fun onDestroy() {
        scope.cancel()
    }

    fun getMetaFor(type: LayoutType, name: String): LayoutMetaOnly? {
        return indexedLayoutRefs[type]?.first { it.second.name == name }?.second
    }

    fun getMetaNameListFor(type: LayoutType): List<String> {
        return indexedLayoutRefs[type]?.map { it.second.name } ?: listOf()
    }

    fun getMetaLabelListFor(type: LayoutType): List<String> {
        return indexedLayoutRefs[type]?.map { it.second.label } ?: listOf()
    }

    private fun indexLayoutRefs() {
        for (type in LayoutType.values()) {
            indexedLayoutRefs[type]?.clear()
            assetManager.listAssets(
                AssetRef(AssetSource.Assets, "ime/text/$type"),
                LayoutMetaOnly::class
            ).onSuccess { assetList ->
                indexedLayoutRefs[type]?.let { indexedList ->
                    for ((ref, layoutMeta) in assetList) {
                        indexedList.add(Pair(ref, layoutMeta))
                    }
                    indexedList.sortBy { it.second.name }
                    if (type == LayoutType.CHARACTERS) {
                        // Move selected layouts to the top of the list
                        for (layoutName in listOf("azerty", "qwertz", "qwerty")) {
                            val index: Int = indexedList.indexOfFirst { it.second.name == layoutName }
                            if (index > 0) {
                                indexedList.add(0, indexedList.removeAt(index))
                            }
                        }
                    }
                }
            }
        }
    }
}
