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
import dev.patrickgold.florisboard.app.florisPreferenceModel
import dev.patrickgold.florisboard.appContext
import dev.patrickgold.florisboard.assetManager
import dev.patrickgold.florisboard.extensionManager
import dev.patrickgold.florisboard.ime.core.Subtype
import dev.patrickgold.florisboard.ime.popup.PopupMapping
import dev.patrickgold.florisboard.ime.popup.PopupMappingComponent
import dev.patrickgold.florisboard.ime.text.key.KeyType
import dev.patrickgold.florisboard.ime.text.keyboard.TextKey
import dev.patrickgold.florisboard.ime.text.keyboard.TextKeyData
import dev.patrickgold.florisboard.ime.text.keyboard.TextKeyboard
import dev.patrickgold.florisboard.keyboardManager
import dev.patrickgold.florisboard.lib.devtools.LogTopic
import dev.patrickgold.florisboard.lib.devtools.flogDebug
import dev.patrickgold.florisboard.lib.devtools.flogWarning
import dev.patrickgold.florisboard.lib.ext.ExtensionComponentName
import dev.patrickgold.florisboard.lib.io.ZipUtils
import dev.patrickgold.florisboard.lib.kotlin.DeferredResult
import dev.patrickgold.florisboard.lib.kotlin.runCatchingAsync
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private data class LTN(
    val type: LayoutType,
    val name: ExtensionComponentName,
)

private data class CachedLayout(
    val type: LayoutType,
    val name: ExtensionComponentName,
    val meta: LayoutArrangementComponent,
    val arrangement: LayoutArrangement,
)

private data class CachedPopupMapping(
    val name: ExtensionComponentName,
    val meta: PopupMappingComponent,
    val mapping: PopupMapping,
)

/**
 * Class which manages layout loading and caching.
 */
class LayoutManager(context: Context) {
    private val prefs by florisPreferenceModel()
    private val appContext by context.appContext()
    private val assetManager by context.assetManager()
    private val extensionManager by context.extensionManager()
    private val keyboardManager by context.keyboardManager()

    private val layoutCache: HashMap<LTN, DeferredResult<CachedLayout>> = hashMapOf()
    private val layoutCacheGuard: Mutex = Mutex(locked = false)
    private val popupMappingCache: HashMap<ExtensionComponentName, DeferredResult<CachedPopupMapping>> = hashMapOf()
    private val popupMappingCacheGuard: Mutex = Mutex(locked = false)
    private val ioScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    /**
     * Loads the layout for the specified type and name.
     *
     * @return A deferred result for a layout.
     */
    private fun loadLayoutAsync(ltn: LTN?) = ioScope.runCatchingAsync {
        require(ltn != null) { "Invalid argument value for 'ltn': null" }
        layoutCacheGuard.withLock {
            val cached = layoutCache[ltn]
            if (cached != null) {
                flogDebug(LogTopic.LAYOUT_MANAGER) { "Using cache for '${ltn.name}'" }
                return@withLock cached
            } else {
                flogDebug(LogTopic.LAYOUT_MANAGER) { "Loading '${ltn.name}'" }
                val meta = keyboardManager.resources.layouts.value?.get(ltn.type)?.get(ltn.name)
                    ?: error("No indexed entry found for ${ltn.type} - ${ltn.name}")
                val ext = extensionManager.getExtensionById(ltn.name.extensionId)
                    ?: error("Extension ${ltn.name.extensionId} not found")
                val path = meta.arrangementFile(ltn.type)
                val layout = async {
                    runCatching {
                        val jsonStr = ZipUtils.readFileFromArchive(appContext, ext.sourceRef!!, path).getOrThrow()
                        val arrangement = assetManager.loadJsonAsset<LayoutArrangement>(jsonStr).getOrThrow()
                        CachedLayout(ltn.type, ltn.name, meta, arrangement)
                    }
                }
                layoutCache[ltn] = layout
                return@withLock layout
            }
        }.await().getOrThrow()
    }

