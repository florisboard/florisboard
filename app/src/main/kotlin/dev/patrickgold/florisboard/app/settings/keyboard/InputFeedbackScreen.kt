/*
 * Copyright (C) 2021-2026 The FlorisBoard Contributors
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

import androidx.compose.material3.ListItemDefaults
import androidx.compose.runtime.Composable
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.app.FlorisPreferenceStore
import dev.patrickgold.florisboard.app.components.ScrollableScreenColumn
import dev.patrickgold.florisboard.app.components.SegmentedListColumn
import dev.patrickgold.florisboard.lib.compose.FlorisScreenNg
import dev.patrickgold.jetpref.datastore.ui.DialogSliderPreference
import dev.patrickgold.jetpref.datastore.ui.ExperimentalJetPrefDatastoreUi
import dev.patrickgold.jetpref.datastore.ui.SwitchPreference
import org.florisboard.lib.compose.stringRes

@OptIn(ExperimentalJetPrefDatastoreUi::class)
@Composable
fun InputFeedbackScreen() {
    val prefs by FlorisPreferenceStore

    FlorisScreenNg(
        title = stringRes(R.string.settings__input_feedback__title),
        previewFieldVisible = true,
    ) { contentPadding ->
        ScrollableScreenColumn(contentPadding) {
            SegmentedListColumn(title = stringRes(R.string.pref__input_feedback__group_audio__label)) {
                val count = 7
                SwitchPreference(
                    prefs.inputFeedback.audioEnabled,
                    shapes = ListItemDefaults.segmentedShapes(0, count),
                    title = stringRes(R.string.pref__input_feedback__audio_enabled__label),
                    summaryOn = stringRes(R.string.enum__input_feedback_activation_mode__audio_respect_system_settings),
                    summaryOff = stringRes(R.string.pref__input_feedback__audio_enabled__summary_disabled),
                )
                DialogSliderPreference(
                    prefs.inputFeedback.audioVolume,
                    shapes = ListItemDefaults.segmentedShapes(1, count),
                    title = stringRes(R.string.pref__input_feedback__audio_volume__label),
                    valueLabel = { stringRes(R.string.unit__percent__symbol, "v" to it) },
                    min = 1,
                    max = 100,
                    stepIncrement = 1,
                    enabledIf = { prefs.inputFeedback.audioEnabled isEqualTo true },
                )
                SwitchPreference(
                    prefs.inputFeedback.audioFeatKeyPress,
                    shapes = ListItemDefaults.segmentedShapes(2, count),
                    title = stringRes(R.string.pref__input_feedback__audio_feat_key_press__label),
                    summary = stringRes(R.string.pref__input_feedback__any_feat_key_press__summary),
                    enabledIf = { prefs.inputFeedback.audioEnabled isEqualTo true },
                )
                SwitchPreference(
                    prefs.inputFeedback.audioFeatKeyLongPress,
                    shapes = ListItemDefaults.segmentedShapes(3, count),
                    title = stringRes(R.string.pref__input_feedback__audio_feat_key_long_press__label),
                    summary = stringRes(R.string.pref__input_feedback__any_feat_key_long_press__summary),
                    enabledIf = { prefs.inputFeedback.audioEnabled isEqualTo true },
                )
                SwitchPreference(
                    prefs.inputFeedback.audioFeatKeyRepeatedAction,
                    shapes = ListItemDefaults.segmentedShapes(4, count),
                    title = stringRes(R.string.pref__input_feedback__audio_feat_key_repeated_action__label),
                    summary = stringRes(R.string.pref__input_feedback__any_feat_key_repeated_action__summary),
                    enabledIf = { prefs.inputFeedback.audioEnabled isEqualTo true },
                )
                SwitchPreference(
                    prefs.inputFeedback.audioFeatGestureSwipe,
                    shapes = ListItemDefaults.segmentedShapes(5, count),
                    title = stringRes(R.string.pref__input_feedback__audio_feat_gesture_swipe__label),
                    summary = stringRes(R.string.pref__input_feedback__any_feat_gesture_swipe__summary),
                    enabledIf = { prefs.inputFeedback.audioEnabled isEqualTo true },
                )
                SwitchPreference(
                    prefs.inputFeedback.audioFeatGestureMovingSwipe,
                    shapes = ListItemDefaults.segmentedShapes(6, count),
                    title = stringRes(R.string.pref__input_feedback__audio_feat_gesture_moving_swipe__label),
                    summary = stringRes(R.string.pref__input_feedback__audio_feat_gesture_moving_swipe__label),
                    enabledIf = { prefs.inputFeedback.audioEnabled isEqualTo true },
                )
            }

            SegmentedListColumn(title = stringRes(R.string.pref__input_feedback__group_haptic__label)) {
                val count = 6
                SwitchPreference(
                    prefs.inputFeedback.hapticEnabled,
                    shapes = ListItemDefaults.segmentedShapes(0, count),
                    title = stringRes(R.string.pref__input_feedback__haptic_enabled__label),
                    summaryOn = stringRes(R.string.enum__input_feedback_activation_mode__haptic_respect_system_settings),
                    summaryOff = stringRes(R.string.pref__input_feedback__haptic_enabled__summary_disabled),
                )
                SwitchPreference(
                    prefs.inputFeedback.hapticFeatKeyPress,
                    shapes = ListItemDefaults.segmentedShapes(1, count),
                    title = stringRes(R.string.pref__input_feedback__haptic_feat_key_press__label),
                    summary = stringRes(R.string.pref__input_feedback__any_feat_key_press__summary),
                    enabledIf = { prefs.inputFeedback.hapticEnabled isEqualTo true },
                )
                SwitchPreference(
                    prefs.inputFeedback.hapticFeatKeyLongPress,
                    shapes = ListItemDefaults.segmentedShapes(2, count),
                    title = stringRes(R.string.pref__input_feedback__haptic_feat_key_long_press__label),
                    summary = stringRes(R.string.pref__input_feedback__any_feat_key_long_press__summary),
                    enabledIf = { prefs.inputFeedback.hapticEnabled isEqualTo true },
                )
                SwitchPreference(
                    prefs.inputFeedback.hapticFeatKeyRepeatedAction,
                    shapes = ListItemDefaults.segmentedShapes(3, count),
                    title = stringRes(R.string.pref__input_feedback__haptic_feat_key_repeated_action__label),
                    summary = stringRes(R.string.pref__input_feedback__any_feat_key_repeated_action__summary),
                    enabledIf = { prefs.inputFeedback.hapticEnabled isEqualTo true },
                )
                SwitchPreference(
                    prefs.inputFeedback.hapticFeatGestureSwipe,
                    shapes = ListItemDefaults.segmentedShapes(4, count),
                    title = stringRes(R.string.pref__input_feedback__haptic_feat_gesture_swipe__label),
                    summary = stringRes(R.string.pref__input_feedback__any_feat_gesture_swipe__summary),
                    enabledIf = { prefs.inputFeedback.hapticEnabled isEqualTo true },
                )
                SwitchPreference(
                    prefs.inputFeedback.hapticFeatGestureMovingSwipe,
                    shapes = ListItemDefaults.segmentedShapes(5, count),
                    title = stringRes(R.string.pref__input_feedback__haptic_feat_gesture_moving_swipe__label),
                    summary = stringRes(R.string.pref__input_feedback__audio_feat_gesture_moving_swipe__label),
                    enabledIf = { prefs.inputFeedback.hapticEnabled isEqualTo true },
                )
            }
        }
    }
}
