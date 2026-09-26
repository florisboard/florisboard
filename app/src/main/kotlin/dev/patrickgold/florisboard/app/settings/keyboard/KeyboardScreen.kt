/*
 * Copyright (C) 2021-2025 The FlorisBoard Contributors
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

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.app.LocalNavController
import dev.patrickgold.florisboard.app.Routes
import dev.patrickgold.florisboard.app.enumDisplayEntriesOf
import dev.patrickgold.florisboard.ime.input.CapitalizationBehavior
import dev.patrickgold.florisboard.ime.keyboard.SpaceBarMode
import dev.patrickgold.florisboard.ime.keyboard3.hint.FlickKeyHintPlacement
import dev.patrickgold.florisboard.ime.keyboard3.hint.LongPressKeyHintPlacement
import dev.patrickgold.florisboard.ime.keyboard3.interaction.LocalInteractionController
import dev.patrickgold.florisboard.ime.landscapeinput.LandscapeInputUiMode
import dev.patrickgold.florisboard.ime.smartbar.IncognitoDisplayMode
import dev.patrickgold.florisboard.lib.compose.FlorisScreen
import dev.patrickgold.jetpref.datastore.model.PreferenceData
import dev.patrickgold.jetpref.datastore.model.collectAsState
import dev.patrickgold.jetpref.datastore.ui.DialogSliderPreference
import dev.patrickgold.jetpref.datastore.ui.DialogSliderPreferenceDefaults
import dev.patrickgold.jetpref.datastore.ui.ExperimentalJetPrefDatastoreUi
import dev.patrickgold.jetpref.datastore.ui.ListPreference
import dev.patrickgold.jetpref.datastore.ui.LocalDefaultDialogPrefStrings
import dev.patrickgold.jetpref.datastore.ui.Preference
import dev.patrickgold.jetpref.datastore.ui.PreferenceGroup
import dev.patrickgold.jetpref.datastore.ui.SwitchPreference
import dev.patrickgold.jetpref.material.ui.JetPrefAlertDialog
import dev.patrickgold.jetpref.material.ui.JetPrefAlertDialogDefaults
import kotlinx.coroutines.launch
import org.florisboard.lib.compose.stringRes
import kotlin.math.round
import kotlin.math.roundToInt
import kotlin.time.Duration

@OptIn(ExperimentalJetPrefDatastoreUi::class)
@Composable
fun KeyboardScreen() = FlorisScreen {
    title = stringRes(R.string.settings__keyboard__title)
    previewFieldVisible = true

    val interactionController = LocalInteractionController.current
    val navController = LocalNavController.current

    content {
        SwitchPreference(
            prefs.keyboard.numberRow,
            title = stringRes(R.string.pref__keyboard__number_row__label),
            summary = stringRes(R.string.pref__keyboard__number_row__summary),
        )
        Preference(
            title = stringRes(R.string.settings__fn_key__title),
            summary = stringRes(R.string.settings__fn_key__summary),
            onClick = { navController.navigate(Routes.Settings.Keyboard.FnKey) },
        )
        ListPreference(
            prefs.keyboard.spaceBarMode,
            title = stringRes(R.string.pref__keyboard__space_bar_mode__label),
            entries = enumDisplayEntriesOf(SpaceBarMode::class),
        )
        DialogSliderPreference(
            primaryPref = prefs.keyboard.fontSizeMultiplierPortrait,
            secondaryPref = prefs.keyboard.fontSizeMultiplierLandscape,
            title = stringRes(R.string.pref__keyboard__font_size_multiplier__label),
            primaryLabel = stringRes(R.string.screen_orientation__portrait),
            secondaryLabel = stringRes(R.string.screen_orientation__landscape),
            valueLabel = { stringRes(R.string.unit__percent__symbol, "v" to it) },
            min = 50,
            max = 150,
            stepIncrement = 5,
        )
        DialogSliderPreference(
            primaryPref = prefs.keyboard.keySpacingVertical,
            secondaryPref = prefs.keyboard.keySpacingHorizontal,
            title = stringRes(R.string.pref__keyboard__key_spacing__label),
            primaryLabel = stringRes(R.string.screen_orientation__vertical),
            secondaryLabel = stringRes(R.string.screen_orientation__horizontal),
            valueLabel = { stringRes(R.string.unit__percent__symbol, "v" to it) },
            min = 50,
            max = 150,
            stepIncrement = 5,
        )
        ListPreference(
            prefs.keyboard.landscapeInputUiMode,
            title = stringRes(R.string.pref__keyboard__landscape_input_ui_mode__label),
            entries = enumDisplayEntriesOf(LandscapeInputUiMode::class),
        )
        ListPreference(
            listPref = prefs.keyboard.incognitoDisplayMode,
            title = stringRes(R.string.pref__keyboard__incognito_indicator__label),
            entries = enumDisplayEntriesOf(IncognitoDisplayMode::class),
        )
        ListPreference(
            prefs.keyboard.capitalizationBehavior,
            title = stringRes(R.string.pref__keyboard__capitalization_behavior__label),
            entries = enumDisplayEntriesOf(CapitalizationBehavior::class),
        )

        PreferenceGroup(title = stringRes(R.string.pref__keyboard__group_keypress__label)) {
            Preference(
                title = stringRes(R.string.settings__input_feedback__title),
                onClick = { navController.navigate(Routes.Settings.Keyboard.InputFeedback) },
            )
            SwitchPreference(
                prefs.keyboard.popupEnabled,
                title = stringRes(R.string.pref__keyboard__popup_enabled__label),
                summary = stringRes(R.string.pref__keyboard__popup_enabled__summary),
            )
            SwitchPreference(
                prefs.keyboard.spaceBarSwitchesToCharacters,
                title = stringRes(R.string.pref__keyboard__space_bar_switches_to_characters__label),
                summary = stringRes(R.string.pref__keyboard__space_bar_switches_to_characters__summary),
            )
        }

        PreferenceGroup(title = stringRes(R.string.pref__keyboard__group_long_press__label)) {
            TimeoutPreference(
                prefs.keyboard.longPressTimeoutUseSystem,
                prefs.keyboard.longPressTimeout,
                title = stringRes(R.string.pref__keyboard__long_press_timeout__label),
                min = 100,
                max = 700,
                stepIncrement = 10,
                getSystemValue = { interactionController.getSystemLongPressTimeout() },
            )
            SwitchPreference(
                prefs.keyboard.longPressKeyHintEnabled,
                title = stringRes(R.string.pref__keyboard__long_press_key_hint_enabled__label),
                summary = stringRes(R.string.pref__keyboard__long_press_key_hint_enabled__summary),
            )
            ListPreference(
                prefs.keyboard.longPressKeyHintPlacement,
                title = stringRes(R.string.pref__keyboard__long_press_key_hint_placement__label),
                entries = enumDisplayEntriesOf(LongPressKeyHintPlacement::class),
                enabledIf = { prefs.keyboard.longPressKeyHintEnabled isEqualTo true },
            )
        }

        PreferenceGroup(title = stringRes(R.string.pref__keyboard__group_multi_tap__label)) {
            TimeoutPreference(
                prefs.keyboard.multiTapTimeoutUseSystem,
                prefs.keyboard.multiTapTimeout,
                title = stringRes(R.string.pref__keyboard__multi_tap_timeout__label),
                min = 100,
                max = 700,
                stepIncrement = 10,
                getSystemValue = { interactionController.getSystemMultiPressTimeout() },
            )
            SwitchPreference(
                prefs.keyboard.multiTapKeyHintEnabled,
                title = stringRes(R.string.pref__keyboard__multi_tap_key_hint_enabled__label),
                summary = stringRes(R.string.pref__keyboard__multi_tap_key_hint_enabled__summary),
            )
        }

        PreferenceGroup(title = stringRes(R.string.pref__keyboard__group_flicks__label)) {
            SwitchPreference(
                prefs.keyboard.flickKeyHintEnabled,
                title = "Show flick hints",
                summary = "Displays the flick key as a hint",
            )
            ListPreference(
                prefs.keyboard.flickKeyHintPlacement,
                title = "Flick hint placement",
                entries = enumDisplayEntriesOf(FlickKeyHintPlacement::class),
                enabledIf = { prefs.keyboard.flickKeyHintEnabled isEqualTo true },
            )
        }
    }
}

@Composable
private fun TimeoutPreference(
    useSystemPref: PreferenceData<Boolean>,
    timeoutPref: PreferenceData<Int>,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    title: String,
    min: Int,
    max: Int,
    stepIncrement: Int,
    getSystemValue: () -> Duration,
) {
    val scope = rememberCoroutineScope()
    val dialogStrings = LocalDefaultDialogPrefStrings.current

    val useSystem by useSystemPref.collectAsState()
    val timeout by timeoutPref.collectAsState()
    var isDialogOpen by remember { mutableStateOf(false) }

    Preference(
        modifier = modifier,
        icon = icon,
        title = title,
        summary = when {
            useSystem -> stringRes(
                R.string.pref__keyboard__timeout_pref_use_system_summary,
                "v" to getSystemValue().inWholeMilliseconds,
            )
            else -> stringRes(R.string.unit__milliseconds__symbol, "v" to timeout)
        },
        onClick = {
            isDialogOpen = true
        },
    )

    if (isDialogOpen) {
        var newUseSystem by remember { mutableStateOf(useSystem) }
        var newTimeout by remember {
            mutableFloatStateOf(
                when {
                    useSystem -> getSystemValue().inWholeMilliseconds.toFloat()
                    else -> timeout.toFloat()
                }
            )
        }

        JetPrefAlertDialog(
            title = title,
            confirmLabel = dialogStrings.confirmLabel,
            onConfirm = {
                scope.launch {
                    useSystemPref.set(newUseSystem)
                    timeoutPref.set(newTimeout.roundToInt())
                }
                isDialogOpen = false
            },
            dismissLabel = dialogStrings.dismissLabel,
            onDismiss = {
                isDialogOpen = false
            },
            neutralLabel = dialogStrings.neutralLabel,
            onNeutral = {
                scope.launch {
                    useSystemPref.reset()
                    timeoutPref.reset()
                }
                isDialogOpen = false
            },
            contentPadding = PaddingValues.Zero,
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .clip(MaterialTheme.shapes.large)
                        .toggleable(
                            value = newUseSystem,
                            onValueChange = {
                                newUseSystem = it
                                if (newUseSystem) {
                                    newTimeout = getSystemValue().inWholeMilliseconds.toFloat()
                                }
                            },
                            role = Role.Switch,
                        )
                        .padding(all = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        modifier = Modifier.weight(1f),
                        text = stringRes(R.string.pref__keyboard__timeout_pref_use_system_switch),
                    )
                    Switch(
                        checked = newUseSystem,
                        onCheckedChange = null,
                    )
                }
                Text(
                    modifier = Modifier
                        .padding(JetPrefAlertDialogDefaults.ContentPadding)
                        .align(Alignment.CenterHorizontally)
                        .padding(top = 16.dp),
                    text = stringRes(R.string.unit__milliseconds__symbol, "v" to newTimeout),
                )
                Slider(
                    modifier = Modifier
                        .padding(JetPrefAlertDialogDefaults.ContentPadding)
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    enabled = !newUseSystem,
                    value = newTimeout,
                    valueRange = min.toFloat()..max.toFloat(),
                    steps = ((max.toFloat() - min.toFloat()) / stepIncrement.toFloat()).roundToInt() - 1,
                    onValueChange = { newTimeout = round(it) },
                    colors = DialogSliderPreferenceDefaults.sliderColors(),
                )
            }
        }
    }
}
