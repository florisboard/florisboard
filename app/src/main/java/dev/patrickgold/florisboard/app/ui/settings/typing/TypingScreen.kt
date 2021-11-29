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

package dev.patrickgold.florisboard.app.ui.settings.typing

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.app.res.stringRes
import dev.patrickgold.florisboard.app.ui.components.FlorisInfoCard
import dev.patrickgold.florisboard.app.ui.components.FlorisScreen
import dev.patrickgold.florisboard.common.android.AndroidVersion
import dev.patrickgold.jetpref.datastore.ui.PreferenceGroup
import dev.patrickgold.jetpref.datastore.ui.SwitchPreference

@Composable
fun TypingScreen() = FlorisScreen(title = stringRes(R.string.settings__typing__title)) {
    PreferenceGroup(title = stringRes(R.string.pref__suggestion__title)) {
        SwitchPreference(
            prefs.suggestion.api30InlineSuggestionsEnabled,
            title = stringRes(R.string.pref__suggestion__api30_inline_suggestions_enabled__label),
            summary = stringRes(R.string.pref__suggestion__api30_inline_suggestions_enabled__summary),
            visibleIf = { AndroidVersion.ATLEAST_API30_R },
        )
        // This card is temporary and is therefore not using a string resource
        FlorisInfoCard(
            modifier = Modifier.padding(8.dp),
            text = if (AndroidVersion.ATLEAST_API30_R) {
                "Suggestions (except autofill) are not available in this beta release"
            } else {
                "Suggestions are not available in this beta release"
            },
        )
        SwitchPreference(
            prefs.suggestion.enabled,
            title = stringRes(R.string.pref__suggestion__enabled__label),
            summary = stringRes(R.string.pref__suggestion__enabled__summary),
            enabledIf = { false },
        )
    }

    PreferenceGroup(title = stringRes(R.string.pref__correction__title)) {
        SwitchPreference(
            prefs.correction.autoCapitalization,
            title = stringRes(R.string.pref__correction__auto_capitalization__label),
            summary = stringRes(R.string.pref__correction__auto_capitalization__summary),
        )
        SwitchPreference(
            prefs.correction.rememberCapsLockState,
            title = stringRes(R.string.pref__correction__remember_caps_lock_state__label),
            summary = stringRes(R.string.pref__correction__remember_caps_lock_state__summary),
        )
        SwitchPreference(
            prefs.correction.doubleSpacePeriod,
            title = stringRes(R.string.pref__correction__double_space_period__label),
            summary = stringRes(R.string.pref__correction__double_space_period__summary),
        )
    }
}
