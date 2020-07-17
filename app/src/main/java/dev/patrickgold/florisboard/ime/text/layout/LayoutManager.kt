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

import android.content.Context
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dev.patrickgold.florisboard.ime.core.Subtype
import dev.patrickgold.florisboard.ime.text.key.KeyData
import dev.patrickgold.florisboard.ime.text.key.KeyTypeAdapter
import dev.patrickgold.florisboard.ime.text.key.KeyVariation
import dev.patrickgold.florisboard.ime.text.key.KeyVariationAdapter
import dev.patrickgold.florisboard.ime.text.keyboard.KeyboardMode
import kotlinx.coroutines.*
import java.util.*

private typealias LTN = Pair<LayoutType, String>
private typealias KMS = Pair<KeyboardMode, Subtype>

/**
 * Class which manages layout loading and caching.
 */
class LayoutManager(private val context: Context) : CoroutineScope by MainScope() {
    private val layoutCache: HashMap<KMS, Deferred<ComputedLayoutData>> = hashMapOf()

    /**
     * Loads the layout for the specified type and name.
     *
     * @returns the [LayoutData] or null.
     */
    private fun loadLayout(ltn: LTN?) = loadLayout(ltn?.first, ltn?.second)
    private fun loadLayout(type: LayoutType?, name: String?): LayoutData? {
        if (type == null || name == null) {
            return null
        }
        val rawJsonData: String = try {
            context.assets.open("ime/text/$type/$name.json").bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            null
        } ?: return null
        val moshi = Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .add(LayoutTypeAdapter())
            .add(KeyTypeAdapter())
            .add(KeyVariationAdapter())
            .build()
        val layoutAdapter = moshi.adapter(LayoutData::class.java)
        return layoutAdapter.fromJson(rawJsonData)
    }

    private fun loadExtendedPopups(subtype: Subtype): Map<String, List<KeyData>> {
        val lang = subtype.locale.language
        val map = loadExtendedPopupsInternal("ime/text/characters/extended_popups/$lang.json")
        return map ?: mapOf()
    }

