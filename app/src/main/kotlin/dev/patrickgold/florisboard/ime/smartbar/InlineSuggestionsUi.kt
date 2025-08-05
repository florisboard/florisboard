/*
 * Copyright (C) 2024-2025 The FlorisBoard Contributors
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

import android.os.Build
import android.view.View
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.viewinterop.AndroidView
import dev.patrickgold.florisboard.ime.nlp.NlpInlineAutofillSuggestion
import dev.patrickgold.florisboard.ime.popup.GlobalStateNumPopupsShowing
import dev.patrickgold.florisboard.ime.theme.FlorisImeUi
import dev.patrickgold.florisboard.lib.toIntOffset
import org.florisboard.lib.compose.florisHorizontalScroll
import org.florisboard.lib.snygg.SnyggSinglePropertySet
import org.florisboard.lib.snygg.ui.rememberSnyggThemeQuery

var CachedInlineSuggestionsChipStyleSet: SnyggSinglePropertySet? = null

@Composable
fun InlineSuggestionsStyleCache() {
    val chipStyleSet = rememberSnyggThemeQuery(FlorisImeUi.InlineAutofillChip.elementName)
    LaunchedEffect(chipStyleSet) {
        CachedInlineSuggestionsChipStyleSet = chipStyleSet
    }
}

@RequiresApi(Build.VERSION_CODES.R)
@Composable
fun InlineSuggestionsUi(
    inlineSuggestions: List<NlpInlineAutofillSuggestion>,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()
    val numPopupsShowing by GlobalStateNumPopupsShowing.collectAsState()
    val isZOrderedOnTop = numPopupsShowing == 0

    Row(
        modifier
            .fillMaxSize()
            .florisHorizontalScroll(
                state = scrollState,
                scrollbarHeight = CandidatesRowScrollbarHeight,
            ),
    ) {
        val xMin = scrollState.value
        val xMax = scrollState.value + scrollState.viewportSize
        for (inlineSuggestion in inlineSuggestions) {
            if (inlineSuggestion.view == null) {
                continue
            }
            var chipPos by remember { mutableStateOf(IntOffset.Zero) }
            AndroidView(
                modifier = Modifier.onGloballyPositioned { chipPos = it.positionInParent().toIntOffset() },
                factory = { inlineSuggestion.view },
                update = { view ->
                    view.isZOrderedOnTop = isZOrderedOnTop
                    view.clipBounds = android.graphics.Rect(
                        (xMin - chipPos.x).coerceAtLeast(0),
                        0,
                        (xMax - chipPos.x).coerceAtMost(view.width),
                        view.height,
                    )
                    view.visibility = if (view.clipBounds.isEmpty) View.INVISIBLE else View.VISIBLE
                }
            )
        }
    }
}
