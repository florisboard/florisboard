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

import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ZoomOutMap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.roundToIntRect
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.app.devtools.DevtoolsOverlay
import dev.patrickgold.florisboard.ime.ImeUiMode
import dev.patrickgold.florisboard.ime.clipboard.ClipboardInputLayout
import dev.patrickgold.florisboard.ime.keyboard.KeyboardState
import dev.patrickgold.florisboard.ime.keyboard.ProvideKeyboardRowBaseHeight
import dev.patrickgold.florisboard.ime.media.MediaInputLayout
import dev.patrickgold.florisboard.ime.sheet.BottomSheetWindow
import dev.patrickgold.florisboard.ime.text.TextInputLayout
import dev.patrickgold.florisboard.ime.theme.FlorisImeUi
import dev.patrickgold.florisboard.keyboardManager
import dev.patrickgold.florisboard.lib.compose.FloatingSystemUiIme
import dev.patrickgold.florisboard.lib.compose.SystemUiIme
import kotlinx.coroutines.delay
import org.florisboard.lib.android.AndroidVersion
import org.florisboard.lib.compose.drawBorder
import org.florisboard.lib.compose.fold
import org.florisboard.lib.compose.ifIsInstance
import org.florisboard.lib.compose.toDp
import org.florisboard.lib.compose.toDpRect
import org.florisboard.lib.snygg.ui.SnyggBox
import org.florisboard.lib.snygg.ui.SnyggIcon
import org.florisboard.lib.snygg.ui.SnyggIconButton
import org.florisboard.lib.snygg.ui.SnyggSurfaceView

@Composable
fun ImeRootWindow() {
    val ims = LocalFlorisImeService.current
    val density = LocalDensity.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures {
                    ims.windowController.editor.disableIfNoGestureInProgress()
                }
            }
            .onGloballyPositioned { coords ->
                val boundsPx = coords.boundsInRoot().roundToIntRect()
                val newInsets = ImeInsets(
                    boundsDp = with(density) { boundsPx.toDpRect() },
                    boundsPx = boundsPx,
                )
                ims.windowController.updateRootInsets(newInsets)
            },
    ) {
        DevtoolsOverlay()
        ImeWindow()
        BottomSheetWindow()
        SystemUiIme()
    }
}

@Composable
fun BoxScope.ImeWindow() {
    val ims = LocalFlorisImeService.current
    val density = LocalDensity.current
    val keyboardManager by ims.keyboardManager()

    val state by keyboardManager.activeState.collectAsState()
    val windowSpec by ims.windowController.activeWindowSpec.collectAsState()
    val navBarFrameView = remember { ims.findNavBarFrameOrNull() }

    LaunchedEffect(windowSpec) {
        navBarFrameView?.scaleY = when (windowSpec) {
            is ImeWindowSpec.Fixed -> 1f
            is ImeWindowSpec.Floating -> 0f
        }
    }

    val layoutDirection = LocalLayoutDirection.current
    LaunchedEffect(layoutDirection) {
        keyboardManager.activeState.layoutDirection = layoutDirection
    }

    val attributes = remember(state.keyboardMode, state.inputShiftState) {
        mapOf(
            FlorisImeUi.Attr.Mode to state.keyboardMode.toString(),
            FlorisImeUi.Attr.ShiftState to state.inputShiftState.toString(),
        )
    }

    val modeModifier = Modifier
        .pointerInput(Unit) {
            detectTapGestures {
                ims.windowController.editor.toggleEnabled()
            }
        }
        .pointerInput(Unit) {
            detectDragGestures(
                onDragStart = {
                    ims.windowController.editor.beginMoveGesture()
                },
                onDrag = { change, dragAmount ->
                    change.consume()
                    ims.windowController.editor.moveBy(dragAmount.toDp())
                },
                onDragEnd = {
                    ims.windowController.editor.endMoveGesture()
                },
                onDragCancel = {
                    ims.windowController.editor.cancelGesture()
                },
            )
        }

    FloatingDockToFixedIndicator()

    SnyggBox(
        elementName = FlorisImeUi.Window.elementName,
        attributes = attributes,
        modifier = Modifier
            .align(Alignment.BottomStart)
            .ifIsInstance<ImeWindowProps.Fixed>(windowSpec.props) {
                Modifier
                    .fillMaxWidth()
            }
            .ifIsInstance<ImeWindowProps.Floating>(windowSpec.props) { props ->
                Modifier
                    .offset(props.offsetLeft, -props.offsetBottom)
                    .width(props.keyboardWidth)
            }
            .wrapContentHeight()
            .onGloballyPositioned { coords ->
                val boundsPx = coords.boundsInRoot().roundToIntRect()
                val newInsets = ImeInsets(
                    boundsDp = with(density) { boundsPx.toDpRect() },
                    boundsPx = boundsPx,
                )
                ims.windowController.updateWindowInsets(newInsets)
            },
        supportsBackgroundImage = !AndroidVersion.ATLEAST_API30_R,
        allowClip = false,
    ) {
        // The SurfaceView is used to render the background image under inline-autofill chips. These are only
        // available on Android >=11, and SurfaceView causes trouble on Android 8/9, thus we render the image
        // in the SurfaceView for Android >=11, and in the Compose View Tree for Android <=10.
        if (AndroidVersion.ATLEAST_API30_R) {
            SnyggSurfaceView(
                elementName = FlorisImeUi.Window.elementName,
                attributes = attributes,
                modifier = Modifier.matchParentSize(),
            )
        }
        OneHandedPanel()
        ProvideKeyboardRowBaseHeight(windowSpec) {
            ImeInnerWindow(state, windowSpec, modeModifier)
        }
        FloatingResizeHandles()
    }
}

