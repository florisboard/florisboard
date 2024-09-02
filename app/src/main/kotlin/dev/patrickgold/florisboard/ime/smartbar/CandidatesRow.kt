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

package dev.patrickgold.florisboard.ime.smartbar

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventTimeoutCancellationException
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.patrickgold.florisboard.app.florisPreferenceModel
import dev.patrickgold.florisboard.ime.nlp.ClipboardSuggestionCandidate
import dev.patrickgold.florisboard.ime.nlp.SuggestionCandidate
import dev.patrickgold.florisboard.ime.theme.FlorisImeTheme
import dev.patrickgold.florisboard.ime.theme.FlorisImeUi
import dev.patrickgold.florisboard.keyboardManager
import dev.patrickgold.florisboard.lib.compose.conditional
import dev.patrickgold.florisboard.lib.compose.florisHorizontalScroll
import dev.patrickgold.florisboard.lib.compose.safeTimes
import dev.patrickgold.florisboard.lib.observeAsNonNullState
import dev.patrickgold.florisboard.nlpManager
import dev.patrickgold.florisboard.subtypeManager
import dev.patrickgold.jetpref.datastore.model.observeAsState
import org.florisboard.lib.android.AndroidVersion
import org.florisboard.lib.snygg.ui.snyggBackground
import org.florisboard.lib.snygg.ui.solidColor
import org.florisboard.lib.snygg.ui.spSize

private val CandidatesRowScrollbarHeight = 2.dp

@Composable
fun CandidatesRow(modifier: Modifier = Modifier) {
    val prefs by florisPreferenceModel()
    val context = LocalContext.current
    val keyboardManager by context.keyboardManager()
    val nlpManager by context.nlpManager()
    val subtypeManager by context.subtypeManager()

    val displayMode by prefs.suggestion.displayMode.observeAsState()
    val candidates by nlpManager.activeCandidatesFlow.collectAsState()
    val inlineSuggestions by nlpManager.inlineSuggestions.observeAsNonNullState()

    val rowStyle = FlorisImeTheme.style.get(FlorisImeUi.SmartbarCandidatesRow)
    val spacerStyle = FlorisImeTheme.style.get(FlorisImeUi.SmartbarCandidateSpacer)

    if (AndroidVersion.ATLEAST_API30_R && inlineSuggestions.isNotEmpty()) {
        Row(
            modifier = modifier
                .fillMaxSize()
                .florisHorizontalScroll(scrollbarHeight = CandidatesRowScrollbarHeight),
        ) {
            for (inlineSuggestion in inlineSuggestions) {
                InlineSuggestionView(inlineSuggestion = inlineSuggestion)
            }
        }
    } else {
        Row(
            modifier = modifier
                .fillMaxSize()
                .snyggBackground(context, rowStyle)
                .conditional(displayMode == CandidatesDisplayMode.DYNAMIC_SCROLLABLE && candidates.size > 1) {
                    florisHorizontalScroll(scrollbarHeight = CandidatesRowScrollbarHeight)
                },
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
                        .conditional(displayMode == CandidatesDisplayMode.CLASSIC) {
                            weight(1f)
                        }
                        .conditional(displayMode != CandidatesDisplayMode.CLASSIC) {
                            wrapContentWidth().widthIn(max = 160.dp)
                        }
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
                                .snyggBackground(context, spacerStyle),
                        )
                    }
                    CandidateItem(
                        modifier = candidateModifier,
                        candidate = candidate,
                        displayMode = displayMode,
                        onClick = {
                            // Can't use candidate directly
                            keyboardManager.commitCandidate(candidates[n])
                        },
                        onLongPress = {
                            // Can't use candidate directly
                            val candidateItem = candidates[n]
                            if (candidateItem.isEligibleForUserRemoval) {
                                nlpManager.removeSuggestion(subtypeManager.activeSubtype, candidateItem)
                            } else {
                                false
                            }
                        },
                        longPressDelay = prefs.keyboard.longPressDelay.get().toLong(),
                    )
                }
            }
        }
    }
}

@Composable
private fun CandidateItem(
    candidate: SuggestionCandidate,
    displayMode: CandidatesDisplayMode,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = { },
    onLongPress: () -> Boolean = { false },
    longPressDelay: Long,
) = with(LocalDensity.current) {
    val context = LocalContext.current
    var isPressed by remember { mutableStateOf(false) }

    val style = if (candidate is ClipboardSuggestionCandidate) {
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
            .snyggBackground(context, style)
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    isPressed = true
                    if (down.pressed != down.previousPressed) down.consume()
                    var upOrCancel: PointerInputChange? = null
                    try {
                        upOrCancel = withTimeout(longPressDelay) {
                            waitForUpOrCancellation()
                        }
                        upOrCancel?.let { if (it.pressed != it.previousPressed) it.consume() }
                    } catch (_: PointerEventTimeoutCancellationException) {
                        if (onLongPress()) {
                            upOrCancel = null
                            isPressed = false
                        }
                        waitForUpOrCancellation()?.let { if (it.pressed != it.previousPressed) it.consume() }
                    }
                    if (upOrCancel != null) {
                        onClick()
                    }
                    isPressed = false
                }
            }
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (candidate.icon != null) {
            Icon(
                modifier = Modifier
                    .requiredSize(
                        style.fontSize
                            .spSize()
                            .toDp() * 1.5f
                    )
                    .padding(end = 4.dp),
                imageVector = candidate.icon!!,
                contentDescription = null,
                tint = style.foreground.solidColor(context),
            )
        }
        Column(
            modifier = if (displayMode == CandidatesDisplayMode.CLASSIC) Modifier.weight(1f) else Modifier,
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = candidate.text.toString(),
                color = style.foreground.solidColor(context),
                fontSize = style.fontSize.spSize(),
                fontWeight = if (candidate.isEligibleForAutoCommit) FontWeight.Bold else FontWeight.Normal,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (candidate.secondaryText != null) {
                Text(
                    text = candidate.secondaryText!!.toString(),
                    color = style.foreground.solidColor(context),
                    fontSize = style.fontSize.spSize() safeTimes 0.6,
                    fontWeight = if (candidate.isEligibleForAutoCommit) FontWeight.Bold else FontWeight.Normal,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
