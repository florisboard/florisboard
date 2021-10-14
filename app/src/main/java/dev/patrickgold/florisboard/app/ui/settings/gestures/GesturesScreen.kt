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

package dev.patrickgold.florisboard.app.ui.settings.gestures

import androidx.compose.runtime.Composable
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.app.res.stringRes
import dev.patrickgold.florisboard.app.ui.components.FlorisScreen
import dev.patrickgold.florisboard.ime.text.gestures.SwipeAction
import dev.patrickgold.jetpref.ui.compose.DialogSliderPreference
import dev.patrickgold.jetpref.ui.compose.ListPreference
import dev.patrickgold.jetpref.ui.compose.PreferenceGroup
import dev.patrickgold.jetpref.ui.compose.SwitchPreference
import dev.patrickgold.jetpref.ui.compose.annotations.ExperimentalJetPrefUi

@OptIn(ExperimentalJetPrefUi::class)
@Composable
fun GesturesScreen() = FlorisScreen(title = stringRes(R.string.settings__gestures__title)) {
    PreferenceGroup(title = stringRes(R.string.pref__glide__title)) {
        SwitchPreference(
            prefs.glide.enabled,
            title = stringRes(R.string.pref__glide__enabled__label),
            summary = stringRes(R.string.pref__glide__enabled__summary),
        )
        SwitchPreference(
            prefs.glide.showTrail,
            title = stringRes(R.string.pref__glide__show_trail__label),
            summary = stringRes(R.string.pref__glide__show_trail__summary),
            enabledIf = { prefs.glide.enabled isEqualTo true },
        )
        DialogSliderPreference(
            prefs.glide.trailDuration,
            title = stringRes(R.string.pref__glide_trail_fade_duration),
            unit = stringRes(R.string.unit__milliseconds__symbol),
            min = 0,
            max = 500,
            stepIncrement = 10,
            enabledIf = { prefs.glide.enabled isEqualTo true && prefs.glide.showTrail isEqualTo true },
        )
        SwitchPreference(
            prefs.glide.showPreview,
            title = stringRes(R.string.pref__glide__show_preview),
            enabledIf = { prefs.glide.enabled isEqualTo true },
        )
        DialogSliderPreference(
            prefs.glide.previewRefreshDelay,
            title = stringRes(R.string.pref__glide_preview_refresh_delay),
            unit = stringRes(R.string.unit__milliseconds__symbol),
            min = 50,
            max = 500,
            stepIncrement = 25,
            enabledIf = { prefs.glide.enabled isEqualTo true && prefs.glide.showPreview isEqualTo true },
        )
    }

    PreferenceGroup(title = stringRes(R.string.pref__gestures__general_title)) {
        ListPreference(
            prefs.gestures.swipeUp,
            title = stringRes(R.string.pref__gestures__swipe_up__label),
            entries = SwipeAction.generalListEntries(),
            enabledIf = { prefs.glide.enabled isEqualTo false },
        )
        ListPreference(
            prefs.gestures.swipeDown,
            title = stringRes(R.string.pref__gestures__swipe_down__label),
            entries = SwipeAction.generalListEntries(),
            enabledIf = { prefs.glide.enabled isEqualTo false },
        )
        ListPreference(
            prefs.gestures.swipeLeft,
            title = stringRes(R.string.pref__gestures__swipe_left__label),
            entries = SwipeAction.generalListEntries(),
            enabledIf = { prefs.glide.enabled isEqualTo false },
        )
        ListPreference(
            prefs.gestures.swipeRight,
            title = stringRes(R.string.pref__gestures__swipe_right__label),
            entries = SwipeAction.generalListEntries(),
            enabledIf = { prefs.glide.enabled isEqualTo false },
        )
    }

    PreferenceGroup(title = stringRes(R.string.pref__gestures__space_bar_title)) {
        ListPreference(
            prefs.gestures.spaceBarSwipeUp,
            title = stringRes(R.string.pref__gestures__space_bar_swipe_up__label),
            entries = SwipeAction.generalListEntries(),
        )
        ListPreference(
            prefs.gestures.spaceBarSwipeLeft,
            title = stringRes(R.string.pref__gestures__space_bar_swipe_left__label),
            entries = SwipeAction.generalListEntries(),
        )
        ListPreference(
            prefs.gestures.spaceBarSwipeRight,
            title = stringRes(R.string.pref__gestures__space_bar_swipe_right__label),
            entries = SwipeAction.generalListEntries(),
        )
        ListPreference(
            prefs.gestures.spaceBarLongPress,
            title = stringRes(R.string.pref__gestures__space_bar_long_press__label),
            entries = SwipeAction.generalListEntries(),
        )
    }

    PreferenceGroup(title = stringRes(R.string.pref__gestures__other_title)) {
        ListPreference(
            prefs.gestures.deleteKeySwipeLeft,
            title = stringRes(R.string.pref__gestures__delete_key_swipe_left__label),
            entries = SwipeAction.deleteSwipeListEntries(),
        )
        DialogSliderPreference(
            prefs.gestures.swipeVelocityThreshold,
            title = stringRes(R.string.pref__gestures__swipe_velocity_threshold__label),
            unit = stringRes(R.string.unit__display_pixel_per_seconds__symbol),
            min = 400,
            max = 4000,
            stepIncrement = 100,
        )
        DialogSliderPreference(
            prefs.gestures.swipeDistanceThreshold,
            title = stringRes(R.string.pref__gestures__swipe_distance_threshold__label),
            unit = stringRes(R.string.unit__display_pixel__symbol),
            min = 12,
            max = 48,
            stepIncrement = 1,
        )
    }
}
