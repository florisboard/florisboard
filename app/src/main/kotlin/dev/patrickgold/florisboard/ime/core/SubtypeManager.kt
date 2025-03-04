/*
 * Copyright (C) 2021-2025 The FlorisBoard Contributors
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
import dev.patrickgold.florisboard.app.florisPreferenceModel
import dev.patrickgold.florisboard.ime.keyboard.CurrencySet
import dev.patrickgold.florisboard.keyboardManager
import dev.patrickgold.florisboard.lib.FlorisLocale
import dev.patrickgold.florisboard.lib.devtools.flogDebug
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

val SubtypeJsonConfig = Json {
    encodeDefaults = true
    ignoreUnknownKeys = true
    isLenient = false
}

/**
 * Class which acts as a high level helper for the raw implementation of subtypes in the prefs. Additionally provides
 * helper methods for the in-keyboard language switch process.
 */
class SubtypeManager(context: Context) {
    private val prefs by florisPreferenceModel()
    private val keyboardManager by context.keyboardManager()

    private val _subtypesFlow = MutableStateFlow(listOf<Subtype>())
    val subtypesFlow = _subtypesFlow.asStateFlow()
    inline var subtypes
        get() = subtypesFlow.value
        private set(v) { _subtypesFlow.value = v }

    private val _activeSubtypeFlow = MutableStateFlow(Subtype.DEFAULT)
    val activeSubtypeFlow = _activeSubtypeFlow.asStateFlow()
    inline var activeSubtype
        get() = activeSubtypeFlow.value
        private set(v) { _activeSubtypeFlow.value = v }

    init {
        prefs.localization.subtypes.observeForever { listRaw ->
            flogDebug { listRaw }
            val list = if (listRaw.isNotBlank()) {
                SubtypeJsonConfig.decodeFromString<List<Subtype>>(listRaw)
            } else {
                emptyList()
            }
            subtypes = list
            evaluateActiveSubtype(list)
        }
    }

    private fun persistNewSubtypeList(list: List<Subtype>) {
        val listRaw = SubtypeJsonConfig.encodeToString(list)
        prefs.localization.subtypes.set(listRaw)
    }

    /**
     * Gets the active subtype and returns it. If the activeSubtypeId points to a non-existent
     * subtype, this method tries to determine a new active subtype.
     *
     * @return The active subtype or null, if the subtype list is empty or no new active subtype
     *  could be determined.
     */
    private fun evaluateActiveSubtype(list: List<Subtype>) {
        val activeSubtypeId = prefs.localization.activeSubtypeId.get()
        val subtype = list.find { it.id == activeSubtypeId } ?: list.firstOrNull() ?: Subtype.DEFAULT
        if (subtype.id != activeSubtypeId) {
            prefs.localization.activeSubtypeId.set(subtype.id)
        }
        activeSubtype = subtype
    }

    /**
     * Adds a given [subtype] to the subtype list, if it does not exist.
     *
     * @param subtype The subtype which should be added.
     * @return True if the subtype was added, false otherwise. A return value of false indicates
     *  that the subtype already exists.
     */
    fun addSubtype(subtype: Subtype): Boolean {
        val subtypeToAdd = subtype.copy(id = System.currentTimeMillis())
        val subtypeList = subtypes
        if (subtypeList.find { it.equalsExcludingId(subtype) } != null) {
            return false
        }
        val newSubtypeList = subtypeList + subtypeToAdd
        persistNewSubtypeList(newSubtypeList)
        return true
    }

    /**
     * Gets the currency set from the given subtype and returns it. Falls back to a default one if the subtype does not
     * exist.
     *
     * @return The currency set or a fallback.
     */
    fun getCurrencySet(subtypeToSearch: Subtype): CurrencySet {
        return keyboardManager.resources.currencySets.value?.get(subtypeToSearch.currencySet) ?: CurrencySet.Fallback
    }

