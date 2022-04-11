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

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.ime.core.DisplayLanguageNamesIn
import dev.patrickgold.florisboard.lib.compose.FlorisScreen
import dev.patrickgold.florisboard.lib.compose.stringRes
import dev.patrickgold.jetpref.datastore.model.observeAsState
import java.util.*

@Composable
fun AndroidLocalesScreen() = FlorisScreen {
    title = stringRes(R.string.devtools__android_locales__title)
    scrollable = false

    val availableLocales = remember { Locale.getAvailableLocales().sortedBy { it.toLanguageTag() } }

    content {
        val displayLanguageNamesIn by prefs.localization.displayLanguageNamesIn.observeAsState()

        SelectionContainer(modifier = Modifier.fillMaxWidth()) {
            LazyColumn {
                items(availableLocales) { locale ->
                    Row {
                        Text(
                            text = locale.toLanguageTag().padEnd(12),
                            fontFamily = FontFamily.Monospace,
                        )
                        Text(
                            modifier = Modifier.weight(1.0f),
                            text = when (displayLanguageNamesIn) {
                                DisplayLanguageNamesIn.SYSTEM_LOCALE -> locale.displayName
                                DisplayLanguageNamesIn.NATIVE_LOCALE -> locale.getDisplayName(locale)
                            },
                            fontFamily = FontFamily.Monospace,
                        )
                    }
                }
            }
        }
    }
}
