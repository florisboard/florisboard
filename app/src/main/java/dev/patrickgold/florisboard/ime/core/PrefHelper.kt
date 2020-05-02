package dev.patrickgold.florisboard.ime.core

import android.content.Context
import android.content.SharedPreferences
import android.provider.Settings

class PrefHelper(
    private val context: Context,
    val shared: SharedPreferences
) {
    companion object {
        const val HEIGHT_FACTOR =           "keyboard__height_factor"
        const val LONG_PRESS_DELAY =        "keyboard__long_press_delay"
        const val ONE_HANDED_MODE =         "keyboard__one_handed_mode"
        const val SOUND_ENABLED =           "keyboard__sound_enabled"
        const val SOUND_VOLUME =            "keyboard__sound_volume"
        const val VIBRATION_ENABLED =       "keyboard__vibration_enabled"
        const val VIBRATION_STRENGTH =      "keyboard__vibration_strength"
    }

    var heightFactor: String = ""
        get() = getPref(HEIGHT_FACTOR, "normal")
        private set
    var longPressDelay: Int = 0
        get() = getPref(LONG_PRESS_DELAY, 300)
        private set
    var oneHandedMode: String
        get() = getPref(ONE_HANDED_MODE, "off")
        set(value) = setPref(ONE_HANDED_MODE, value)
    var soundEnabled: Boolean = false
        get() = getPref(SOUND_ENABLED, true)
        private set
    var soundEnabledSystem: Boolean = false
        get() = getPref(SOUND_ENABLED, true)
        private set
    var soundVolume: Int = 0
        get() = getPref(SOUND_VOLUME, 0)
        private set
    var vibrationEnabled: Boolean = false
        get() = getPref(VIBRATION_ENABLED, true)
        private set
    var vibrationEnabledSystem: Boolean = false
        private set
    var vibrationStrength: Int = 0
        get() = getPref(VIBRATION_STRENGTH, 0)
        private set

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

        soundEnabledSystem = Settings.System.getInt(
            contentResolver, Settings.System.SOUND_EFFECTS_ENABLED, 0
        ) != 0

        vibrationEnabledSystem = Settings.System.getInt(
            contentResolver, Settings.System.HAPTIC_FEEDBACK_ENABLED, 0
        ) != 0
    }
}