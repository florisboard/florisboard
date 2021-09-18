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
import android.os.Build
import androidx.core.os.UserManagerCompat
import androidx.preference.PreferenceManager
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.ime.text.gestures.DistanceThreshold
import dev.patrickgold.florisboard.ime.text.gestures.SwipeAction
import dev.patrickgold.florisboard.ime.text.gestures.VelocityThreshold
import dev.patrickgold.florisboard.ime.text.smartbar.CandidateView
import dev.patrickgold.florisboard.ime.theme.ThemeMode
import dev.patrickgold.florisboard.util.TimeUtil
import dev.patrickgold.florisboard.util.VersionName
import java.lang.ref.WeakReference

/**
 * Helper class for an organized access to the shared preferences.
 */
class Preferences(
    context: Context,
) {
    var shared: SharedPreferences = if (!UserManagerCompat.isUserUnlocked(context) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
        context.createDeviceProtectedStorageContext().getSharedPreferences("shared_psfs", Context.MODE_PRIVATE)
    else
        PreferenceManager.getDefaultSharedPreferences(context)

    private val applicationContext: WeakReference<Context> = WeakReference(context.applicationContext)

    val correction = Correction(this)
    val dictionary = Dictionary(this)
    val gestures = Gestures(this)
    val glide = Glide(this)
    val internal = Internal(this)
    val localization = Localization(this)
    val smartbar = Smartbar(this)
    val spelling = Spelling(this)
    val suggestion = Suggestion(this)
    val theme = Theme(this)


    /**
     * Gets the value for given [key]. The type is automatically derived from the given [default] value.
     *
     * @return The value for [key] or [default].
     */
    private inline fun <reified T> getPref(key: String, default: T): T {
        return when {
            false is T -> {
                shared.getBoolean(key, default as Boolean) as T
            }
            0 is T -> {
                shared.getInt(key, default as Int) as T
            }
            "" is T -> {
                (shared.getString(key, default as String) ?: (default as String)) as T
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
            }
            0 is T -> {
                shared.edit().putInt(key, value as Int).apply()
            }
            "" is T -> {
                shared.edit().putString(key, value as String).apply()
            }
        }
    }

    companion object {
        // old settings are id/language/layout and id/language/currencySet/layout
        // new settings have composer
        private val OLD_SUBTYPES_REGEX = """^([\-0-9]+/[\-a-zA-Z0-9]+(/[a-zA-Z_]+)?/[a-zA-Z_]+[;]*)+${'$'}""".toRegex()
        private var defaultInstance: Preferences? = null

        @Synchronized
        fun initDefault(context: Context): Preferences {
            val instance = Preferences(context.applicationContext)
            defaultInstance = instance
            return instance
        }

        fun default(): Preferences {
            return defaultInstance
                ?: throw UninitializedPropertyAccessException("""
                    Default preferences not initialized! Make sure to call initDefault()
                    before accessing the default preferences.
                """.trimIndent())
        }
    }

    /**
     * Tells the [PreferenceManager] to set the defined preferences to their default values, if
     * they have not been initialized yet.
     */
    fun initDefaultPreferences() {
        try {
            applicationContext.get()?.let { context ->
                PreferenceManager.setDefaultValues(context, R.xml.prefs_gestures, true)
                PreferenceManager.setDefaultValues(context, R.xml.prefs_keyboard, true)
                PreferenceManager.setDefaultValues(context, R.xml.prefs_theme, true)
                PreferenceManager.setDefaultValues(context, R.xml.prefs_typing, true)
            }

            //theme.dayThemeRef = "assets:ime/theme/floris_day.json"
            //theme.nightThemeRef = "assets:ime/theme/floris_night.json"
            //setPref(Localization.SUBTYPES, "-234/de-AT/euro/c=qwertz")
            val subtypes = getPref(Localization.SUBTYPES, "")
            if (subtypes.matches(OLD_SUBTYPES_REGEX)) {
                setPref(Localization.SUBTYPES, "")
            }
        } catch (e: Exception) {
            e.fillInStackTrace()
        }
    }

    /**
     * Wrapper class for correction preferences.
     */
    class Correction(private val prefs: Preferences) {
        companion object {
            const val AUTO_CAPITALIZATION =         "correction__auto_capitalization"
            const val DOUBLE_SPACE_PERIOD =         "correction__double_space_period"
            const val MANAGE_SPELL_CHECKER =        "correction__manage_spell_checker"
            const val REMEMBER_CAPS_LOCK_STATE =    "correction__remember_caps_lock_state"
        }

        var autoCapitalization: Boolean
            get() =  prefs.getPref(AUTO_CAPITALIZATION, true)
            set(v) = prefs.setPref(AUTO_CAPITALIZATION, v)
        var doubleSpacePeriod: Boolean
            get() =  prefs.getPref(DOUBLE_SPACE_PERIOD, true)
            set(v) = prefs.setPref(DOUBLE_SPACE_PERIOD, v)
        var rememberCapsLockState: Boolean
            get() =  prefs.getPref(REMEMBER_CAPS_LOCK_STATE, false)
            set(v) = prefs.setPref(REMEMBER_CAPS_LOCK_STATE, v)
    }

    /**
     * Wrapper class for dictionary preferences.
     */
    class Dictionary(private val prefs: Preferences) {
        companion object {
            const val ENABLE_SYSTEM_USER_DICTIONARY =   "suggestion__enable_system_user_dictionary"
            const val MANAGE_SYSTEM_USER_DICTIONARY =   "suggestion__manage_system_user_dictionary"
            const val ENABLE_FLORIS_USER_DICTIONARY =   "suggestion__enable_floris_user_dictionary"
            const val MANAGE_FLORIS_USER_DICTIONARY =   "suggestion__manage_floris_user_dictionary"
        }

        var enableSystemUserDictionary: Boolean
            get() =  prefs.getPref(ENABLE_SYSTEM_USER_DICTIONARY, true)
            set(v) = prefs.setPref(ENABLE_SYSTEM_USER_DICTIONARY, v)
        var enableFlorisUserDictionary: Boolean
            get() =  prefs.getPref(ENABLE_FLORIS_USER_DICTIONARY, true)
            set(v) = prefs.setPref(ENABLE_FLORIS_USER_DICTIONARY, v)
    }

    /**
     * Wrapper class for gestures preferences.
     */
    class Gestures(private val prefs: Preferences) {
        companion object {
            const val SWIPE_UP =                    "gestures__swipe_up"
            const val SWIPE_DOWN =                  "gestures__swipe_down"
            const val SWIPE_LEFT =                  "gestures__swipe_left"
            const val SWIPE_RIGHT =                 "gestures__swipe_right"
            const val SPACE_BAR_LONG_PRESS =        "gestures__space_bar_long_press"
            const val SPACE_BAR_SWIPE_LEFT =        "gestures__space_bar_swipe_left"
            const val SPACE_BAR_SWIPE_RIGHT =       "gestures__space_bar_swipe_right"
            const val SPACE_BAR_SWIPE_UP =          "gestures__space_bar_swipe_up"
            const val DELETE_KEY_SWIPE_LEFT =       "gestures__delete_key_swipe_left"
            const val SWIPE_VELOCITY_THRESHOLD =    "gestures__swipe_velocity_threshold"
            const val SWIPE_DISTANCE_THRESHOLD =    "gestures__swipe_distance_threshold"
        }

        var swipeUp: SwipeAction
            get() =  SwipeAction.fromString(prefs.getPref(SWIPE_UP, "no_action"))
            set(v) = prefs.setPref(SWIPE_UP, v)
        var swipeDown: SwipeAction
            get() =  SwipeAction.fromString(prefs.getPref(SWIPE_DOWN, "no_action"))
            set(v) = prefs.setPref(SWIPE_DOWN, v)
        var swipeLeft: SwipeAction
            get() =  SwipeAction.fromString(prefs.getPref(SWIPE_LEFT, "no_action"))
            set(v) = prefs.setPref(SWIPE_LEFT, v)
        var swipeRight: SwipeAction
            get() =  SwipeAction.fromString(prefs.getPref(SWIPE_RIGHT, "no_action"))
            set(v) = prefs.setPref(SWIPE_RIGHT, v)
        var spaceBarLongPress: SwipeAction
            get() =  SwipeAction.fromString(prefs.getPref(SPACE_BAR_LONG_PRESS, "no_action"))
            set(v) = prefs.setPref(SPACE_BAR_LONG_PRESS, v)
        var spaceBarSwipeUp: SwipeAction
            get() =  SwipeAction.fromString(prefs.getPref(SPACE_BAR_SWIPE_UP, "no_action"))
            set(v) = prefs.setPref(SPACE_BAR_SWIPE_UP, v)
        var spaceBarSwipeLeft: SwipeAction
            get() =  SwipeAction.fromString(prefs.getPref(SPACE_BAR_SWIPE_LEFT, "no_action"))
            set(v) = prefs.setPref(SPACE_BAR_SWIPE_LEFT, v)
        var spaceBarSwipeRight: SwipeAction
            get() =  SwipeAction.fromString(prefs.getPref(SPACE_BAR_SWIPE_RIGHT, "no_action"))
            set(v) = prefs.setPref(SPACE_BAR_SWIPE_RIGHT, v)
        var deleteKeySwipeLeft: SwipeAction
            get() =  SwipeAction.fromString(prefs.getPref(DELETE_KEY_SWIPE_LEFT, "no_action"))
            set(v) = prefs.setPref(DELETE_KEY_SWIPE_LEFT, v)
        var swipeVelocityThreshold: VelocityThreshold
            get() =  VelocityThreshold.fromString(prefs.getPref(SWIPE_VELOCITY_THRESHOLD, "normal"))
            set(v) = prefs.setPref(SWIPE_VELOCITY_THRESHOLD, v)
        var swipeDistanceThreshold: DistanceThreshold
            get() =  DistanceThreshold.fromString(prefs.getPref(SWIPE_DISTANCE_THRESHOLD, "normal"))
            set(v) = prefs.setPref(SWIPE_DISTANCE_THRESHOLD, v)
    }

    /**
     * Wrapper class for glide preferences.
     */
    class Glide(private val prefs: Preferences) {
        companion object {
            const val ENABLED =                     "glide__enabled"
            const val SHOW_TRAIL =                  "glide__show_trail"
            const val TRAIL_DURATION =              "glide__trail_fade_duration"
            const val SHOW_PREVIEW =                "glide__show_preview"
            const val PREVIEW_REFRESH_DELAY =       "glide__preview_refresh_delay"
        }

        var enabled: Boolean
            get() =  prefs.getPref(ENABLED, false)
            set(v) = prefs.setPref(ENABLED, v)
        var showTrail: Boolean
            get() =  prefs.getPref(SHOW_TRAIL, false)
            set(v) = prefs.setPref(SHOW_TRAIL, v)
        var trailDuration: Int
            get() =  prefs.getPref(TRAIL_DURATION, 200)
            set(v) = prefs.setPref(TRAIL_DURATION, v)
        var showPreview: Boolean
            get() = prefs.getPref(SHOW_PREVIEW, true)
            set(v) = prefs.setPref(SHOW_PREVIEW, v)
        var previewRefreshDelay: Int
            get() = prefs.getPref(PREVIEW_REFRESH_DELAY, 150)
            set(v) = prefs.setPref(PREVIEW_REFRESH_DELAY, v)
    }

    /**
     * Wrapper class for internal preferences. A preference qualifies as an internal pref if the
     * user has no ability to control this preference's value directly (via a UI pref view).
     */
    class Internal(private val prefs: Preferences) {
        companion object {
            const val IS_IME_SET_UP =               "internal__is_ime_set_up"
            const val VERSION_ON_INSTALL =          "internal__version_on_install"
            const val VERSION_LAST_USE =            "internal__version_last_use"
            const val VERSION_LAST_CHANGELOG =      "internal__version_last_changelog"
        }

        var isImeSetUp: Boolean
            get() =  prefs.getPref(IS_IME_SET_UP, false)
            set(v) = prefs.setPref(IS_IME_SET_UP, v)
        var versionOnInstall: String
            get() =  prefs.getPref(VERSION_ON_INSTALL, VersionName.DEFAULT_RAW)
            set(v) = prefs.setPref(VERSION_ON_INSTALL, v)
        var versionLastUse: String
            get() =  prefs.getPref(VERSION_LAST_USE, VersionName.DEFAULT_RAW)
            set(v) = prefs.setPref(VERSION_LAST_USE, v)
        var versionLastChangelog: String
            get() =  prefs.getPref(VERSION_LAST_CHANGELOG, VersionName.DEFAULT_RAW)
            set(v) = prefs.setPref(VERSION_LAST_CHANGELOG, v)
    }

    /**
     * Wrapper class for localization preferences.
     */
    class Localization(private val prefs: Preferences) {
        companion object {
            const val ACTIVE_SUBTYPE_ID =       "localization__active_subtype_id"
            const val SUBTYPES =                "localization__subtypes"
        }

        var activeSubtypeId: Int
            get() =  prefs.getPref(ACTIVE_SUBTYPE_ID, Subtype.DEFAULT.id)
            set(v) = prefs.setPref(ACTIVE_SUBTYPE_ID, v)
        var subtypes: String
            get() =  prefs.getPref(SUBTYPES, "")
            set(v) = prefs.setPref(SUBTYPES, v)
    }

    /**
     * Wrapper class for Smartbar preferences.
     */
    class Smartbar(private val prefs: Preferences) {
        companion object {
            const val ENABLED =                     "smartbar__enabled"
        }

        var enabled: Boolean
            get() =  prefs.getPref(ENABLED, true)
            set(v) = prefs.setPref(ENABLED, v)
    }

    /**
     * Wrapper class for Spelling preferences.
     */
    class Spelling(private val prefs: Preferences) {
        companion object {
            const val ACTIVE_SPELLCHECKER =         "spelling__active_spellchecker"
            const val MANAGE_DICTIONARIES =         "spelling__manage_dictionaries"
            const val USE_CONTACTS =                "spelling__use_contacts"
            const val USE_UDM_ENTRIES =             "spelling__use_udm_entries"
        }

        var useContacts: Boolean
            get() =  prefs.getPref(USE_CONTACTS, true)
            set(v) = prefs.setPref(USE_CONTACTS, v)
        var useUdmEntries: Boolean
            get() =  prefs.getPref(USE_UDM_ENTRIES, true)
            set(v) = prefs.setPref(USE_UDM_ENTRIES, v)
    }

    /**
     * Wrapper class for suggestion preferences.
     */
    class Suggestion(private val prefs: Preferences) {
        companion object {
            const val API30_INLINE_SUGGESTIONS_ENABLED =    "suggestion__api30_inline_suggestions_enabled"
            const val BLOCK_POSSIBLY_OFFENSIVE =            "suggestion__block_possibly_offensive"
            const val CLIPBOARD_CONTENT_ENABLED =           "suggestion__clipboard_content_enabled"
            const val CLIPBOARD_CONTENT_TIMEOUT =           "suggestion__clipboard_content_timeout"
            const val DISPLAY_MODE =                        "suggestion__display_mode"
            const val ENABLED =                             "suggestion__enabled"
            const val USE_PREV_WORDS =                      "suggestion__use_prev_words"
        }

        var api30InlineSuggestionsEnabled: Boolean
            get() =  prefs.getPref(API30_INLINE_SUGGESTIONS_ENABLED, true)
            set(v) = prefs.setPref(API30_INLINE_SUGGESTIONS_ENABLED, v)
        var blockPossiblyOffensive: Boolean
            get() =  prefs.getPref(BLOCK_POSSIBLY_OFFENSIVE, true)
            set(v) = prefs.setPref(BLOCK_POSSIBLY_OFFENSIVE, v)
        var clipboardContentEnabled: Boolean
            get() =  prefs.getPref(CLIPBOARD_CONTENT_ENABLED, false)
            set(v) = prefs.setPref(CLIPBOARD_CONTENT_ENABLED, v)
        var clipboardContentTimeout: Int
            get() =  prefs.getPref(CLIPBOARD_CONTENT_TIMEOUT, 30)
            set(v) = prefs.setPref(CLIPBOARD_CONTENT_TIMEOUT, v)
        var displayMode: CandidateView.DisplayMode
            get() =  CandidateView.DisplayMode.fromString(prefs.getPref(DISPLAY_MODE, CandidateView.DisplayMode.DYNAMIC_SCROLLABLE.toString()))
            set(v) = prefs.setPref(DISPLAY_MODE, v)
        var enabled: Boolean
            get() =  prefs.getPref(ENABLED, true)
            set(v) = prefs.setPref(ENABLED, v)
        var usePrevWords: Boolean
            get() =  prefs.getPref(USE_PREV_WORDS, true)
            set(v) = prefs.setPref(USE_PREV_WORDS, v)
    }

    /**
     * Wrapper class for theme preferences.
     */
    class Theme(private val prefs: Preferences) {
        companion object {
            const val MODE =                        "theme__mode"
            const val DAY_THEME_REF =               "theme__day_theme_ref"
            const val DAY_THEME_ADAPT_TO_APP =      "theme__day_theme_adapt_to_app"
            const val NIGHT_THEME_REF =             "theme__night_theme_ref"
            const val NIGHT_THEME_ADAPT_TO_APP =    "theme__night_theme_adapt_to_app"
            const val SUNRISE_TIME =                "theme__sunrise_time"
            const val SUNSET_TIME =                 "theme__sunset_time"
        }

        var mode: ThemeMode
            get() =  ThemeMode.fromString(prefs.getPref(MODE, ThemeMode.FOLLOW_SYSTEM.toString()))
            set(v) = prefs.setPref(MODE, v)
        var dayThemeRef: String
            get() =  prefs.getPref(DAY_THEME_REF, "assets:ime/theme/floris_day.json")
            set(v) = prefs.setPref(DAY_THEME_REF, v)
        var dayThemeAdaptToApp: Boolean
            get() =  prefs.getPref(DAY_THEME_ADAPT_TO_APP, false)
            set(v) = prefs.setPref(DAY_THEME_ADAPT_TO_APP, v)
        var nightThemeRef: String
            get() =  prefs.getPref(NIGHT_THEME_REF, "assets:ime/theme/floris_night.json")
            set(v) = prefs.setPref(NIGHT_THEME_REF, v)
        var nightThemeAdaptToApp: Boolean
            get() =  prefs.getPref(NIGHT_THEME_ADAPT_TO_APP, false)
            set(v) = prefs.setPref(NIGHT_THEME_ADAPT_TO_APP, v)
        var sunriseTime: Int
            get() =  prefs.getPref(SUNRISE_TIME, TimeUtil.encode(6, 0))
            set(v) = prefs.setPref(SUNRISE_TIME, v)
        var sunsetTime: Int
            get() =  prefs.getPref(SUNSET_TIME, TimeUtil.encode(18, 0))
            set(v) = prefs.setPref(SUNSET_TIME, v)
    }
}
