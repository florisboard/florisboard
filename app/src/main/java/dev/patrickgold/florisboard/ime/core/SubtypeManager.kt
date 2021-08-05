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

package dev.patrickgold.florisboard.ime.core

import android.content.Context
import dev.patrickgold.florisboard.common.FlorisLocale
import dev.patrickgold.florisboard.debug.*
import dev.patrickgold.florisboard.res.AssetManager
import dev.patrickgold.florisboard.ime.text.key.CurrencySet
import dev.patrickgold.florisboard.res.FlorisRef
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlin.collections.ArrayList

/**
 * Class which acts as a high level helper for the raw implementation of subtypes in the prefs.
 * Also interprets the default subtype list defined in ime/config.json and provides helper
 * arrays for the language spinner.
 * @property packageName The package name this SubtypeManager is for.
 * @property prefs Reference to the preferences, where the raw subtype settings are accessible.
 * @property imeConfig The [FlorisBoard.ImeConfig] of this input method editor.
 * @property subtypes The currently active subtypes.
 */
class SubtypeManager(
    private val packageName: String
) : CoroutineScope by MainScope() {
    private val assetManager get() = AssetManager.default()
    private val prefs get() = Preferences.default()

    companion object {
        const val IME_CONFIG_FILE_PATH = "ime/config.json"
        const val SUBTYPE_LIST_STR_DELIMITER = ";"

        private var instance: SubtypeManager? = null

        fun init(context: Context): SubtypeManager {
            val defaultInstance = SubtypeManager(context.packageName)
            instance = defaultInstance
            return defaultInstance
        }

        fun default(): SubtypeManager = instance!!

        fun defaultOrNull(): SubtypeManager? = instance
    }

    var imeConfig: FlorisBoard.ImeConfig = FlorisBoard.ImeConfig(packageName)
    private val _subtypes: ArrayList<Subtype> = ArrayList()
    val subtypes: List<Subtype> get() = _subtypes

    init {
        imeConfig = loadImeConfig(IME_CONFIG_FILE_PATH)

        val listRaw = prefs.localization.subtypes
        if (listRaw.isNotBlank()) {
            listRaw.split(SUBTYPE_LIST_STR_DELIMITER).forEach {
                _subtypes.add(Subtype.fromString(it))
            }
        }
    }

    private fun syncSubtypeListToPrefs() {
        prefs.localization.subtypes = _subtypes.joinToString(SUBTYPE_LIST_STR_DELIMITER)
    }

    /**
     * Loads the [FlorisBoard.ImeConfig] from ime/config.json.
     *
     * @param path The path to to IME config file.
     * @return The [FlorisBoard.ImeConfig] or a default config.
     */
    private fun loadImeConfig(path: String): FlorisBoard.ImeConfig {
        return assetManager.loadJsonAsset<FlorisBoard.ImeConfig>(FlorisRef.assets(path)).getOrElse {
            flogError(LogTopic.SUBTYPE_MANAGER) { "Failed to retrieve IME config: $it" }
            FlorisBoard.ImeConfig(packageName)
        }
    }

    /**
     * Adds a given [subtypeToAdd] to the subtype list, if it does not exist.
     *
     * @param subtypeToAdd The subtype which should be added.
     * @return True if the subtype was added, false otherwise. A return value of false indicates
     *  that the subtype already exists.
     */
    private fun addSubtype(subtypeToAdd: Subtype): Boolean {
        if (_subtypes.contains(subtypeToAdd)) {
            return false
        }
        _subtypes.add(subtypeToAdd)
        syncSubtypeListToPrefs()
        return true
    }

    /**
     * Creates a [Subtype] from the given [locale] and [layoutMap] and adds it to the subtype
     * list, if it does not exist.
     *
     * @param locale The locale of the subtype to be added.
     * @param composerName The composer name of the subtype to be added.
     * @param currencySetName The currency set name of the subtype to be added.
     * @param layoutMap The layout map of the subtype to be added.
     * @return True if the subtype was added, false otherwise. A return value of false indicates
     *  that the subtype already exists.
     */
    fun addSubtype(locale: FlorisLocale, composerName: String, currencySetName: String, layoutMap: SubtypeLayoutMap): Boolean {
        return addSubtype(
            Subtype(
                (locale.hashCode() + 31 * layoutMap.hashCode() + 31 * currencySetName.hashCode()),
                locale,
                composerName,
                currencySetName,
                layoutMap
            )
        )
    }

    /**
     * Gets the currency set from the given subtype and returns it. Falls back to a default one if the subtype does not
     * exist.
     *
     * @return The currency set or a fallback.
     */
    fun getCurrencySet(subtypeToSearch: Subtype): CurrencySet {
        return imeConfig.currencySets.find { it.name == subtypeToSearch.currencySetName } ?: CurrencySet.default()
    }

    /**
     * Gets the active subtype and returns it. If the activeSubtypeId points to a non-existent
     * subtype, this method tries to determine a new active subtype.
     *
     * @return The active subtype or null, if the subtype list is empty or no new active subtype
     *  could be determined.
     */
    fun getActiveSubtype(): Subtype? {
        val subtypeList = _subtypes
        for (subtype in subtypeList) {
            if (subtype.id == prefs.localization.activeSubtypeId) {
                return subtype
            }
        }
        return if (subtypeList.isNotEmpty()) {
            prefs.localization.activeSubtypeId = subtypeList[0].id
            subtypeList[0]
        } else {
            prefs.localization.activeSubtypeId = Subtype.DEFAULT.id
            null
        }
    }

    /**
     * Gets a subtype by the given [id].
     *
     * @param id The id of the subtype you want to get.
     * @return The subtype or null, if no matching subtype could be found.
     */
    fun getSubtypeById(id: Int): Subtype? {
        val subtypeList = _subtypes
        for (subtype in subtypeList) {
            if (subtype.id == id) {
                return subtype
            }
        }
        return null
    }

    /**
     * Gets the default system subtype for a given [locale].
     *
     * @param locale The locale of the default system subtype to get.
     * @return The default system locale or null, if no matching default system subtype could be
     *  found.
     */
    fun getDefaultSubtypeForLocale(locale: FlorisLocale): DefaultSubtype? {
        for (defaultSubtype in imeConfig.defaultSubtypes) {
            if (defaultSubtype.locale == locale) {
                return defaultSubtype
            }
        }
        return null
    }

    /**
     * Modifies an existing subtype with the newly provided details. In order to determine which
     * subtype should be updated, the id must be the same.
     *
     * @param subtypeToModify The subtype with the new details but same id.
     */
    fun modifySubtypeWithSameId(subtypeToModify: Subtype) {
        val index = _subtypes.indexOfFirst { subtypeToModify.id == it.id }
        if (index >= 0 && index < _subtypes.size) {
            _subtypes[index] = subtypeToModify
            syncSubtypeListToPrefs()
        }
    }

    /**
     * Removes a given [subtypeToRemove]. Nothing happens if the given [subtypeToRemove] does not
     * exist.
     *
     * @param subtypeToRemove The subtype which should be removed.
     */
    fun removeSubtype(subtypeToRemove: Subtype) {
        val subtypeList = _subtypes
        for (subtype in subtypeList) {
            if (subtype == subtypeToRemove) {
                subtypeList.remove(subtypeToRemove)
                break
            }
        }
        syncSubtypeListToPrefs()
        if (subtypeToRemove.id == prefs.localization.activeSubtypeId) {
            getActiveSubtype()
        }
    }

    /**
     * Switch to the previous subtype in the subtype list if possible.
     *
     * @return The new active subtype or null if the determination process failed.
     */
    fun switchToPrevSubtype(): Subtype? {
        val subtypeList = _subtypes
        val activeSubtype = getActiveSubtype() ?: return null
        var triggerNextSubtype = false
        var newActiveSubtype: Subtype? = null
        for (subtype in subtypeList.reversed()) {
            if (triggerNextSubtype) {
                triggerNextSubtype = false
                newActiveSubtype = subtype
            } else if (subtype == activeSubtype) {
                triggerNextSubtype = true
            }
        }
        if (triggerNextSubtype) {
            newActiveSubtype = subtypeList.last()
        }
        prefs.localization.activeSubtypeId = when (newActiveSubtype) {
            null -> Subtype.DEFAULT.id
            else -> newActiveSubtype.id
        }
        return newActiveSubtype
    }

    /**
     * Switch to the next subtype in the subtype list if possible.
     *
     * @return The new active subtype or null if the determination process failed.
     */
    fun switchToNextSubtype(): Subtype? {
        val subtypeList = _subtypes
        val activeSubtype = getActiveSubtype() ?: return null
        var triggerNextSubtype = false
        var newActiveSubtype: Subtype? = null
        for (subtype in subtypeList) {
            if (triggerNextSubtype) {
                triggerNextSubtype = false
                newActiveSubtype = subtype
            } else if (subtype == activeSubtype) {
                triggerNextSubtype = true
            }
        }
        if (triggerNextSubtype) {
            newActiveSubtype = subtypeList.first()
        }
        prefs.localization.activeSubtypeId = when (newActiveSubtype) {
            null -> Subtype.DEFAULT.id
            else -> newActiveSubtype.id
        }
        return newActiveSubtype
    }
}
