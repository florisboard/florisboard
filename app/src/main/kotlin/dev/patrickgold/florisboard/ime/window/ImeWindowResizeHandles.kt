/*
 * Copyright (C) 2025 The FlorisBoard Contributors
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

package dev.patrickgold.florisboard.ime.window

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.ime.theme.FlorisImeUi
import kotlinx.coroutines.launch
import org.florisboard.lib.compose.drawableRes
import org.florisboard.lib.compose.stringRes
import org.florisboard.lib.compose.toDp
import org.florisboard.lib.snygg.ui.SnyggButton
import org.florisboard.lib.snygg.ui.SnyggIcon
import org.florisboard.lib.snygg.ui.SnyggIconButton
import org.florisboard.lib.snygg.ui.SnyggText
import org.florisboard.lib.snygg.ui.rememberSnyggThemeQuery

enum class ImeWindowResizeHandle(
    val left: Boolean = false,
    val top: Boolean = false,
    val right: Boolean = false,
    val bottom: Boolean = false,
) {
    LEFT(left = true),
    TOP_LEFT(top = true, left = true),
    TOP(top = true),
    TOP_RIGHT(top = true, right = true),
    RIGHT(right = true),
    BOTTOM_RIGHT(bottom = true, right = true),
    BOTTOM(bottom = true),
    BOTTOM_LEFT(bottom = true, left = true),
}

@Composable
private fun ImeWindowResizeHandle(
    handle: ImeWindowResizeHandle,
    modifier: Modifier,
) {
    val ims = LocalFlorisImeService.current

    val windowSpec by ims.windowController.activeWindowSpec.collectAsState()
    val windowDefaults by ImeWindowDefaults.rememberDerivedStateOf { windowSpec.orientation }

    val elementName by remember {
        derivedStateOf {
            when (windowSpec) {
                is ImeWindowSpec.Fixed -> FlorisImeUi.WindowResizeHandleFixed.elementName
                is ImeWindowSpec.Floating -> FlorisImeUi.WindowResizeHandleFloating.elementName
            }
        }
    }

    val style = rememberSnyggThemeQuery(elementName)
    val handleColor = style.background()

    Box(
        modifier = modifier
            .size(windowDefaults.resizeHandleTouchSize)
            .pointerInput(Unit) {
                var unconsumed = DpOffset.Zero
                detectDragGestures(
                    onDragStart = {
                        ims.windowController.editor.beginResizeGesture()
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        unconsumed += dragAmount.toDp()
                        val consumed = ims.windowController.editor.resizeBy(handle, unconsumed)
                        unconsumed -= consumed
                    },
                    onDragEnd = {
                        ims.windowController.editor.endResizeGesture()
                        unconsumed = DpOffset.Zero
                    },
                    onDragCancel = {
                        ims.windowController.editor.cancelGesture()
                        unconsumed = DpOffset.Zero
                    },
                )
            }
            .background(Color.Yellow)
            .padding(windowDefaults.resizeHandleDrawPadding)
            .drawBehind {
                val thickness = windowDefaults.resizeHandleDrawThickness.toPx()
                val cornerRadius = windowDefaults.resizeHandleDrawCornerRadius.toPx().let { CornerRadius(it, it) }
                if (handle.top || handle.bottom) {
                    val handleSize = Size(
                        width = size.width,
                        height = thickness,
                    )
                    val topLeft = when {
                        handle.bottom -> Offset(
                            x = 0f,
                            y = size.height - handleSize.height,
                        )
                        else -> Offset.Zero
                    }
                    drawRoundRect(handleColor, topLeft, handleSize, cornerRadius)
                }
                if (handle.left || handle.right) {
                    val handleSize = Size(
                        width = thickness,
                        height = size.height,
                    )
                    val topLeft = when {
                        handle.right -> Offset(
                            x = size.width - handleSize.width,
                            y = 0f,
                        )
                        else -> Offset.Zero
                    }
                    drawRoundRect(handleColor, topLeft, handleSize, cornerRadius)
                }
            },
    )
}

@Composable
fun BoxScope.ImeWindowResizeHandlesFixed(moveModifier: Modifier) {
    val ims = LocalFlorisImeService.current

    val windowSpec by ims.windowController.activeWindowSpec.collectAsState()
    val editorState by ims.windowController.editor.state.collectAsState()
    val windowDefaults by ImeWindowDefaults.rememberDerivedStateOf { windowSpec.orientation }

    val visible by remember {
        derivedStateOf {
            editorState.isEnabled && windowSpec is ImeWindowSpec.Fixed
        }
    }
    val animatedAlpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(350),
    )
    val alphaModifier = Modifier.graphicsLayer { alpha = animatedAlpha }

    if (visible) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .pointerInteropFilter { true }
                .background(Color.Gray.copy(alpha = 0.5f))
        )

        Column(
            modifier = Modifier
                .matchParentSize()
                .padding(vertical = windowDefaults.resizeHandleTouchSize),
            verticalArrangement = Arrangement.SpaceEvenly,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            SnyggButton(
                elementName = FlorisImeUi.WindowResizeActionFixed.elementName,
                onClick = {
                    ims.windowController.scope.launch {
                        ims.windowController.updateWindowConfig { config ->
                            config.copy(
                                fixedProps = config.fixedProps.filterNot { it.key == config.fixedMode },
                            )
                        }
                    }
                },
                contentPadding = PaddingValues(all = 4.dp)
            ) {
                Icon(drawableRes(R.drawable.ic_restart_alt), null)
                SnyggText(text = stringRes(R.string.action__reset))
            }

            SnyggIconButton(FlorisImeUi.WindowMoveHandleFixed.elementName, onClick = {}) {
                SnyggIcon(
                    elementName = FlorisImeUi.WindowMoveHandleFixed.elementName,
                    imageVector = drawableRes(R.drawable.ic_drag_pan),
                    modifier = moveModifier,
                )
            }

            SnyggButton(
                elementName = FlorisImeUi.WindowResizeActionFixed.elementName,
                onClick = {
                    ims.windowController.editor.disable()
                },
                contentPadding = PaddingValues(all = 4.dp)
            ) {
                Icon(drawableRes(R.drawable.ic_check), null)
                Text(stringRes(R.string.action__done))
            }
        }

        ImeWindowResizeHandle(
            handle = ImeWindowResizeHandle.LEFT,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .then(alphaModifier),
        )
        ImeWindowResizeHandle(
            handle = ImeWindowResizeHandle.TOP_LEFT,
            modifier = Modifier
                .align(Alignment.TopStart)
                .then(alphaModifier),
        )
        ImeWindowResizeHandle(
            handle = ImeWindowResizeHandle.TOP,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .then(alphaModifier),
        )
        ImeWindowResizeHandle(
            handle = ImeWindowResizeHandle.TOP_RIGHT,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .then(alphaModifier),
        )
        ImeWindowResizeHandle(
            handle = ImeWindowResizeHandle.RIGHT,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .then(alphaModifier),
        )
        ImeWindowResizeHandle(
            handle = ImeWindowResizeHandle.BOTTOM_RIGHT,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .then(alphaModifier),
        )
        ImeWindowResizeHandle(
            handle = ImeWindowResizeHandle.BOTTOM,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .then(alphaModifier),
        )
        ImeWindowResizeHandle(
            handle = ImeWindowResizeHandle.BOTTOM_LEFT,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .then(alphaModifier),
        )
    }

}

@Composable
fun BoxScope.ImeWindowResizeHandlesFloating() {
    val ims = LocalFlorisImeService.current

    val windowSpec by ims.windowController.activeWindowSpec.collectAsState()
    val editorState by ims.windowController.editor.state.collectAsState()
    val windowDefaults by ImeWindowDefaults.rememberDerivedStateOf { windowSpec.orientation }

    val visible by remember {
        derivedStateOf {
            editorState.isEnabled && !editorState.isMoveGesture && windowSpec is ImeWindowSpec.Floating
        }
    }
    val animatedAlpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(350),
    )
    val alphaModifier = Modifier.graphicsLayer { alpha = animatedAlpha }

    val offset = windowDefaults.resizeHandleTouchOffsetFloating
    if (visible) {
        ImeWindowResizeHandle(
            handle = ImeWindowResizeHandle.TOP_LEFT,
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(-offset, -offset)
                .then(alphaModifier),
        )
        ImeWindowResizeHandle(
            handle = ImeWindowResizeHandle.TOP_RIGHT,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(offset, -offset)
                .then(alphaModifier),
        )
        ImeWindowResizeHandle(
            handle = ImeWindowResizeHandle.BOTTOM_RIGHT,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(offset, offset)
                .then(alphaModifier),
        )
        ImeWindowResizeHandle(
            handle = ImeWindowResizeHandle.BOTTOM_LEFT,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset(-offset, offset)
                .then(alphaModifier),
        )
    }
}
