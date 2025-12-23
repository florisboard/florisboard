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

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ZoomOutMap
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
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
import org.florisboard.lib.android.AndroidVersion
import org.florisboard.lib.compose.FlorisIconButton
import org.florisboard.lib.compose.ifIsInstance
import org.florisboard.lib.compose.toDp
import org.florisboard.lib.compose.toDpRect
import org.florisboard.lib.compose.toggeled
import org.florisboard.lib.snygg.ui.SnyggBox
import org.florisboard.lib.snygg.ui.SnyggSurfaceView

@Composable
fun ImeRootWindow() {
    val ims = LocalFlorisImeService.current
    val density = LocalDensity.current

    Box(
        modifier = Modifier
            .fillMaxSize()
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

    LaunchedEffect(navBarFrameView, windowSpec) {
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

    val modeModifier = Modifier.pointerInput(Unit) {
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
        when (val spec = windowSpec) {
            is ImeWindowSpec.Fixed if spec.mode == ImeWindowMode.Fixed.COMPACT -> {
                OneHandPanel()
            }

            else -> {/*Do nothing*/}
        }
        ProvideKeyboardRowBaseHeight(windowSpec) {
            ImeInnerWindow(state, windowSpec, modeModifier)
        }
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
            val spec = windowSpec
            editorState.isMoveGesture && spec is ImeWindowSpec.Floating &&
                spec.props.offsetBottom <= spec.floatingDockHeight
        }
    }

    val animatedHeight by animateDpAsState(
        targetValue = if (visible) windowSpec.floatingDockHeight else 0.dp,
        animationSpec = tween(durationMillis = 150),
        finishedListener = { height ->
            if (height == windowSpec.floatingDockHeight) {
                ims.inputFeedbackController.keyPress()
            }
        },
    )

    //TODO: Make snygg themeable
    val color = Color.Gray


    AnimatedVisibility(
        visible = visible,
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth()
            .wrapContentHeight(),
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        Box(
            modifier = Modifier
                .height(animatedHeight)
                .navigationBarsPadding()
                .border(2.dp, color)
                .alpha(0.5f)
                .background(color),
        )
    }
}

@Composable
private fun BoxScope.OneHandPanel() {
    val ims = LocalFlorisImeService.current
    val windowController = ims.windowController
    val windowSpec by windowController.activeWindowSpec.collectAsState()
    val activeOrientation by windowController.activeOrientation.collectAsState()

    val showToLeftIcon by remember {
        derivedStateOf {
            val spec = windowSpec
            spec is ImeWindowSpec.Fixed &&
                spec.props.paddingLeft > spec.props.paddingRight
        }
    }

    Box(
        modifier = Modifier
            .matchParentSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .ifIsInstance<ImeWindowProps.Fixed>(windowSpec.props) {
                    Modifier
                        .safeDrawingPadding()
                        .padding(bottom = it.paddingBottom)
                        .toggeled(
                            condition = it.paddingLeft >= it.paddingRight,
                            ifTrue = {
                                Modifier
                                    .width(it.paddingLeft)
                                    .align(Alignment.CenterStart)
                            },
                            ifFalse = {
                                Modifier
                                    .width(it.paddingRight)
                                    .align(Alignment.CenterEnd)
                            }
                        )
                },
            verticalArrangement = Arrangement.SpaceAround,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            FlorisIconButton(
                onClick = {
                    ims.windowController.updateWindowConfig { currentConfig ->
                        currentConfig.copy(fixedMode = ImeWindowMode.Fixed.NORMAL)
                    }
                },
                size = DpSize(40.dp, 40.dp)
            ) {
                Icon(Icons.Default.ZoomOutMap, null)
            }
            FlorisIconButton(
                onClick = {
                    windowController.updateWindowConfig { currentConfig ->
                        val fixedProps = currentConfig.getFixedPropsOrDefault(activeOrientation)
                        val newProps = fixedProps.copy(
                            paddingLeft = fixedProps.paddingRight,
                            paddingRight = fixedProps.paddingLeft,
                        )
                        currentConfig.copy(
                            fixedProps = currentConfig.fixedProps.plus(
                                ImeWindowMode.Fixed.COMPACT to newProps,
                            ),
                        )
                    }
                },
                size = DpSize(40.dp, 40.dp)
            ) {
                if (showToLeftIcon) {
                    Icon(Icons.Default.ChevronLeft, null)
                } else {
                    Icon(Icons.Default.ChevronRight, null)
                }
            }
            FlorisIconButton(
                onClick = {
                    // TODO: Implement resize
                },
                size = DpSize(40.dp, 40.dp)
            ) {
                Icon(ImageVector.vectorResource(R.drawable.ic_resize), null)
            }
        }
    }
}



