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

package dev.patrickgold.florisboard.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalConfiguration
import dev.patrickgold.florisboard.app.settings.theme.DisplayColorsAs
import dev.patrickgold.florisboard.app.settings.theme.DisplayKbdAfterDialogs
import dev.patrickgold.florisboard.ime.core.DisplayLanguageNamesIn
import dev.patrickgold.florisboard.ime.core.Subtype
import dev.patrickgold.florisboard.ime.landscapeinput.LandscapeInputUiMode
import dev.patrickgold.florisboard.ime.media.emoji.EmojiHairStyle
import dev.patrickgold.florisboard.ime.media.emoji.EmojiRecentlyUsedHelper
import dev.patrickgold.florisboard.ime.media.emoji.EmojiSkinTone
import dev.patrickgold.florisboard.ime.onehanded.OneHandedMode
import dev.patrickgold.florisboard.ime.spelling.SpellingLanguageMode
import dev.patrickgold.florisboard.ime.text.gestures.SwipeAction
import dev.patrickgold.florisboard.ime.text.key.KeyHintConfiguration
import dev.patrickgold.florisboard.ime.text.key.KeyHintMode
import dev.patrickgold.florisboard.ime.text.key.UtilityKeyAction
import dev.patrickgold.florisboard.ime.text.smartbar.CandidatesDisplayMode
import dev.patrickgold.florisboard.ime.text.smartbar.SecondaryRowPlacement
import dev.patrickgold.florisboard.ime.text.smartbar.SmartbarRowType
import dev.patrickgold.florisboard.ime.theme.ThemeMode
import dev.patrickgold.florisboard.ime.theme.extCoreTheme
import dev.patrickgold.florisboard.lib.android.isOrientationPortrait
import dev.patrickgold.florisboard.lib.ext.ExtensionComponentName
import dev.patrickgold.florisboard.lib.observeAsTransformingState
import dev.patrickgold.florisboard.lib.snygg.SnyggLevel
import dev.patrickgold.florisboard.lib.util.VersionName
import dev.patrickgold.jetpref.datastore.JetPref
import dev.patrickgold.jetpref.datastore.model.PreferenceModel
import dev.patrickgold.jetpref.datastore.model.observeAsState

fun florisPreferenceModel() = JetPref.getOrCreatePreferenceModel(AppPrefs::class, ::AppPrefs)

