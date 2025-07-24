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

package dev.patrickgold.florisboard.app.settings.clipboard

import androidx.compose.runtime.Composable
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.ime.clipboard.CLIPBOARD_HISTORY_NUM_GRID_COLUMNS_AUTO
import dev.patrickgold.florisboard.lib.compose.FlorisScreen
import dev.patrickgold.florisboard.lib.compose.pluralsRes
import dev.patrickgold.florisboard.lib.compose.stringRes
import dev.patrickgold.jetpref.datastore.ui.DialogSliderPreference
import dev.patrickgold.jetpref.datastore.ui.ExperimentalJetPrefDatastoreUi
import dev.patrickgold.jetpref.datastore.ui.PreferenceGroup
import dev.patrickgold.jetpref.datastore.ui.SwitchPreference
import org.florisboard.lib.android.AndroidVersion

@OptIn(ExperimentalJetPrefDatastoreUi::class)
@Composable
fun ClipboardScreen() = FlorisScreen {
    title = stringRes(R.string.settings__clipboard__title)
    previewFieldVisible = true

    content {
        SwitchPreference(
            prefs.clipboard.useInternalClipboard,
            title = stringRes(R.string.pref__clipboard__use_internal_clipboard__label),
            summary = stringRes(R.string.pref__clipboard__use_internal_clipboard__summary),
        )
        SwitchPreference(
            prefs.clipboard.syncToFloris,
            title = stringRes(R.string.pref__clipboard__sync_from_system_clipboard__label),
            summary = stringRes(R.string.pref__clipboard__sync_from_system_clipboard__summary),
            enabledIf = { prefs.clipboard.useInternalClipboard isEqualTo true },
        )
        SwitchPreference(
            prefs.clipboard.syncToSystem,
            title = stringRes(R.string.pref__clipboard__sync_to_system_clipboard__label),
            summary = stringRes(R.string.pref__clipboard__sync_to_system_clipboard__summary),
            enabledIf = { prefs.clipboard.useInternalClipboard isEqualTo true },
        )

        PreferenceGroup(title = stringRes(R.string.pref__clipboard__group_clipboard_suggestion__label)) {
            SwitchPreference(
                prefs.clipboard.suggestionEnabled,
                title = stringRes(R.string.pref__clipboard__suggestion_enabled__label),
                summary = stringRes(R.string.pref__clipboard__suggestion_enabled__summary),
            )
            DialogSliderPreference(
                prefs.clipboard.suggestionTimeout,
                title = stringRes(R.string.pref__clipboard__suggestion_timeout__label),
                valueLabel = { stringRes(R.string.pref__clipboard__suggestion_timeout__summary, "v" to it) },
                min = 30,
                max = 300,
                stepIncrement = 5,
                enabledIf = { prefs.clipboard.suggestionEnabled isEqualTo true },
            )
        }

        PreferenceGroup(title = stringRes(R.string.pref__clipboard__group_clipboard_history__label)) {
            SwitchPreference(
                prefs.clipboard.historyEnabled,
                title = stringRes(R.string.pref__clipboard__enable_clipboard_history__label),
                summary = stringRes(R.string.pref__clipboard__enable_clipboard_history__summary),
            )
            DialogSliderPreference(
                primaryPref = prefs.clipboard.numHistoryGridColumnsPortrait,
                secondaryPref = prefs.clipboard.numHistoryGridColumnsLandscape,
                title = stringRes(R.string.pref__clipboard__num_history_grid_columns__label),
                primaryLabel = stringRes(R.string.screen_orientation__portrait),
                secondaryLabel = stringRes(R.string.screen_orientation__landscape),
                valueLabel = { numGridColumns ->
                    if (numGridColumns == CLIPBOARD_HISTORY_NUM_GRID_COLUMNS_AUTO) {
                        stringRes(R.string.general__auto)
                    } else {
                        numGridColumns.toString()
                    }
                },
                min = 0,
                max = 10,
                stepIncrement = 1,
                enabledIf = { prefs.clipboard.historyEnabled isEqualTo true },
            )
            SwitchPreference(
                prefs.clipboard.cleanUpOld,
                title = stringRes(R.string.pref__clipboard__clean_up_old__label),
                enabledIf = { prefs.clipboard.historyEnabled isEqualTo true },
            )
            DialogSliderPreference(
                prefs.clipboard.cleanUpAfter,
                title = stringRes(R.string.pref__clipboard__clean_up_after__label),
                valueLabel = { pluralsRes(R.plurals.unit__minutes__written, it, "v" to it) },
                min = 0,
                max = 120,
                stepIncrement = 5,
                enabledIf = { prefs.clipboard.historyEnabled isEqualTo true && prefs.clipboard.cleanUpOld isEqualTo true },
            )
            SwitchPreference(
                prefs.clipboard.autoCleanSensitive,
                title = stringRes(R.string.pref__clipboard__auto_clean_sensitive__label),
                enabledIf = { prefs.clipboard.historyEnabled isEqualTo true },
                visibleIf = { AndroidVersion.ATLEAST_API33_T },
            )
            DialogSliderPreference(
                prefs.clipboard.autoCleanSensitiveAfter,
                title = stringRes(R.string.pref__clipboard__auto_clean_sensitive_after__label),
                valueLabel = { pluralsRes(R.plurals.unit__seconds__written, it, "v" to it) },
                min = 0,
                max = 300,
                stepIncrement = 10,
                enabledIf = { prefs.clipboard.historyEnabled isEqualTo true && prefs.clipboard.autoCleanSensitive isEqualTo true },
                visibleIf = { AndroidVersion.ATLEAST_API33_T },
            )
            SwitchPreference(
                prefs.clipboard.limitHistorySize,
                title = stringRes(R.string.pref__clipboard__limit_history_size__label),
                enabledIf = { prefs.clipboard.historyEnabled isEqualTo true },
            )
            DialogSliderPreference(
                prefs.clipboard.maxHistorySize,
                title = stringRes(R.string.pref__clipboard__max_history_size__label),
                valueLabel = { pluralsRes(R.plurals.unit__items__written, it, "v" to it) },
                min = 5,
                max = 100,
                stepIncrement = 5,
                enabledIf = { prefs.clipboard.historyEnabled isEqualTo true && prefs.clipboard.limitHistorySize isEqualTo true },
            )
            SwitchPreference(
                prefs.clipboard.clearPrimaryClipDeletesLastItem,
                title = stringRes(R.string.pref__clipboard__clear_primary_clip_deletes_last_item__label),
                summary = stringRes(R.string.pref__clipboard__clear_primary_clip_deletes_last_item__summary),
                enabledIf = { prefs.clipboard.historyEnabled isEqualTo true },
            )
        }
    }
}
