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

package dev.patrickgold.florisboard.app.settings.smartbar

import androidx.compose.material3.ListItemDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.app.FlorisPreferenceStore
import dev.patrickgold.florisboard.app.components.ScrollableScreenColumn
import dev.patrickgold.florisboard.app.components.SegmentedListColumn
import dev.patrickgold.florisboard.app.enumDisplayEntriesOf
import dev.patrickgold.florisboard.ime.smartbar.CandidatesDisplayMode
import dev.patrickgold.florisboard.ime.smartbar.ExtendedActionsPlacement
import dev.patrickgold.florisboard.ime.smartbar.SmartbarLayout
import dev.patrickgold.florisboard.lib.compose.FlorisScreenNg
import dev.patrickgold.jetpref.datastore.ui.ListPreference
import dev.patrickgold.jetpref.datastore.ui.SwitchPreference
import org.florisboard.lib.compose.stringRes

@Composable
fun SmartbarScreen() {
    val prefs by FlorisPreferenceStore

    FlorisScreenNg(
        title = stringRes(R.string.settings__smartbar__title),
        previewFieldVisible = true,
    ) { contentPadding ->
        ScrollableScreenColumn(contentPadding) {
            SegmentedListColumn {
                val count = 2
                SwitchPreference(
                    prefs.smartbar.enabled,
                    shapes = ListItemDefaults.segmentedShapes(0, count),
                    title = stringRes(R.string.pref__smartbar__enabled__label),
                    summary = stringRes(R.string.pref__smartbar__enabled__summary),
                )
                ListPreference(
                    listPref = prefs.smartbar.layout,
                    shapes = ListItemDefaults.segmentedShapes(1, count),
                    title = stringRes(R.string.pref__smartbar__layout__label),
                    entries = enumDisplayEntriesOf(SmartbarLayout::class),
                    enabledIf = { prefs.smartbar.enabled isEqualTo true },
                )
            }

            SegmentedListColumn(title = stringRes(R.string.pref__smartbar__group_layout_specific__label)) {
                val count = 4
                ListPreference(
                    prefs.suggestion.displayMode,
                    shapes = ListItemDefaults.segmentedShapes(0, count),
                    title = stringRes(R.string.pref__suggestion__display_mode__label),
                    entries = enumDisplayEntriesOf(CandidatesDisplayMode::class),
                    enabledIf = { prefs.smartbar.enabled isEqualTo true },
                    visibleIf = { prefs.smartbar.layout isNotEqualTo SmartbarLayout.ACTIONS_ONLY },
                )
                SwitchPreference(
                    prefs.smartbar.flipToggles,
                    shapes = ListItemDefaults.segmentedShapes(1, count),
                    title = stringRes(R.string.pref__smartbar__flip_toggles__label),
                    summary = stringRes(R.string.pref__smartbar__flip_toggles__summary),
                    enabledIf = { prefs.smartbar.enabled isEqualTo true },
                    visibleIf = {
                        prefs.smartbar.layout isEqualTo SmartbarLayout.SUGGESTIONS_ACTIONS_SHARED ||
                            prefs.smartbar.layout isEqualTo SmartbarLayout.SUGGESTIONS_ACTIONS_EXTENDED
                    },
                )
                // TODO: schedule to remove this preference in the future, but keep it for now so users
                //  know why the setting is not available anymore. Also force enable it for UI display.
                SideEffect {
                    // prefs.smartbar.sharedActionsAutoExpandCollapse.set(true)
                }
                SwitchPreference(
                    prefs.smartbar.sharedActionsAutoExpandCollapse,
                    shapes = ListItemDefaults.segmentedShapes(2, count),
                    title = stringRes(R.string.pref__smartbar__shared_actions_auto_expand_collapse__label),
                    summary = "[Since v0.4.1] Always enabled due to UX issues",
                    enabledIf = { false },
                    visibleIf = { prefs.smartbar.layout isEqualTo SmartbarLayout.SUGGESTIONS_ACTIONS_SHARED },
                )
                ListPreference(
                    listPref = prefs.smartbar.extendedActionsPlacement,
                    shapes = ListItemDefaults.segmentedShapes(3, count),
                    title = stringRes(R.string.pref__smartbar__extended_actions_placement__label),
                    entries = enumDisplayEntriesOf(ExtendedActionsPlacement::class),
                    enabledIf = { prefs.smartbar.enabled isEqualTo true },
                    visibleIf = { prefs.smartbar.layout isEqualTo SmartbarLayout.SUGGESTIONS_ACTIONS_EXTENDED },
                )
            }
        }
    }
}
