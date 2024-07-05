package dev.patrickgold.florisboard.app

import androidx.compose.runtime.Composable
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.app.settings.theme.DisplayColorsAs
import dev.patrickgold.florisboard.app.settings.theme.DisplayKbdAfterDialogs
import dev.patrickgold.florisboard.ime.core.DisplayLanguageNamesIn
import dev.patrickgold.florisboard.ime.input.CapitalizationBehavior
import dev.patrickgold.florisboard.ime.input.HapticVibrationMode
import dev.patrickgold.florisboard.ime.input.InputFeedbackActivationMode
import dev.patrickgold.florisboard.ime.keyboard.IncognitoMode
import dev.patrickgold.florisboard.ime.keyboard.SpaceBarMode
import dev.patrickgold.florisboard.ime.landscapeinput.LandscapeInputUiMode
import dev.patrickgold.florisboard.ime.media.emoji.EmojiSkinTone
import dev.patrickgold.florisboard.ime.nlp.SpellingLanguageMode
import dev.patrickgold.florisboard.ime.onehanded.OneHandedMode
import dev.patrickgold.florisboard.ime.smartbar.CandidatesDisplayMode
import dev.patrickgold.florisboard.ime.smartbar.ExtendedActionsPlacement
import dev.patrickgold.florisboard.ime.smartbar.IncognitoDisplayMode
import dev.patrickgold.florisboard.ime.smartbar.SmartbarLayout
import dev.patrickgold.florisboard.ime.text.gestures.SwipeAction
import dev.patrickgold.florisboard.ime.text.key.KeyHintMode
import dev.patrickgold.florisboard.ime.text.key.UtilityKeyAction
import dev.patrickgold.florisboard.ime.theme.ThemeMode
import dev.patrickgold.florisboard.lib.compose.stringRes
import org.florisboard.lib.snygg.SnyggLevel
import dev.patrickgold.jetpref.datastore.ui.ListPreferenceEntry
import dev.patrickgold.jetpref.datastore.ui.listPrefEntries
import org.florisboard.lib.kotlin.curlyFormat
import kotlin.reflect.KClass

private const val DEFAULT = ""