    private fun loadExtendedPopupsInternal(path: String): Map<String, List<KeyData>>? {
        val rawJsonData: String = try {
            context.assets.open(path).bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            null
        } ?: return null
        val moshi = Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .add(KeyTypeAdapter())
            .build()
        val mapAdaptor: JsonAdapter<Map<String, List<KeyData>>> =
            moshi.adapter(
                Types.newParameterizedType(
                    Map::class.java,
                    String::class.java,
                    Types.newParameterizedType(
                        List::class.java,
                        KeyData::class.java
                    )
                )
            )
        return mapAdaptor.fromJson(rawJsonData)
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
     * @param keyboardMode The keyboard mode for the returning [ComputedLayoutData].
     * @param subtype The subtype used for populating the extended popups.
     * @param main The main layout type and name.
     * @param modifier The modifier (mod) layout type and name.
     * @param extension The extension layout type and name.
     * @returns a [ComputedLayoutData] object, regardless of the specified LTNs or errors.
     */
    private fun mergeLayouts(
        keyboardMode: KeyboardMode,
        subtype: Subtype,
        main: LTN? = null,
        modifier: LTN? = null,
        extension: LTN? = null
    ): ComputedLayoutData {
        val computedArrangement: ComputedLayoutDataArrangement = mutableListOf()

        val mainLayout = loadLayout(main)
        val modifierLayout =  loadLayout(modifier)
        val extensionLayout = loadLayout(extension)

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
                    val mergedRow = mutableListOf<KeyData>()
                    val firstModRow = modifierLayout.arrangement.firstOrNull()
                    val firstModKey = firstModRow?.firstOrNull()
                    if (firstModKey != null) {
                        mergedRow.add(firstModKey)
                    }
                    mergedRow.addAll(mainRow)
                    val lastModKey = firstModRow?.lastOrNull()
                    if (lastModKey != null && firstModKey != lastModKey) {
                        mergedRow.add(lastModKey)
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

        // TODO: rewrite this part
        if (keyboardMode == KeyboardMode.CHARACTERS) {
            val extendedPopups = loadExtendedPopups(subtype)
            for (computedRow in computedArrangement) {
                for (keyData in computedRow) {
                    if (keyData.variation != KeyVariation.ALL) {
                        if (keyData.variation == KeyVariation.NORMAL ||
                            keyData.variation == KeyVariation.PASSWORD) {
                            if (extendedPopups.containsKey(keyData.label + "~normal")) {
                                keyData.popup.addAll(extendedPopups[keyData.label + "~normal"] ?: listOf())
                            }
                        }
                        if (keyData.variation == KeyVariation.EMAIL_ADDRESS ||
                            keyData.variation == KeyVariation.URI) {
                            if (extendedPopups.containsKey(keyData.label + "~uri")) {
                                keyData.popup.addAll(extendedPopups[keyData.label + "~uri"] ?: listOf())
                            }
                        }
                    }
                    if (extendedPopups.containsKey(keyData.label)) {
                        keyData.popup.addAll(extendedPopups[keyData.label] ?: listOf())
                    }
                }
            }
        }

        return ComputedLayoutData(
            keyboardMode,
            "computed",
            mainLayout?.direction ?: "ltr",
            computedArrangement
        )
    }

    /**
     * Computes a layout for [keyboardMode] based on the given [subtype] and returns it.
     *
     * TODO: used layouts for symbols should be dynamically selected based on subtype
     *
     * @param keyboardMode The keyboard mode for which the layout should be computed.
     * @param subtype The subtype which localizes the computed layout.
     */
    private fun computeLayoutFor(
        keyboardMode: KeyboardMode,
        subtype: Subtype
    ): ComputedLayoutData {
        var main: LTN? = null
        var modifier: LTN? = null
        var extension: LTN? = null

        when (keyboardMode) {
            KeyboardMode.CHARACTERS -> {
                main = LTN(LayoutType.CHARACTERS, subtype.layout)
                modifier = LTN(LayoutType.CHARACTERS_MOD, "default")
            }
            KeyboardMode.NUMERIC -> {
                main = LTN(LayoutType.NUMERIC, "default")
            }
            KeyboardMode.NUMERIC_ADVANCED -> {
                main = LTN(LayoutType.NUMERIC_ADVANCED, "default")
            }
            KeyboardMode.PHONE -> {
                main = LTN(LayoutType.PHONE, "default")
            }
            KeyboardMode.PHONE2 -> {
                main = LTN(LayoutType.PHONE2, "default")
            }
            KeyboardMode.SYMBOLS -> {
                main = LTN(LayoutType.SYMBOLS, "western_default")
                modifier = LTN(LayoutType.SYMBOLS_MOD, "default")
                extension = LTN(LayoutType.EXTENSION, "number_row")
            }
            KeyboardMode.SYMBOLS2 -> {
                main = LTN(LayoutType.SYMBOLS2, "western_default")
                modifier = LTN(LayoutType.SYMBOLS2_MOD, "default")
            }
        }

        return mergeLayouts(keyboardMode, subtype, main, modifier, extension)
    }

    /**
     * Preloads the layout for the given [keyboardMode]/[subtype] combo or returns the cached entry,
     * if it exists. The returned value is a deferred  computed layout data. This function returns
     * immediately and won't block. To retrieve the actual computed layout await the returned value.
     *
     * @param keyboardMode The keyboard mode for which the layout should be computed.
     * @param subtype The subtype which localizes the computed layout.
     */
    @Synchronized
    fun fetchComputedLayoutAsync(
        keyboardMode: KeyboardMode,
        subtype: Subtype
    ): Deferred<ComputedLayoutData> {
        val kms = KMS(keyboardMode, subtype)
        val cachedComputedLayout = layoutCache[kms]
        return if (cachedComputedLayout != null) {
            cachedComputedLayout
        } else {
            val computedLayout = async(Dispatchers.IO) {
                computeLayoutFor(keyboardMode, subtype)
            }
            layoutCache[kms] = computedLayout
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
        subtype: Subtype
    ) {
        val kms = KMS(keyboardMode, subtype)
        if (layoutCache[kms] == null) {
            layoutCache[kms] = async(Dispatchers.IO) {
                computeLayoutFor(keyboardMode, subtype)
            }
        }
    }

    /**
     * Called when the application is destroyed. Used to cancel any pending coroutines.
     */
    fun onDestroy() {
        cancel()
    }
}
