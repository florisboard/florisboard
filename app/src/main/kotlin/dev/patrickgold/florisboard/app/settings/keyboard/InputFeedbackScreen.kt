/*
 * Copyright (C) 2021 Patrick Goldinger
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

package dev.patrickgold.florisboard.app.settings.keyboard

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.app.enumDisplayEntriesOf
import dev.patrickgold.florisboard.ime.input.InputFeedbackActivationMode
import dev.patrickgold.florisboard.ime.input.HapticVibrationMode
import org.florisboard.lib.android.AndroidVersion
import org.florisboard.lib.android.systemVibratorOrNull
import org.florisboard.lib.android.vibrate
import dev.patrickgold.florisboard.lib.compose.FlorisScreen
import dev.patrickgold.florisboard.lib.compose.stringRes
import dev.patrickgold.jetpref.datastore.ui.DialogSliderPreference
import dev.patrickgold.jetpref.datastore.ui.ExperimentalJetPrefDatastoreUi
import dev.patrickgold.jetpref.datastore.ui.ListPreference
import dev.patrickgold.jetpref.datastore.ui.PreferenceGroup
import dev.patrickgold.jetpref.datastore.ui.SwitchPreference

@OptIn(ExperimentalJetPrefDatastoreUi::class)
@Composable
fun InputFeedbackScreen() = FlorisScreen {
    title = stringRes(R.string.settings__input_feedback__title)
    previewFieldVisible = true
    iconSpaceReserved = false

    val context = LocalContext.current
    val vibrator = context.systemVibratorOrNull()

    content {
        PreferenceGroup(title = stringRes(R.string.pref__input_feedback__group_audio__label)) {
            ListPreference(
                listPref = prefs.inputFeedback.audioActivationMode,
                switchPref = prefs.inputFeedback.audioEnabled,
                title = stringRes(R.string.pref__input_feedback__audio_enabled__label),
                summarySwitchDisabled = stringRes(R.string.pref__input_feedback__audio_enabled__summary_disabled),
                entries = enumDisplayEntriesOf(InputFeedbackActivationMode::class, "audio"),
            )
            DialogSliderPreference(
                prefs.inputFeedback.audioVolume,
                title = stringRes(R.string.pref__input_feedback__audio_volume__label),
                valueLabel = { stringRes(R.string.unit__percent__symbol, "v" to it) },
                min = 1,
                max = 100,
                stepIncrement = 1,
                enabledIf = { prefs.inputFeedback.audioEnabled isEqualTo true },
            )
            SwitchPreference(
                prefs.inputFeedback.audioFeatKeyPress,
                title = stringRes(R.string.pref__input_feedback__audio_feat_key_press__label),
                summary = stringRes(R.string.pref__input_feedback__any_feat_key_press__summary),
                enabledIf = { prefs.inputFeedback.audioEnabled isEqualTo true },
            )
            SwitchPreference(
                prefs.inputFeedback.audioFeatKeyLongPress,
                title = stringRes(R.string.pref__input_feedback__audio_feat_key_long_press__label),
                summary = stringRes(R.string.pref__input_feedback__any_feat_key_long_press__summary),
                enabledIf = { prefs.inputFeedback.audioEnabled isEqualTo true },
            )
            SwitchPreference(
                prefs.inputFeedback.audioFeatKeyRepeatedAction,
                title = stringRes(R.string.pref__input_feedback__audio_feat_key_repeated_action__label),
                summary = stringRes(R.string.pref__input_feedback__any_feat_key_repeated_action__summary),
                enabledIf = { prefs.inputFeedback.audioEnabled isEqualTo true },
            )
            SwitchPreference(
                prefs.inputFeedback.audioFeatGestureSwipe,
                title = stringRes(R.string.pref__input_feedback__audio_feat_gesture_swipe__label),
                summary = stringRes(R.string.pref__input_feedback__any_feat_gesture_swipe__summary),
                enabledIf = { prefs.inputFeedback.audioEnabled isEqualTo true },
            )
            SwitchPreference(
                prefs.inputFeedback.audioFeatGestureMovingSwipe,
                title = stringRes(R.string.pref__input_feedback__audio_feat_gesture_moving_swipe__label),
                summary = stringRes(R.string.pref__input_feedback__audio_feat_gesture_moving_swipe__label),
                enabledIf = { prefs.inputFeedback.audioEnabled isEqualTo true },
            )
        }

        PreferenceGroup(title = stringRes(R.string.pref__input_feedback__group_haptic__label)) {
            ListPreference(
                listPref = prefs.inputFeedback.hapticActivationMode,
                switchPref = prefs.inputFeedback.hapticEnabled,
                title = stringRes(R.string.pref__input_feedback__haptic_enabled__label),
                summarySwitchDisabled = stringRes(R.string.pref__input_feedback__haptic_enabled__summary_disabled),
                entries = enumDisplayEntriesOf(InputFeedbackActivationMode::class, "haptic")
            )
            ListPreference(
                prefs.inputFeedback.hapticVibrationMode,
                title = stringRes(R.string.pref__input_feedback__haptic_vibration_mode__label),
                enabledIf = { prefs.inputFeedback.hapticEnabled isEqualTo true },
                entries = enumDisplayEntriesOf(HapticVibrationMode::class),
            )
            DialogSliderPreference(
                prefs.inputFeedback.hapticVibrationDuration,
                title = stringRes(R.string.pref__input_feedback__haptic_vibration_duration__label),
                valueLabel = { stringRes(R.string.unit__milliseconds__symbol, "v" to it) },
                summary = {
                    if (vibrator == null || !vibrator.hasVibrator()) {
                        stringRes(R.string.pref__input_feedback__haptic_vibration_strength__summary_no_vibrator)
                    } else {
                        stringRes(R.string.unit__milliseconds__symbol, "v" to it)
                    }
                },
                min = 1,
                max = 100,
                stepIncrement = 1,
                onPreviewSelectedValue = { duration ->
                    val strength = prefs.inputFeedback.hapticVibrationStrength.get()
                    vibrator?.vibrate(duration, strength)
                },
                enabledIf = {
                    prefs.inputFeedback.hapticEnabled isEqualTo true &&
                        prefs.inputFeedback.hapticVibrationMode isEqualTo HapticVibrationMode.USE_VIBRATOR_DIRECTLY &&
                        vibrator != null && vibrator.hasVibrator()
                },
            )
            DialogSliderPreference(
                prefs.inputFeedback.hapticVibrationStrength,
                title = stringRes(R.string.pref__input_feedback__haptic_vibration_strength__label),
                valueLabel = { stringRes(R.string.unit__percent__symbol, "v" to it) },
                summary = { strength ->
                    if (vibrator == null || !vibrator.hasVibrator()) {
                        stringRes(R.string.pref__input_feedback__haptic_vibration_strength__summary_no_vibrator)
                    } else if (AndroidVersion.ATMOST_API25_N_MR1) {
                        stringRes(R.string.pref__input_feedback__haptic_vibration_strength__summary_unsupported_android_version)
                    } else if (!vibrator.hasAmplitudeControl()) {
                        stringRes(R.string.pref__input_feedback__haptic_vibration_strength__summary_no_amplitude_ctrl)
                    } else {
                        stringRes(R.string.unit__percent__symbol, "v" to strength)
                    }
                },
                min = 1,
                max = 100,
                stepIncrement = 1,
                onPreviewSelectedValue = { strength ->
                    val duration = prefs.inputFeedback.hapticVibrationDuration.get()
                    vibrator?.vibrate(duration, strength)
                },
                enabledIf = {
                    prefs.inputFeedback.hapticEnabled isEqualTo true &&
                        prefs.inputFeedback.hapticVibrationMode isEqualTo HapticVibrationMode.USE_VIBRATOR_DIRECTLY &&
                        vibrator != null && vibrator.hasVibrator() &&
                        AndroidVersion.ATLEAST_API26_O && vibrator.hasAmplitudeControl()
                },
            )
            SwitchPreference(
                prefs.inputFeedback.hapticFeatKeyPress,
                title = stringRes(R.string.pref__input_feedback__haptic_feat_key_press__label),
                summary = stringRes(R.string.pref__input_feedback__any_feat_key_press__summary),
                enabledIf = { prefs.inputFeedback.hapticEnabled isEqualTo true },
            )
            SwitchPreference(
                prefs.inputFeedback.hapticFeatKeyLongPress,
                title = stringRes(R.string.pref__input_feedback__haptic_feat_key_long_press__label),
                summary = stringRes(R.string.pref__input_feedback__any_feat_key_long_press__summary),
                enabledIf = { prefs.inputFeedback.hapticEnabled isEqualTo true },
            )
            SwitchPreference(
                prefs.inputFeedback.hapticFeatKeyRepeatedAction,
                title = stringRes(R.string.pref__input_feedback__haptic_feat_key_repeated_action__label),
                summary = stringRes(R.string.pref__input_feedback__any_feat_key_repeated_action__summary),
                enabledIf = { prefs.inputFeedback.hapticEnabled isEqualTo true },
            )
            SwitchPreference(
                prefs.inputFeedback.hapticFeatGestureSwipe,
                title = stringRes(R.string.pref__input_feedback__haptic_feat_gesture_swipe__label),
                summary = stringRes(R.string.pref__input_feedback__any_feat_gesture_swipe__summary),
                enabledIf = { prefs.inputFeedback.hapticEnabled isEqualTo true },
            )
            SwitchPreference(
                prefs.inputFeedback.hapticFeatGestureMovingSwipe,
                title = stringRes(R.string.pref__input_feedback__haptic_feat_gesture_moving_swipe__label),
                summary = stringRes(R.string.pref__input_feedback__audio_feat_gesture_moving_swipe__label),
                enabledIf = { prefs.inputFeedback.hapticEnabled isEqualTo true },
            )
        }
    }
}