    private fun loadPopupMappingAsync(subtype: Subtype? = null) = ioScope.runCatchingAsync {
        val name = subtype?.popupMapping ?: extCorePopupMapping("default")
        popupMappingCacheGuard.withLock {
            val cached = popupMappingCache[name]
            if (cached != null) {
                flogDebug(LogTopic.LAYOUT_MANAGER) { "Using cache for '$name'" }
                return@withLock cached
            } else {
                flogDebug(LogTopic.LAYOUT_MANAGER) { "Loading '$name'" }
                val meta = keyboardManager.resources.popupMappings.value?.get(name)
                    ?: error("No indexed entry found for $name")
                val ext = extensionManager.getExtensionById(name.extensionId)
                    ?: error("Extension ${name.extensionId} not found")
                val path = meta.mappingFile()
                val popupMapping = async {
                    runCatching {
                        val jsonStr = ZipUtils.readFileFromArchive(appContext, ext.sourceRef!!, path).getOrThrow()
                        val mapping = assetManager.loadJsonAsset<PopupMapping>(jsonStr).getOrThrow()
                        CachedPopupMapping(name, meta, mapping)
                    }
                }
                popupMappingCache[name] = popupMapping
                return@withLock popupMapping
            }
        }.await().getOrThrow()
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
     * @param keyboardMode The keyboard mode for the returning [TextKeyboard].
     * @param subtype The subtype used for populating the extended popups.
     * @param main The main layout type and name.
     * @param modifier The modifier (mod) layout type and name.
     * @param extension The extension layout type and name.
     * @return a [TextKeyboard] object, regardless of the specified LTNs or errors.
     */
    private suspend fun mergeLayouts(
        keyboardMode: KeyboardMode,
        subtype: Subtype,
        main: LTN? = null,
        modifier: LTN? = null,
        extension: LTN? = null,
    ): TextKeyboard {
        val extendedPopupsDefault = loadPopupMappingAsync()
        val extendedPopups = loadPopupMappingAsync(subtype)

        val mainLayout = loadLayoutAsync(main).await().onFailure {
            flogWarning { "$keyboardMode - main - $it" }
        }.getOrNull()
        val modifierToLoad = if (mainLayout?.meta?.modifier != null) {
            val layoutType = when (mainLayout.type) {
                LayoutType.SYMBOLS -> {
                    LayoutType.SYMBOLS_MOD
                }
                LayoutType.SYMBOLS2 -> {
                    LayoutType.SYMBOLS2_MOD
                }
                else -> {
                    LayoutType.CHARACTERS_MOD
                }
            }
            LTN(layoutType, mainLayout.meta.modifier)
        } else {
            modifier
        }
        val modifierLayout = loadLayoutAsync(modifierToLoad).await().onFailure {
            flogWarning { "$keyboardMode - mod - $it" }
        }.getOrNull()
        val extensionLayout = loadLayoutAsync(extension).await().onFailure {
            flogWarning { "$keyboardMode - ext - $it" }
        }.getOrNull()

        val computedArrangement: ArrayList<Array<TextKey>> = arrayListOf()

        if (extensionLayout != null) {
            for (row in extensionLayout.arrangement) {
                val rowArray = Array(row.size) { TextKey(row[it]) }
                computedArrangement.add(rowArray)
            }
        }

        if (mainLayout != null && modifierLayout != null) {
            for (mainRowI in mainLayout.arrangement.indices) {
                val mainRow = mainLayout.arrangement[mainRowI]
                if (mainRowI + 1 < mainLayout.arrangement.size) {
                    val rowArray = Array(mainRow.size) { TextKey(mainRow[it]) }
                    computedArrangement.add(rowArray)
                } else {
                    // merge main and mod here
                    val rowArray = arrayListOf<TextKey>()
                    val firstModRow = modifierLayout.arrangement.firstOrNull()
                    for (modKey in (firstModRow ?: listOf())) {
                        if (modKey is TextKeyData && modKey.code == 0) {
                            rowArray.addAll(mainRow.map { TextKey(it) })
                        } else {
                            rowArray.add(TextKey(modKey))
                        }
                    }
                    val temp = Array(rowArray.size) { rowArray[it] }
                    computedArrangement.add(temp)
                }
            }
            for (modRowI in 1 until modifierLayout.arrangement.size) {
                val modRow = modifierLayout.arrangement[modRowI]
                val rowArray = Array(modRow.size) { TextKey(modRow[it]) }
                computedArrangement.add(rowArray)
            }
        } else if (mainLayout != null && modifierLayout == null) {
            for (mainRow in mainLayout.arrangement) {
                val rowArray = Array(mainRow.size) { TextKey(mainRow[it]) }
                computedArrangement.add(rowArray)
            }
        } else if (mainLayout == null && modifierLayout != null) {
            for (modRow in modifierLayout.arrangement) {
                val rowArray = Array(modRow.size) { TextKey(modRow[it]) }
                computedArrangement.add(rowArray)
            }
        }

        // Add hints to keys
        if (keyboardMode == KeyboardMode.CHARACTERS && computedArrangement.isNotEmpty()) {
            val symbolsComputedArrangement = computeKeyboardAsync(KeyboardMode.SYMBOLS, subtype).await().arrangement
            // number row hint always happens on first row
            if (prefs.keyboard.hintedNumberRowEnabled.get() && symbolsComputedArrangement.isNotEmpty()) {
                val row = computedArrangement[0]
                val symbolRow = symbolsComputedArrangement[0]
                addRowHints(row, symbolRow, KeyType.NUMERIC)
            }
            // all other symbols are added bottom-aligned
            val rOffset = computedArrangement.size - symbolsComputedArrangement.size
            for ((r, row) in computedArrangement.withIndex()) {
                if (r < rOffset) {
                    continue
                }
                val symbolRow = symbolsComputedArrangement.getOrNull(r - rOffset)
                if (symbolRow != null) {
                    addRowHints(row, symbolRow, KeyType.CHARACTER)
                }
            }
        }

        val array = Array(computedArrangement.size) { computedArrangement[it] }
        return TextKeyboard(
            arrangement = array,
            mode = keyboardMode,
            extendedPopupMapping = extendedPopups.await().onFailure {
                flogWarning(LogTopic.LAYOUT_MANAGER) { it.toString() }
            }.getOrNull()?.mapping,
            extendedPopupMappingDefault = extendedPopupsDefault.await().onFailure {
                flogWarning(LogTopic.LAYOUT_MANAGER) { it.toString() }
            }.getOrNull()?.mapping
        )
    }

    private fun addRowHints(main: Array<TextKey>, hint: Array<TextKey>, hintType: KeyType) {
        for ((k,key) in main.withIndex()) {
            val hintKey = hint.getOrNull(k)?.data?.compute(DefaultComputingEvaluator)
            if (hintKey?.type != hintType) {
                continue
            }

            when (hintType) {
                KeyType.CHARACTER -> {
                    key.computedSymbolHint = hintKey
                }
                KeyType.NUMERIC -> {
                    key.computedNumberHint = hintKey
                }
                else -> {
                    // do nothing
                }
            }
        }
    }

    /**
     * Computes a layout for [keyboardMode] based on the given [subtype] and returns it.
     *
     * @param keyboardMode The keyboard mode for which the layout should be computed.
     * @param subtype The subtype which localizes the computed layout.
     */
    fun computeKeyboardAsync(
        keyboardMode: KeyboardMode,
        subtype: Subtype,
    ): Deferred<TextKeyboard> = ioScope.async {
        var main: LTN? = null
        var modifier: LTN? = null
        var extension: LTN? = null

        when (keyboardMode) {
            KeyboardMode.CHARACTERS -> {
                if (prefs.keyboard.numberRow.get()) {
                    extension = LTN(LayoutType.NUMERIC_ROW, subtype.layoutMap.numericRow)
                }
                main = LTN(LayoutType.CHARACTERS, subtype.layoutMap.characters)
                modifier = LTN(LayoutType.CHARACTERS_MOD, extCoreLayout("default"))
            }
            KeyboardMode.EDITING -> {
                // Layout for this mode is defined in custom layout xml file.
                return@async TextKeyboard(arrayOf(), keyboardMode, null, null)
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
                modifier = LTN(LayoutType.SYMBOLS_MOD, extCoreLayout("default"))
            }
            KeyboardMode.SYMBOLS2 -> {
                main = LTN(LayoutType.SYMBOLS2, subtype.layoutMap.symbols2)
                modifier = LTN(LayoutType.SYMBOLS2_MOD, extCoreLayout("default"))
            }
            KeyboardMode.SMARTBAR_CLIPBOARD_CURSOR_ROW -> {
                extension = LTN(LayoutType.EXTENSION, extCoreLayout("clipboard_cursor_row"))
            }
            KeyboardMode.SMARTBAR_NUMBER_ROW -> {
                extension = LTN(LayoutType.NUMERIC_ROW, subtype.layoutMap.numericRow)
            }
            else -> {
                // Default values are already provided
            }
        }

        return@async mergeLayouts(keyboardMode, subtype, main, modifier, extension)
    }

    /**
     * Called when the application is destroyed. Used to cancel any pending coroutines.
     */
    fun onDestroy() {
        ioScope.cancel()
    }
}
