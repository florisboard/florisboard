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

package dev.patrickgold.florisboard.app.settings.smartbar

import androidx.compose.runtime.Composable
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.ime.smartbar.SecondaryRowPlacement
import dev.patrickgold.florisboard.ime.smartbar.SmartbarRowType
import dev.patrickgold.florisboard.lib.compose.FlorisScreen
import dev.patrickgold.florisboard.lib.compose.stringRes
import dev.patrickgold.jetpref.datastore.ui.ListPreference
import dev.patrickgold.jetpref.datastore.ui.PreferenceGroup
import dev.patrickgold.jetpref.datastore.ui.SwitchPreference

@Composable
fun SmartbarScreen() = FlorisScreen {
    title = stringRes(R.string.settings__smartbar__title)
    previewFieldVisible = true

    content {
        SwitchPreference(
            prefs.smartbar.enabled,
            title = stringRes(R.string.pref__smartbar__enabled__label),
            summary = stringRes(R.string.pref__smartbar__enabled__summary),
        )
        SwitchPreference(
            prefs.smartbar.flipToggles,
            title = stringRes(R.string.pref__smartbar__flip_toggles__label),
            summary = stringRes(R.string.pref__smartbar__flip_toggles__summary),
            enabledIf = { prefs.smartbar.enabled isEqualTo true },
        )

        PreferenceGroup(title = stringRes(R.string.pref__smartbar__group_primary_actions__label)) {
            SwitchPreference(
                prefs.smartbar.primaryActionsAutoExpandCollapse,
                title = stringRes(R.string.pref__smartbar__primary_actions_auto_expand_collapse__label),
                summary = stringRes(R.string.pref__smartbar__primary_actions_auto_expand_collapse__summary),
                enabledIf = { prefs.smartbar.enabled isEqualTo true },
            )
            ListPreference(
                prefs.smartbar.primaryActionsRowType,
                title = stringRes(R.string.pref__smartbar__any_row_type__label),
                entries = SmartbarRowType.listEntries(),
                enabledIf = { prefs.smartbar.enabled isEqualTo true },
            )
        }

        PreferenceGroup(title = stringRes(R.string.pref__smartbar__group_secondary_actions__label)) {
            ListPreference(
                listPref = prefs.smartbar.secondaryActionsPlacement,
                switchPref = prefs.smartbar.secondaryActionsEnabled,
                title = stringRes(R.string.pref__smartbar__secondary_actions_enabled__label),
                entries = SecondaryRowPlacement.listEntries(),
                enabledIf = { prefs.smartbar.enabled isEqualTo true },
            )
            ListPreference(
                prefs.smartbar.secondaryActionsRowType,
                title = stringRes(R.string.pref__smartbar__any_row_type__label),
                entries = SmartbarRowType.listEntries(),
                enabledIf = { prefs.smartbar.enabled isEqualTo true && prefs.smartbar.secondaryActionsEnabled isEqualTo true },
            )
        }
    }
}
