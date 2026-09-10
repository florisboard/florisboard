/*
 * Copyright (C) 2026 The FlorisBoard Contributors
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

package dev.patrickgold.florisboard.ime.keyboard3.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import dev.patrickgold.florisboard.ime.keyboard3.interaction.LongPress
import dev.patrickgold.florisboard.ime.theme.FlorisImeUi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import org.florisboard.lib.snygg.SnyggQueryAttributes
import org.florisboard.lib.snygg.SnyggSelector
import org.florisboard.lib.snygg.ui.SnyggBox
import org.florisboard.lib.snygg.ui.SnyggIcon

val GlobalStateNumPopupsShowing: StateFlow<Int>
    field = MutableStateFlow(0)

@Composable
fun LongPressBox(
    longPress: LongPress,
    modifier: Modifier = Modifier,
    attributes: SnyggQueryAttributes = emptyMap(),
) {
    val isPopupShowing = longPress.shouldShowSimplePopup() || longPress.shouldShowExtendedPopup()
    DisposableEffect(isPopupShowing) {
        val delta = if (isPopupShowing) 1 else 0
        GlobalStateNumPopupsShowing.update { it + delta }
        onDispose {
            GlobalStateNumPopupsShowing.update { it - delta }
        }
    }

    if (longPress.shouldShowSimplePopup()) {
        SnyggBox(
            elementName = FlorisImeUi.KeyPopupBox.elementName,
            attributes = attributes,
            modifier = modifier.layoutNormalized(longPress.simpleBounds),
        ) {
            Box(Modifier.layoutNormalized(longPress.anchorBounds.localTo(longPress.simpleBounds))) {
                Display3(
                    display = longPress.simpleLabel,
                    modifier = Modifier.align(Alignment.Center),
                )
                if (longPress.simpleIndicateExtended) {
                    SnyggIcon(
                        elementName = FlorisImeUi.KeyPopupExtendedIndicator.elementName,
                        attributes = attributes,
                        modifier = Modifier.align(Alignment.BottomEnd),
                        imageVector = Icons.Default.MoreHoriz,
                    )
                }
            }
        }
    }

    if (longPress.shouldShowExtendedPopup()) {
        SnyggBox(
            elementName = FlorisImeUi.KeyPopupBox.elementName,
            attributes = attributes,
            modifier = modifier.layoutNormalized(longPress.extendedBounds),
        ) {
            val extendedKeys = longPress.extendedKeys
            val focusedIndex = longPress.extendedFocusedIndex
            extendedKeys.forEachIndexed { index, extendedKey ->
                val output = extendedKey.data.output
                val attributes = remember(attributes, extendedKey) {
                    if (output != null) {
                        attributes.plus(FlorisImeUi.Attr.Output to output.asAttrValue())
                    } else {
                        attributes
                    }
                }
                val selector = if (focusedIndex == index) SnyggSelector.FOCUS else null
                SnyggBox(
                    elementName = FlorisImeUi.KeyPopupElement.elementName,
                    attributes = attributes,
                    selector = selector,
                    modifier = modifier.layoutNormalized(extendedKey.bounds.localTo(longPress.extendedBounds)),
                ) {
                    Display3(
                        display = extendedKey.label,
                        modifier = Modifier.align(Alignment.Center),
                    )
                }
            }
        }
    }
}

private fun Rect.localTo(parentBounds: Rect): Rect {
    return Rect(
        left = (left - parentBounds.left) / parentBounds.width,
        top = (top - parentBounds.top) / parentBounds.height,
        right = (right - parentBounds.left) / parentBounds.width,
        bottom = (bottom - parentBounds.top) / parentBounds.height,
    )
}
