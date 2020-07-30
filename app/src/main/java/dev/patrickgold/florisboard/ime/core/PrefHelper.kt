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
    val internal = Internal(this)
    val keyboard = Keyboard(this)
    val looknfeel = Looknfeel(this)
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

    /**
     * Tells the [PreferenceManager] to set the defined preferences to their default values, if
     * they have not been initialized yet.
     */
    fun initDefaultPreferences() {
        PreferenceManager.setDefaultValues(context, R.xml.prefs_advanced, true)
        PreferenceManager.setDefaultValues(context, R.xml.prefs_keyboard, true)
        PreferenceManager.setDefaultValues(context, R.xml.prefs_looknfeel, true)
        PreferenceManager.setDefaultValues(context, R.xml.prefs_theme, true)
        //setPref(Keyboard.SUBTYPES, "")
        //setPref(Internal.IS_IME_SET_UP, false)
    }

    /**
     * Syncs the system preference values and clears the cache.
     */
    fun sync() {
        val contentResolver = context.contentResolver
        looknfeel.soundEnabledSystem = Settings.System.getInt(
            contentResolver, Settings.System.SOUND_EFFECTS_ENABLED, 0
        ) != 0
        looknfeel.vibrationEnabledSystem = Settings.System.getInt(
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
            const val DOUBLE_SPACE_PERIOD =     "correction__double_space_period"
        }

        var doubleSpacePeriod: Boolean = false
            get() = prefHelper.getPref(DOUBLE_SPACE_PERIOD, true)
            private set
    }

    /**
     * Wrapper class for internal preferences.
     */
    class Internal(private val prefHelper: PrefHelper) {
        companion object {
            const val IS_IME_SET_UP =           "internal__is_ime_set_up"
            const val VERSION_ON_INSTALL =      "internal__version_on_install"
            const val VERSION_LAST_USE =        "internal__version_last_use"
            const val VERSION_LAST_CHANGELOG =  "internal__version_last_changelog"
        }

        var isImeSetUp: Boolean
            get() = prefHelper.getPref(IS_IME_SET_UP, false)
            set(value) = prefHelper.setPref(IS_IME_SET_UP, value)
        var versionOnInstall: String
            get() = prefHelper.getPref(VERSION_ON_INSTALL, VersionName.DEFAULT_RAW)
            set(value) = prefHelper.setPref(VERSION_ON_INSTALL, value)
        var versionLastUse: String
            get() = prefHelper.getPref(VERSION_LAST_USE, VersionName.DEFAULT_RAW)
            set(value) = prefHelper.setPref(VERSION_LAST_USE, value)
        var versionLastChangelog: String
            get() = prefHelper.getPref(VERSION_LAST_CHANGELOG, VersionName.DEFAULT_RAW)
            set(value) = prefHelper.setPref(VERSION_LAST_CHANGELOG, value)
    }

    /**
     * Wrapper class for keyboard preferences.
     */
    class Keyboard(private val prefHelper: PrefHelper) {
        companion object {
            const val ACTIVE_SUBTYPE_ID =       "keyboard__active_subtype_id"
            const val SUBTYPES =                "keyboard__subtypes"
        }

        var activeSubtypeId: Int
            get() = prefHelper.getPref(ACTIVE_SUBTYPE_ID, -1)
            set(v) = prefHelper.setPref(ACTIVE_SUBTYPE_ID, v)
        var subtypes: String
            get() = prefHelper.getPref(SUBTYPES, "")
            set(v) = prefHelper.setPref(SUBTYPES, v)
    }

    /**
     * Wrapper class for looknfeel preferences.
     */
    class Looknfeel(private val prefHelper: PrefHelper) {
        companion object {
            const val HEIGHT_FACTOR =           "looknfeel__height_factor"
            const val LONG_PRESS_DELAY =        "looknfeel__long_press_delay"
            const val ONE_HANDED_MODE =         "looknfeel__one_handed_mode"
            const val SOUND_ENABLED =           "looknfeel__sound_enabled"
            const val SOUND_VOLUME =            "looknfeel__sound_volume"
            const val VIBRATION_ENABLED =       "looknfeel__vibration_enabled"
            const val VIBRATION_STRENGTH =      "looknfeel__vibration_strength"
        }

        var heightFactor: String = ""
            get() = prefHelper.getPref(HEIGHT_FACTOR, "normal")
            private set
        var longPressDelay: Int = 0
            get() = prefHelper.getPref(LONG_PRESS_DELAY, 300)
            private set
        var oneHandedMode: String
            get() = prefHelper.getPref(ONE_HANDED_MODE, "off")
            set(value) = prefHelper.setPref(ONE_HANDED_MODE, value)
        var soundEnabled: Boolean = false
            get() = prefHelper.getPref(SOUND_ENABLED, true)
            private set
        var soundEnabledSystem: Boolean = false
        var soundVolume: Int = 0
            get() = prefHelper.getPref(SOUND_VOLUME, 0)
            private set
        var vibrationEnabled: Boolean = false
            get() = prefHelper.getPref(VIBRATION_ENABLED, true)
            private set
        var vibrationEnabledSystem: Boolean = false
        var vibrationStrength: Int = 0
            get() = prefHelper.getPref(VIBRATION_STRENGTH, 0)
            private set
    }

    /**
     * Wrapper class for suggestion preferences.
     */
    class Suggestion(private val prefHelper: PrefHelper) {
        companion object {
            const val ENABLED =                 "suggestion__enabled"
            const val USE_PREV_WORDS =          "suggestion__use_prev_words"
        }

        var enabled: Boolean = false
            get() = prefHelper.getPref(ENABLED, true)
            private set
        var usePrevWords: Boolean = false
            get() = prefHelper.getPref(USE_PREV_WORDS, true)
            private set
    }

    /**
     * Wrapper class for theme preferences.
     */
    class Theme(private val prefHelper: PrefHelper) {
        companion object {
            const val NAME =                    "theme__name"
        }

        var name: String = ""
            get() = prefHelper.getPref(NAME, "floris_light")
            private set
        fun getSelectedThemeResId(): Int {
            return when (name) {
                "floris_light" -> R.style.KeyboardTheme_FlorisLight
                "floris_dark" -> R.style.KeyboardTheme_FlorisDark
                else -> R.style.KeyboardTheme_FlorisLight
            }
        }
    }
}
