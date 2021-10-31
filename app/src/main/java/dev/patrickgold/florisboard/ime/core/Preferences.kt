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
import dev.patrickgold.florisboard.ime.text.smartbar.CandidateView

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

    val correction = Correction(this)
    val dictionary = Dictionary(this)
    val localization = Localization(this)
    val smartbar = Smartbar(this)
    val suggestion = Suggestion(this)


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
}