@Composable
private fun ImeInnerWindow(state: KeyboardState, windowSpec: ImeWindowSpec, moveModifier: Modifier) {
    SnyggBox(
        elementName = FlorisImeUi.WindowInner.elementName,
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .ifIsInstance<ImeWindowProps.Fixed>(windowSpec.props) { props ->
                Modifier
                    .safeDrawingPadding()
                    .padding(
                        start = props.paddingLeft,
                        end = props.paddingRight,
                        bottom = props.paddingBottom,
                    )
            },
        allowClip = false,
    ) {
        Column {
            when (state.imeUiMode) {
                ImeUiMode.TEXT -> TextInputLayout()
                ImeUiMode.MEDIA -> MediaInputLayout()
                ImeUiMode.CLIPBOARD -> ClipboardInputLayout()
            }
            if (windowSpec is ImeWindowSpec.Floating) {
                FloatingSystemUiIme(moveModifier)
            }
        }
    }
}

@Composable
private fun BoxScope.FloatingDockToFixedIndicator() {
    val ims = LocalFlorisImeService.current

    val windowSpec by ims.windowController.activeWindowSpec.collectAsState()
    val editorState by ims.windowController.editor.state.collectAsState()

    val visible by remember {
        derivedStateOf {
            windowSpec.let { spec ->
                editorState.isMoveGesture && spec is ImeWindowSpec.Floating &&
                    spec.props.offsetBottom <= spec.floatingDockToFixedHeight
            }
        }
    }

    val transition = updateTransition(
        targetState = visible,
        label = "FloatingDockToFixedIndicator_visibility",
    )
    val animatedAlpha by transition.animateFloat(label = "alpha") { visible ->
        if (visible) 1f else 0f
    }
    val animatedHeightRatio by transition.animateFloat(label = "height") { visible ->
        if (visible) 1f else 0f
    }

    //TODO: Make snygg themeable
    val color = Color.Gray

    LaunchedEffect(visible) {
        if (visible) {
            delay(150)
            ims.inputFeedbackController.keyPress()
        }
    }

    Box(
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth()
            .height(windowSpec.floatingDockToFixedHeight)
            .navigationBarsPadding()
            .graphicsLayer { alpha = animatedAlpha }
            .drawBehind {
                val animatedTopLeft = Offset(
                    x = 0f,
                    y = size.height * (1f - animatedHeightRatio),
                )
                drawRect(
                    color = color,
                    topLeft = animatedTopLeft,
                    alpha = 0.5f,
                )
                drawBorder(
                    color = color,
                    topLeft = animatedTopLeft,
                    stroke = Stroke(windowSpec.floatingDockToFixedBorder.toPx()),
                )
            },
    )
}

