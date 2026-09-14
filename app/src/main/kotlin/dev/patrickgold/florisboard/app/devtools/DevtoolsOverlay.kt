/*
 * Copyright (C) 2021-2025 The FlorisBoard Contributors
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

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.patrickgold.florisboard.app.FlorisPreferenceStore
import dev.patrickgold.florisboard.clipboardManager
import dev.patrickgold.florisboard.ime.keyboard3.LocalImeController
import dev.patrickgold.florisboard.ime.mozc.MozcEngine
import dev.patrickgold.florisboard.ime.nlp.NlpInlineAutofill
import dev.patrickgold.florisboard.ime.theme.LocalThemeController
import dev.patrickgold.florisboard.ime.theme.ThemeController
import dev.patrickgold.florisboard.inferFlorisApplication
import dev.patrickgold.florisboard.lib.FlorisLocale
import dev.patrickgold.florisboard.nlpManager
import dev.patrickgold.jetpref.datastore.model.collectAsState
import org.florisboard.lib.android.AndroidVersion
import org.florisboard.lib.snygg.SnyggMissingSchemaException
import java.text.SimpleDateFormat
import java.util.*

private val CardBackground = Color.Black.copy(0.6f)
private val DateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH-mm-ss", FlorisLocale.default().base)

@Composable
fun DevtoolsOverlay(modifier: Modifier = Modifier) {
    val prefs by FlorisPreferenceStore
    val context = LocalContext.current
    val appContext = context.inferFlorisApplication()
    val themeController = LocalThemeController.current

    val devtoolsEnabled by prefs.devtools.enabled.collectAsState()
    val showPrimaryClip by prefs.devtools.showPrimaryClip.collectAsState()
    val showInputStateOverlay by prefs.devtools.showInputStateOverlay.collectAsState()
    val showSpellingOverlay by prefs.devtools.showSpellingOverlay.collectAsState()
    val showInlineAutofillOverlay by prefs.devtools.showInlineAutofillOverlay.collectAsState()
    val showMozcOverlay by prefs.devtools.showMozcOverlay.collectAsState()
    val prefsLoaded by appContext.preferenceStoreLoaded.collectAsState()

    val theme by themeController.effectiveTheme.collectAsState()

    CompositionLocalProvider(
        LocalContentColor provides Color.White,
    ) {
        Column(modifier = modifier.fillMaxSize()) {
            if (devtoolsEnabled && showPrimaryClip) {
                DevtoolsClipboardOverlay()
            }
            if (devtoolsEnabled && showInputStateOverlay) {
                DevtoolsInputStateOverlay()
            }
            if (devtoolsEnabled && showSpellingOverlay) {
                DevtoolsSpellingOverlay()
            }
            if (devtoolsEnabled && showInlineAutofillOverlay && AndroidVersion.ATLEAST_API30_R) {
                DevtoolsInlineAutofillOverlay()
            }
            val loadFailure = theme.loadFailure
            if (loadFailure != null && prefsLoaded) {
                DevtoolsStylesheetFailedToLoadOverlay(loadFailure)
            }
            if (devtoolsEnabled && showMozcOverlay) {
                DevtoolsMozcOverlay()
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
    val imeController = LocalImeController.current

    val imeState by imeController.activeState.collectAsState()
    val info by remember { derivedStateOf { imeState.editor.info } }
    val content by remember { derivedStateOf { imeState.content } }

    DevtoolsOverlayBox(title = "Input state overlay") {
        DevtoolsSubGroup(title = "EditorInfo") {
            DevtoolsText(text = "Type=${info.inputAttributes.type} Variation=${info.inputAttributes.variation} IsRich=${info.isRichInputEditor}")
            DevtoolsText(text = "InitialSelection: ${info.initialSelection}")
        }
        DevtoolsSubGroup(title = "EditorContent") {
            DevtoolsText(text = "Selection:   { start=${content.selection.start}, end=${content.selection.end} }")
            DevtoolsText(text = "Composition: { start=${content.composition?.start}, end=${content.composition?.end} }")
            DevtoolsText(text = "Before: \"${content.surroundingText.textBefore}\"")
            DevtoolsText(text = "Selected: \"${content.surroundingText.textSelected}\"")
            DevtoolsText(text = "After: \"${content.surroundingText.textAfter}\"")
            DevtoolsText(text = "Composition Text: ${content.compositionText}")
        }
    }
}

@Composable
private fun DevtoolsSpellingOverlay() {
    val context = LocalContext.current
    val nlpManager by context.nlpManager()

    val debugOverlayVersion by nlpManager.debugOverlayVersion.collectAsState()
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

@RequiresApi(Build.VERSION_CODES.R)
@Composable
private fun DevtoolsInlineAutofillOverlay() {
    val inlineSuggestions by NlpInlineAutofill.suggestions.collectAsState()

    DevtoolsOverlayBox(title = "Inline autofill overlay (${inlineSuggestions.size})") {
        for (inlineSuggestion in inlineSuggestions) {
            DevtoolsSubGroup(title = "NlpInlineSuggestion") {
                val info = inlineSuggestion.info
                DevtoolsText(text = "info.type:     ${info.type}")
                DevtoolsText(text = "info.source:   ${info.source}")
                DevtoolsText(text = "info.isPinned: ${info.isPinned}")
                val view = inlineSuggestion.view
                DevtoolsText(text = "view: ${view?.javaClass?.name}")
            }
        }
    }
}

@Composable
private fun DevtoolsMozcOverlay() {
    val mozc = MozcEngine.instance

    DevtoolsOverlayBox(title = "Mozc overlay") {
        DevtoolsSubGroup(title = "MozcPreedit") {
            DevtoolsText(text = "Preedit=${mozc.preedit.collectAsState().value}")
        }
        DevtoolsSubGroup(title = "MozcCandidates") {
            for (candidate in mozc.candidates.collectAsState().value) {
                DevtoolsText(text = "candidate:     ${candidate.value}")
            }
        }
    }
}

@Composable
private fun DevtoolsStylesheetFailedToLoadOverlay(loadFailure: ThemeController.LoadFailure) {
    DevtoolsOverlayBox(title = "Failed to load stylesheet, fell back to base style") {
        DevtoolsSubGroup(title = "Extension") {
            DevtoolsText(text = "id:       ${loadFailure.manifest?.meta?.id}")
            DevtoolsText(text = "title:    ${loadFailure.manifest?.meta?.title}")
            DevtoolsText(text = "version:  ${loadFailure.manifest?.meta?.version}")
        }
        DevtoolsSubGroup(title = "Component") {
            DevtoolsText(text = "id:       ${loadFailure.component?.id}")
            DevtoolsText(text = "label:    ${loadFailure.component?.name}")
            DevtoolsText(text = "path:     ${loadFailure.component?.stylesheetPath()}")
        }
        val cause = loadFailure.cause
        DevtoolsSubGroup(title = "Cause") {
            DevtoolsText(text = "${cause.message}")
        }
        if (cause is SnyggMissingSchemaException) {
            DevtoolsSubGroup(title = "Explanation") {
                DevtoolsText(
                    text = """
                    It appears you’re trying to load a theme designed for FlorisBoard v0.4 (Snygg v1), which isn’t compatible with the latest release using Snygg v2.

                    If you are the theme author, please update your theme to support Snygg v2.

                    If you’re a user, please update your theme via the Addons Store. If an updated version isn’t available yet, please select one of the built-in themes during this transition period.
                """.trimIndent()
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
