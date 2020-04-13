package dev.patrickgold.florisboard.ime.layout

import android.content.Context
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dev.patrickgold.florisboard.ime.kbd.KeyData
import dev.patrickgold.florisboard.ime.kbd.KeyTypeAdapter
import dev.patrickgold.florisboard.ime.kbd.KeyboardMode
import java.util.*

class LayoutManager(private val context: Context) {

    private val layoutAssociations = EnumMap<LayoutType, String>(LayoutType::class.java)

    fun associate(type: LayoutType, name: String) {
        layoutAssociations[type] = name
    }

    fun disassociate(type: LayoutType) {
        layoutAssociations.remove(type)
    }

    /**
     * This method automatically fetches the current user selected keyboard layout prefs from
     * the shared preferences and sets the associations for each layout type.
     */
    fun autoFetchAssociationsFromPrefs() {
        // TODO: Fetch current layout preferences instead of using dev constants
        associate(LayoutType.CHARACTERS, "qwerty")
        associate(LayoutType.CHARACTERS_MOD, "default")
    }

    private fun loadLayout(type: LayoutType?): LayoutData? {
        if (type == null || !layoutAssociations.containsKey(type)) {
            return null
        }
        val name = layoutAssociations[type]
        val rawJsonData: String = try {
            context.assets.open("ime/$type/$name.json").bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            null
        } ?: return null
        val moshi = Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .add(LayoutTypeAdapter())
            .add(KeyTypeAdapter())
            .build()
        val layoutAdapter = moshi.adapter(LayoutData::class.java)
        return layoutAdapter.fromJson(rawJsonData)
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

        return ComputedLayoutData(
            keyboardMode,
            "computed",
            mainLayout?.direction ?: "ltr",
            computedArrangement
        )
    }

    fun computeLayoutFor(keyboardMode: KeyboardMode): ComputedLayoutData? {
        // TODO: add support for more keyboard types
        return when (keyboardMode) {
            KeyboardMode.CHARACTERS -> {
                mergeLayouts(
                    keyboardMode, LayoutType.CHARACTERS, LayoutType.CHARACTERS_MOD, false
                )
            }
            KeyboardMode.SYMBOLS -> {
                mergeLayouts(
                    keyboardMode, LayoutType.SYMBOLS, LayoutType.SYMBOLS_MOD, true
                )
            }
            KeyboardMode.SYMBOLS2 -> {
                mergeLayouts(
                    keyboardMode, LayoutType.SYMBOLS2, LayoutType.SYMBOLS2_MOD, true
                )
            }
            else -> null
        }
    }
}