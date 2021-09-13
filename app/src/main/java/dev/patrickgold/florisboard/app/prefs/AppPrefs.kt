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

package dev.patrickgold.florisboard.app.prefs

import dev.patrickgold.florisboard.app.AppTheme
import dev.patrickgold.jetpref.datastore.model.PreferenceModel
import dev.patrickgold.jetpref.datastore.model.PreferenceSerializer
import dev.patrickgold.jetpref.datastore.preferenceModel

fun florisPreferenceModel() = preferenceModel(AppPrefs::class, ::AppPrefs)

class AppPrefs : PreferenceModel("florisboard-app-prefs") {
    val advanced = Advanced()
    inner class Advanced {
        val settingsTheme = custom(
            key = "advanced__settings_theme",
            default = AppTheme.AUTO,
            serializer = object : PreferenceSerializer<AppTheme> {
                override fun serialize(value: AppTheme) = value.id
                override fun deserialize(value: String) = AppTheme.values().find { it.id == value }
            }
        )
        val settingsLanguage = string(
            key = "advanced__settings_language",
            default = "auto",
        )
        val showAppIcon = boolean(
            key = "advanced__show_app_icon",
            default = true,
        )
        val forcePrivateMode = boolean(
            key = "advanced__force_private_mode",
            default = false,
        )
    }

    val devtools = Devtools()
    inner class Devtools {
        val enabled = boolean(
            key = "devtools__enabled",
            default = false,
        )
        val showHeapMemoryStats = boolean(
            key = "devtools__show_heap_memory_stats",
            default = false,
        )
        val overrideWordSuggestionsMinHeapRestriction = boolean(
            key = "devtools__override_word_suggestions_min_heap_restriction",
            default = false,
        )
    }

    val internal = Internal()
    inner class Internal {
        val homeIsBetaToolboxCollapsed = boolean(
            key = "internal__home_is_beta_toolbox_collapsed",
            default = false,
        )
    }
}
