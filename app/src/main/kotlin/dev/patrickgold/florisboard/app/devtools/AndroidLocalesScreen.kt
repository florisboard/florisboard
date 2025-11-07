/*
 * Copyright (C) 2021-2025 The OmniBoard Contributors
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

package dev.silo.omniboard.app.devtools

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import dev.silo.omniboard.R
import dev.silo.omniboard.ime.core.DisplayLanguageNamesIn
import dev.silo.omniboard.lib.compose.OmniScreen
import dev.silo.jetpref.datastore.model.observeAsState
import org.omniboard.lib.android.showLongToastSync
import org.omniboard.lib.compose.OmniIconButton
import org.omniboard.lib.compose.stringRes
import org.omniboard.lib.kotlin.io.subDir
import org.omniboard.lib.kotlin.io.subFile
import java.util.Locale

@Composable
fun AndroidLocalesScreen() = OmniScreen {
    title = stringRes(R.string.devtools__android_locales__title)
    scrollable = false

    val context = LocalContext.current
    val availableLocales = remember { Locale.getAvailableLocales().sortedBy { it.toLanguageTag() } }

    actions {
        OmniIconButton(
            onClick = {
                try {
                    val devtoolsDir = context.noBackupFilesDir.subDir("devtools")
                    devtoolsDir.mkdirs()
                    val txtFile = devtoolsDir.subFile("system_locales.tsv")
                    txtFile.bufferedWriter().use { out ->
                        for (locale in availableLocales) {
                            out.append(locale.toLanguageTag())
                            out.append('\t')
                            out.append(locale.getDisplayName(Locale.ENGLISH))
                            out.append('\t')
                            out.append(locale.getDisplayName(locale))
                            out.appendLine()
                        }
                    }
                    context.showLongToastSync("Exported available system locales to \"${txtFile.path}\"")
                } catch (e: Exception) {
                    context.showLongToastSync(
                        R.string.error__snackbar_message_template,
                        "error_message" to e.message.toString(),
                    )
                }
            },
            icon = Icons.Default.Save,
        )
    }

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
