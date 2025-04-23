/*
 * Copyright (C) 2022-2025 The FlorisBoard Contributors
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

package dev.patrickgold.florisboard.app.settings.theme

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.app.enumDisplayEntriesOf
import dev.patrickgold.florisboard.app.florisPreferenceModel
import dev.patrickgold.florisboard.lib.compose.stringRes
import dev.patrickgold.jetpref.datastore.ui.ListPreference
import dev.patrickgold.jetpref.datastore.ui.PreferenceLayout
import dev.patrickgold.jetpref.material.ui.ColorRepresentation
import dev.patrickgold.jetpref.material.ui.JetPrefAlertDialog

private val FineTuneContentPadding = PaddingValues(horizontal = 8.dp)

@Composable
fun FineTuneDialog(onDismiss: () -> Unit) {
    JetPrefAlertDialog(
        title = stringRes(R.string.settings__theme_editor__fine_tune__title),
        onDismiss = onDismiss,
        contentPadding = FineTuneContentPadding,
    ) {
        PreferenceLayout(florisPreferenceModel(), iconSpaceReserved = false) {
            ListPreference(
                listPref = prefs.theme.editorLevel,
                title = stringRes(R.string.settings__theme_editor__fine_tune__level),
                entries = enumDisplayEntriesOf(SnyggLevel::class),
            )
            ListPreference(
                listPref = prefs.theme.editorColorRepresentation,
                title = stringRes(R.string.settings__theme_editor__fine_tune__color_representation),
                entries = enumDisplayEntriesOf(ColorRepresentation::class),
            )
            ListPreference(
                listPref = prefs.theme.editorDisplayKbdAfterDialogs,
                title = stringRes(R.string.settings__theme_editor__fine_tune__display_kbd_after_dialogs),
                entries = enumDisplayEntriesOf(DisplayKbdAfterDialogs::class),
            )
        }
    }
}
