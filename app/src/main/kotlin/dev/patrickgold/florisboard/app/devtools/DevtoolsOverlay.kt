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

import android.view.textservice.SuggestionsInfo
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.LocalContentColor
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.patrickgold.florisboard.FlorisImeService
import dev.patrickgold.florisboard.app.florisPreferenceModel
import dev.patrickgold.florisboard.clipboardManager
import dev.patrickgold.florisboard.keyboardManager
import dev.patrickgold.florisboard.lib.FlorisLocale
import dev.patrickgold.florisboard.lib.observeAsNonNullState
import dev.patrickgold.florisboard.spellingManager
import dev.patrickgold.jetpref.datastore.model.observeAsState
import java.text.SimpleDateFormat
import java.util.*

private val CardBackground = Color.Black.copy(0.6f)
private val DateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH-mm-ss", FlorisLocale.default().base)

@Composable
fun DevtoolsOverlay(modifier: Modifier = Modifier) {
    val prefs by florisPreferenceModel()

    val showPrimaryClip by prefs.devtools.showPrimaryClip.observeAsState()
    val showInputStateOverlay by prefs.devtools.showInputStateOverlay.observeAsState()
    val showSpellingOverlay by prefs.devtools.showSpellingOverlay.observeAsState()

    CompositionLocalProvider(
        LocalContentColor provides Color.White,
        LocalLayoutDirection provides LayoutDirection.Ltr,
    ) {
        Column(modifier = modifier) {
            if (showPrimaryClip) {
                DevtoolsClipboardOverlay()
            }
            if (showInputStateOverlay) {
                DevtoolsInputStateOverlay()
            }
            if (showSpellingOverlay) {
                DevtoolsSpellingOverlay()
            }
        }
    }
}

@Composable
private fun DevtoolsClipboardOverlay() {
    val context = LocalContext.current
    val clipboardManager by context.clipboardManager()

    DevtoolsOverlayBox(title = "Clipboard overlay") {
        val primaryClip by clipboardManager.primaryClip.observeAsState()
        Text(
            modifier = Modifier.padding(bottom = 8.dp, start = 8.dp, end = 8.dp),
            text = primaryClip.toString(),
            color = Color.White,
        )
    }
}

@Composable
private fun DevtoolsInputStateOverlay() {
    val context = LocalContext.current
    val keyboardManager by context.keyboardManager()

    val activeEditorInfo by keyboardManager.observeActiveEditorInfo()
    val activeEditorInstance = FlorisImeService.activeEditorInstance() ?: return
    val selection = activeEditorInstance.activeSelection
    val editorContent by activeEditorInstance.inputCache.editorContent.observeAsNonNullState()

    DevtoolsOverlayBox(title = "Input state overlay") {
        DevtoolsSubGroup(title = "EditorInfo") {
            DevtoolsText(text = "Type=${activeEditorInfo.inputAttributes.type} Variation=${activeEditorInfo.inputAttributes.variation} IsRich=${activeEditorInfo.isRichInputEditor}")
            DevtoolsText(text = "Selection { start=${selection.start}, end=${selection.end} }")
        }
        DevtoolsSubGroup(title = "EditorContent") {
            DevtoolsText(text = "Before: \"${editorContent.textBeforeSelection}\"")
            DevtoolsText(text = "Selected: \"${editorContent.selectedText}\"")
            DevtoolsText(text = "After: \"${editorContent.textAfterSelection}\"")
            DevtoolsText(text = "ComposingWord: ${editorContent.composing}")
        }
    }
}


@Composable
private fun DevtoolsSpellingOverlay() {
    val context = LocalContext.current
    val spellingManager by context.spellingManager()

    val debugOverlayVersion by spellingManager.debugOverlayVersion.observeAsNonNullState()
    val suggestionsInfos = remember(debugOverlayVersion) { spellingManager.debugOverlaySuggestionsInfos.snapshot() }

    val sortedEntries = suggestionsInfos.entries.sortedByDescending { it.key }
    DevtoolsOverlayBox(title = "Spelling overlay (${sortedEntries.size})") {
        for ((timestamp, wordInfoPair) in sortedEntries) {
            val (word, info) = wordInfoPair
            val isTypo = (info.suggestionsAttributes and SuggestionsInfo.RESULT_ATTR_LOOKS_LIKE_TYPO) > 0
            val suggestions = Array(info.suggestionsCount) { n -> info.getSuggestionAt(n) }
            Column(modifier = Modifier.padding(horizontal = 8.dp)) {
                val date = DateFormat.format(Date(timestamp))
                Text(
                    text = "$date - \"$word\"",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                )
                val details = buildString {
                    appendLine("isTypo: $isTypo")
                    if (isTypo) {
                        appendLine("providing corrections list of size n=${suggestions.size}")
                        for ((n, suggestion) in suggestions.withIndex()) {
                            append("  [$n] = string[${suggestion.length}] { \"")
                            append(suggestion)
                            appendLine("\" }")
                        }
                    }
                }.prependIndent("  ")
                Text(
                    text = details,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                )
            }
        }
    }
}

@Composable
private fun DevtoolsOverlayBox(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier
            .padding(all = 8.dp)
            .fillMaxWidth()
            .background(CardBackground),
    ) {
        Text(
            modifier = Modifier.padding(all = 8.dp),
            text = title,
            fontSize = 14.sp,
        )
        content()
    }
}

@Composable
private fun DevtoolsSubGroup(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Text(
        modifier = Modifier.padding(start = 8.dp),
        text = title,
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        fontSize = 12.sp,
    )
    Column(modifier = Modifier.padding(start = 12.dp, bottom = 8.dp), content = content)
}

@Composable
private fun DevtoolsText(text: String) {
    Text(
        text = text,
        fontFamily = FontFamily.Monospace,
        fontSize = 12.sp,
    )
}