class AppPrefs : PreferenceModel("florisboard-app-prefs") {
    val advanced = Advanced()
    inner class Advanced {
        val settingsTheme = enum(
            key = "advanced__settings_theme",
            default = AppTheme.AUTO,
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
        val historyEnabled = boolean(
            key = "clipboard__history_enabled",
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
        val clearPrimaryClipDeletesLastItem = boolean(
            key = "clipboard__clear_primary_clip_deletes_last_item",
            default = true,
        )
    }

    val correction = Correction()
    inner class Correction {
        val autoCapitalization = boolean(
            key = "correction__auto_capitalization",
            default = true,
        )
        val doubleSpacePeriod = boolean(
            key = "correction__double_space_period",
            default = true,
        )
        val rememberCapsLockState = boolean(
            key = "correction__remember_caps_lock_state",
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
        val showPrimaryClip = boolean(
            key = "devtools__show_primary_clip",
            default = false,
        )
        val showSpellingOverlay = boolean(
            key = "devtools__show_spelling_overlay",
            default = false,
        )
        val showKeyTouchBoundaries = boolean(
            key = "devtools__show_touch_boundaries",
            default = false,
        )
    }

    val dictionary = Dictionary()
    inner class Dictionary {
        val enableSystemUserDictionary = boolean(
            key = "suggestion__enable_system_user_dictionary",
            default = true,
        )
        val enableFlorisUserDictionary = boolean(
            key = "suggestion__enable_floris_user_dictionary",
            default = true,
        )
    }

    val gestures = Gestures()
    inner class Gestures {
        val swipeUp = enum(
            key = "gestures__swipe_up",
            default = SwipeAction.SHIFT,
        )
        val swipeDown = enum(
            key = "gestures__swipe_down",
            default = SwipeAction.HIDE_KEYBOARD,
        )
        val swipeLeft = enum(
            key = "gestures__swipe_left",
            default = SwipeAction.SWITCH_TO_NEXT_SUBTYPE,
        )
        val swipeRight = enum(
            key = "gestures__swipe_right",
            default = SwipeAction.SWITCH_TO_PREV_SUBTYPE,
        )
        val spaceBarSwipeUp = enum(
            key = "gestures__space_bar_swipe_up",
            default = SwipeAction.NO_ACTION,
        )
        val spaceBarSwipeLeft = enum(
            key = "gestures__space_bar_swipe_left",
            default = SwipeAction.MOVE_CURSOR_LEFT,
        )
        val spaceBarSwipeRight = enum(
            key = "gestures__space_bar_swipe_right",
            default = SwipeAction.MOVE_CURSOR_RIGHT,
        )
        val spaceBarLongPress = enum(
            key = "gestures__space_bar_long_press",
            default = SwipeAction.SHOW_INPUT_METHOD_PICKER,
        )
        val deleteKeySwipeLeft = enum(
            key = "gestures__delete_key_swipe_left",
            default = SwipeAction.DELETE_CHARACTERS_PRECISELY,
        )
        val swipeDistanceThreshold = int(
            key = "gestures__swipe_distance_threshold",
            default = 32,
        )
        val swipeVelocityThreshold = int(
            key = "gestures__swipe_velocity_threshold",
            default = 1900,
        )
    }

    val glide = Glide()
    inner class Glide {
        val enabled = boolean(
            key = "glide__enabled",
            default = false,
        )
        val showTrail = boolean(
            key = "glide__show_trail",
            default = true,
        )
        val trailDuration = int(
            key = "glide__trail_fade_duration",
            default = 200,
        )
        val showPreview = boolean(
            key = "glide__show_preview",
            default = true,
        )
        val previewRefreshDelay = int(
            key = "glide__preview_refresh_delay",
            default = 150,
        )
    }

    val inputFeedback = InputFeedback()
    inner class InputFeedback {
        val audioEnabled = boolean(
            key = "input_feedback__audio_enabled",
            default = true,
        )
        val audioIgnoreSystemSettings = boolean(
            key = "input_feedback__audio_ignore_system_settings",
            default = false,
        )
        val audioVolume = int(
            key = "input_feedback__audio_volume",
            default = 50,
        )
        val audioFeatKeyPress = boolean(
            key = "input_feedback__audio_feat_key_press",
            default = true,
        )
        val audioFeatKeyLongPress = boolean(
            key = "input_feedback__audio_feat_key_long_press",
            default = false,
        )
        val audioFeatKeyRepeatedAction = boolean(
            key = "input_feedback__audio_feat_key_repeated_action",
            default = false,
        )
        val audioFeatGestureSwipe = boolean(
            key = "input_feedback__audio_feat_gesture_swipe",
            default = false,
        )
        val audioFeatGestureMovingSwipe = boolean(
            key = "input_feedback__audio_feat_gesture_moving_swipe",
            default = false,
        )

        val hapticEnabled = boolean(
            key = "input_feedback__haptic_enabled",
            default = true,
        )
        val hapticIgnoreSystemSettings = boolean(
            key = "input_feedback__haptic_ignore_system_settings",
            default = false,
        )
        val hapticUseVibrator = boolean(
            key = "input_feedback__haptic_use_vibrator",
            default = true,
        )
        val hapticVibrationDuration = int(
            key = "input_feedback__haptic_vibration_duration",
            default = 50,
        )
        val hapticVibrationStrength = int(
            key = "input_feedback__haptic_vibration_strength",
            default = 50,
        )
        val hapticFeatKeyPress = boolean(
            key = "input_feedback__haptic_feat_key_press",
            default = true,
        )
        val hapticFeatKeyLongPress = boolean(
            key = "input_feedback__haptic_feat_key_long_press",
            default = false,
        )
        val hapticFeatKeyRepeatedAction = boolean(
            key = "input_feedback__haptic_feat_key_repeated_action",
            default = true,
        )
        val hapticFeatGestureSwipe = boolean(
            key = "input_feedback__haptic_feat_gesture_swipe",
            default = false,
        )
        val hapticFeatGestureMovingSwipe = boolean(
            key = "input_feedback__haptic_feat_gesture_moving_swipe",
            default = true,
        )
    }

    val internal = Internal()
    inner class Internal {
        val homeIsBetaToolboxCollapsed = boolean(
            key = "internal__home_is_beta_toolbox_collapsed_0314release",
            default = false,
        )
        val isImeSetUp = boolean(
            key = "internal__is_ime_set_up",
            default = false,
        )
        val versionOnInstall = string(
            key = "internal__version_on_install",
            default = VersionName.DEFAULT_RAW,
        )
        val versionLastUse = string(
            key = "internal__version_last_use",
            default = VersionName.DEFAULT_RAW,
        )
        val versionLastChangelog = string(
            key = "internal__version_last_changelog",
            default = VersionName.DEFAULT_RAW,
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
        val hintedNumberRowMode = enum(
            key = "keyboard__hinted_number_row_mode",
            default = KeyHintMode.SMART_PRIORITY,
        )
        val hintedSymbolsEnabled = boolean(
            key = "keyboard__hinted_symbols_enabled",
            default = true,
        )
        val hintedSymbolsMode = enum(
            key = "keyboard__hinted_symbols_mode",
            default = KeyHintMode.SMART_PRIORITY,
        )
        val utilityKeyEnabled = boolean(
            key = "keyboard__utility_key_enabled",
            default = true,
        )
        val utilityKeyAction = enum(
            key = "keyboard__utility_key_action",
            default = UtilityKeyAction.DYNAMIC_SWITCH_LANGUAGE_EMOJIS,
        )
        val spaceBarLanguageDisplayEnabled = boolean(
            key = "keyboard__space_bar_language_display_enabled",
            default = true,
        )
        val fontSizeMultiplierPortrait = int(
            key = "keyboard__font_size_multiplier_portrait",
            default = 100,
        )
        val fontSizeMultiplierLandscape = int(
            key = "keyboard__font_size_multiplier_landscape",
            default = 100,
        )
        val oneHandedMode = enum(
            key = "keyboard__one_handed_mode",
            default = OneHandedMode.OFF,
        )
        val oneHandedModeScaleFactor = int(
            key = "keyboard__one_handed_mode_scale_factor",
            default = 87,
        )
        val landscapeInputUiMode = enum(
            key = "keyboard__landscape_input_ui_mode",
            default = LandscapeInputUiMode.DYNAMICALLY_SHOW,
        )
        val heightFactorPortrait = int(
            key = "keyboard__height_factor_portrait",
            default = 100,
        )
        val heightFactorLandscape = int(
            key = "keyboard__height_factor_landscape",
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

        fun keyHintConfiguration(): KeyHintConfiguration {
            return KeyHintConfiguration(
                numberHintMode = when {
                    hintedNumberRowEnabled.get() -> hintedNumberRowMode.get()
                    else -> KeyHintMode.DISABLED
                },
                symbolHintMode = when {
                    hintedSymbolsEnabled.get() -> hintedSymbolsMode.get()
                    else -> KeyHintMode.DISABLED
                },
                mergeHintPopups = mergeHintPopupsEnabled.get(),
            )
        }

        @Composable
        fun fontSizeMultiplier(): Float {
            val configuration = LocalConfiguration.current
            val oneHandedMode by oneHandedMode.observeAsState()
            val oneHandedModeFactor by oneHandedModeScaleFactor.observeAsTransformingState { it / 100.0f }
            val fontSizeMultiplierBase by if (configuration.isOrientationPortrait()) {
                fontSizeMultiplierPortrait
            } else {
                fontSizeMultiplierLandscape
            }.observeAsTransformingState { it / 100.0f }
            val fontSizeMultiplier = fontSizeMultiplierBase * if (oneHandedMode != OneHandedMode.OFF && configuration.isOrientationPortrait()) {
                oneHandedModeFactor
            } else {
                1.0f
            }
            return fontSizeMultiplier
        }
    }

    val localization = Localization()
    inner class Localization {
        val displayLanguageNamesIn = enum(
            key = "localization__display_language_names_in",
            default = DisplayLanguageNamesIn.NATIVE_LOCALE,
        )
        val activeSubtypeId = long(
            key = "localization__active_subtype_id",
            default = Subtype.DEFAULT.id,
        )
        val subtypes = string(
            key = "localization__subtypes",
            default = "[]",
        )
    }

    val media = Media()
    inner class Media {
        val emojiRecentlyUsed = custom(
            key = "media__emoji_recently_used",
            default = emptyList(),
            serializer = EmojiRecentlyUsedHelper.Serializer,
        )
        val emojiRecentlyUsedMaxSize = int(
            key = "media__emoji_recently_used_max_size",
            default = 90,
        )
        val emojiPreferredSkinTone = enum(
            key = "media__emoji_preferred_skin_tone",
            default = EmojiSkinTone.DEFAULT,
        )
        val emojiPreferredHairStyle = enum(
            key = "media__emoji_preferred_hair_style",
            default = EmojiHairStyle.DEFAULT,
        )
    }

    val smartbar = Smartbar()
    inner class Smartbar {
        val enabled = boolean(
            key = "smartbar__enabled",
            default = true,
        )
        val flipToggles = boolean(
            key = "smartbar__flip_toggles",
            default = false,
        )
        val primaryActionsExpanded = boolean(
            key = "smartbar__primary_actions_expanded",
            default = false,
        )
        val primaryActionsRowType = enum(
            key = "smartbar__primary_actions_row_type",
            default = SmartbarRowType.QUICK_ACTIONS,
        )
        val primaryActionsAutoExpandCollapse = boolean(
            key = "smartbar__primary_actions_auto_expand_collapse",
            default = true,
        )
        val primaryActionsExpandWithAnimation = boolean(
            key = "smartbar__primary_actions_expand_with_animation",
            default = true,
        )
        val secondaryActionsEnabled = boolean(
            key = "smartbar__secondary_actions_enabled",
            default = true,
        )
        val secondaryActionsExpanded = boolean(
            key = "smartbar__secondary_actions_expanded",
            default = false,
        )
        val secondaryActionsPlacement = enum(
            key = "smartbar__secondary_actions_placement",
            default = SecondaryRowPlacement.ABOVE_PRIMARY,
        )
        val secondaryActionsRowType = enum(
            key = "smartbar__secondary_actions_row_type",
            default = SmartbarRowType.CLIPBOARD_CURSOR_TOOLS,
        )
        val quickActions = string(
            key = "smartbar__quick_actions",
            default = "[]",
        )
    }

    val spelling = Spelling()
    inner class Spelling {
        val languageMode = enum(
            key = "spelling__language_mode",
            default = SpellingLanguageMode.USE_KEYBOARD_SUBTYPES,
        )
        val useContacts = boolean(
            key = "spelling__use_contacts",
            default = true,
        )
        val useUdmEntries = boolean(
            key = "spelling__use_udm_entries",
            default = true,
        )
    }

    val suggestion = Suggestion()
    inner class Suggestion {
        val api30InlineSuggestionsEnabled = boolean(
            key = "suggestion__api30_inline_suggestions_enabled",
            default = true,
        )
        val enabled = boolean(
            key = "suggestion__enabled",
            default = false,
        )
        val displayMode = enum(
            key = "suggestion__display_mode",
            default = CandidatesDisplayMode.DYNAMIC_SCROLLABLE,
        )
        val usePrevWords = boolean(
            key = "suggestion__use_prev_words",
            default = true,
        )
        val blockPossiblyOffensive = boolean(
            key = "suggestion__block_possibly_offensive",
            default = true,
        )
        val clipboardContentEnabled = boolean(
            key = "suggestion__clipboard_content_enabled",
            default = true,
        )
        val clipboardContentTimeout = int(
            key = "suggestion__clipboard_content_timeout",
            default = 30,
        )
    }

    val theme = Theme()
    inner class Theme {
        val mode = enum(
            key = "theme__mode",
            default = ThemeMode.FOLLOW_SYSTEM,
        )
        val dayThemeAdaptToApp = boolean(
            key = "theme__day_theme_adapt_to_app",
            default = false,
        )
        val dayThemeId = custom(
            key = "theme__day_theme_id",
            default = extCoreTheme("floris_day"),
            serializer = ExtensionComponentName.Serializer,
        )
        val nightThemeAdaptToApp = boolean(
            key = "theme__night_theme_adapt_to_app",
            default = false,
        )
        val nightThemeId = custom(
            key = "theme__night_theme_id",
            default = extCoreTheme("floris_night"),
            serializer = ExtensionComponentName.Serializer,
        )
        //val sunriseTime = localTime(
        //    key = "theme__sunrise_time",
        //    default = LocalTime.of(6, 0),
        //)
        //val sunsetTime = localTime(
        //    key = "theme__sunset_time",
        //    default = LocalTime.of(18, 0),
        //)
        val editorDisplayColorsAs = enum(
            key = "theme__editor_display_colors_as",
            default = DisplayColorsAs.HEX8,
        )
        val editorDisplayKbdAfterDialogs = enum(
            key = "theme__editor_display_kbd_after_dialogs",
            default = DisplayKbdAfterDialogs.REMEMBER,
        )
        val editorLevel = enum(
            key = "theme__editor_level",
            default = SnyggLevel.ADVANCED,
        )
    }
}
