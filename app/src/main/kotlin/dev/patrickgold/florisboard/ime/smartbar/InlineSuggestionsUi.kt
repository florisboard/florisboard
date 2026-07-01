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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.ime.nlp.NlpInlineAutofillSuggestion
import dev.patrickgold.florisboard.ime.popup.GlobalStateNumPopupsShowing
import dev.patrickgold.florisboard.ime.theme.FlorisImeUi
import dev.patrickgold.florisboard.lib.toIntOffset
import org.florisboard.lib.compose.florisHorizontalScroll
import org.florisboard.lib.snygg.SnyggSinglePropertySet
import org.florisboard.lib.snygg.ui.rememberSnyggThemeQuery

val InlineSuggestionsChipMargin = PaddingValues(5.dp)

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
    val isZOrderedOnTop by remember { derivedStateOf { numPopupsShowing == 0 } }

    Row(
        modifier
            .fillMaxSize()
            .florisHorizontalScroll(
                state = scrollState,
                scrollbarHeight = CandidatesRowScrollbarHeight,
            ),
    ) {
        for (inlineSuggestion in inlineSuggestions) {
            if (inlineSuggestion.view == null) {
                continue
            }
            var chipPos by remember { mutableStateOf(IntOffset.Zero) }
            val corderRadius = dimensionResource(R.dimen.suggestions_chip_corner_radius)
            val shape = remember(corderRadius) { RoundedCornerShape(corderRadius) }
            AndroidView(
                modifier = Modifier
                    .onGloballyPositioned { chipPos = it.positionInParent().toIntOffset() }
                    .padding(InlineSuggestionsChipMargin)
                    .clip(shape),
                factory = { inlineSuggestion.view },
                update = { view ->
                    view.isZOrderedOnTop = isZOrderedOnTop
                    // TODO scroll clip can probably also be done in Jetpack Compose
                    val xMin = scrollState.value
                    val xMax = scrollState.value + scrollState.viewportSize
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
