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

package dev.patrickgold.florisboard.app.devtools

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.lib.android.AndroidSettings
import dev.patrickgold.florisboard.lib.compose.FlorisScreen
import dev.patrickgold.florisboard.lib.compose.stringRes
import dev.patrickgold.jetpref.datastore.ui.Preference
import dev.patrickgold.jetpref.material.ui.JetPrefAlertDialog

@Composable
fun AndroidSettingsScreen(name: String?) = FlorisScreen {
    title = when (name) {
        AndroidSettings.Global.groupId -> stringRes(R.string.devtools__android_settings_global__title)
        AndroidSettings.Secure.groupId -> stringRes(R.string.devtools__android_settings_secure__title)
        AndroidSettings.System.groupId -> stringRes(R.string.devtools__android_settings_system__title)
        else -> "invalid"
    }
    scrollable = false

    val context = LocalContext.current

    val settingsGroup = when (name) {
        AndroidSettings.Global.groupId -> AndroidSettings.Global
        AndroidSettings.Secure.groupId -> AndroidSettings.Secure
        AndroidSettings.System.groupId -> AndroidSettings.System
        else -> AndroidSettings.Global
    }
    val nameValueTable = remember(name) { settingsGroup.getAllKeys().toList() }
    var dialogKey by remember { mutableStateOf<String?>(null) }

    content {
        LazyColumn {
            items(nameValueTable) { (fieldName, key) ->
                Preference(
                    title = fieldName,
                    summary = key,
                    onClick = { dialogKey = key },
                )
            }
        }

        if (dialogKey != null) {
            JetPrefAlertDialog(
                title = dialogKey!!,
                onDismiss = { dialogKey = null },
            ) {
                SelectionContainer {
                    Text(
                        text = remember {
                            (settingsGroup.getString(context, dialogKey!!) ?: "(null)").ifBlank { "(blank)" }
                        },
                    )
                }
            }
        }
    }
}
