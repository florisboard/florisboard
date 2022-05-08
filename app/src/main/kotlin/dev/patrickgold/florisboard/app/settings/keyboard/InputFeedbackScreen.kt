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
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.ime.input.InputFeedbackController
import dev.patrickgold.florisboard.lib.compose.FlorisScreen
import dev.patrickgold.florisboard.lib.compose.stringRes
import dev.patrickgold.jetpref.datastore.ui.DialogSliderPreference
import dev.patrickgold.jetpref.datastore.ui.ExperimentalJetPrefDatastoreUi
import dev.patrickgold.jetpref.datastore.ui.PreferenceGroup
import dev.patrickgold.jetpref.datastore.ui.SwitchPreference

@OptIn(ExperimentalJetPrefDatastoreUi::class)
@Composable
fun InputFeedbackScreen() = FlorisScreen {
    title = stringRes(R.string.settings__input_feedback__title)
    previewFieldVisible = true

    content {
        PreferenceGroup(title = stringRes(R.string.pref__input_feedback__group_audio__label)) {
            SwitchPreference(
                prefs.inputFeedback.audioEnabled,
                title = stringRes(R.string.pref__input_feedback__audio_enabled__label),
                summary = stringRes(R.string.pref__input_feedback__audio_enabled__summary),
            )
            SwitchPreference(
                prefs.inputFeedback.audioIgnoreSystemSettings,
                title = stringRes(R.string.pref__input_feedback__audio_ignore_system_settings__label),
                summary = stringRes(R.string.pref__input_feedback__audio_ignore_system_settings__summary),
                enabledIf = { prefs.inputFeedback.audioEnabled isEqualTo true },
            )
            DialogSliderPreference(
                prefs.inputFeedback.audioVolume,
                title = stringRes(R.string.pref__input_feedback__audio_volume__label),
                unit = stringRes(R.string.unit__percent__symbol),
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
            SwitchPreference(
                prefs.inputFeedback.hapticEnabled,
                title = stringRes(R.string.pref__input_feedback__haptic_enabled__label),
                summary = stringRes(R.string.pref__input_feedback__haptic_enabled__summary),
            )
            SwitchPreference(
                prefs.inputFeedback.hapticIgnoreSystemSettings,
                title = stringRes(R.string.pref__input_feedback__haptic_ignore_system_settings__label),
                summary = stringRes(R.string.pref__input_feedback__haptic_ignore_system_settings__summary),
                enabledIf = { prefs.inputFeedback.hapticEnabled isEqualTo true },
            )
            SwitchPreference(
                prefs.inputFeedback.hapticUseVibrator,
                title = stringRes(R.string.pref__input_feedback__haptic_use_vibrator__label),
                summary = stringRes(R.string.pref__input_feedback__haptic_use_vibrator__summary),
                enabledIf = { prefs.inputFeedback.hapticEnabled isEqualTo true },
            )
            DialogSliderPreference(
                prefs.inputFeedback.hapticVibrationDuration,
                title = stringRes(R.string.pref__input_feedback__haptic_vibration_duration__label),
                unit = stringRes(R.string.unit__milliseconds__symbol),
                min = 1,
                max = 100,
                stepIncrement = 1,
                enabledIf = { prefs.inputFeedback.hapticEnabled isEqualTo true && prefs.inputFeedback.hapticUseVibrator isEqualTo true },
            )
            DialogSliderPreference(
                prefs.inputFeedback.hapticVibrationStrength,
                title = stringRes(R.string.pref__input_feedback__haptic_vibration_strength__label),
                summary = InputFeedbackController.generateVibrationStrengthErrorSummary() ?:
                stringRes(R.string.unit__percent__symbol),
                unit = stringRes(R.string.unit__percent__symbol),
                min = 1,
                max = 100,
                stepIncrement = 1,
                enabledIf = { prefs.inputFeedback.hapticEnabled isEqualTo true && prefs.inputFeedback.hapticUseVibrator isEqualTo true && InputFeedbackController.hasAmplitudeControl() },
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
