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
import dev.patrickgold.florisboard.ime.landscapeinput.LandscapeInputUiMode
import dev.patrickgold.florisboard.ime.onehanded.OneHandedMode
import dev.patrickgold.florisboard.ime.text.key.KeyHintMode
import dev.patrickgold.florisboard.ime.text.key.UtilityKeyAction
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

    val clipboard = Clipboard()
    inner class Clipboard {
        val useInternalClipboard = boolean(
            key = "clipboard__use_internal_clipboard",
            default = false,
        )
        val syncToFloris = boolean(
            key = "clipboard__sync_to_floris",
            default = true,
        )
        val syncToSystem = boolean(
            key = "clipboard__sync_to_system",
            default = false,
        )
        val enableHistory = boolean(
            key = "clipboard__enable_history",
            default = false,
        )
        val cleanUpOld = boolean(
            key = "clipboard__clean_up_old",
            default = false,
        )
        val cleanUpAfter = int(
            key = "clipboard__clean_up_after",
            default = 20,
        )
        val limitHistorySize = boolean(
            key = "clipboard__limit_history_size",
            default = true,
        )
        val maxHistorySize = int(
            key = "clipboard__max_history_size",
            default = 20,
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

    val keyboard = Keyboard()
    inner class Keyboard {
        val numberRow = boolean(
            key = "keyboard__number_row",
            default = false,
        )
        val hintedNumberRowEnabled = boolean(
            key = "keyboard__hinted_number_row_enabled",
            default = true,
        )
        val hintedNumberRowMode = custom(
            key = "keyboard__hinted_number_row_mode",
            default = KeyHintMode.SMART_PRIORITY,
            serializer = KeyHintMode.Serializer,
        )
        val hintedSymbolsEnabled = boolean(
            key = "keyboard__hinted_symbols_enabled",
            default = true,
        )
        val hintedSymbolsMode = custom(
            key = "keyboard__hinted_symbols_mode",
            default = KeyHintMode.SMART_PRIORITY,
            serializer = KeyHintMode.Serializer,
        )
        val utilityKeyEnabled = boolean(
            key = "keyboard__utility_key_enabled",
            default = true,
        )
        val utilityKeyAction = custom(
            key = "keyboard__utility_key_action",
            default = UtilityKeyAction.DYNAMIC_SWITCH_LANGUAGE_EMOJIS,
            serializer = UtilityKeyAction.Serializer,
        )
        val fontSizeMultiplierPortrait = int(
            key = "keyboard__font_size_multiplier_portrait",
            default = 100,
        )
        val fontSizeMultiplierLandscape = int(
            key = "keyboard__font_size_multiplier_landscape",
            default = 100,
        )
        val oneHandedMode = string(
            key = "keyboard__one_handed_mode",
            default = OneHandedMode.OFF,
        )
        val oneHandedModeScaleFactor = int(
            key = "keyboard__one_handed_mode_scale_factor",
            default = 87,
        )
        val landscapeInputUiMode = custom(
            key = "keyboard__landscape_input_ui_mode",
            default = LandscapeInputUiMode.DYNAMICALLY_SHOW,
            serializer = LandscapeInputUiMode.Serializer,
        )
        val heightFactor = int(
            key = "keyboard__height_factor",
            default = 100,
        )
        val keySpacingVertical = float(
            key = "keyboard__key_spacing_vertical",
            default = 5.0f,
        )
        val keySpacingHorizontal = float(
            key = "keyboard__key_spacing_horizontal",
            default = 2.0f,
        )
        val bottomOffsetPortrait = int(
            key = "keyboard__bottom_offset_portrait",
            default = 0,
        )
        val bottomOffsetLandscape = int(
            key = "keyboard__bottom_offset_landscape",
            default = 0,
        )
        val popupEnabled = boolean(
            key = "keyboard__popup_enabled",
            default = true,
        )
        val mergeHintPopupsEnabled = boolean(
            key = "keyboard__merge_hint_popups_enabled",
            default = false,
        )
        val longPressDelay = int(
            key = "keyboard__long_press_delay",
            default = 300,
        )
        val spaceBarSwitchesToCharacters = boolean(
            key = "keyboard__space_bar_switches_to_characters",
            default = true,
        )
    }
}
