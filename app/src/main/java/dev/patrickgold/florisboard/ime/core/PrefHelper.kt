package dev.patrickgold.florisboard.ime.core

import android.content.Context
import android.content.SharedPreferences
import android.provider.Settings

class PrefHelper(
    private val context: Context,
    private val shared: SharedPreferences
) {
    val advanced = Advanced(this)
    val looknfeel = Looknfeel(this)

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

    fun sync() {
        val contentResolver = context.contentResolver

        looknfeel.soundEnabledSystem = Settings.System.getInt(
            contentResolver, Settings.System.SOUND_EFFECTS_ENABLED, 0
        ) != 0

        looknfeel.vibrationEnabledSystem = Settings.System.getInt(
            contentResolver, Settings.System.HAPTIC_FEEDBACK_ENABLED, 0
        ) != 0
    }

    class Advanced(private val prefHelper: PrefHelper) {
        companion object {
            const val SETTINGS_THEME =          "advanced__settings_theme"
            const val SHOW_APP_ICON =           "advanced__show_app_icon"
        }

        var settingsTheme: String = ""
            get() = prefHelper.getPref(SETTINGS_THEME, "auto")
            private set
        var longPressDelay: Boolean = false
            get() = prefHelper.getPref(SHOW_APP_ICON, true)
            private set
    }

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
}