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

package dev.patrickgold.florisboard.app.ui.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.app.ui.components.FlorisScreen
import dev.patrickgold.florisboard.app.ui.components.pluralResource
import dev.patrickgold.jetpref.ui.compose.DialogSliderPreference
import dev.patrickgold.jetpref.ui.compose.ExperimentalJetPrefUi
import dev.patrickgold.jetpref.ui.compose.PreferenceGroup
import dev.patrickgold.jetpref.ui.compose.SwitchPreference

@OptIn(ExperimentalJetPrefUi::class)
@Composable
fun ClipboardScreen() = FlorisScreen(title = stringResource(R.string.settings__clipboard__title)) {
    SwitchPreference(
        prefs.clipboard.useInternalClipboard,
        title = stringResource(R.string.pref__clipboard__use_internal_clipboard__label),
        summary = stringResource(R.string.pref__clipboard__use_internal_clipboard__summary),
    )
    SwitchPreference(
        prefs.clipboard.syncToFloris,
        title = stringResource(R.string.pref__clipboard__sync_from_system_clipboard__label),
        summary = stringResource(R.string.pref__clipboard__sync_from_system_clipboard__summary),
        enabledIf = { prefs.clipboard.useInternalClipboard isEqualTo true },
    )
    SwitchPreference(
        prefs.clipboard.syncToSystem,
        title = stringResource(R.string.pref__clipboard__sync_to_system_clipboard__label),
        summary = stringResource(R.string.pref__clipboard__sync_to_system_clipboard__summary),
        enabledIf = { prefs.clipboard.useInternalClipboard isEqualTo true },
    )

    PreferenceGroup(title = stringResource(R.string.pref__clipboard__clipboard_history_title)) {
        SwitchPreference(
            prefs.clipboard.enableHistory,
            title = stringResource(R.string.pref__clipboard__enable_clipboard_history__label),
            summary = stringResource(R.string.pref__clipboard__enable_clipboard_history__summary),
        )
        SwitchPreference(
            prefs.clipboard.cleanUpOld,
            title = stringResource(R.string.pref__clipboard__clean_up_old__label),
            enabledIf = { prefs.clipboard.enableHistory isEqualTo true },
        )
        DialogSliderPreference(
            prefs.clipboard.cleanUpAfter,
            title = stringResource(R.string.pref__clipboard__clean_up_after__label),
            unit = pluralResource(R.plurals.unit__minutes__written, prefs.clipboard.cleanUpAfter.get()),
            min = 0,
            max = 120,
            stepIncrement = 5,
            enabledIf = { prefs.clipboard.enableHistory isEqualTo true },
        )
        SwitchPreference(
            prefs.clipboard.limitHistorySize,
            title = stringResource(R.string.pref__clipboard__limit_history_size__label),
            enabledIf = { prefs.clipboard.cleanUpOld isEqualTo true },
        )
        DialogSliderPreference(
            prefs.clipboard.maxHistorySize,
            title = stringResource(R.string.pref__clipboard__max_history_size__label),
            unit = pluralResource(R.plurals.unit__items__written, prefs.clipboard.maxHistorySize.get()),
            min = 5,
            max = 100,
            stepIncrement = 5,
            enabledIf = { prefs.clipboard.limitHistorySize isEqualTo true },
        )
    }
}
