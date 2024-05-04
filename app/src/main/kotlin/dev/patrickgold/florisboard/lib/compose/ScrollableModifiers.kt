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

package dev.patrickgold.florisboard.lib.compose

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridItemInfo
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

private val DefaultScrollbarSize = 4.dp
// IgnoreInVeryFastOut (basically)
private val ScrollbarAnimationEasing = CubicBezierEasing(1f, 0f, 0.82f, -0.13f)

fun Modifier.florisVerticalScroll(
    state: ScrollState? = null,
    showScrollbar: Boolean = true,
    scrollbarWidth: Dp = DefaultScrollbarSize,
) = composed {
    val scrollState = state ?: rememberScrollState()
    if (showScrollbar) {
        verticalScroll(scrollState).florisScrollbar(scrollState, scrollbarWidth, isVertical = true)
    } else {
        verticalScroll(scrollState)
    }
}

fun Modifier.florisHorizontalScroll(
    state: ScrollState? = null,
    showScrollbar: Boolean = true,
    scrollbarHeight: Dp = DefaultScrollbarSize,
) = composed {
    val scrollState = state ?: rememberScrollState()
    if (showScrollbar) {
        horizontalScroll(scrollState).florisScrollbar(scrollState, scrollbarHeight, isVertical = false)
    } else {
        horizontalScroll(scrollState)
    }
}

fun Modifier.florisScrollbar(
    state: ScrollState,
    scrollbarSize: Dp = DefaultScrollbarSize,
    isVertical: Boolean,
): Modifier = composed {
    var isInitial by remember { mutableStateOf(true) }
    val targetAlpha = if (state.isScrollInProgress || isInitial) 1f else 0f
    val duration = if (state.isScrollInProgress || isInitial) 0 else 950
    val alpha by animateFloatAsState(
        targetValue = targetAlpha,
        animationSpec = tween(durationMillis = duration, easing = ScrollbarAnimationEasing),
    )
    val scrollbarColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.28f)

    LaunchedEffect(Unit) {
        delay(1850)
        isInitial = false
    }

    drawWithContent {
        drawContent()
        val needDrawScrollbar = state.isScrollInProgress || isInitial || alpha > 0f
        if (needDrawScrollbar && state.maxValue > 0) {
            val scrollValue = state.value.toFloat()
            val scrollMax = state.maxValue.toFloat()

            val scrollbarWidth: Float
            val scrollbarHeight: Float
            val scrollbarOffsetX: Float
            val scrollbarOffsetY: Float

            if (isVertical) {
                val containerHeight = size.height - scrollMax
                scrollbarWidth = scrollbarSize.toPx()
                scrollbarHeight = containerHeight * (1f - scrollMax / size.height)
                scrollbarOffsetX = size.width - scrollbarWidth
                scrollbarOffsetY = state.value + (containerHeight - scrollbarHeight) * (scrollValue / scrollMax)
            } else {
                val containerWidth = size.width - scrollMax
                scrollbarWidth = containerWidth * (1f - scrollMax / size.width)
                scrollbarHeight = scrollbarSize.toPx()
                scrollbarOffsetX = state.value + (containerWidth - scrollbarWidth) * (scrollValue / scrollMax)
                scrollbarOffsetY = size.height - scrollbarHeight
            }

            drawRect(
                color = scrollbarColor,
                topLeft = Offset(scrollbarOffsetX, scrollbarOffsetY),
                size = Size(scrollbarWidth, scrollbarHeight),
                alpha = alpha,
            )
        }
    }
}

fun Modifier.florisScrollbar(
    state: LazyListState,
    size: Dp = DefaultScrollbarSize,
    color: Color = Color.Unspecified,
    isVertical: Boolean,
): Modifier = composed {
    var isInitial by remember { mutableStateOf(true) }
    val targetAlpha = if (state.isScrollInProgress || isInitial) 1f else 0f
    val duration = if (state.isScrollInProgress || isInitial) 0 else 950
    val alpha by animateFloatAsState(
        targetValue = targetAlpha,
        animationSpec = tween(durationMillis = duration, easing = ScrollbarAnimationEasing),
    )
    val scrollbarColor = color.takeOrElse { MaterialTheme.colorScheme.onSurface.copy(alpha = 0.28f) }

    LaunchedEffect(Unit) {
        delay(1850)
        isInitial = false
    }

    val visibleItemsInfo = remember { derivedStateOf { state.layoutInfo } }.value.visibleItemsInfo
    val visibleItems = if (visibleItemsInfo.isNotEmpty()) remember { visibleItemsInfo.size } else 0
    drawWithContent {
        drawContent()
        val firstVisibleElementIndex = state.layoutInfo.visibleItemsInfo.firstOrNull()?.index
        val needDrawScrollbar = state.isScrollInProgress || isInitial || alpha > 0f
        if (needDrawScrollbar && firstVisibleElementIndex != null) {
            val scrollbarWidth: Float
            val scrollbarHeight: Float
            val scrollbarOffsetX: Float
            val scrollbarOffsetY: Float
            val first = state.layoutInfo.visibleItemsInfo.first()
            if (isVertical) {
                val elementHeight = this.size.height / state.layoutInfo.totalItemsCount
                scrollbarWidth = size.toPx()
                scrollbarHeight = visibleItems * elementHeight
                scrollbarOffsetX = this.size.width - scrollbarWidth
                scrollbarOffsetY = (firstVisibleElementIndex - percentOffset(first)) * elementHeight
            } else {
                val elementWidth = this.size.width / state.layoutInfo.totalItemsCount
                scrollbarWidth = visibleItems * elementWidth
                scrollbarHeight = size.toPx()
                scrollbarOffsetX = (firstVisibleElementIndex - percentOffset(first)) * elementWidth
                scrollbarOffsetY = this.size.height - scrollbarHeight
            }

            drawRect(
                color = scrollbarColor,
                topLeft = Offset(scrollbarOffsetX, scrollbarOffsetY),
                size = Size(scrollbarWidth, scrollbarHeight),
                alpha = alpha,
            )
        }
    }
}

