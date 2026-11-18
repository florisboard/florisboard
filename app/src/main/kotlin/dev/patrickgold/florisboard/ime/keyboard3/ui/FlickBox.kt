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
import dev.patrickgold.florisboard.ime.keyboard3.interaction.Flick
import dev.patrickgold.florisboard.ime.keyboard3.interaction.LongPress
import dev.patrickgold.florisboard.ime.keyboard3.touch.TouchFlickSegment
import dev.patrickgold.florisboard.ime.theme.FlorisImeUi
import dev.patrickgold.florisboard.ime.theme.ImeUiAttrs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import org.florisboard.lib.snygg.SnyggQueryAttributes
import org.florisboard.lib.snygg.SnyggSelector
import org.florisboard.lib.snygg.ui.SnyggBox
import org.florisboard.lib.snygg.ui.SnyggIcon
import org.k3lp.lib.text.K3StringOrDescriptor
import org.k3lp.model.flick.K3FlickDirection.EAST
import org.k3lp.model.flick.K3FlickDirection.NORTH
import org.k3lp.model.flick.K3FlickDirection.NORTH_EAST
import org.k3lp.model.flick.K3FlickDirection.NORTH_WEST
import org.k3lp.model.flick.K3FlickDirection.SOUTH
import org.k3lp.model.flick.K3FlickDirection.SOUTH_EAST
import org.k3lp.model.flick.K3FlickDirection.SOUTH_WEST
import org.k3lp.model.flick.K3FlickDirection.WEST

val GlobalStateNumFlickPopupsShowing: StateFlow<Int>
    field = MutableStateFlow(0)

@Composable
fun FlickBox(
    flick: Flick,
    modifier: Modifier = Modifier,
    attributes: SnyggQueryAttributes = emptyMap(),
) {
    val isPopupShowing = flick.shouldShowPopup()
    DisposableEffect(isPopupShowing) {
        val delta = if (isPopupShowing) 1 else 0
        GlobalStateNumFlickPopupsShowing.update { it + delta }
        onDispose {
            GlobalStateNumFlickPopupsShowing.update { it - delta }
        }
    }

    if (isPopupShowing) {
        SnyggBox(
            elementName = FlorisImeUi.KeyPopupBox.elementName,
            attributes = attributes,
            modifier = modifier.layoutNormalized(flick.simpleBounds),
        ) {
            Box(Modifier.layoutNormalized(flick.anchorBounds.localTo(flick.simpleBounds))) {
                val flickByDirection = flick.flicks.find { flick.direction == it.direction }
                if (flickByDirection == null) {
                    Display3(
                        display = flick.simpleLabel,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .scaleToFitHorizontally(),
                    )
                    val flicks = flick.flicks
                    flicks.forEach { flick ->
                        val output = flick.data.output
                        val attributes = remember(attributes, flick) {
                            if (output != null) {
                                attributes.plus(ImeUiAttrs.Output.eq(output))
                            } else {
                                attributes
                            }
                        }
                        Display3(
                            elementName = FlorisImeUi.KeyHint.elementName,
                            attributes = attributes,
                            display = flick.display,
                            modifier = Modifier
                                .align(Alignment.fromFlick(flick))
                                .scaleToFitHorizontally(),
                        )
                    }
                } else {
                    Display3(
                        display = flickByDirection.display,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .scaleToFitHorizontally(),
                    )
                }
            }
        }
    }
}

fun Alignment.Companion.fromFlick(flick: TouchFlickSegment): Alignment {
    return when (flick.direction) {
        NORTH -> Alignment.TopCenter
        EAST -> Alignment.CenterEnd // assuming Ltr, TODO: Figure out how flick would work Rtl
        SOUTH -> Alignment.BottomCenter
        WEST -> Alignment.CenterStart // assuming Ltr, TODO: Figure out how flick would work Rtl
        NORTH_EAST -> Alignment.TopEnd // assuming Ltr, TODO: Figure out how flick would work Rtl
        NORTH_WEST -> Alignment.TopStart // assuming Ltr, TODO: Figure out how flick would work Rtl
        SOUTH_EAST -> Alignment.BottomEnd // assuming Ltr, TODO: Figure out how flick would work Rtl
        SOUTH_WEST -> Alignment.BottomStart // assuming Ltr, TODO: Figure out how flick would work Rtl
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
