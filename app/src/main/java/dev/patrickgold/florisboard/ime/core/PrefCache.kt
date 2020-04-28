package dev.patrickgold.florisboard.ime.core

import android.content.Context
import android.content.SharedPreferences
import android.provider.Settings

class PrefCache(
    private val context: Context,
    val shared: SharedPreferences
) {
    var heightFactor: String = ""
        private set
    var longPressDelay: Int = 0
        private set
    var soundEnabled: Boolean = false
        private set
    var soundEnabledSystem: Boolean = false
        private set
    var soundVolume: Int = 0
        private set
    var vibrationEnabled: Boolean = false
        private set
    var vibrationEnabledSystem: Boolean = false
        private set
    var vibrationStrength: Int = 0
        private set

    fun sync() {
        val contentResolver = context.contentResolver

        heightFactor = shared.getString("keyboard__height_factor", "") ?: ""

        longPressDelay = shared.getInt("keyboard__long_press_delay", 0)

        soundEnabled = shared.getBoolean("keyboard__sound_enabled", false)
        soundEnabledSystem = Settings.System.getInt(
            contentResolver, Settings.System.SOUND_EFFECTS_ENABLED, 0
        ) != 0
        soundVolume = shared.getInt("keyboard__sound_volume", 0)

        vibrationEnabled = shared.getBoolean("keyboard__vibration_enabled", false)
        vibrationEnabledSystem = Settings.System.getInt(
            contentResolver, Settings.System.HAPTIC_FEEDBACK_ENABLED, 0
        ) != 0
        vibrationStrength = shared.getInt("keyboard__vibration_strength", 0)
    }
}