    /**
     * Gets a subtype by the given [id].
     *
     * @param id The id of the subtype you want to get.
     * @return The subtype or null, if no matching subtype could be found.
     */
    fun getSubtypeById(id: Long): Subtype? {
        val subtypeList = subtypes
        return subtypeList.find { it.id == id }
    }

    /**
     * Gets the default system subtype for a given [locale].
     *
     * @param locale The locale of the default system subtype to get.
     * @return The default system locale or null, if no matching default system subtype could be
     *  found.
     */
    fun getSubtypePresetForLocale(locale: FlorisLocale): SubtypePreset? {
        val presets = keyboardManager.resources.subtypePresets.value
        return presets?.find { it.locale == locale } ?: presets?.find { it.locale.language == locale.language }
    }

    /**
     * Modifies an existing subtype with the newly provided details. In order to determine which
     * subtype should be updated, the id must be the same.
     *
     * @param subtypeToModify The subtype with the new details but same id.
     */
    fun modifySubtypeWithSameId(subtypeToModify: Subtype) {
        val subtypeList = subtypes
        val index = subtypeList.indexOfFirst { subtypeToModify.id == it.id }
        if (index >= 0 && index < subtypeList.size) {
            val newSubtypeList = subtypeList.mapIndexed { n, subtype ->
                if (n == index) {
                    subtypeToModify
                } else {
                    subtype
                }
            }
            persistNewSubtypeList(newSubtypeList)
        }
    }

    /**
     * Removes a given [subtypeToRemove]. Nothing happens if the given [subtypeToRemove] does not
     * exist.
     *
     * @param subtypeToRemove The subtype which should be removed.
     */
    fun removeSubtype(subtypeToRemove: Subtype) {
        val subtypeList = subtypes
        val indexToRemove = subtypeList.indexOf(subtypeToRemove)
        if (indexToRemove in subtypeList.indices) {
            val newSubtypeList = subtypeList.mapIndexedNotNull { n, subtype ->
                if (n != indexToRemove) {
                    subtype
                } else {
                    null
                }
            }
            persistNewSubtypeList(newSubtypeList)
            evaluateActiveSubtype(newSubtypeList)
        }
    }

    /**
     * Switch to the previous subtype in the subtype list if possible.
     */
    fun switchToPrevSubtype() {
        val subtypeList = subtypes
        val cachedActiveSubtype = activeSubtype
        var triggerNextSubtype = false
        var newActiveSubtype: Subtype = Subtype.DEFAULT
        for (subtype in subtypeList.asReversed()) {
            if (triggerNextSubtype) {
                triggerNextSubtype = false
                newActiveSubtype = subtype
            } else if (subtype == cachedActiveSubtype) {
                triggerNextSubtype = true
            }
        }
        if (triggerNextSubtype) {
            newActiveSubtype = subtypeList.last()
        }
        prefs.localization.activeSubtypeId.set(newActiveSubtype.id)
        activeSubtype = newActiveSubtype
    }

    /**
     * Switch to the next subtype in the subtype list if possible.
     */
    fun switchToNextSubtype() {
        val subtypeList = subtypes
        val cachedActiveSubtype = activeSubtype
        var triggerNextSubtype = false
        var newActiveSubtype: Subtype = Subtype.DEFAULT
        for (subtype in subtypeList) {
            if (triggerNextSubtype) {
                triggerNextSubtype = false
                newActiveSubtype = subtype
            } else if (subtype == cachedActiveSubtype) {
                triggerNextSubtype = true
            }
        }
        if (triggerNextSubtype) {
            newActiveSubtype = subtypeList.first()
        }
        prefs.localization.activeSubtypeId.set(newActiveSubtype.id)
        activeSubtype = newActiveSubtype
    }

    fun switchToSubtypeById(id: Long) {
        if (subtypes.any { it.id == id }) {
            activeSubtype = getSubtypeById(id)!!
            prefs.localization.activeSubtypeId.set(id)
        }
    }
}
