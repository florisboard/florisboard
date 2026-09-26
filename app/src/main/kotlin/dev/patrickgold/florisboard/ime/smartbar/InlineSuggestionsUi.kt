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

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.annotation.RequiresApi
import androidx.autofill.inline.UiVersions
import androidx.autofill.inline.common.ImageViewStyle
import androidx.autofill.inline.common.TextViewStyle
import androidx.autofill.inline.common.ViewStyle
import androidx.autofill.inline.v1.InlineSuggestionUi
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.graphics.ColorUtils
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.ime.keyboard3.ui.GlobalStateNumPopupsShowing
import dev.patrickgold.florisboard.ime.nlp.NlpInlineAutofillSuggestion
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

/**
 * Creates a new inline suggestion UI bundle.
 *
 * @param context The context of the parent view/controller.
 *
 * @return A bundle containing all necessary attributes for the inline suggestion views to properly display.
 */
@SuppressLint("RestrictedApi")
@RequiresApi(Build.VERSION_CODES.R)
fun createInlineSuggestionUiStyleBundle(context: Context): Bundle? {
    val styleSet = CachedInlineSuggestionsChipStyleSet ?: return null
    val bgColor = styleSet.background(default = Color.White)
    val fgColor = styleSet.foreground(default = Color.Black)

    val bgDrawableId = R.drawable.inline_autofill_chip_bg
    val bgDrawable = Icon.createWithResource(context, bgDrawableId).apply {
        setTint(bgColor.toArgb())
    }
    val chipStyle = ViewStyle.Builder().run {
        setBackground(bgDrawable)
        setPadding(
            context.resources.getDimension(R.dimen.suggestion_chip_bg_padding_start).toInt(),
            context.resources.getDimension(R.dimen.suggestion_chip_bg_padding_top).toInt(),
            context.resources.getDimension(R.dimen.suggestion_chip_bg_padding_end).toInt(),
            context.resources.getDimension(R.dimen.suggestion_chip_bg_padding_bottom).toInt(),
        )
        build()
    }
    val iconStyle = ImageViewStyle.Builder().run {
        setLayoutMargin(0, 0, 0, 0)
        build()
    }
    val titleStyle = TextViewStyle.Builder().run {
        setLayoutMargin(
            context.resources.getDimension(R.dimen.suggestion_chip_fg_title_margin_start).toInt(),
            context.resources.getDimension(R.dimen.suggestion_chip_fg_title_margin_top).toInt(),
            context.resources.getDimension(R.dimen.suggestion_chip_fg_title_margin_end).toInt(),
            context.resources.getDimension(R.dimen.suggestion_chip_fg_title_margin_bottom).toInt(),
        )
        setTextColor(fgColor.toArgb())
        setTextSize(16f)
        build()
    }
    val subtitleStyle = TextViewStyle.Builder().run {
        setLayoutMargin(
            context.resources.getDimension(R.dimen.suggestion_chip_fg_subtitle_margin_start).toInt(),
            context.resources.getDimension(R.dimen.suggestion_chip_fg_subtitle_margin_top).toInt(),
            context.resources.getDimension(R.dimen.suggestion_chip_fg_subtitle_margin_end).toInt(),
            context.resources.getDimension(R.dimen.suggestion_chip_fg_subtitle_margin_bottom).toInt(),
        )
        setTextColor(ColorUtils.setAlphaComponent(fgColor.toArgb(), 150))
        setTextSize(14f)
        build()
    }
    val suggestionStyle = InlineSuggestionUi.newStyleBuilder().run {
        setSingleIconChipStyle(chipStyle)
        setChipStyle(chipStyle)
        setStartIconStyle(iconStyle)
        setEndIconStyle(iconStyle)
        setTitleStyle(titleStyle)
        setSubtitleStyle(subtitleStyle)
        build()
    }
    return UiVersions.newStylesBuilder().run {
        addStyle(suggestionStyle)
        build()
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
