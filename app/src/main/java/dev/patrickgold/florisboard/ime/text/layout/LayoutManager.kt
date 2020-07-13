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
import dev.patrickgold.florisboard.ime.core.PrefHelper
import dev.patrickgold.florisboard.ime.core.Subtype
import dev.patrickgold.florisboard.ime.core.SubtypeManager
import dev.patrickgold.florisboard.ime.text.key.KeyData
import dev.patrickgold.florisboard.ime.text.key.KeyTypeAdapter
import dev.patrickgold.florisboard.ime.text.key.KeyVariation
import dev.patrickgold.florisboard.ime.text.key.KeyVariationAdapter
import dev.patrickgold.florisboard.ime.text.keyboard.KeyboardMode
import java.util.*

class LayoutManager(private val context: Context) {
    private var subtype: Subtype = SubtypeManager.fallbackSubtype
    private val layoutAssociations = EnumMap<LayoutType, String>(LayoutType::class.java)

    private fun associate(type: LayoutType, name: String) {
        layoutAssociations[type] = name
    }

    private fun disassociate(type: LayoutType) {
        layoutAssociations.remove(type)
    }

    /**
     * This method automatically fetches the current user selected keyboard layout prefs from
     * the shared preferences and sets the associations for each layout type.
     */
    fun autoFetchAssociationsFromPrefs(prefs: PrefHelper, subtypeManager: SubtypeManager) {
        // TODO: Fetch current layout preferences instead of using dev constants
        subtype = subtypeManager.getActiveSubtype() ?: SubtypeManager.fallbackSubtype
        associate(LayoutType.CHARACTERS, subtype.layout)
        associate(LayoutType.CHARACTERS_MOD, "default")
        associate(LayoutType.EXTENSION, "number_row")
        associate(LayoutType.NUMERIC, "default")
        associate(LayoutType.NUMERIC_ADVANCED, "default")
        associate(LayoutType.PHONE, "default")
        associate(LayoutType.PHONE2, "default")
        associate(LayoutType.SYMBOLS, "western_default")
        associate(LayoutType.SYMBOLS_MOD, "default")
        associate(LayoutType.SYMBOLS2, "western_default")
        associate(LayoutType.SYMBOLS2_MOD, "default")
    }

    private fun loadLayout(
        type: LayoutType?, name: String? = layoutAssociations[type]
    ): LayoutData? {
        if (type == null || name == null || !layoutAssociations.containsKey(type)) {
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

    private fun mergeLayouts(
        keyboardMode: KeyboardMode,
        main: LayoutType?,
        modifier: LayoutType?,
        includeExtension: Boolean
    ): ComputedLayoutData {
        val computedArrangement: ComputedLayoutDataArrangement = mutableListOf()

        if (includeExtension) {
            val extensionLayout = loadLayout(LayoutType.EXTENSION) ?: LayoutData.empty()
            val row = extensionLayout.arrangement.firstOrNull()
            if (row != null) {
                computedArrangement.add(row.toMutableList())
            }
        }

        val mainLayout = loadLayout(main)
        val modifierLayout =  loadLayout(modifier)

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

    fun getComputedLayout(keyboardMode: KeyboardMode, type: LayoutType, name: String) : ComputedLayoutData? {
        val loadedLayout = loadLayout(type, name) ?: return null
        return loadedLayout.toComputedLayoutData(keyboardMode)
    }

    fun computeLayoutFor(keyboardMode: KeyboardMode): ComputedLayoutData? {
        return when (keyboardMode) {
            KeyboardMode.CHARACTERS -> {
                mergeLayouts(
                    keyboardMode, LayoutType.CHARACTERS, LayoutType.CHARACTERS_MOD, false
                )
            }
            KeyboardMode.NUMERIC -> {
                mergeLayouts(
                    keyboardMode, LayoutType.NUMERIC, null, false
                )
            }
            KeyboardMode.NUMERIC_ADVANCED -> {
                mergeLayouts(
                    keyboardMode, LayoutType.NUMERIC_ADVANCED, null, false
                )
            }
            KeyboardMode.PHONE -> {
                mergeLayouts(
                    keyboardMode, LayoutType.PHONE, null, false
                )
            }
            KeyboardMode.PHONE2 -> {
                mergeLayouts(
                    keyboardMode, LayoutType.PHONE2, null, false
                )
            }
            KeyboardMode.SYMBOLS -> {
                mergeLayouts(
                    keyboardMode, LayoutType.SYMBOLS, LayoutType.SYMBOLS_MOD, true
                )
            }
            KeyboardMode.SYMBOLS2 -> {
                mergeLayouts(
                    keyboardMode, LayoutType.SYMBOLS2, LayoutType.SYMBOLS2_MOD, false
                )
            }
            else -> null
        }
    }
}
