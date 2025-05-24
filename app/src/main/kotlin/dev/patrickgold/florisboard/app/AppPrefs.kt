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

package dev.patrickgold.florisboard.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import dev.patrickgold.florisboard.app.settings.theme.DisplayKbdAfterDialogs
import dev.patrickgold.florisboard.app.settings.theme.SnyggLevel
import dev.patrickgold.florisboard.app.setup.NotificationPermissionState
import dev.patrickgold.florisboard.ime.clipboard.CLIPBOARD_HISTORY_NUM_GRID_COLUMNS_AUTO
import dev.patrickgold.florisboard.ime.core.DisplayLanguageNamesIn
import dev.patrickgold.florisboard.ime.core.Subtype
import dev.patrickgold.florisboard.ime.input.CapitalizationBehavior
import dev.patrickgold.florisboard.ime.input.HapticVibrationMode
import dev.patrickgold.florisboard.ime.input.InputFeedbackActivationMode
import dev.patrickgold.florisboard.ime.keyboard.IncognitoMode
import dev.patrickgold.florisboard.ime.keyboard.SpaceBarMode
import dev.patrickgold.florisboard.ime.landscapeinput.LandscapeInputUiMode
import dev.patrickgold.florisboard.ime.media.emoji.EmojiHairStyle
import dev.patrickgold.florisboard.ime.media.emoji.EmojiHistory
import dev.patrickgold.florisboard.ime.media.emoji.EmojiSkinTone
import dev.patrickgold.florisboard.ime.media.emoji.EmojiSuggestionType
import dev.patrickgold.florisboard.ime.nlp.SpellingLanguageMode
import dev.patrickgold.florisboard.ime.onehanded.OneHandedMode
import dev.patrickgold.florisboard.ime.smartbar.CandidatesDisplayMode
import dev.patrickgold.florisboard.ime.smartbar.ExtendedActionsPlacement
import dev.patrickgold.florisboard.ime.smartbar.IncognitoDisplayMode
import dev.patrickgold.florisboard.ime.smartbar.SmartbarLayout
import dev.patrickgold.florisboard.ime.smartbar.quickaction.QuickAction
import dev.patrickgold.florisboard.ime.smartbar.quickaction.QuickActionArrangement
import dev.patrickgold.florisboard.ime.smartbar.quickaction.QuickActionJsonConfig
import dev.patrickgold.florisboard.ime.text.gestures.SwipeAction
import dev.patrickgold.florisboard.ime.text.key.KeyCode
import dev.patrickgold.florisboard.ime.text.key.KeyHintConfiguration
import dev.patrickgold.florisboard.ime.text.key.KeyHintMode
import dev.patrickgold.florisboard.ime.text.key.UtilityKeyAction
import dev.patrickgold.florisboard.ime.text.keyboard.TextKeyData
import dev.patrickgold.florisboard.ime.theme.ThemeMode
import dev.patrickgold.florisboard.ime.theme.extCoreTheme
import dev.patrickgold.florisboard.lib.compose.ColorPreferenceSerializer
import dev.patrickgold.florisboard.lib.ext.ExtensionComponentName
import dev.patrickgold.florisboard.lib.observeAsTransformingState
import dev.patrickgold.florisboard.lib.util.VersionName
import dev.patrickgold.jetpref.datastore.JetPref
import dev.patrickgold.jetpref.datastore.model.PreferenceData
import dev.patrickgold.jetpref.datastore.model.PreferenceMigrationEntry
import dev.patrickgold.jetpref.datastore.model.PreferenceModel
import dev.patrickgold.jetpref.datastore.model.observeAsState
import dev.patrickgold.jetpref.material.ui.ColorRepresentation
import kotlinx.serialization.json.Json
import org.florisboard.lib.android.AndroidVersion
import org.florisboard.lib.android.isOrientationPortrait
import org.florisboard.lib.color.DEFAULT_GREEN

fun florisPreferenceModel() = JetPref.getOrCreatePreferenceModel(AppPrefs::class, ::AppPrefs)