@Composable
private fun BoxScope.FloatingResizeHandles() {
    val ims = LocalFlorisImeService.current

    val windowSpec by ims.windowController.activeWindowSpec.collectAsState()
    val editorState by ims.windowController.editor.state.collectAsState()

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

    val offset = ImeWindowDefaults.ResizeHandleTouchOffset
    if (visible) {
        FloatingResizeHandle(
            handle = ImeWindowResizeHandle.TOP_LEFT,
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(-offset, -offset)
                .then(alphaModifier),
        )
        FloatingResizeHandle(
            handle = ImeWindowResizeHandle.TOP_RIGHT,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(offset, -offset)
                .then(alphaModifier),
        )
        FloatingResizeHandle(
            handle = ImeWindowResizeHandle.BOTTOM_RIGHT,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(offset, offset)
                .then(alphaModifier),
        )
        FloatingResizeHandle(
            handle = ImeWindowResizeHandle.BOTTOM_LEFT,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset(-offset, offset)
                .then(alphaModifier),
        )
    }
}

@Composable
private fun FloatingResizeHandle(
    handle: ImeWindowResizeHandle,
    modifier: Modifier,
) {
    val ims = LocalFlorisImeService.current

    val handleColor = Color.Red

    Box(
        modifier = modifier
            .size(ImeWindowDefaults.ResizeHandleTouchSize)
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
                    },
                    onDragCancel = {
                        ims.windowController.editor.cancelGesture()
                    },
                )
            }
            .drawWithContent {
                drawContent()
                val thickness = ImeWindowDefaults.ResizeHandleThickness.toPx()
                val cornerRadius = ImeWindowDefaults.ResizeHandleCornerRadius.toPx().let { CornerRadius(it, it) }
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
private fun BoxScope.OneHandedPanel() {
    val ims = LocalFlorisImeService.current
    val windowController = ims.windowController
    val windowSpec by windowController.activeWindowSpec.collectAsState()

    when (val spec = windowSpec) {
        is ImeWindowSpec.Fixed if spec.mode == ImeWindowMode.Fixed.COMPACT -> {
            OneHandedPanel(spec)
        }
        else -> { }
    }
}

@Composable
private fun BoxScope.OneHandedPanel(spec: ImeWindowSpec.Fixed) {
    val ims = LocalFlorisImeService.current
    val windowController = ims.windowController
    val activeOrientation by windowController.activeOrientation.collectAsState()

    Box(Modifier.matchParentSize()) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .safeDrawingPadding()
                .padding(bottom = spec.props.paddingBottom)
                .fold(
                    condition = spec.props.paddingLeft >= spec.props.paddingRight,
                    ifTrue = {
                        Modifier
                            .width(spec.props.paddingLeft)
                            .align(Alignment.CenterStart)
                    },
                    ifFalse = {
                        Modifier
                            .width(spec.props.paddingRight)
                            .align(Alignment.CenterEnd)
                    }
                ),
            verticalArrangement = Arrangement.SpaceAround,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            SnyggIconButton(
                elementName = FlorisImeUi.OneHandedPanelButton.elementName,
                onClick = {
                    ims.windowController.updateWindowConfig { config ->
                        config.copy(fixedMode = ImeWindowMode.Fixed.NORMAL)
                    }
                },
            ) {
                SnyggIcon(imageVector = Icons.Default.ZoomOutMap)
            }
            SnyggIconButton(
                elementName = FlorisImeUi.OneHandedPanelButton.elementName,
                onClick = {
                    windowController.updateWindowConfig { config ->
                        val fixedProps = config.getFixedPropsOrDefault(activeOrientation)
                        val newProps = fixedProps.copy(
                            paddingLeft = fixedProps.paddingRight,
                            paddingRight = fixedProps.paddingLeft,
                        )
                        config.copy(
                            fixedProps = config.fixedProps.plus(
                                ImeWindowMode.Fixed.COMPACT to newProps,
                            ),
                        )
                    }
                },
            ) {
                if (spec.props.paddingLeft > spec.props.paddingRight) {
                    SnyggIcon(imageVector = Icons.Default.ChevronLeft)
                } else {
                    SnyggIcon(imageVector = Icons.Default.ChevronRight)
                }
            }
            SnyggIconButton(
                elementName = FlorisImeUi.OneHandedPanelButton.elementName,
                onClick = {
                    // TODO: Implement resize
                },
            ) {
                SnyggIcon(imageVector = ImageVector.vectorResource(R.drawable.ic_resize))
            }
        }
    }
}
