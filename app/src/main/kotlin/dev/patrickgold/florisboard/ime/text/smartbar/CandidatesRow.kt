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

package dev.patrickgold.florisboard.ime.text.smartbar

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.app.florisPreferenceModel
import dev.patrickgold.florisboard.ime.nlp.NlpManager
import dev.patrickgold.florisboard.ime.theme.FlorisImeTheme
import dev.patrickgold.florisboard.ime.theme.FlorisImeUi
import dev.patrickgold.florisboard.keyboardManager
import dev.patrickgold.florisboard.lib.android.AndroidVersion
import dev.patrickgold.florisboard.lib.compose.florisHorizontalScroll
import dev.patrickgold.florisboard.lib.observeAsNonNullState
import dev.patrickgold.florisboard.lib.snygg.ui.snyggBackground
import dev.patrickgold.florisboard.lib.snygg.ui.solidColor
import dev.patrickgold.florisboard.lib.snygg.ui.spSize
import dev.patrickgold.florisboard.nlpManager
import dev.patrickgold.jetpref.datastore.model.observeAsState

@Composable
fun CandidatesRow(modifier: Modifier = Modifier) {
    val prefs by florisPreferenceModel()
    val context = LocalContext.current
    val keyboardManager by context.keyboardManager()
    val nlpManager by context.nlpManager()

    val displayMode by prefs.suggestion.displayMode.observeAsState()
    val candidates by nlpManager.candidates.observeAsNonNullState()
    val inlineSuggestions by nlpManager.inlineSuggestions.observeAsNonNullState()

    val spacerStyle = FlorisImeTheme.style.get(FlorisImeUi.SmartbarCandidateSpacer)

    if (AndroidVersion.ATLEAST_API30_R && inlineSuggestions.isNotEmpty()) {
        Row(
            modifier = modifier
                .fillMaxSize()
                .florisHorizontalScroll(),
        ) {
            for (inlineSuggestion in inlineSuggestions) {
                InlineSuggestionView(inlineSuggestion = inlineSuggestion)
            }
        }
    } else {
        Row(
            modifier = modifier
                .fillMaxSize()
                .then(
                    if (displayMode == CandidatesDisplayMode.DYNAMIC_SCROLLABLE && candidates.size > 1) {
                        Modifier.florisHorizontalScroll()
                    } else {
                        Modifier
                    }
                ),
            horizontalArrangement = if (candidates.size > 1) {
                Arrangement.Start
            } else {
                Arrangement.Center
            },
        ) {
            if (candidates.isNotEmpty()) {
                val candidateModifier = if (candidates.size == 1) {
                    Modifier
                        .fillMaxHeight()
                        .weight(1f, fill = false)
                } else {
                    Modifier
                        .fillMaxHeight()
                        .then(
                            if (displayMode == CandidatesDisplayMode.CLASSIC) {
                                Modifier.weight(1f)
                            } else {
                                Modifier.wrapContentWidth()
                            }
                        )
                        .widthIn(max = 180.dp)
                }
                val list = when (displayMode) {
                    CandidatesDisplayMode.CLASSIC -> candidates.subList(0, 3.coerceAtMost(candidates.size))
                    else -> candidates
                }
                for ((n, candidate) in list.withIndex()) {
                    if (n > 0) {
                        Spacer(
                            modifier = Modifier
                                .width(1.dp)
                                .fillMaxHeight(0.6f)
                                .align(Alignment.CenterVertically)
                                .snyggBackground(spacerStyle),
                        )
                    }
                    CandidateItem(
                        modifier = candidateModifier,
                        candidate = candidate,
                        displayMode = displayMode,
                        onClick = {
                            // Can't use candidate directly, reason unknown
                            keyboardManager.commitCandidate(candidates[n])
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun CandidateItem(
    candidate: NlpManager.Candidate,
    displayMode: CandidatesDisplayMode,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = { },
) = with(LocalDensity.current) {
    var isPressed by remember { mutableStateOf(false) }

    val style = if (candidate is NlpManager.Candidate.Clip) {
        FlorisImeTheme.style.get(
            element = FlorisImeUi.SmartbarCandidateClip,
            isPressed = isPressed,
        )
    } else {
        FlorisImeTheme.style.get(
            element = FlorisImeUi.SmartbarCandidateWord,
            isPressed = isPressed,
        )
    }

    Row(
        modifier = modifier
            .snyggBackground(style)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        tryAwaitRelease()
                        isPressed = false
                    },
                    onTap = {
                        onClick()
                    },
                )
            }
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (candidate is NlpManager.Candidate.Clip) {
            Icon(
                modifier = Modifier
                    .requiredSize(
                        style.fontSize
                            .spSize()
                            .toDp() * 1.5f
                    )
                    .padding(end = 4.dp),
                painter = painterResource(R.drawable.ic_assignment),
                contentDescription = null,
                tint = style.foreground.solidColor(),
            )
        }
        Text(
            modifier = Modifier
                .wrapContentHeight()
                .then(
                    if (displayMode == CandidatesDisplayMode.CLASSIC) {
                        Modifier.weight(1f)
                    } else {
                        Modifier
                    }
                ),
            text = candidate.text(),
            color = style.foreground.solidColor(),
            fontSize = style.fontSize.spSize(),
            fontWeight = if (candidate.isAutoInsertWord()) FontWeight.Bold else FontWeight.Normal,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