class AppPrefs : PreferenceModel("florisboard-app-prefs") {
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
        val numHistoryGridColumnsPortrait = int(
            key = "clipboard__num_history_grid_columns_portrait",
            default = CLIPBOARD_HISTORY_NUM_GRID_COLUMNS_AUTO,
        )
        val numHistoryGridColumnsLandscape = int(
            key = "clipboard__num_history_grid_columns_landscape",
            default = CLIPBOARD_HISTORY_NUM_GRID_COLUMNS_AUTO,
        )
        @Composable
        fun numHistoryGridColumns(): PreferenceData<Int> {
            val configuration = LocalConfiguration.current
            return if (configuration.isOrientationPortrait()) {
                numHistoryGridColumnsPortrait
            } else {
                numHistoryGridColumnsLandscape
            }
        }
        val cleanUpOld = boolean(
            key = "clipboard__clean_up_old",
            default = false,
        )
        val cleanUpAfter = int(
            key = "clipboard__clean_up_after",
            default = 20,
        )
        val autoCleanSensitive = boolean(
            key = "clipboard__auto_clean_sensitive",
            default = false,
        )
        val autoCleanSensitiveAfter = int(
            key = "clipboard__auto_clean_sensitive_after",
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
        val suggestionEnabled = boolean(
            key = "clipboard__suggestion_enabled",
            default = true,
        )
        val suggestionTimeout = int(
            key = "clipboard__suggestion_timeout",
            default = 60,
        )
    }

