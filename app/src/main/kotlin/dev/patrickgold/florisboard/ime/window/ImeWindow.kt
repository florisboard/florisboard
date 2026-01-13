/*
 * Copyright (C) 2025-2026 The FlorisBoard Contributors
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
import androidx.compose.animation.core.updateTransition
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.systemGestureExclusion
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.roundToIntRect
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.app.devtools.DevtoolsOverlay
import dev.patrickgold.florisboard.ime.ImeUiMode
import dev.patrickgold.florisboard.ime.clipboard.ClipboardInputLayout
import dev.patrickgold.florisboard.ime.input.LocalInputFeedbackController
import dev.patrickgold.florisboard.ime.keyboard.ProvideKeyboardRowBaseHeight
import dev.patrickgold.florisboard.ime.media.MediaInputLayout
import dev.patrickgold.florisboard.ime.sheet.BottomSheetWindow
import dev.patrickgold.florisboard.ime.text.TextInputLayout
import dev.patrickgold.florisboard.ime.theme.FlorisImeUi
import dev.patrickgold.florisboard.keyboardManager
import kotlinx.coroutines.delay
import org.florisboard.lib.compose.ProvideActualLayoutDirection
import org.florisboard.lib.compose.conditional
import org.florisboard.lib.compose.drawBorder
import org.florisboard.lib.compose.drawableRes
import org.florisboard.lib.compose.fold
import org.florisboard.lib.compose.ifIsInstance
import org.florisboard.lib.snygg.ui.SnyggBox
import org.florisboard.lib.snygg.ui.SnyggIcon
import org.florisboard.lib.snygg.ui.SnyggIconButton
import org.florisboard.lib.snygg.ui.rememberSnyggThemeQuery

/**
 * The main entry point of the IME user interface. This includes the keyboard itself, devtools overlays,
 * bottom sheets, and system bars management.
 *
 * Typically, the root window corresponds to the screen bounds, however this is not guaranteed. For
 * consistency reasons, all sub sizes and positions should be derived from the root window.
 *
 * The size and position of this composable are used to calculate [ImeWindowController.activeRootInsets].
 *
 * @see ImeWindow
 * @see BottomSheetWindow
 * @see DevtoolsOverlay
 */
