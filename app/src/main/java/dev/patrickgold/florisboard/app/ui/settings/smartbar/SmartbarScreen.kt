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

package dev.patrickgold.florisboard.app.ui.settings.smartbar

import androidx.compose.runtime.Composable
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.app.res.stringRes
import dev.patrickgold.florisboard.app.ui.components.FlorisScreen
import dev.patrickgold.jetpref.datastore.ui.PreferenceGroup
import dev.patrickgold.jetpref.datastore.ui.SwitchPreference

@Composable
fun SmartbarScreen() = FlorisScreen {
    title = stringRes(R.string.settings__smartbar__title)

    content {
        SwitchPreference(
            prefs.smartbar.enabled,
            title = stringRes(R.string.pref__smartbar__enabled__label),
            summary = stringRes(R.string.pref__smartbar__enabled__summary),
        )

        PreferenceGroup(title = stringRes(R.string.pref__smartbar__group_primary_row__label)) {
            SwitchPreference(
                prefs.smartbar.primaryRowFlipToggles,
                title = stringRes(R.string.pref__smartbar__primary_row_flip_toggles__label),
                summary = stringRes(R.string.pref__smartbar__primary_row_flip_toggles__summary),
                enabledIf = { prefs.smartbar.enabled isEqualTo true },
            )
        }

        PreferenceGroup(title = stringRes(R.string.pref__smartbar__group_secondary_row__label)) {
            SwitchPreference(
                prefs.smartbar.secondaryRowEnabled,
                title = stringRes(R.string.pref__smartbar__secondary_row_enabled__label),
                summary = stringRes(R.string.pref__smartbar__secondary_row_enabled__summary),
                enabledIf = { prefs.smartbar.enabled isEqualTo true },
            )
            SwitchPreference(
                prefs.smartbar.secondaryRowShowBelowPrimary,
                title = stringRes(R.string.pref__smartbar__secondary_row_show_below_primary__label),
                summary = stringRes(R.string.pref__smartbar__secondary_row_show_below_primary__summary),
                enabledIf = {
                    (prefs.smartbar.enabled isEqualTo true) && (prefs.smartbar.secondaryRowEnabled isEqualTo true)
                },
            )
        }

        PreferenceGroup(title = stringRes(R.string.pref__smartbar__group_action_row__label)) {
            //
        }
    }
}
