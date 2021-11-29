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

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.app.res.stringRes
import dev.patrickgold.florisboard.app.ui.components.FlorisInfoCard
import dev.patrickgold.florisboard.app.ui.components.FlorisScreen
import dev.patrickgold.jetpref.datastore.ui.SwitchPreference

@Composable
fun SmartbarScreen() = FlorisScreen(title = stringRes(R.string.settings__smartbar__title)) {
    SwitchPreference(
        prefs.smartbar.enabled,
        title = stringRes(R.string.pref__smartbar__enabled__label),
        summary = stringRes(R.string.pref__smartbar__enabled__summary),
    )
    // This card is temporary and is therefore not using a string resource
    FlorisInfoCard(
        modifier = Modifier.padding(8.dp),
        text = "Smartbar will soon be customizable a lot more, thus this seemingly pointless screen currently",
    )
}
