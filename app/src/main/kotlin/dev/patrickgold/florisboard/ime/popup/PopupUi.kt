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

package dev.patrickgold.florisboard.ime.popup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import dev.patrickgold.florisboard.ime.keyboard.Key
import dev.patrickgold.florisboard.ime.text.key.KeyCode
import dev.patrickgold.florisboard.ime.theme.FlorisImeTheme
import dev.patrickgold.florisboard.ime.theme.FlorisImeUi
import dev.patrickgold.florisboard.lib.compose.safeTimes
import org.florisboard.lib.snygg.ui.SnyggSurface
import org.florisboard.lib.snygg.ui.snyggBackground
import org.florisboard.lib.snygg.ui.snyggBorder
import org.florisboard.lib.snygg.ui.snyggShadow
import org.florisboard.lib.snygg.ui.solidColor
import org.florisboard.lib.snygg.ui.spSize

@Composable
fun PopupBaseBox(
    modifier: Modifier = Modifier,
    key: Key,
    fontSizeMultiplier: Float,
    shouldIndicateExtendedPopups: Boolean,
): Unit = with(LocalDensity.current) {
    val context = LocalContext.current

    val popupStyle = FlorisImeTheme.style.get(
        element = FlorisImeUi.KeyPopup,
    )
    val fontSize = popupStyle.fontSize.spSize() safeTimes fontSizeMultiplier
    SnyggSurface(
        modifier = modifier,
        style = popupStyle,
        clip = true,
    ) {
        key.label?.let { label ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(key.visibleBounds.height.toDp())
                    .align(Alignment.TopCenter),
            ) {
                Text(
                    modifier = Modifier.align(Alignment.Center),
                    text = label,
                    color = popupStyle.foreground.solidColor(context),
                    fontSize = fontSize,
                    maxLines = 1,
                    softWrap = false,
                )
            }
        }
        if (shouldIndicateExtendedPopups) {
            Icon(
                modifier = Modifier
                    .requiredSize(fontSize.toDp() * 0.65f)
                    .align(Alignment.CenterEnd),
                imageVector = Icons.Default.MoreHoriz,
                contentDescription = null,
                tint = popupStyle.foreground.solidColor(context),
            )
        }
    }
}

@Composable
fun PopupExtBox(
    modifier: Modifier = Modifier,
    elements: List<List<PopupUiController.Element>>,
    fontSizeMultiplier: Float,
    elemArrangement: Arrangement.Horizontal,
    elemWidth: Dp,
    elemHeight: Dp,
    activeElementIndex: Int,
): Unit = with(LocalDensity.current) {
    val context = LocalContext.current

    val popupStyle = FlorisImeTheme.style.get(
        element = FlorisImeUi.KeyPopup,
        isFocus = false,
    )
    Column(
        modifier = modifier
            .snyggShadow(popupStyle)
            .snyggBorder(context, popupStyle)
            .snyggBackground(context, popupStyle),
    ) {
        for (row in elements.asReversed()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .requiredHeight(elemHeight),
                horizontalArrangement = elemArrangement,
            ) {
                for (element in row) {
                    val elemStyle = if (activeElementIndex == element.orderedIndex) {
                        FlorisImeTheme.style.get(
                            element = FlorisImeUi.KeyPopup,
                            isFocus = true,
                        )
                    } else {
                        popupStyle
                    }
                    val elemFontSize = elemStyle.fontSize.spSize() safeTimes fontSizeMultiplier safeTimes
                        if (element.data.code == KeyCode.URI_COMPONENT_TLD) { 0.6f } else { 1.0f }
                    Box(
                        modifier = Modifier
                            .size(elemWidth, elemHeight)
                            .run {
                                if (activeElementIndex == element.orderedIndex) {
                                    snyggBackground(context, elemStyle)
                                } else {
                                    this
                                }
                            },
                    ) {
                        element.label?.let { label ->
                            Text(
                                modifier = Modifier.align(Alignment.Center),
                                text = label,
                                color = elemStyle.foreground.solidColor(context),
                                fontSize = elemFontSize,
                                maxLines = 1,
                                softWrap = false,
                            )
                        }
                        element.icon?.let { icon ->
                            Icon(
                                modifier = Modifier
                                    .requiredSize(elemFontSize.toDp() * 1.1f)
                                    .align(Alignment.Center),
                                imageVector = icon,
                                contentDescription = null,
                                tint = elemStyle.foreground.solidColor(context),
                            )
                        }
                    }
                }
            }
        }
    }
}
