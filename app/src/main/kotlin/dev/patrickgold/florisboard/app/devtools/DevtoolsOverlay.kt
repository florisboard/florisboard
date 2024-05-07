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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import dev.patrickgold.florisboard.app.florisPreferenceModel
import dev.patrickgold.florisboard.clipboardManager
import dev.patrickgold.florisboard.editorInstance
import dev.patrickgold.florisboard.lib.FlorisLocale
import dev.patrickgold.florisboard.lib.observeAsNonNullState
import dev.patrickgold.florisboard.nlpManager
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
        val primaryClip by clipboardManager.primaryClipFlow.collectAsState()
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
    val editorInstance by context.editorInstance()

    val info by editorInstance.activeInfoFlow.collectAsState()
    val content by editorInstance.activeContentFlow.collectAsState()
    val selection = content.selection

    DevtoolsOverlayBox(title = "Input state overlay") {
        DevtoolsSubGroup(title = "EditorInfo") {
            DevtoolsText(text = "Type=${info.inputAttributes.type} Variation=${info.inputAttributes.variation} IsRich=${info.isRichInputEditor}")
            DevtoolsText(text = "InitialSelection: ${info.initialSelection}")
        }
        DevtoolsSubGroup(title = "EditorContent") {
            DevtoolsText(text = "Selection: { start=${selection.start}, end=${selection.end} }")
            DevtoolsText(text = "Before: \"${content.textBeforeSelection}\"")
            DevtoolsText(text = "Selected: \"${content.selectedText}\"")
            DevtoolsText(text = "After: \"${content.textAfterSelection}\"")
            DevtoolsText(text = "Composing: ${content.composing}")
            DevtoolsText(text = "CurrentWord: ${content.currentWord}")
            DevtoolsText(text = "LastCommit: ${editorInstance.lastCommitPosition}")
        }
    }
}


@Composable
private fun DevtoolsSpellingOverlay() {
    val context = LocalContext.current
    val nlpManager by context.nlpManager()

    val debugOverlayVersion by nlpManager.debugOverlayVersion.observeAsNonNullState()
    val suggestionsInfos = remember(debugOverlayVersion) { nlpManager.debugOverlaySuggestionsInfos.snapshot() }

    val sortedEntries = suggestionsInfos.entries.sortedByDescending { it.key }
    DevtoolsOverlayBox(title = "Spelling overlay (${sortedEntries.size})") {
        for ((timestamp, wordInfoPair) in sortedEntries) {
            val (word, info) = wordInfoPair
            val suggestions = info.suggestions()
            Column(modifier = Modifier.padding(horizontal = 8.dp)) {
                val date = DateFormat.format(Date(timestamp))
                Text(
                    text = "$date - \"$word\"",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                )
                val details = buildString {
                    appendLine("isTypo: ${info.isTypo} | isGrammarError: ${info.isGrammarError}")
                    if (info.isTypo || info.isGrammarError) {
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
