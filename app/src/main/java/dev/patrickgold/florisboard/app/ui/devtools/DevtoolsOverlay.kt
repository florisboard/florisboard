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

package dev.patrickgold.florisboard.app.ui.devtools

import android.view.textservice.SuggestionsInfo
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
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
import dev.patrickgold.florisboard.app.prefs.florisPreferenceModel
import dev.patrickgold.florisboard.clipboardManager
import dev.patrickgold.florisboard.common.FlorisLocale
import dev.patrickgold.florisboard.common.observeAsNonNullState
import dev.patrickgold.florisboard.spellingManager
import dev.patrickgold.jetpref.datastore.model.observeAsState
import java.text.SimpleDateFormat
import java.util.*

private val CardBackground = Color.Black.copy(0.6f)
private val DateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH-mm-ss", FlorisLocale.default().base)

@Composable
fun DevtoolsOverlay(
    modifier: Modifier = Modifier,
) {
    val prefs by florisPreferenceModel()
    val context = LocalContext.current
    val clipboardManager by context.clipboardManager()

    val showPrimaryClip by prefs.devtools.showPrimaryClip.observeAsState()
    val showSpellingOverlay by prefs.devtools.showSpellingOverlay.observeAsState()

    CompositionLocalProvider(
        LocalContentColor provides Color.White,
        LocalLayoutDirection provides LayoutDirection.Ltr,
    ) {
        Column(modifier = modifier) {
            if (showPrimaryClip) {
                val primaryClip by clipboardManager.primaryClip.observeAsState()
                Text(
                    text = primaryClip.toString(),
                    color = Color.White,
                )
            }
            if (showSpellingOverlay) {
                DevtoolsSpellingOverlay()
            }
        }
    }
}

@Composable
private fun DevtoolsSpellingOverlay() {
    val context = LocalContext.current
    val spellingManager by context.spellingManager()

    val debugOverlayVersion by spellingManager.debugOverlayVersion.observeAsNonNullState()
    val suggestionsInfos = remember(debugOverlayVersion) { spellingManager.debugOverlaySuggestionsInfos.snapshot() }

    Column(
        modifier = Modifier
            .padding(all = 8.dp)
            .fillMaxWidth()
            .background(CardBackground),
    ) {
        val sortedEntries = suggestionsInfos.entries.sortedByDescending { it.key }
        Text(
            modifier = Modifier.padding(all = 8.dp),
            text = "Spelling overlay (${sortedEntries.size})",
            fontSize = 14.sp,
        )
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
