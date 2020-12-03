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
import android.content.SharedPreferences
import android.provider.Settings
import androidx.preference.PreferenceManager
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.ime.text.gestures.DistanceThreshold
import dev.patrickgold.florisboard.ime.text.gestures.SwipeAction
import dev.patrickgold.florisboard.ime.text.gestures.VelocityThreshold
import dev.patrickgold.florisboard.util.VersionName
import kotlin.collections.HashMap

/**
 * Helper class for an organized access to the shared preferences.
 */
class PrefHelper(
    private val context: Context,
    val shared: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
) {
    private val cacheBoolean: HashMap<String, Boolean> = hashMapOf()
    private val cacheInt: HashMap<String, Int> = hashMapOf()
    private val cacheString: HashMap<String, String> = hashMapOf()

    val advanced = Advanced(this)
    val correction = Correction(this)
    val gestures = Gestures(this)
    val glide = Glide(this)
    val internal = Internal(this)
    val keyboard = Keyboard(this)
    val localization = Localization(this)
    val suggestion = Suggestion(this)
    val theme = Theme(this)

    /**
     * Checks the cache if an entry for [key] exists, else calls [getPrefInternal] to retrieve the
     * value. The type is automatically derived from the given [default] value.
     * @return The value for [key] or [default].
     */
    private inline fun <reified T> getPref(key: String, default: T): T {
        return when {
            false is T -> {
                (cacheBoolean[key] ?: getPrefInternal(key, default)) as T
            }
            0 is T -> {
                (cacheInt[key] ?: getPrefInternal(key, default)) as T
            }
            "" is T -> {
                (cacheString[key] ?: getPrefInternal(key, default)) as T
            }
            else -> null as T
        }
    }

    /**
     * Fetches the value for [key] from the shared preferences, puts the value into the
     * corresponding cache and returns it.
     * @return The value for [key] or [default].
     */
    private inline fun <reified T> getPrefInternal(key: String, default: T): T {
        return when {
            false is T -> {
                val value = shared.getBoolean(key, default as Boolean)
                cacheBoolean[key] = value
                value as T
            }
            0 is T -> {
                val value = shared.getInt(key, default as Int)
                cacheInt[key] = value
                value as T
            }
            "" is T -> {
                val value = (shared.getString(key, default as String) ?: (default as String))
                cacheString[key] = value
                value as T
            }
            else -> null as T
        }
    }

    /**
     * Sets the [value] for [key] in the shared preferences, puts the value into the corresponding
     * cache and returns it.
     */
    private inline fun <reified T> setPref(key: String, value: T) {
        when {
            false is T -> {
                shared.edit().putBoolean(key, value as Boolean).apply()
                cacheBoolean[key] = value as Boolean
            }
            0 is T -> {
                shared.edit().putInt(key, value as Int).apply()
                cacheInt[key] = value as Int
            }
            "" is T -> {
                shared.edit().putString(key, value as String).apply()
                cacheString[key] = value as String
            }
        }
    }

    companion object {
        private var defaultInstance: PrefHelper? = null

        @Synchronized
        fun getDefaultInstance(context: Context): PrefHelper {
            if (defaultInstance == null) {
                defaultInstance = PrefHelper(context)
            }
            return defaultInstance!!
        }
    }

    /**
     * Tells the [PreferenceManager] to set the defined preferences to their default values, if
     * they have not been initialized yet.
     */
    fun initDefaultPreferences() {
        PreferenceManager.setDefaultValues(context, R.xml.prefs_advanced, true)
        PreferenceManager.setDefaultValues(context, R.xml.prefs_gestures, true)
        PreferenceManager.setDefaultValues(context, R.xml.prefs_keyboard, true)
        PreferenceManager.setDefaultValues(context, R.xml.prefs_theme, true)
        PreferenceManager.setDefaultValues(context, R.xml.prefs_typing, true)
        //setPref(Keyboard.SUBTYPES, "")
        //setPref(Internal.IS_IME_SET_UP, false)
    }

    /**
     * Syncs the system preference values and clears the cache.
     */
    fun sync() {
        val contentResolver = context.contentResolver
        keyboard.soundEnabledSystem = Settings.System.getInt(
            contentResolver, Settings.System.SOUND_EFFECTS_ENABLED, 0
        ) != 0
        keyboard.vibrationEnabledSystem = Settings.System.getInt(
            contentResolver, Settings.System.HAPTIC_FEEDBACK_ENABLED, 0
        ) != 0

        cacheBoolean.clear()
        cacheInt.clear()
        cacheString.clear()
    }

    /**
     * Wrapper class for advanced preferences.
     */
    class Advanced(private val prefHelper: PrefHelper) {
        companion object {
            const val SETTINGS_THEME =          "advanced__settings_theme"
            const val SHOW_APP_ICON =           "advanced__show_app_icon"
        }

        var settingsTheme: String = ""
            get() = prefHelper.getPref(SETTINGS_THEME, "auto")
            private set
        var showAppIcon: Boolean = false
            get() = prefHelper.getPref(SHOW_APP_ICON, true)
            private set
    }

    /**
     * Wrapper class for correction preferences.
     */
    class Correction(private val prefHelper: PrefHelper) {
        companion object {
            const val AUTO_CAPITALIZATION =         "correction__auto_capitalization"
            const val DOUBLE_SPACE_PERIOD =         "correction__double_space_period"
            const val REMEMBER_CAPS_LOCK_STATE =    "correction__remember_caps_lock_state"
        }

        var autoCapitalization: Boolean
            get() =  prefHelper.getPref(AUTO_CAPITALIZATION, true)
            set(v) = prefHelper.setPref(AUTO_CAPITALIZATION, v)
        var doubleSpacePeriod: Boolean
            get() =  prefHelper.getPref(DOUBLE_SPACE_PERIOD, true)
            set(v) = prefHelper.setPref(DOUBLE_SPACE_PERIOD, v)
        var rememberCapsLockState: Boolean
            get() =  prefHelper.getPref(REMEMBER_CAPS_LOCK_STATE, false)
            set(v) = prefHelper.setPref(REMEMBER_CAPS_LOCK_STATE, v)
    }

    /**
     * Wrapper class for gestures preferences.
     */
    class Gestures(private val prefHelper: PrefHelper) {
        companion object {
            const val SWIPE_UP =                    "gestures__swipe_up"
            const val SWIPE_DOWN =                  "gestures__swipe_down"
            const val SWIPE_LEFT =                  "gestures__swipe_left"
            const val SWIPE_RIGHT =                 "gestures__swipe_right"
            const val SPACE_BAR_SWIPE_LEFT =        "gestures__space_bar_swipe_left"
            const val SPACE_BAR_SWIPE_RIGHT =       "gestures__space_bar_swipe_right"
            const val DELETE_KEY_SWIPE_LEFT =       "gestures__delete_key_swipe_left"
            const val SWIPE_VELOCITY_THRESHOLD =    "gestures__swipe_velocity_threshold"
            const val SWIPE_DISTANCE_THRESHOLD =    "gestures__swipe_distance_threshold"
        }

        var swipeUp: SwipeAction
            get() =  SwipeAction.fromString(prefHelper.getPref(SWIPE_UP, "no_action"))
            set(v) = prefHelper.setPref(SWIPE_UP, v)
        var swipeDown: SwipeAction
            get() =  SwipeAction.fromString(prefHelper.getPref(SWIPE_DOWN, "no_action"))
            set(v) = prefHelper.setPref(SWIPE_DOWN, v)
        var swipeLeft: SwipeAction
            get() =  SwipeAction.fromString(prefHelper.getPref(SWIPE_LEFT, "no_action"))
            set(v) = prefHelper.setPref(SWIPE_LEFT, v)
        var swipeRight: SwipeAction
            get() =  SwipeAction.fromString(prefHelper.getPref(SWIPE_RIGHT, "no_action"))
            set(v) = prefHelper.setPref(SWIPE_RIGHT, v)
        var spaceBarSwipeLeft: SwipeAction
            get() =  SwipeAction.fromString(prefHelper.getPref(SPACE_BAR_SWIPE_LEFT, "no_action"))
            set(v) = prefHelper.setPref(SPACE_BAR_SWIPE_LEFT, v)
        var spaceBarSwipeRight: SwipeAction
            get() =  SwipeAction.fromString(prefHelper.getPref(SPACE_BAR_SWIPE_RIGHT, "no_action"))
            set(v) = prefHelper.setPref(SPACE_BAR_SWIPE_RIGHT, v)
        var deleteKeySwipeLeft: SwipeAction
            get() =  SwipeAction.fromString(prefHelper.getPref(DELETE_KEY_SWIPE_LEFT, "no_action"))
            set(v) = prefHelper.setPref(DELETE_KEY_SWIPE_LEFT, v)
        var swipeVelocityThreshold: VelocityThreshold
            get() =  VelocityThreshold.fromString(prefHelper.getPref(SWIPE_VELOCITY_THRESHOLD, "normal"))
            set(v) = prefHelper.setPref(SWIPE_VELOCITY_THRESHOLD, v)
        var swipeDistanceThreshold: DistanceThreshold
            get() =  DistanceThreshold.fromString(prefHelper.getPref(SWIPE_DISTANCE_THRESHOLD, "normal"))
            set(v) = prefHelper.setPref(SWIPE_DISTANCE_THRESHOLD, v)
    }

    /**
     * Wrapper class for glide preferences.
     */
    class Glide(private val prefHelper: PrefHelper) {
        companion object {
            const val ENABLED =                     "glide__enabled"
            const val SHOW_TRAIL =                  "glide__show_trail"
        }

        var enabled: Boolean
            get() =  prefHelper.getPref(ENABLED, false)
            set(v) = prefHelper.setPref(ENABLED, v)
        var showTrail: Boolean
            get() =  prefHelper.getPref(SHOW_TRAIL, false)
            set(v) = prefHelper.setPref(SHOW_TRAIL, v)
    }

    /**
     * Wrapper class for internal preferences. A preference qualifies as an internal pref if the
     * user has no ability to control this preference's value directly (via a UI pref view).
     */
    class Internal(private val prefHelper: PrefHelper) {
        companion object {
            const val IS_IME_SET_UP =               "internal__is_ime_set_up"
            const val THEME_CURRENT_BASED_ON =      "internal__theme_current_based_on"
            const val THEME_CURRENT_IS_MODIFIED =   "internal__theme_current_is_modified"
            const val THEME_CURRENT_IS_NIGHT =      "internal__theme_current_is_night"
            const val VERSION_ON_INSTALL =          "internal__version_on_install"
            const val VERSION_LAST_USE =            "internal__version_last_use"
            const val VERSION_LAST_CHANGELOG =      "internal__version_last_changelog"
        }

        var isImeSetUp: Boolean
            get() =  prefHelper.getPref(IS_IME_SET_UP, false)
            set(v) = prefHelper.setPref(IS_IME_SET_UP, v)
        var themeCurrentBasedOn: String
            get() =  prefHelper.getPref(THEME_CURRENT_BASED_ON, "undefined")
            set(v) = prefHelper.setPref(THEME_CURRENT_BASED_ON, v)
        var themeCurrentIsModified: Boolean
            get() =  prefHelper.getPref(THEME_CURRENT_IS_MODIFIED, false)
            set(v) = prefHelper.setPref(THEME_CURRENT_IS_MODIFIED, v)
        var themeCurrentIsNight: Boolean
            get() =  prefHelper.getPref(THEME_CURRENT_IS_NIGHT, false)
            set(v) = prefHelper.setPref(THEME_CURRENT_IS_NIGHT, v)
        var versionOnInstall: String
            get() =  prefHelper.getPref(VERSION_ON_INSTALL, VersionName.DEFAULT_RAW)
            set(v) = prefHelper.setPref(VERSION_ON_INSTALL, v)
        var versionLastUse: String
            get() =  prefHelper.getPref(VERSION_LAST_USE, VersionName.DEFAULT_RAW)
            set(v) = prefHelper.setPref(VERSION_LAST_USE, v)
        var versionLastChangelog: String
            get() =  prefHelper.getPref(VERSION_LAST_CHANGELOG, VersionName.DEFAULT_RAW)
            set(v) = prefHelper.setPref(VERSION_LAST_CHANGELOG, v)
    }

    /**
     * Wrapper class for keyboard preferences.
     */
    class Keyboard(private val prefHelper: PrefHelper) {
        companion object {
            const val BOTTOM_OFFSET =                   "keyboard__bottom_offset"
            const val FONT_SIZE_MULTIPLIER_PORTRAIT =   "keyboard__font_size_multiplier_portrait"
            const val FONT_SIZE_MULTIPLIER_LANDSCAPE =  "keyboard__font_size_multiplier_landscape"
            const val HEIGHT_FACTOR =                   "keyboard__height_factor"
            const val HEIGHT_FACTOR_CUSTOM =            "keyboard__height_factor_custom"
            const val HINTED_NUMBER_ROW =               "keyboard__hinted_number_row"
            const val HINTED_SYMBOLS =                  "keyboard__hinted_symbols"
            const val LONG_PRESS_DELAY =                "keyboard__long_press_delay"
            const val ONE_HANDED_MODE =                 "keyboard__one_handed_mode"
            const val POPUP_ENABLED =                   "keyboard__popup_enabled"
            const val SOUND_ENABLED =                   "keyboard__sound_enabled"
            const val SOUND_VOLUME =                    "keyboard__sound_volume"
            const val VIBRATION_ENABLED =               "keyboard__vibration_enabled"
            const val VIBRATION_STRENGTH =              "keyboard__vibration_strength"
        }

        var bottomOffset: Int = 0
            get() = prefHelper.getPref(BOTTOM_OFFSET, 0)
            private set
        var fontSizeMultiplierPortrait: Int
            get() =  prefHelper.getPref(FONT_SIZE_MULTIPLIER_PORTRAIT, 100)
            set(v) = prefHelper.setPref(FONT_SIZE_MULTIPLIER_PORTRAIT, v)
        var fontSizeMultiplierLandscape: Int
            get() =  prefHelper.getPref(FONT_SIZE_MULTIPLIER_LANDSCAPE, 100)
            set(v) = prefHelper.setPref(FONT_SIZE_MULTIPLIER_LANDSCAPE, v)
        var heightFactor: String = ""
            get() = prefHelper.getPref(HEIGHT_FACTOR, "normal")
            private set
        var heightFactorCustom: Int
            get() =  prefHelper.getPref(HEIGHT_FACTOR_CUSTOM, 100)
            set(v) = prefHelper.setPref(HEIGHT_FACTOR_CUSTOM, v)
        var hintedNumberRow: Boolean
            get() =  prefHelper.getPref(HINTED_NUMBER_ROW, true)
            set(v) = prefHelper.setPref(HINTED_NUMBER_ROW, v)
        var hintedSymbols: Boolean
            get() =  prefHelper.getPref(HINTED_SYMBOLS, true)
            set(v) = prefHelper.setPref(HINTED_SYMBOLS, v)
        var longPressDelay: Int = 0
            get() = prefHelper.getPref(LONG_PRESS_DELAY, 300)
            private set
        var oneHandedMode: String
            get() = prefHelper.getPref(ONE_HANDED_MODE, "off")
            set(value) = prefHelper.setPref(ONE_HANDED_MODE, value)
        var popupEnabled: Boolean = false
            get() = prefHelper.getPref(POPUP_ENABLED, true)
            private set
        var soundEnabled: Boolean = false
            get() = prefHelper.getPref(SOUND_ENABLED, true)
            private set
        var soundEnabledSystem: Boolean = false
        var soundVolume: Int = 0
            get() = prefHelper.getPref(SOUND_VOLUME, -1)
            private set
        var vibrationEnabled: Boolean = false
            get() = prefHelper.getPref(VIBRATION_ENABLED, true)
            private set
        var vibrationEnabledSystem: Boolean = false
        var vibrationStrength: Int = 0
            get() = prefHelper.getPref(VIBRATION_STRENGTH, -1)
            private set
    }

    /**
     * Wrapper class for localization preferences.
     */
    class Localization(private val prefHelper: PrefHelper) {
        companion object {
            const val ACTIVE_SUBTYPE_ID =       "localization__active_subtype_id"
            const val SUBTYPES =                "localization__subtypes"
        }

        var activeSubtypeId: Int
            get() =  prefHelper.getPref(ACTIVE_SUBTYPE_ID, Subtype.DEFAULT.id)
            set(v) = prefHelper.setPref(ACTIVE_SUBTYPE_ID, v)
        var subtypes: String
            get() =  prefHelper.getPref(SUBTYPES, "")
            set(v) = prefHelper.setPref(SUBTYPES, v)
    }

    /**
     * Wrapper class for suggestion preferences.
     */
    class Suggestion(private val prefHelper: PrefHelper) {
        companion object {
            const val ENABLED =                     "suggestion__enabled"
            const val SHOW_INSTEAD =                "suggestion__show_instead"
            const val SUGGEST_CLIPBOARD_CONTENT =   "suggestion__suggest_clipboard_content"
            const val USE_PREV_WORDS =              "suggestion__use_prev_words"
        }

        var enabled: Boolean
            get() =  prefHelper.getPref(ENABLED, true)
            set(v) = prefHelper.setPref(ENABLED, v)
        var showInstead: String
            get() =  prefHelper.getPref(SHOW_INSTEAD, "number_row")
            set(v) = prefHelper.setPref(SHOW_INSTEAD, v)
        var suggestClipboardContent: Boolean
            get() =  prefHelper.getPref(SUGGEST_CLIPBOARD_CONTENT, false)
            set(v) = prefHelper.setPref(SUGGEST_CLIPBOARD_CONTENT, v)
        var usePrevWords: Boolean
            get() =  prefHelper.getPref(USE_PREV_WORDS, true)
            set(v) = prefHelper.setPref(USE_PREV_WORDS, v)
    }

    /**
     * Wrapper class for theme preferences.
     */
    class Theme(private val prefHelper: PrefHelper) {
        companion object {
            const val COLOR_PRIMARY =                       "theme__colorPrimary"
            const val COLOR_PRIMARY_DARK =                  "theme__colorPrimaryDark"
            const val COLOR_ACCENT =                        "theme__colorAccent"
            const val NAV_BAR_COLOR =                       "theme__navBarColor"
            const val NAV_BAR_IS_LIGHT =                    "theme__navBarIsLight"
            const val KEYBOARD_BG_COLOR =                   "theme__keyboard_bgColor"
            const val KEY_BG_COLOR =                        "theme__key_bgColor"
            const val KEY_BG_COLOR_PRESSED =                "theme__key_bgColorPressed"
            const val KEY_FG_COLOR =                        "theme__key_fgColor"
            const val KEY_ENTER_BG_COLOR =                  "theme__keyEnter_bgColor"
            const val KEY_ENTER_BG_COLOR_PRESSED =          "theme__keyEnter_bgColorPressed"
            const val KEY_ENTER_FG_COLOR =                  "theme__keyEnter_fgColor"
            const val KEY_SHIFT_BG_COLOR =                  "theme__keyShift_bgColor"
            const val KEY_SHIFT_BG_COLOR_PRESSED =          "theme__keyShift_bgColorPressed"
            const val KEY_SHIFT_FG_COLOR =                  "theme__keyShift_fgColor"
            const val KEY_SHIFT_FG_COLOR_CAPSLOCK =         "theme__keyShift_fgColorCapsLock"
            const val KEY_POPUP_BG_COLOR =                  "theme__keyPopup_bgColor"
            const val KEY_POPUP_BG_COLOR_ACTIVE =           "theme__keyPopup_bgColorActive"
            const val KEY_POPUP_FG_COLOR =                  "theme__keyPopup_fgColor"
            const val MEDIA_FG_COLOR =                      "theme__media_fgColor"
            const val MEDIA_FG_COLOR_ALT =                  "theme__media_fgColorAlt"
            const val ONE_HANDED_BG_COLOR =                 "theme__oneHanded_bgColor"
            const val ONE_HANDED_BUTTON_FG_COLOR =          "theme__oneHandedButton_fgColor"
            const val SMARTBAR_BG_COLOR =                   "theme__smartbar_bgColor"
            const val SMARTBAR_FG_COLOR =                   "theme__smartbar_fgColor"
            const val SMARTBAR_FG_COLOR_ALT =               "theme__smartbar_fgColorAlt"
            const val SMARTBAR_BUTTON_BG_COLOR =            "theme__smartbarButton_bgColor"
            const val SMARTBAR_BUTTON_FG_COLOR =            "theme__smartbarButton_fgColor"
        }

        var colorPrimary: Int
            get() =  prefHelper.getPref(COLOR_PRIMARY, 0)
            set(v) = prefHelper.setPref(COLOR_PRIMARY, v)
        var colorPrimaryDark: Int
            get() =  prefHelper.getPref(COLOR_PRIMARY_DARK, 0)
            set(v) = prefHelper.setPref(COLOR_PRIMARY_DARK, v)
        var colorAccent: Int
            get() =  prefHelper.getPref(COLOR_ACCENT, 0)
            set(v) = prefHelper.setPref(COLOR_ACCENT, v)
        var navBarColor: Int
            get() =  prefHelper.getPref(NAV_BAR_COLOR, 0)
            set(v) = prefHelper.setPref(NAV_BAR_COLOR, v)
        var navBarIsLight: Boolean
            get() =  prefHelper.getPref(NAV_BAR_IS_LIGHT, false)
            set(v) = prefHelper.setPref(NAV_BAR_IS_LIGHT, v)
        var keyboardBgColor: Int
            get() =  prefHelper.getPref(KEYBOARD_BG_COLOR, 0)
            set(v) = prefHelper.setPref(KEYBOARD_BG_COLOR, v)
        var keyBgColor: Int
            get() =  prefHelper.getPref(KEY_BG_COLOR, 0)
            set(v) = prefHelper.setPref(KEY_BG_COLOR, v)
        var keyBgColorPressed: Int
            get() =  prefHelper.getPref(KEY_BG_COLOR_PRESSED, 0)
            set(v) = prefHelper.setPref(KEY_BG_COLOR_PRESSED, v)
        var keyFgColor: Int
            get() =  prefHelper.getPref(KEY_FG_COLOR, 0)
            set(v) = prefHelper.setPref(KEY_FG_COLOR, v)
        var keyEnterBgColor: Int
            get() =  prefHelper.getPref(KEY_ENTER_BG_COLOR, 0)
            set(v) = prefHelper.setPref(KEY_ENTER_BG_COLOR, v)
        var keyEnterBgColorPressed: Int
            get() =  prefHelper.getPref(KEY_ENTER_BG_COLOR_PRESSED, 0)
            set(v) = prefHelper.setPref(KEY_ENTER_BG_COLOR_PRESSED, v)
        var keyEnterFgColor: Int
            get() =  prefHelper.getPref(KEY_ENTER_FG_COLOR, 0)
            set(v) = prefHelper.setPref(KEY_ENTER_FG_COLOR, v)
        var keyShiftBgColor: Int
            get() =  prefHelper.getPref(KEY_SHIFT_BG_COLOR, 0)
            set(v) = prefHelper.setPref(KEY_SHIFT_BG_COLOR, v)
        var keyShiftBgColorPressed: Int
            get() =  prefHelper.getPref(KEY_SHIFT_BG_COLOR_PRESSED, 0)
            set(v) = prefHelper.setPref(KEY_SHIFT_BG_COLOR_PRESSED, v)
        var keyShiftFgColor: Int
            get() =  prefHelper.getPref(KEY_SHIFT_FG_COLOR, 0)
            set(v) = prefHelper.setPref(KEY_SHIFT_FG_COLOR, v)
        var keyShiftFgColorCapsLock: Int
            get() =  prefHelper.getPref(KEY_SHIFT_FG_COLOR_CAPSLOCK, 0)
            set(v) = prefHelper.setPref(KEY_SHIFT_FG_COLOR_CAPSLOCK, v)
        var keyPopupBgColor: Int
            get() =  prefHelper.getPref(KEY_POPUP_BG_COLOR, 0)
            set(v) = prefHelper.setPref(KEY_POPUP_BG_COLOR, v)
        var keyPopupBgColorActive: Int
            get() =  prefHelper.getPref(KEY_POPUP_BG_COLOR_ACTIVE, 0)
            set(v) = prefHelper.setPref(KEY_POPUP_BG_COLOR_ACTIVE, v)
        var keyPopupFgColor: Int
            get() =  prefHelper.getPref(KEY_POPUP_FG_COLOR, 0)
            set(v) = prefHelper.setPref(KEY_POPUP_FG_COLOR, v)
        var mediaFgColor: Int
            get() =  prefHelper.getPref(MEDIA_FG_COLOR, 0)
            set(v) = prefHelper.setPref(MEDIA_FG_COLOR, v)
        var mediaFgColorAlt: Int
            get() =  prefHelper.getPref(MEDIA_FG_COLOR_ALT, 0)
            set(v) = prefHelper.setPref(MEDIA_FG_COLOR_ALT, v)
        var oneHandedBgColor: Int
            get() =  prefHelper.getPref(ONE_HANDED_BG_COLOR, 0)
            set(v) = prefHelper.setPref(ONE_HANDED_BG_COLOR, v)
        var oneHandedButtonFgColor: Int
            get() =  prefHelper.getPref(ONE_HANDED_BUTTON_FG_COLOR, 0)
            set(v) = prefHelper.setPref(ONE_HANDED_BUTTON_FG_COLOR, v)
        var smartbarBgColor: Int
            get() =  prefHelper.getPref(SMARTBAR_BG_COLOR, 0)
            set(v) = prefHelper.setPref(SMARTBAR_BG_COLOR, v)
        var smartbarFgColor: Int
            get() =  prefHelper.getPref(SMARTBAR_FG_COLOR, 0)
            set(v) = prefHelper.setPref(SMARTBAR_FG_COLOR, v)
        var smartbarFgColorAlt: Int
            get() =  prefHelper.getPref(SMARTBAR_FG_COLOR_ALT, 0)
            set(v) = prefHelper.setPref(SMARTBAR_FG_COLOR_ALT, v)
        var smartbarButtonBgColor: Int
            get() =  prefHelper.getPref(SMARTBAR_BUTTON_BG_COLOR, 0)
            set(v) = prefHelper.setPref(SMARTBAR_BUTTON_BG_COLOR, v)
        var smartbarButtonFgColor: Int
            get() =  prefHelper.getPref(SMARTBAR_BUTTON_FG_COLOR, 0)
            set(v) = prefHelper.setPref(SMARTBAR_BUTTON_FG_COLOR, v)
    }
}