    val correction = Correction()
    inner class Correction {
        val autoCapitalization = boolean(
            key = "correction__auto_capitalization",
            default = true,
        )
        val autoSpacePunctuation = boolean(
            key = "correction__auto_space_punctuation",
            default = false,
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
        val showPrimaryClip = boolean(
            key = "devtools__show_primary_clip",
            default = false,
        )
        val showInputStateOverlay = boolean(
            key = "devtools__show_input_state_overlay",
            default = false,
        )
        val showSpellingOverlay = boolean(
            key = "devtools__show_spelling_overlay",
            default = false,
        )
        val showInlineAutofillOverlay = boolean(
            key = "devtools__show_inline_autofill_overlay",
            default = false,
        )
        val showKeyTouchBoundaries = boolean(
            key = "devtools__show_touch_boundaries",
            default = false,
        )
        val showDragAndDropHelpers = boolean(
            key = "devtools__show_drag_and_drop_helpers",
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

    val emoji = Emoji()
    inner class Emoji {
        val preferredSkinTone = enum(
            key = "emoji__preferred_skin_tone",
            default = EmojiSkinTone.DEFAULT,
        )
        val preferredHairStyle = enum(
            key = "emoji__preferred_hair_style",
            default = EmojiHairStyle.DEFAULT,
        )
        val historyEnabled = boolean(
            key = "emoji__history_enabled",
            default = true,
        )
        val historyData = custom(
            key = "emoji__history_data",
            default = EmojiHistory.Empty,
            serializer = EmojiHistory.Serializer,
        )
        val historyPinnedUpdateStrategy = enum(
            key = "emoji__history_pinned_update_strategy",
            default = EmojiHistory.UpdateStrategy.MANUAL_SORT_PREPEND,
        )
        val historyPinnedMaxSize = int(
            key = "emoji__history_pinned_max_size",
            default = EmojiHistory.MaxSizeUnlimited,
        )
        val historyRecentUpdateStrategy = enum(
            key = "emoji__history_recent_update_strategy",
            default = EmojiHistory.UpdateStrategy.AUTO_SORT_PREPEND,
        )
        val historyRecentMaxSize = int(
            key = "emoji__history_recent_max_size",
            default = 90,
        )
        val suggestionEnabled = boolean(
            key = "emoji__suggestion_enabled",
            default = true,
        )
        val suggestionType = enum(
            key = "emoji__suggestion_type",
            default = EmojiSuggestionType.LEADING_COLON,
        )
        val suggestionUpdateHistory = boolean(
            key = "emoji__suggestion_update_history",
            default = true,
        )
        val suggestionCandidateShowName = boolean(
            key = "emoji__suggestion_candidate_show_name",
            default = false,
        )
        val suggestionQueryMinLength = int(
            key = "emoji__suggestion_query_min_length",
            default = 3,
        )
        val suggestionCandidateMaxCount = int(
            key = "emoji__suggestion_candidate_max_count",
            default = 5,
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
        val deleteKeyLongPress = enum(
            key = "gestures__delete_key_long_press",
            default = SwipeAction.DELETE_CHARACTER,
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
        val immediateBackspaceDeletesWord = boolean(
            key = "glide__immediate_backspace_deletes_word",
            default = true,
        )
    }

    val inputFeedback = InputFeedback()
    inner class InputFeedback {
        val audioEnabled = boolean(
            key = "input_feedback__audio_enabled",
            default = true,
        )
        val audioActivationMode = enum(
            key = "input_feedback__audio_activation_mode",
            default = InputFeedbackActivationMode.RESPECT_SYSTEM_SETTINGS,
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
        val hapticActivationMode = enum(
            key = "input_feedback__haptic_activation_mode",
            default = InputFeedbackActivationMode.RESPECT_SYSTEM_SETTINGS,
        )
        val hapticVibrationMode = enum(
            key = "input_feedback__haptic_vibration_mode",
            default = HapticVibrationMode.USE_VIBRATOR_DIRECTLY,
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
            key = "internal__home_is_beta_toolbox_collapsed_040a01",
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
        val notificationPermissionState = enum(
            key = "internal__notification_permission_state",
            default = NotificationPermissionState.NOT_SET,
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
        val spaceBarMode = enum(
            key = "keyboard__space_bar_display_mode",
            default = SpaceBarMode.CURRENT_LANGUAGE,
        )
        val capitalizationBehavior = enum(
            key = "keyboard__capitalization_behavior",
            default = CapitalizationBehavior.CAPSLOCK_BY_DOUBLE_TAP,
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
            default = OneHandedMode.END,
        )
        val oneHandedModeEnabled = boolean(
            key = "keyboard__one_handed_mode_enabled",
            default = false,
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
        val incognitoDisplayMode = enum(
            key = "keyboard__incognito_indicator",
            default = IncognitoDisplayMode.DISPLAY_BEHIND_KEYBOARD,
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
            val oneHandedModeEnabled by oneHandedModeEnabled.observeAsState()
            val oneHandedModeFactor by oneHandedModeScaleFactor.observeAsTransformingState { it / 100.0f }
            val fontSizeMultiplierBase by if (configuration.isOrientationPortrait()) {
                fontSizeMultiplierPortrait
            } else {
                fontSizeMultiplierLandscape
            }.observeAsTransformingState { it / 100.0f }
            val fontSizeMultiplier = fontSizeMultiplierBase * if (oneHandedModeEnabled && configuration.isOrientationPortrait()) {
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
            default = DisplayLanguageNamesIn.SYSTEM_LOCALE,
        )
        val displayKeyboardLabelsInSubtypeLanguage = boolean(
            key = "localization__display_keyboard_labels_in_subtype_language",
            default = false,
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

    val other = Other()
    inner class Other {
        val settingsTheme = enum(
            key = "other__settings_theme",
            default = AppTheme.AUTO,
        )
        val accentColor = custom(
            key = "other__accent_color",
            default = when (AndroidVersion.ATLEAST_API31_S) {
                true -> Color.Unspecified
                false -> DEFAULT_GREEN
            },
            serializer = ColorPreferenceSerializer,
        )
        val settingsLanguage = string(
            key = "other__settings_language",
            default = "auto",
        )
        val showAppIcon = boolean(
            key = "other__show_app_icon",
            default = true,
        )
    }

    val smartbar = Smartbar()
    inner class Smartbar {
        val enabled = boolean(
            key = "smartbar__enabled",
            default = true,
        )
        val layout = enum(
            key = "smartbar__layout",
            default = SmartbarLayout.SUGGESTIONS_ACTIONS_SHARED,
        )
        val actionArrangement = custom(
            key = "smartbar__action_arrangement",
            default = QuickActionArrangement.Default,
            serializer = QuickActionArrangement.Serializer,
        )
        val flipToggles = boolean(
            key = "smartbar__flip_toggles",
            default = false,
        )
        val sharedActionsExpanded = boolean(
            key = "smartbar__shared_actions_expanded",
            default = false,
        )
        @Deprecated("Always enabled due to UX issues")
        val sharedActionsAutoExpandCollapse = boolean(
            key = "smartbar__shared_actions_auto_expand_collapse",
            default = true,
        )
        val sharedActionsExpandWithAnimation = boolean(
            key = "smartbar__shared_actions_expand_with_animation",
            default = true,
        )
        val extendedActionsExpanded = boolean(
            key = "smartbar__extended_actions_expanded",
            default = false,
        )
        val extendedActionsPlacement = enum(
            key = "smartbar__extended_actions_placement",
            default = ExtendedActionsPlacement.ABOVE_CANDIDATES,
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
        val blockPossiblyOffensive = boolean(
            key = "suggestion__block_possibly_offensive",
            default = true,
        )
        val incognitoMode = enum(
            key = "suggestion__incognito_mode",
            default = IncognitoMode.DYNAMIC_ON_OFF,
        )
        // Internal pref
        val forceIncognitoModeFromDynamic = boolean(
            key = "suggestion__force_incognito_mode_from_dynamic",
            default = false,
        )
    }

    val theme = Theme()
    inner class Theme {
        val mode = enum(
            key = "theme__mode",
            default = ThemeMode.FOLLOW_SYSTEM,
        )
        val dayThemeId = custom(
            key = "theme__day_theme_id",
            default = extCoreTheme("floris_day"),
            serializer = ExtensionComponentName.Serializer,
        )
        val nightThemeId = custom(
            key = "theme__night_theme_id",
            default = extCoreTheme("floris_night"),
            serializer = ExtensionComponentName.Serializer,
        )
        val accentColor = custom(
            key = "theme__accent_color",
            default = when (AndroidVersion.ATLEAST_API31_S) {
                true -> Color.Unspecified
                false -> DEFAULT_GREEN
            },
            serializer = ColorPreferenceSerializer,
        )
        //val sunriseTime = localTime(
        //    key = "theme__sunrise_time",
        //    default = LocalTime.of(6, 0),
        //)
        //val sunsetTime = localTime(
        //    key = "theme__sunset_time",
        //    default = LocalTime.of(18, 0),
        //)
        val editorColorRepresentation = enum(
            key = "theme__editor_color_representation",
            default = ColorRepresentation.HEX,
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

    override fun migrate(entry: PreferenceMigrationEntry): PreferenceMigrationEntry {
        return when (entry.key) {

            // Migrate media prefs to emoji prefs
            // Keep migration rule until: 0.6 dev cycle
            "media__emoji_recently_used" -> {
                val emojiValues = entry.rawValue.split(";")
                val recent = emojiValues.map {
                    dev.patrickgold.florisboard.ime.media.emoji.Emoji(it, "", emptyList())
                }
                val data = EmojiHistory(emptyList(), recent)
                entry.transform(key = "emoji__history_data", rawValue = Json.encodeToString(data))
            }
            "media__emoji_recently_used_max_size" -> {
                entry.transform(key = "emoji__history_recent_max_size")
            }

            // Migrate advanced prefs to other prefs
            // Keep migration rules until: 0.7 dev cycle
            "advanced__settings_theme" -> {
                entry.transform(key = "other__settings_theme")
            }
            "advanced__accent_color" -> {
                entry.transform(key = "other__accent_color")
            }
            "advanced__settings_language" -> {
                entry.transform(key = "other__settings_language")
            }
            "advanced__show_app_icon" -> {
                entry.transform(key = "other__show_app_icon")
            }
            "advanced__incognito_mode" -> {
                entry.transform(key = "suggestion__incognito_mode")
            }
            "advanced__force_incognito_mode_from_dynamic" -> {
                entry.transform(key = "suggestion__force_incognito_mode_from_dynamic")
            }
            // Migrate clipboard suggestion prefs to clipboard
            // Keep migration rules until: 0.7 dev cycle
            "suggestion__clipboard_content_enabled" -> {
                entry.transform(key = "clipboard__suggestion_enabled")
            }
            "suggestion__clipboard_content_timeout" -> {
                entry.transform(key = "clipboard__suggestion_timeout")
            }

            //Migrate one hand mode prefs keep until: 0.7 dev cycle
            "keyboard__one_handed_mode" -> {
                if (entry.rawValue != "OFF") {
                    val prefs by florisPreferenceModel()
                    prefs.keyboard.oneHandedModeEnabled.set(true)
                    entry.keepAsIs()
                } else {
                    entry.reset()
                }
            }
            "smartbar__action_arrangement" -> {
                fun migrateAction(action: QuickAction): QuickAction {
                    return if (action is QuickAction.InsertKey && action.data.code == KeyCode.COMPACT_LAYOUT_TO_RIGHT) {
                        action.copy(TextKeyData.TOGGLE_COMPACT_LAYOUT)
                    } else {
                        action
                    }
                }

                val arrangement = QuickActionJsonConfig.decodeFromString<QuickActionArrangement>(entry.rawValue)
                var newArrangement = arrangement.copy(
                    stickyAction = arrangement.stickyAction?.let{ migrateAction(it) },
                    dynamicActions = arrangement.dynamicActions.map { migrateAction(it) },
                    hiddenActions = arrangement.hiddenActions.map { migrateAction(it) },
                )
                if (QuickAction.InsertKey(TextKeyData.LANGUAGE_SWITCH) !in newArrangement) {
                    newArrangement = newArrangement.copy(
                        dynamicActions = newArrangement.dynamicActions.plus(QuickAction.InsertKey(TextKeyData.LANGUAGE_SWITCH))
                    )
                }
                val json = QuickActionJsonConfig.encodeToString(newArrangement.distinct())
                entry.transform(rawValue = json)
            }

            // Migrate theme editor fine-tuning
            // Keep migration rule until: 0.6 dev cycle
            "theme__editor_display_colors_as" -> {
                val colorRepresentation = when (entry.rawValue) {
                    "RGBA" -> ColorRepresentation.RGB
                    else -> ColorRepresentation.HEX
                }
                entry.transform(
                    key = "theme__editor_color_representation",
                    rawValue = colorRepresentation.name,
                )
            }


            // Default: keep entry
            else -> entry.keepAsIs()
        }
    }
}
