package dev.patrickgold.florisboard.ime.mozc

import android.content.res.Configuration

/**
 * Name of keyboard (or keyboard-like-view).
 */
class MozcKeyboardSpecificationName(val baseName: String, val major: Int, val minor: Int, val revision: Int) {

    /**
     * Get formatted keyboard name based on given parameters.
     */
    fun formattedKeyboardName(configuration: Configuration): String {
        return StringBuilder(baseName).append('-')
            .append(major)
            .append('.')
            .append(minor)
            .append('.')
            .append(revision)
            .append('-')
            .append(getDeviceOrientationString(configuration))
            .toString()
    }

    companion object {
        /**
         * Returns *Canonical* orientation string, which is used as a part of keyboard name.
         */
        @Suppress("DEPRECATION")
        fun getDeviceOrientationString(configuration: Configuration): String {
            when (configuration.orientation) {
                Configuration.ORIENTATION_PORTRAIT -> return "PORTRAIT"
                Configuration.ORIENTATION_LANDSCAPE -> return "LANDSCAPE"
                Configuration.ORIENTATION_SQUARE -> return "SQUARE"
                Configuration.ORIENTATION_UNDEFINED -> return "UNDEFINED"
            }
            // If none of above is matched to the orientation, we return "UNKNOWN".
            return "UNKNOWN"
        }
    }
}
