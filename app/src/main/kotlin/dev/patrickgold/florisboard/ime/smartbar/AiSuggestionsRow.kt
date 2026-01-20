/*
 * Copyright (C) 2025 The FlorisBoard Contributors
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

package dev.patrickgold.florisboard.ime.smartbar

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import dev.patrickgold.florisboard.ime.nlp.NlpManager
import dev.patrickgold.florisboard.ime.nlp.ai.TextRewriteProvider
import dev.patrickgold.florisboard.ime.nlp.ai.ToneAdjustmentProvider
import dev.patrickgold.florisboard.ime.theme.FlorisImeUi
import dev.patrickgold.florisboard.keyboardManager
import dev.patrickgold.florisboard.nlpManager
import org.florisboard.lib.snygg.ui.SnyggRow

@Composable
fun AiSuggestionsRow(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val nlpManager by context.nlpManager()
    val keyboardManager by context.keyboardManager()
    
    val aiReplies by nlpManager.aiReplyCandidatesFlow.collectAsState()
    val isLoading by nlpManager.aiLoadingFlow.collectAsState()

    SnyggRow(
        elementName = FlorisImeUi.SmartbarCandidatesRow.elementName,
        modifier = modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // AI Icon / Trigger
        IconButton(
            onClick = { 
                if (keyboardManager.activeState.isSelectionMode) {
                    // Logic for rewrite handled by secondary row or expanding this one
                } else {
                    nlpManager.generateAiReplies() 
                }
            },
            modifier = Modifier.padding(horizontal = 4.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary
                )
            } else {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = "AI Actions",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        if (keyboardManager.activeState.isSelectionMode) {
            // Show Rewrite and Tone Options
            RewriteOptions(nlpManager, keyboardManager)
            ToneOptions(nlpManager, keyboardManager)
        } else if (aiReplies.isNotEmpty()) {
            // Re-use logic for displaying candidates but for AI replies
            for (reply in aiReplies) {
                CandidateItem(
                    candidate = reply,
                    displayMode = CandidatesDisplayMode.DYNAMIC_SCROLLABLE,
                    modifier = Modifier
                        .fillMaxHeight()
                        .wrapContentWidth()
                        .padding(horizontal = 4.dp),
                    onClick = {
                        keyboardManager.commitCandidate(reply)
                        nlpManager.clearAiReplies()
                    },
                    longPressDelay = 500L
                )
            }
        }
    }
}

import dev.patrickgold.florisboard.ime.keyboard.KeyboardManager

@Composable
private fun RowScope.RewriteOptions(nlpManager: NlpManager, keyboardManager: KeyboardManager) {
    val modes = mapOf(
        "Formal" to TextRewriteProvider.RewriteMode.FORMAL,
        "Casual" to TextRewriteProvider.RewriteMode.CASUAL,
        "Short" to TextRewriteProvider.RewriteMode.SHORTER,
        "Long" to TextRewriteProvider.RewriteMode.LONGER,
        "Fix" to TextRewriteProvider.RewriteMode.FIX_GRAMMAR
    )
    for ((label, mode) in modes) {
        CandidateItem(
            candidate = AiSuggestionCandidate(text = label),
            displayMode = CandidatesDisplayMode.DYNAMIC_SCROLLABLE,
            modifier = Modifier
                .fillMaxHeight()
                .wrapContentWidth()
                .padding(horizontal = 4.dp),
            onClick = {
                nlpManager.rewriteSelectedText(mode)
            },
            longPressDelay = 500L
        )
    }
}

@Composable
private fun RowScope.ToneOptions(nlpManager: NlpManager, keyboardManager: KeyboardManager) {
    val tones = mapOf(
        "Prof" to ToneAdjustmentProvider.Tone.PROFESSIONAL,
        "Friend" to ToneAdjustmentProvider.Tone.FRIENDLY,
        "Polite" to ToneAdjustmentProvider.Tone.POLITE,
        "Direct" to ToneAdjustmentProvider.Tone.DIRECT
    )
    for ((label, tone) in tones) {
        CandidateItem(
            candidate = AiSuggestionCandidate(text = label),
            displayMode = CandidatesDisplayMode.DYNAMIC_SCROLLABLE,
            modifier = Modifier
                .fillMaxHeight()
                .wrapContentWidth()
                .padding(horizontal = 4.dp),
            onClick = {
                nlpManager.adjustToneOfSelectedText(tone)
            },
            longPressDelay = 500L
        )
    }
}
