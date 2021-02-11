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
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dev.patrickgold.florisboard.util.LocaleUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import java.util.*

/**
 * Class which acts as a high level helper for the raw implementation of subtypes in the prefs.
 * Also interprets the default subtype list defined in ime/config.json and provides helper
 * arrays for the language spinner.
 * @property context Android context, used for interacting with the system.
 * @property prefs Reference to the preferences, where the raw subtype settings are accessible.
 * @property imeConfig The [FlorisBoard.ImeConfig] of this input method editor.
 * @property subtypes Dynamic property which parses the raw subtype list from prefs and returns a
 *  list of [Subtype]s. When setting this property, the given list is converted to a raw string
 *  and written to prefs.
 */
@Suppress("SameParameterValue")
class SubtypeManager(
    private val context: Context,
    private val prefs: PrefHelper
) : CoroutineScope by MainScope() {

    companion object {
        const val IME_CONFIG_FILE_PATH = "ime/config.json"
        const val SUBTYPE_LIST_STR_DELIMITER = ";"
    }

    var imeConfig: FlorisBoard.ImeConfig = FlorisBoard.ImeConfig(context.packageName)
    var subtypes: List<Subtype>
        get() {
            val listRaw = prefs.localization.subtypes
            return if (listRaw.isBlank()) {
                listOf()
            } else {
                listRaw.split(SUBTYPE_LIST_STR_DELIMITER).map {
                    Subtype.fromString(it)
                }
            }
        }
        set(v) {
            prefs.localization.subtypes = v.joinToString(SUBTYPE_LIST_STR_DELIMITER)
        }

    init {
        launch(Dispatchers.IO) {
            imeConfig = loadImeConfig(IME_CONFIG_FILE_PATH)
        }
    }

    /**
     * Loads the [FlorisBoard.ImeConfig] from ime/config.json.
     *
     * @param path The path to to IME config file.
     * @return The [FlorisBoard.ImeConfig] or a default config.
     */
    private fun loadImeConfig(path: String): FlorisBoard.ImeConfig {
        val rawJsonData: String = try {
            context.assets.open(path).bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            null
        } ?: return FlorisBoard.ImeConfig(context.packageName)
        val moshi = Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .add(LocaleUtils.JsonAdapter())
            .build()
        val layoutAdapter = moshi.adapter(FlorisBoard.ImeConfig::class.java)
        return layoutAdapter.fromJson(rawJsonData) ?: FlorisBoard.ImeConfig(
            context.packageName
        )
    }

    /**
     * Adds a given [subtypeToAdd] to the subtype list, if it does not exist.
     *
     * @param subtypeToAdd The subtype which should be added.
     * @return True if the subtype was added, false otherwise. A return value of false indicates
     *  that the subtype already exists.
     */
    private fun addSubtype(subtypeToAdd: Subtype): Boolean {
        val subtypeList = subtypes.toMutableList()
        if (subtypeList.contains(subtypeToAdd)) {
            return false
        }
        subtypeList.add(subtypeToAdd)
        subtypes = subtypeList
        return true
    }

    /**
     * Creates a [Subtype] from the given [locale] and [layoutName] and adds it to the subtype
     * list, if it does not exist.
     *
     * @param locale The locale of the subtype to be added.
     * @param layoutName The layout name of the subtype to be added.
     * @return True if the subtype was added, false otherwise. A return value of false indicates
     *  that the subtype already exists.
     */
    fun addSubtype(locale: Locale, layoutName: String): Boolean {
        return addSubtype(
            Subtype(
                (locale.hashCode() + layoutName.hashCode()),
                locale,
                layoutName
            )
        )
    }

    /**
     * Gets the active subtype and returns it. If the activeSubtypeId points to a non-existent
     * subtype, this method tries to determine a new active subtype.
     *
     * @return The active subtype or null, if the subtype list is empty or no new active subtype
     *  could be determined.
     */
    fun getActiveSubtype(): Subtype? {
        for (subtype in subtypes) {
            if (subtype.id == prefs.localization.activeSubtypeId) {
                return subtype
            }
        }
        val subtypeList = subtypes
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
        for (subtype in subtypes) {
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
    fun getDefaultSubtypeForLocale(locale: Locale): DefaultSubtype? {
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
        val subtypeList = subtypes
        for (subtype in subtypeList) {
            if (subtype.id == subtypeToModify.id) {
                subtype.locale = subtypeToModify.locale
                subtype.layout = subtypeToModify.layout
                break
            }
        }
        subtypes = subtypeList
    }

    /**
     * Removes a given [subtypeToRemove]. Nothing happens if the given [subtypeToRemove] does not
     * exist.
     *
     * @param subtypeToRemove The subtype which should be removed.
     */
    fun removeSubtype(subtypeToRemove: Subtype) {
        val subtypeList = subtypes.toMutableList()
        for (subtype in subtypeList) {
            if (subtype == subtypeToRemove) {
                subtypeList.remove(subtypeToRemove)
                break
            }
        }
        subtypes = subtypeList
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
        val subtypeList = subtypes
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
        val subtypeList = subtypes
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