private val ENUM_DISPLAY_ENTRIES = mapOf<Pair<KClass<*>, String>, @Composable () -> List<ListPreferenceEntry<*>>>(
    AppTheme::class to DEFAULT to {
        listPrefEntries {
            entry(
                key = AppTheme.AUTO,
                label = stringRes(R.string.settings__system_default),
            )
            entry(
                key = AppTheme.AUTO_AMOLED,
                label = stringRes(R.string.pref__advanced__settings_theme__auto_amoled),
            )
            entry(
                key = AppTheme.LIGHT,
                label = stringRes(R.string.pref__advanced__settings_theme__light),
            )
            entry(
                key = AppTheme.DARK,
                label = stringRes(R.string.pref__advanced__settings_theme__dark),
            )
            entry(
                key = AppTheme.AMOLED_DARK,
                label = stringRes(R.string.pref__advanced__settings_theme__amoled_dark),
            )
        }
    },
    CandidatesDisplayMode::class to DEFAULT to {
        listPrefEntries {
            entry(
                key = CandidatesDisplayMode.CLASSIC,
                label = stringRes(R.string.enum__candidates_display_mode__classic),
            )
            entry(
                key = CandidatesDisplayMode.DYNAMIC,
                label = stringRes(R.string.enum__candidates_display_mode__dynamic),
            )
            entry(
                key = CandidatesDisplayMode.DYNAMIC_SCROLLABLE,
                label = stringRes(R.string.enum__candidates_display_mode__dynamic_scrollable),
            )
        }
    },
    CapitalizationBehavior::class to DEFAULT to {
        listPrefEntries {
            entry(
                key = CapitalizationBehavior.CAPSLOCK_BY_DOUBLE_TAP,
                label = stringRes(R.string.enum__capitalization_behavior__capslock_by_double_tap),
            )
            entry(
                key = CapitalizationBehavior.CAPSLOCK_BY_CYCLE,
                label = stringRes(R.string.enum__capitalization_behavior__capslock_by_cycle),
            )
        }
    },
    DisplayColorsAs::class to DEFAULT to {
        listPrefEntries {
            entry(
                key = DisplayColorsAs.HEX8,
                label = stringRes(R.string.enum__display_colors_as__hex8),
                description = stringRes(R.string.general__example_given).curlyFormat("example" to "#4caf50ff"),
                showDescriptionOnlyIfSelected = true,
            )
            entry(
                key = DisplayColorsAs.RGBA,
                label = stringRes(R.string.enum__display_colors_as__rgba),
                description = stringRes(R.string.general__example_given).curlyFormat("example" to "rgba(76,175,80,1.0)"),
                showDescriptionOnlyIfSelected = true,
            )
        }
    },
    DisplayKbdAfterDialogs::class to DEFAULT to {
        listPrefEntries {
            entry(
                key = DisplayKbdAfterDialogs.ALWAYS,
                label = stringRes(R.string.enum__display_kbd_after_dialogs__always),
                description = stringRes(R.string.enum__display_kbd_after_dialogs__always__description),
                showDescriptionOnlyIfSelected = true,
            )
            entry(
                key = DisplayKbdAfterDialogs.NEVER,
                label = stringRes(R.string.enum__display_kbd_after_dialogs__never),
                description = stringRes(R.string.enum__display_kbd_after_dialogs__never__description),
                showDescriptionOnlyIfSelected = true,
            )
            entry(
                key = DisplayKbdAfterDialogs.REMEMBER,
                label = stringRes(R.string.enum__display_kbd_after_dialogs__remember),
                description = stringRes(R.string.enum__display_kbd_after_dialogs__remember__description),
                showDescriptionOnlyIfSelected = true,
            )
        }
    },
    DisplayLanguageNamesIn::class to DEFAULT to {
        listPrefEntries {
            entry(
                key = DisplayLanguageNamesIn.SYSTEM_LOCALE,
                label = stringRes(R.string.enum__display_language_names_in__system_locale),
                description = stringRes(R.string.enum__display_language_names_in__system_locale__description),
                showDescriptionOnlyIfSelected = true,
            )
            entry(
                key = DisplayLanguageNamesIn.NATIVE_LOCALE,
                label = stringRes(R.string.enum__display_language_names_in__native_locale),
                description = stringRes(R.string.enum__display_language_names_in__native_locale__description),
                showDescriptionOnlyIfSelected = true,
            )
        }
    },
    EmojiSkinTone::class to DEFAULT to {
        listPrefEntries {
            entry(
                key = EmojiSkinTone.DEFAULT,
                label = stringRes(
                    R.string.enum__emoji_skin_tone__default,
                    "emoji" to "\uD83D\uDC4B" // 👋
                ),
            )
            entry(
                key = EmojiSkinTone.LIGHT_SKIN_TONE,
                label = stringRes(
                    R.string.enum__emoji_skin_tone__light_skin_tone,
                    "emoji" to "\uD83D\uDC4B\uD83C\uDFFB" // 👋🏻
                ),
            )
            entry(
                key = EmojiSkinTone.MEDIUM_LIGHT_SKIN_TONE,
                label = stringRes(
                    R.string.enum__emoji_skin_tone__medium_light_skin_tone,
                    "emoji" to "\uD83D\uDC4B\uD83C\uDFFC" // 👋🏼
                ),
            )
            entry(
                key = EmojiSkinTone.MEDIUM_SKIN_TONE,
                label = stringRes(
                    R.string.enum__emoji_skin_tone__medium_skin_tone,
                    "emoji" to "\uD83D\uDC4B\uD83C\uDFFD" // 👋🏽
                ),
            )
            entry(
                key = EmojiSkinTone.MEDIUM_DARK_SKIN_TONE,
                label = stringRes(
                    R.string.enum__emoji_skin_tone__medium_dark_skin_tone,
                    "emoji" to "\uD83D\uDC4B\uD83C\uDFFE" // 👋🏾
                ),
            )
            entry(
                key = EmojiSkinTone.DARK_SKIN_TONE,
                label = stringRes(
                    R.string.enum__emoji_skin_tone__dark_skin_tone,
                    "emoji" to "\uD83D\uDC4B\uD83C\uDFFF" // 👋🏿
                ),
            )
        }
    },
    ExtendedActionsPlacement::class to DEFAULT to {
        listPrefEntries {
            entry(
                key = ExtendedActionsPlacement.ABOVE_CANDIDATES,
                label = stringRes(R.string.enum__extended_actions_placement__above_candidates),
                description = stringRes(R.string.enum__extended_actions_placement__above_candidates__description),
                showDescriptionOnlyIfSelected = true,
            )
            entry(
                key = ExtendedActionsPlacement.BELOW_CANDIDATES,
                label = stringRes(R.string.enum__extended_actions_placement__below_candidates),
                description = stringRes(R.string.enum__extended_actions_placement__below_candidates__description),
                showDescriptionOnlyIfSelected = true,
            )
            entry(
                key = ExtendedActionsPlacement.OVERLAY_APP_UI,
                label = stringRes(R.string.enum__extended_actions_placement__overlay_app_ui),
                description = stringRes(R.string.enum__extended_actions_placement__overlay_app_ui__description),
                showDescriptionOnlyIfSelected = true,
            )
        }
    },
    HapticVibrationMode::class to DEFAULT to {
        listPrefEntries {
            entry(
                key = HapticVibrationMode.USE_VIBRATOR_DIRECTLY,
                label = stringRes(R.string.enum__haptic_vibration_mode__use_vibrator_directly),
                description = stringRes(R.string.enum__haptic_vibration_mode__use_vibrator_directly__description),
                showDescriptionOnlyIfSelected = true,
            )
            entry(
                key = HapticVibrationMode.USE_HAPTIC_FEEDBACK_INTERFACE,
                label = stringRes(R.string.enum__haptic_vibration_mode__use_haptic_feedback_interface),
                description = stringRes(R.string.enum__haptic_vibration_mode__use_haptic_feedback_interface__description),
                showDescriptionOnlyIfSelected = true,
            )
        }
    },
    KeyHintMode::class to DEFAULT to {
        listPrefEntries {
            entry(
                key = KeyHintMode.ACCENT_PRIORITY,
                label = stringRes(R.string.enum__key_hint_mode__accent_priority),
                description = stringRes(R.string.enum__key_hint_mode__accent_priority__description),
                showDescriptionOnlyIfSelected = true,
            )
            entry(
                key = KeyHintMode.HINT_PRIORITY,
                label = stringRes(R.string.enum__key_hint_mode__hint_priority),
                description = stringRes(R.string.enum__key_hint_mode__hint_priority__description),
                showDescriptionOnlyIfSelected = true,
            )
            entry(
                key = KeyHintMode.SMART_PRIORITY,
                label = stringRes(R.string.enum__key_hint_mode__smart_priority),
                description = stringRes(R.string.enum__key_hint_mode__smart_priority__description),
                showDescriptionOnlyIfSelected = true,
            )
        }
    },
    IncognitoDisplayMode::class to DEFAULT to {
        listPrefEntries {
            entry(
                key = IncognitoDisplayMode.REPLACE_SHARED_ACTIONS_TOGGLE,
                label = stringRes(id = R.string.enum__incognito_display_mode__replace_shared_actions_toggle),
            )
            entry(
                key = IncognitoDisplayMode.DISPLAY_BEHIND_KEYBOARD,
                label = stringRes(id = R.string.enum__incognito_display_mode__display_behind_keyboard),
            )
        }
    },
    IncognitoMode::class to DEFAULT to {
        listPrefEntries {
            entry(
                key = IncognitoMode.FORCE_OFF,
                label = stringRes(R.string.enum__incognito_mode__force_off),
                description = stringRes(R.string.enum__incognito_mode__force_off__description),
                showDescriptionOnlyIfSelected = true,
            )
            entry(
                key = IncognitoMode.DYNAMIC_ON_OFF,
                label = stringRes(R.string.enum__incognito_mode__dynamic_on_off),
                description = stringRes(R.string.enum__incognito_mode__dynamic_on_off__description),
                showDescriptionOnlyIfSelected = true,
            )
            entry(
                key = IncognitoMode.FORCE_ON,
                label = stringRes(R.string.enum__incognito_mode__force_on),
                description = stringRes(R.string.enum__incognito_mode__force_on__description),
                showDescriptionOnlyIfSelected = true,
            )
        }
    },
    InputFeedbackActivationMode::class to "audio" to {
        listPrefEntries {
            entry(
                key = InputFeedbackActivationMode.RESPECT_SYSTEM_SETTINGS,
                label = stringRes(R.string.enum__input_feedback_activation_mode__audio_respect_system_settings),
            )
            entry(
                key = InputFeedbackActivationMode.IGNORE_SYSTEM_SETTINGS,
                label = stringRes(R.string.enum__input_feedback_activation_mode__audio_ignore_system_settings),
            )
        }
    },
    InputFeedbackActivationMode::class to "haptic" to {
        listPrefEntries {
            entry(
                key = InputFeedbackActivationMode.RESPECT_SYSTEM_SETTINGS,
                label = stringRes(R.string.enum__input_feedback_activation_mode__haptic_respect_system_settings),
            )
            entry(
                key = InputFeedbackActivationMode.IGNORE_SYSTEM_SETTINGS,
                label = stringRes(R.string.enum__input_feedback_activation_mode__haptic_ignore_system_settings),
            )
        }
    },
    LandscapeInputUiMode::class to DEFAULT to {
        listPrefEntries {
            entry(
                key = LandscapeInputUiMode.NEVER_SHOW,
                label = stringRes(R.string.enum__landscape_input_ui_mode__never_show),
            )
            entry(
                key = LandscapeInputUiMode.ALWAYS_SHOW,
                label = stringRes(R.string.enum__landscape_input_ui_mode__always_show),
            )
            entry(
                key = LandscapeInputUiMode.DYNAMICALLY_SHOW,
                label = stringRes(R.string.enum__landscape_input_ui_mode__dynamically_show),
            )
        }
    },
    OneHandedMode::class to DEFAULT to {
        listPrefEntries {
            entry(
                key = OneHandedMode.OFF,
                label = stringRes(R.string.enum__one_handed_mode__off),
            )
            entry(
                key = OneHandedMode.START,
                label = stringRes(R.string.enum__one_handed_mode__start),
            )
            entry(
                key = OneHandedMode.END,
                label = stringRes(R.string.enum__one_handed_mode__end),
            )
        }
    },
    SmartbarLayout::class to DEFAULT to {
        listPrefEntries {
            entry(
                key = SmartbarLayout.SUGGESTIONS_ONLY,
                label = stringRes(R.string.enum__smartbar_layout__suggestions_only),
                description = stringRes(R.string.enum__smartbar_layout__suggestions_only__description),
            )
            entry(
                key = SmartbarLayout.ACTIONS_ONLY,
                label = stringRes(R.string.enum__smartbar_layout__actions_only),
                description = stringRes(R.string.enum__smartbar_layout__actions_only__description),
            )
            entry(
                key = SmartbarLayout.SUGGESTIONS_ACTIONS_SHARED,
                label = stringRes(R.string.enum__smartbar_layout__suggestions_action_shared),
                description = stringRes(R.string.enum__smartbar_layout__suggestions_action_shared__description),
            )
            entry(
                key = SmartbarLayout.SUGGESTIONS_ACTIONS_EXTENDED,
                label = stringRes(R.string.enum__smartbar_layout__suggestions_actions_extended),
                description = stringRes(R.string.enum__smartbar_layout__suggestions_actions_extended__description),
            )
        }
    },
    SnyggLevel::class to DEFAULT to {
        listPrefEntries {
            entry(
                key = SnyggLevel.BASIC,
                label = stringRes(R.string.enum__snygg_level__basic),
                description = stringRes(R.string.enum__snygg_level__basic__description),
                showDescriptionOnlyIfSelected = true,
            )
            entry(
                key = SnyggLevel.ADVANCED,
                label = stringRes(R.string.enum__snygg_level__advanced),
                description = stringRes(R.string.enum__snygg_level__advanced__description),
                showDescriptionOnlyIfSelected = true,
            )
            entry(
                key = SnyggLevel.DEVELOPER,
                label = stringRes(R.string.enum__snygg_level__developer),
                description = stringRes(R.string.enum__snygg_level__developer__description),
                showDescriptionOnlyIfSelected = true,
            )
        }
    },
    SpaceBarMode::class to DEFAULT to {
        listPrefEntries {
            entry(
                key = SpaceBarMode.NOTHING,
                label = stringRes(R.string.enum__space_bar_mode__nothing),
            )
            entry(
                key = SpaceBarMode.CURRENT_LANGUAGE,
                label = stringRes(R.string.enum__space_bar_mode__current_language),
            )
            entry(
                key = SpaceBarMode.SPACE_BAR_KEY,
                label = stringRes(R.string.enum__space_bar_mode__space_bar_key),
            )
        }
    },
    SpellingLanguageMode::class to DEFAULT to {
        listPrefEntries {
            entry(
                key = SpellingLanguageMode.USE_SYSTEM_LANGUAGES,
                label = stringRes(R.string.enum__spelling_language_mode__use_system_languages),
            )
            entry(
                key = SpellingLanguageMode.USE_KEYBOARD_SUBTYPES,
                label = stringRes(R.string.enum__spelling_language_mode__use_keyboard_subtypes),
            )
        }
    },
    SwipeAction::class to "general" to {
        listPrefEntries {
            entry(
                key = SwipeAction.NO_ACTION,
                label = stringRes(R.string.enum__swipe_action__no_action),
            )
            entry(
                key = SwipeAction.CYCLE_TO_PREVIOUS_KEYBOARD_MODE,
                label = stringRes(R.string.enum__swipe_action__cycle_to_previous_keyboard_mode),
            )
            entry(
                key = SwipeAction.CYCLE_TO_NEXT_KEYBOARD_MODE,
                label = stringRes(R.string.enum__swipe_action__cycle_to_next_keyboard_mode),
            )
            entry(
                key = SwipeAction.DELETE_WORD,
                label = stringRes(R.string.enum__swipe_action__delete_word),
            )
            entry(
                key = SwipeAction.HIDE_KEYBOARD,
                label = stringRes(R.string.enum__swipe_action__hide_keyboard),
            )
            entry(
                key = SwipeAction.INSERT_SPACE,
                label = stringRes(R.string.enum__swipe_action__insert_space),
            )
            entry(
                key = SwipeAction.MOVE_CURSOR_UP,
                label = stringRes(R.string.enum__swipe_action__move_cursor_up),
            )
            entry(
                key = SwipeAction.MOVE_CURSOR_DOWN,
                label = stringRes(R.string.enum__swipe_action__move_cursor_down),
            )
            entry(
                key = SwipeAction.MOVE_CURSOR_LEFT,
                label = stringRes(R.string.enum__swipe_action__move_cursor_left),
            )
            entry(
                key = SwipeAction.MOVE_CURSOR_RIGHT,
                label = stringRes(R.string.enum__swipe_action__move_cursor_right),
            )
            entry(
                key = SwipeAction.MOVE_CURSOR_START_OF_LINE,
                label = stringRes(R.string.enum__swipe_action__move_cursor_start_of_line),
            )
            entry(
                key = SwipeAction.MOVE_CURSOR_END_OF_LINE,
                label = stringRes(R.string.enum__swipe_action__move_cursor_end_of_line),
            )
            entry(
                key = SwipeAction.MOVE_CURSOR_START_OF_PAGE,
                label = stringRes(R.string.enum__swipe_action__move_cursor_start_of_page),
            )
            entry(
                key = SwipeAction.MOVE_CURSOR_END_OF_PAGE,
                label = stringRes(R.string.enum__swipe_action__move_cursor_end_of_page),
            )
            entry(
                key = SwipeAction.SHIFT,
                label = stringRes(R.string.enum__swipe_action__shift),
            )
            entry(
                key = SwipeAction.REDO,
                label = stringRes(R.string.enum__swipe_action__redo),
            )
            entry(
                key = SwipeAction.UNDO,
                label = stringRes(R.string.enum__swipe_action__undo),
            )
            entry(
                key = SwipeAction.SWITCH_TO_CLIPBOARD_CONTEXT,
                label = stringRes(R.string.enum__swipe_action__switch_to_clipboard_context),
            )
            entry(
                key = SwipeAction.SHOW_INPUT_METHOD_PICKER,
                label = stringRes(R.string.enum__swipe_action__show_input_method_picker),
            )
            entry(
                key = SwipeAction.SWITCH_TO_PREV_SUBTYPE,
                label = stringRes(R.string.enum__swipe_action__switch_to_prev_subtype),
            )
            entry(
                key = SwipeAction.SWITCH_TO_NEXT_SUBTYPE,
                label = stringRes(R.string.enum__swipe_action__switch_to_next_subtype),
            )
            entry(
                key = SwipeAction.SWITCH_TO_PREV_KEYBOARD,
                label = stringRes(R.string.enum__swipe_action__switch_to_prev_keyboard),
            )
            entry(
                key = SwipeAction.TOGGLE_SMARTBAR_VISIBILITY,
                label = stringRes(R.string.enum__swipe_action__toggle_smartbar_visibility),
            )
        }
    },
    SwipeAction::class to "deleteSwipe" to {
        listPrefEntries {
            entry(
                key = SwipeAction.NO_ACTION,
                label = stringRes(R.string.enum__swipe_action__no_action),
            )
            entry(
                key = SwipeAction.DELETE_CHARACTERS_PRECISELY,
                label = stringRes(R.string.enum__swipe_action__delete_characters_precisely),
            )
            entry(
                key = SwipeAction.DELETE_WORD,
                label = stringRes(R.string.enum__swipe_action__delete_word),
            )
            entry(
                key = SwipeAction.DELETE_WORDS_PRECISELY,
                label = stringRes(R.string.enum__swipe_action__delete_words_precisely),
            )
            entry(
                key = SwipeAction.SELECT_CHARACTERS_PRECISELY,
                label = stringRes(R.string.enum__swipe_action__select_characters_precisely),
            )
            entry(
                key = SwipeAction.SELECT_WORDS_PRECISELY,
                label = stringRes(R.string.enum__swipe_action__select_words_precisely),
            )
        }
    },
    SwipeAction::class to "deleteLongPress" to {
        listPrefEntries {
            entry(
                key = SwipeAction.DELETE_CHARACTER,
                label = stringRes(R.string.enum__swipe_action__delete_character),
            )
            entry(
                key = SwipeAction.DELETE_WORD,
                label = stringRes(R.string.enum__swipe_action__delete_word),
            )
        }
    },
    ThemeMode::class to DEFAULT to {
        listPrefEntries {
            entry(
                key = ThemeMode.ALWAYS_DAY,
                label = stringRes(R.string.enum__theme_mode__always_day),
            )
            entry(
                key = ThemeMode.ALWAYS_NIGHT,
                label = stringRes(R.string.enum__theme_mode__always_night),
            )
            entry(
                key = ThemeMode.FOLLOW_SYSTEM,
                label = stringRes(R.string.enum__theme_mode__follow_system),
            )
            entry(
                key = ThemeMode.FOLLOW_TIME,
                label = stringRes(R.string.enum__theme_mode__follow_time),
            )
        }
    },
    UtilityKeyAction::class to DEFAULT to {
        listPrefEntries {
            entry(
                key = UtilityKeyAction.SWITCH_TO_EMOJIS,
                label = stringRes(R.string.enum__utility_key_action__switch_to_emojis),
            )
            entry(
                key = UtilityKeyAction.SWITCH_LANGUAGE,
                label = stringRes(R.string.enum__utility_key_action__switch_language),
            )
            entry(
                key = UtilityKeyAction.SWITCH_KEYBOARD_APP,
                label = stringRes(R.string.enum__utility_key_action__switch_keyboard_app),
            )
            entry(
                key = UtilityKeyAction.DYNAMIC_SWITCH_LANGUAGE_EMOJIS,
                label = stringRes(R.string.enum__utility_key_action__dynamic_switch_language_emojis),
            )
        }
    },
)

@Composable
fun <V : Any> enumDisplayEntriesOf(
    enumClass: KClass<V>,
    variant: String = DEFAULT,
): List<ListPreferenceEntry<V>> {
    @Suppress("UNCHECKED_CAST")
    return ENUM_DISPLAY_ENTRIES[enumClass to variant]?.invoke()
        as List<ListPreferenceEntry<V>>
}
