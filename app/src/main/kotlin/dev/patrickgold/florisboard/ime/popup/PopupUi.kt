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

package dev.patrickgold.florisboard.ime.popup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import dev.patrickgold.florisboard.ime.keyboard.Key
import dev.patrickgold.florisboard.ime.theme.FlorisImeUi
import org.florisboard.lib.snygg.SnyggQueryAttributes
import org.florisboard.lib.snygg.SnyggSelector
import org.florisboard.lib.snygg.ui.SnyggBox
import org.florisboard.lib.snygg.ui.SnyggColumn
import org.florisboard.lib.snygg.ui.SnyggIcon
import org.florisboard.lib.snygg.ui.SnyggRow
import org.florisboard.lib.snygg.ui.SnyggText

@Composable
fun PopupBaseBox(
    modifier: Modifier = Modifier,
    attributes: SnyggQueryAttributes,
    key: Key,
    shouldIndicateExtendedPopups: Boolean,
): Unit = with(LocalDensity.current) {
    SnyggBox(
        elementName = FlorisImeUi.KeyPopupBox.elementName,
        attributes = attributes,
        modifier = modifier,
    ) {
        key.label?.let { label ->
            SnyggBox(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(key.visibleBounds.height.toDp())
                    .align(Alignment.TopCenter),
            ) {
                SnyggText(
                    modifier = Modifier.align(Alignment.Center),
                    text = label,
                )
            }
        }
        if (shouldIndicateExtendedPopups) {
            SnyggIcon(
                elementName = FlorisImeUi.KeyPopupExtendedIndicator.elementName,
                attributes = attributes,
                modifier = Modifier.align(Alignment.CenterEnd),
                imageVector = Icons.Default.MoreHoriz,
            )
        }
    }
}

@Composable
fun PopupExtBox(
    modifier: Modifier = Modifier,
    attributes: SnyggQueryAttributes,
    elements: List<List<PopupUiController.Element>>,
    elemArrangement: Arrangement.Horizontal,
    elemWidth: Dp,
    elemHeight: Dp,
    activeElementIndex: Int,
): Unit = with(LocalDensity.current) {
    SnyggColumn(FlorisImeUi.KeyPopupBox.elementName, attributes, modifier = modifier) {
        for (row in elements.asReversed()) {
            SnyggRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .requiredHeight(elemHeight),
                horizontalArrangement = elemArrangement,
            ) {
                for (element in row) {
                    val selector = if (activeElementIndex == element.orderedIndex) {
                        SnyggSelector.FOCUS
                    } else {
                        null
                    }
                    SnyggBox(
                        elementName = FlorisImeUi.KeyPopupElement.elementName,
                        attributes = attributes,
                        selector = selector,
                        modifier = Modifier.size(elemWidth, elemHeight),
                    ) {
                        element.label?.let { label ->
                            SnyggText(
                                modifier = Modifier.align(Alignment.Center),
                                text = label,
                            )
                        }
                        element.icon?.let { icon ->
                            SnyggIcon(
                                modifier = Modifier.align(Alignment.Center),
                                imageVector = icon,
                            )
                        }
                    }
                }
            }
        }
    }
}