@Composable
fun ImeRootWindow() {
    val density = LocalDensity.current
    val windowController = LocalWindowController.current

    val editorState by windowController.editor.state.collectAsState()
    val isEditorEnabled by remember {
        derivedStateOf {
            editorState.isEnabled
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .conditional(isEditorEnabled) {
                Modifier.pointerInput(isEditorEnabled) {
                    detectTapGestures {
                        windowController.editor.disableIfNoGestureInProgress()
                    }
                }
            }
            .onGloballyPositioned { coords ->
                val boundsPx = coords.boundsInRoot().roundToIntRect()
                val newInsets = with(density) { ImeInsets.Root.of(boundsPx) }
                windowController.updateRootInsets(newInsets)
            },
    ) {
        DevtoolsOverlay()
        ImeWindow()
        BottomSheetWindow()
        ImeSystemUi()
    }
}

/**
 * The IME window contains all components that users would describe as the keyboard user interface. Its placement
 * and size is dependent on the window config, with the main factor being the window mode.
 *
 * Most of the time all draw operations are contained within the bounds of this composable. Exceptions to
 * this may be resize handles, popups, tooltips, and any other composable positioned absolutely.
 *
 * The size and position of this composable are used to calculate [ImeWindowController.activeWindowInsets].
 *
 * @see ImeWindowMode
 */
@Composable
fun BoxScope.ImeWindow() {
    val density = LocalDensity.current
    val windowController = LocalWindowController.current

    val windowSpec by windowController.activeWindowSpec.collectAsState()
    val windowConfig by windowController.activeWindowConfig.collectAsState()

    val attributes = remember(windowConfig.mode) {
        mapOf(
            FlorisImeUi.Attr.WindowMode to windowConfig.mode.toString(),
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
                val newInsets = with(density) { ImeInsets.Window.of(boundsPx) }
                windowController.updateWindowInsets(newInsets)
            },
        supportsBackgroundImage = true,
        allowClip = false,
    ) {
        OneHandedPanel()
        ProvideKeyboardRowBaseHeight {
            ImeInnerWindow()
        }
        ImeWindowResizeHandlesFloating()
    }
}

@Composable
private fun ImeInnerWindow() {
    val context = LocalContext.current
    val windowController = LocalWindowController.current

    val keyboardManager by context.keyboardManager()

    val state by keyboardManager.activeState.collectAsState()
    val windowSpec by windowController.activeWindowSpec.collectAsState()

    ProvideActualLayoutDirection {
        val layoutDirection = LocalLayoutDirection.current
        LaunchedEffect(layoutDirection) {
            keyboardManager.activeState.layoutDirection = layoutDirection
        }
    }

    SnyggBox(
        elementName = FlorisImeUi.WindowInner.elementName,
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .ifIsInstance<ImeWindowProps.Fixed>(windowSpec.props) { props ->
                Modifier
                    .safeDrawingPadding()
                    .systemGestureExclusion()
                    .padding(
                        start = props.paddingLeft.coerceAtLeast(0.dp),
                        end = props.paddingRight.coerceAtLeast(0.dp),
                        bottom = props.paddingBottom.coerceAtLeast(0.dp),
                    )
            }
            .ifIsInstance<ImeWindowProps.Floating>(windowSpec.props) {
                Modifier.systemGestureExclusion()
            },
        allowClip = false,
    ) {
        Column {
            when (state.imeUiMode) {
                ImeUiMode.TEXT -> TextInputLayout()
                ImeUiMode.MEDIA -> ProvideActualLayoutDirection { MediaInputLayout() }
                ImeUiMode.CLIPBOARD -> ProvideActualLayoutDirection { ClipboardInputLayout() }
            }
            ImeSystemUiFloating()
        }
        ImeWindowResizeHandlesFixed()
    }
}

@Composable
private fun BoxScope.FloatingDockToFixedIndicator() {
    val inputFeedbackController = LocalInputFeedbackController.current
    val windowController = LocalWindowController.current

    val windowSpec by windowController.activeWindowSpec.collectAsState()
    val editorState by windowController.editor.state.collectAsState()

    val visible by remember {
        derivedStateOf {
            windowSpec.let { spec ->
                editorState.isMoveGesture && spec is ImeWindowSpec.Floating &&
                    spec.props.offsetBottom <= spec.constraints.dockToFixedHeight
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
    val indicatorTheme = rememberSnyggThemeQuery(FlorisImeUi.FloatingDockToFixedIndicator.elementName)
    val color = indicatorTheme.background(default = Color.Gray)

    LaunchedEffect(visible) {
        if (visible) {
            delay(150)
            inputFeedbackController.keyPress()
        }
    }

    Box(
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth()
            .height(windowSpec.constraints.dockToFixedHeight)
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
                    stroke = Stroke(windowSpec.constraints.dockToFixedBorder.toPx()),
                )
            },
    )
}

@Composable
private fun BoxScope.OneHandedPanel() {
    val windowController = LocalWindowController.current
    val windowSpec by windowController.activeWindowSpec.collectAsState()

    when (val spec = windowSpec) {
        is ImeWindowSpec.Fixed if spec.fixedMode == ImeWindowMode.Fixed.COMPACT -> {
            OneHandedPanel(spec)
        }
        else -> { }
    }
}

@Composable
private fun BoxScope.OneHandedPanel(spec: ImeWindowSpec.Fixed) {
    val windowController = LocalWindowController.current
    val editorState by windowController.editor.state.collectAsState()
    val windowConfig by windowController.activeWindowConfig.collectAsState()

    val attributes = remember(windowConfig.mode) {
        mapOf(
            FlorisImeUi.Attr.WindowMode to windowConfig.mode.toString()
        )
    }

    if (!editorState.isEnabled) {
        Box(Modifier.matchParentSize()) {
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .safeDrawingPadding()
                    .padding(bottom = spec.props.paddingBottom.coerceAtLeast(0.dp))
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
                    attributes = attributes,
                    onClick = {
                        windowController.actions.toggleCompactLayout()
                    },
                ) {
                    SnyggIcon(
                        attributes = attributes,
                        imageVector = drawableRes(R.drawable.ic_zoom_out_map),
                    )
                }
                SnyggIconButton(
                    elementName = FlorisImeUi.OneHandedPanelButton.elementName,
                    attributes = attributes,
                    onClick = {
                        windowController.actions.compactLayoutFlipSide()
                    },
                ) {
                    if (spec.props.paddingLeft > spec.props.paddingRight) {
                        SnyggIcon(
                            imageVector = drawableRes(R.drawable.ic_chevron_left),
                            attributes = attributes,
                        )
                    } else {
                        SnyggIcon(
                            attributes = attributes,
                            imageVector = drawableRes(R.drawable.ic_chevron_right),
                        )
                    }
                }
                SnyggIconButton(
                    elementName = FlorisImeUi.OneHandedPanelButton.elementName,
                    attributes = attributes,
                    onClick = {
                        windowController.editor.toggleEnabled()
                    },
                ) {
                    SnyggIcon(
                        imageVector = drawableRes(R.drawable.ic_resize),
                        attributes = attributes,
                    )
                }
            }
        }
    }
}
