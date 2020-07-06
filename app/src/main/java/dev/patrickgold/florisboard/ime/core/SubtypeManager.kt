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
 * @property fallbackSubtype Subtype to use when prefs do not contain any valid subtypes.
 */
class SubtypeManager(private val context: Context, private val prefs: PrefHelper) {
    companion object {
        val fallbackSubtype: Subtype = Subtype(-1, Locale.ENGLISH, "qwerty")
    }

    val imeConfig: FlorisBoard.ImeConfig
    var subtypes: List<Subtype>
        get() {
            val listRaw = prefs.keyboard.subtypes
            return if (listRaw == "") {
                listOf()
            } else {
                listRaw.split(";").map {
                    Subtype.fromString(it)
                }
            }
        }
        set(v) {
            prefs.keyboard.subtypes = v.joinToString(";")
        }

    init {
        imeConfig = loadImeConfig()
    }

    /**
     * Loads the [FlorisBoard.ImeConfig] from ime/config.json.
     * @returns The [FlorisBoard.ImeConfig] or a default config.
     */
    private fun loadImeConfig(): FlorisBoard.ImeConfig {
        val rawJsonData: String = try {
            context.assets.open("ime/config.json").bufferedReader().use { it.readText() }
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

    private fun addSubtype(subtype: Subtype) {
        val oldListRaw = prefs.keyboard.subtypes
        if (oldListRaw.isBlank()) {
            prefs.keyboard.subtypes = "$subtype"
        } else {
            prefs.keyboard.subtypes = "$oldListRaw;$subtype"
        }
    }
    fun addSubtype(locale: Locale, layoutName: String) {
        addSubtype(
            Subtype(
                getDefaultSubtypeForLocale(locale)?.id
                    ?: (locale.hashCode() + layoutName.hashCode()),
                locale,
                layoutName
            )
        )
    }
    fun getActiveSubtype(): Subtype? {
        for (s in subtypes) {
            if (s.id == prefs.keyboard.activeSubtypeId) {
                return s
            }
        }
        val subtypes = this.subtypes
        return if (subtypes.isNotEmpty()) {
            prefs.keyboard.activeSubtypeId = subtypes[0].id
            subtypes[0]
        } else {
            prefs.keyboard.activeSubtypeId = -1
            null
        }
    }
    fun getSubtypeById(id: Int): Subtype? {
        for (s in subtypes) {
            if (s.id == id) {
                return s
            }
        }
        return null
    }
    private fun getDefaultSubtypeForLocale(locale: Locale): DefaultSubtype? {
        for (systemSubtype in imeConfig.defaultSubtypes) {
            if (systemSubtype.locale == locale) {
                return systemSubtype
            }
        }
        return null
    }
    fun modifySubtypeWithSameId(subtypeToModify: Subtype) {
        val subtypes = this.subtypes
        for (subtype in subtypes) {
            if (subtype.id == subtypeToModify.id) {
                subtype.locale = subtypeToModify.locale
                subtype.layout = subtypeToModify.layout
            }
        }
        this.subtypes = subtypes
    }
    fun removeSubtype(subtype: Subtype) {
        val oldListRaw = prefs.keyboard.subtypes
        var newListRaw = ""
        for (s in oldListRaw.split(";")) {
            if (s != subtype.toString()) {
                newListRaw += "$s;"
            }
        }
        if (newListRaw.isNotEmpty()) {
            newListRaw = newListRaw.substring(0, newListRaw.length - 1)
        }
        prefs.keyboard.subtypes = newListRaw
        if (subtype.id == prefs.keyboard.activeSubtypeId) {
            getActiveSubtype()
        }
    }
    fun switchToNextSubtype(): Subtype? {
        val subtypes = this.subtypes
        val activeSubtype = getActiveSubtype() ?: return null
        var triggerNextSubtype = false
        var newActiveSubtype: Subtype? = null
        for (s in subtypes) {
            if (triggerNextSubtype) {
                triggerNextSubtype = false
                newActiveSubtype = s
            } else if (s == activeSubtype) {
                triggerNextSubtype = true
            }
        }
        if (triggerNextSubtype) {
            newActiveSubtype = subtypes[0]
        }
        return if (newActiveSubtype == null) {
            prefs.keyboard.activeSubtypeId = -1
            null
        } else {
            prefs.keyboard.activeSubtypeId = newActiveSubtype.id
            newActiveSubtype
        }
    }
}
