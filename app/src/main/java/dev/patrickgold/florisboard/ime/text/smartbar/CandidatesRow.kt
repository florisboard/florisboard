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
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.app.prefs.florisPreferenceModel
import dev.patrickgold.florisboard.app.ui.components.florisHorizontalScroll
import dev.patrickgold.florisboard.clipboardManager
import dev.patrickgold.florisboard.ime.clipboard.provider.ClipboardItem
import dev.patrickgold.florisboard.ime.theme.FlorisImeTheme
import dev.patrickgold.florisboard.ime.theme.FlorisImeUi
import dev.patrickgold.florisboard.nlpManager
import dev.patrickgold.florisboard.snygg.ui.snyggBackground
import dev.patrickgold.florisboard.snygg.ui.solidColor
import dev.patrickgold.florisboard.snygg.ui.spSize
import dev.patrickgold.jetpref.datastore.model.observeAsState

@Composable
fun CandidatesRow() {
    val prefs by florisPreferenceModel()
    val context = LocalContext.current
    val clipboardManager by context.clipboardManager()
    val nlpManager by context.nlpManager()

    val displayMode by prefs.suggestion.displayMode.observeAsState()
    val primaryClip by clipboardManager.primaryClip.observeAsState()
    val candidates by nlpManager.candidates.observeAsState()

    val isPrimaryWordBold = remember(candidates) { candidates?.isPrimaryTokenAutoInsert == true }
    val computedCandidates = remember(primaryClip, candidates) {
        buildList {
            primaryClip?.let { add(ComputedCandidate.Clip(it)) }
            candidates?.forEach { add(ComputedCandidate.Word(it)) }
        }
    }

    val rowStyle = FlorisImeTheme.style.get(FlorisImeUi.SmartbarCandidateRow)
    val spacerStyle = FlorisImeTheme.style.get(FlorisImeUi.SmartbarCandidateSpacer)

    Row(
        modifier = Modifier
            .fillMaxSize()
            .snyggBackground(rowStyle.background)
            .then(
                if (displayMode == CandidatesDisplayMode.DYNAMIC_SCROLLABLE && computedCandidates.size > 1) {
                    Modifier.florisHorizontalScroll()
                } else {
                    Modifier
                }
            ),
        horizontalArrangement = if (computedCandidates.size > 1) {
            Arrangement.Start
        } else {
            Arrangement.Center
        },
    ) {
        if (computedCandidates.isNotEmpty()) {
            val modifier = if (computedCandidates.size == 1) {
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
                CandidatesDisplayMode.CLASSIC -> computedCandidates.subList(0, 3.coerceAtMost(computedCandidates.size))
                else -> computedCandidates
            }
            for ((n, computedCandidate) in list.withIndex()) {
                if (n > 0) {
                    Spacer(
                        modifier = Modifier
                            .width(1.dp)
                            .fillMaxHeight(0.6f)
                            .align(Alignment.CenterVertically)
                            .snyggBackground(spacerStyle.background),
                    )
                }
                CandidateItem(
                    modifier = modifier,
                    computedCandidate = computedCandidate,
                    displayMode = displayMode,
                )
            }
        }
    }
}

@Composable
private fun CandidateItem(
    computedCandidate: ComputedCandidate,
    displayMode: CandidatesDisplayMode,
    modifier: Modifier = Modifier,
) = with(LocalDensity.current) {
    var isPressed by remember { mutableStateOf(false) }

    val style = if (computedCandidate is ComputedCandidate.Clip) {
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
            .snyggBackground(style.background, style.shape)
            .pointerInput(Unit) {
                detectTapGestures(onPress = {
                    isPressed = true
                    tryAwaitRelease()
                    isPressed = false
                })
            }
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (computedCandidate is ComputedCandidate.Clip) {
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
            text = computedCandidate.text(),
            color = style.foreground.solidColor(),
            fontSize = style.fontSize.spSize(),
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * Data class describing a computed candidate item.
 */
private sealed class ComputedCandidate {
    fun text(): String {
        return when (this) {
            is Clip -> clipboardItem.stringRepresentation()
            is Word -> word
        }
    }

    /**
     * Computed word candidate, used for suggestions provided by the NLP algorithm.
     *
     * @property word The word this computed candidate item represents. Used in the callback to provide which word
     *  should be filled out.
     */
    class Word(val word: String) : ComputedCandidate()

    /**
     * Computed word candidate, used for clipboard paste suggestions.
     *
     * @property clipboardItem The clipboard item this computed candidate item represents. Used in the callback to
     *  provide which item should be pasted.
     */
    class Clip(val clipboardItem: ClipboardItem) : ComputedCandidate()
}
