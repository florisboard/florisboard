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

package dev.patrickgold.florisboard.settings.fragments

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.Vibrator
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.ime.core.Preferences

class InputFeedbackFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.prefs_input_feedback)

        val vibrator = context?.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        if (vibrator != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (!vibrator.hasAmplitudeControl()) {
                    findPreference<Preference>(Preferences.InputFeedback.HAPTIC_VIBRATION_STRENGTH)?.let {
                        it.isEnabled = false
                        it.summary = resources.getString(R.string.pref__input_feedback__haptic_vibration_strength__summary_no_amplitude_ctrl)
                    }
                }
            } else {
                findPreference<Preference>(Preferences.InputFeedback.HAPTIC_VIBRATION_STRENGTH)?.let {
                    it.isEnabled = false
                    it.summary = resources.getString(R.string.pref__input_feedback__haptic_vibration_strength__summary_unsupported_android_version)
                }
            }
        }
    }
}