fun Modifier.florisScrollbar(
    state: LazyGridState,
    size: Dp = DefaultScrollbarSize,
    color: Color = Color.Unspecified,
): Modifier = composed {
    var isInitial by remember { mutableStateOf(true) }
    val targetAlpha = if (state.isScrollInProgress || isInitial) 1f else 0f
    val duration = if (state.isScrollInProgress || isInitial) 0 else 950
    val alpha by animateFloatAsState(
        targetValue = targetAlpha,
        animationSpec = tween(durationMillis = duration, easing = ScrollbarAnimationEasing),
    )
    val scrollbarColor = color.takeOrElse { MaterialTheme.colorScheme.onSurface.copy(alpha = 0.28f) }

    LaunchedEffect(Unit) {
        delay(1850)
        isInitial = false
    }

    val orientation = remember { derivedStateOf { state.layoutInfo } }.value.orientation
    val visibleItemsInfo = remember { derivedStateOf { state.layoutInfo } }.value.visibleItemsInfo
    val visibleItems = if (visibleItemsInfo.isNotEmpty()) remember { visibleItemsInfo.size } else 0
    val last = visibleItemsInfo.lastOrNull()
    val stacks = if (last != null && orientation == Orientation.Vertical) {
        remember { last.column }
    } else if (last != null && orientation == Orientation.Horizontal) {
        remember { last.row }
    } else {
        0
    }
    drawWithContent {
        drawContent()
        val firstVisibleElementIndex = state.layoutInfo.visibleItemsInfo.firstOrNull()?.index
        val needDrawScrollbar = state.isScrollInProgress || isInitial || alpha > 0f
        if (needDrawScrollbar && firstVisibleElementIndex != null) {
            val scrollbarWidth: Float
            val scrollbarHeight: Float
            val scrollbarOffsetX: Float
            val scrollbarOffsetY: Float
            val first = state.layoutInfo.visibleItemsInfo.first()

            if (orientation == Orientation.Vertical) {
                val elementHeight = this.size.height / state.layoutInfo.totalItemsCount
                scrollbarWidth = size.toPx()
                scrollbarOffsetX = this.size.width - scrollbarWidth
                scrollbarOffsetY = (firstVisibleElementIndex - stacks*percentOffset(first, orientation)) * elementHeight
                scrollbarHeight = (visibleItems * elementHeight).coerceAtMost(this.size.height - scrollbarOffsetY)
            } else {
                val elementWidth = this.size.width / state.layoutInfo.totalItemsCount
                scrollbarHeight = size.toPx()
                scrollbarOffsetX = (firstVisibleElementIndex - stacks*percentOffset(first, orientation)) * elementWidth
                scrollbarOffsetY = this.size.height - scrollbarHeight
                scrollbarWidth = (visibleItems * elementWidth).coerceAtMost(this.size.height - scrollbarOffsetX)
            }

            drawRect(
                color = scrollbarColor,
                topLeft = Offset(scrollbarOffsetX, scrollbarOffsetY),
                size = Size(scrollbarWidth, scrollbarHeight),
                alpha = alpha,
            )
        }
    }
}

/**
 * Item's offset on main axis as a percentage of size
 */
internal fun percentOffset (
    item: LazyListItemInfo,
): Float {
    return item.offset.toFloat() / item.size
}

internal fun percentOffset (
    item: LazyGridItemInfo,
    orientation: Orientation
): Float {
    val offset = if (orientation == Orientation.Horizontal) {
        item.offset.x
    } else {
        item.offset.y
    }
    val size = if (orientation == Orientation.Horizontal) {
        item.size.width
    } else {
        item.size.height
    }
    return offset.toFloat() / size
}
