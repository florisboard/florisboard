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

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.findRootCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.coerceIn
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.roundToIntRect
import androidx.compose.ui.unit.width
import androidx.lifecycle.lifecycleScope
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
import kotlinx.coroutines.launch
import org.florisboard.lib.android.AndroidVersion
import org.florisboard.lib.compose.conditional
import org.florisboard.lib.compose.toDp
import org.florisboard.lib.compose.toDpRect
import org.florisboard.lib.snygg.ui.SnyggBox
import org.florisboard.lib.snygg.ui.SnyggColumn
import org.florisboard.lib.snygg.ui.SnyggSurfaceView

@Composable
fun ImeRootWindow() {
    Box(Modifier.fillMaxSize()) {
        DevtoolsOverlay()
        ImeWindow()
        BottomSheetWindow()
        SystemUiIme()
    }
}

@Composable
fun BoxScope.ImeWindow() {
    val ims = LocalFlorisImeService.current
    val keyboardManager by ims.keyboardManager()
    val state by keyboardManager.activeState.collectAsState()
    val imeInsets by ims.windowController.activeImeInsets.collectAsState()
    val windowConfig by ims.windowController.activeWindowConfig.collectAsState()
    val navBarFrameView = remember { ims.findNavBarFrameOrNull() }

    LaunchedEffect(navBarFrameView, windowConfig.mode) {
        navBarFrameView?.scaleY = when (windowConfig.mode) {
            ImeWindowMode.FIXED -> 1f
            ImeWindowMode.FLOATING -> 0f
        }
    }

    val density = LocalDensity.current
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

    var localMoveOffset by remember { mutableStateOf(DpOffset.Zero) }
    val effectiveSpec = rememberEffectiveSpec(imeInsets, windowConfig, localMoveOffset)
    val specForPersisting = rememberUpdatedState(effectiveSpec)

    val localMoveModifier = Modifier.pointerInput(Unit) {
        detectDragGestures(
            onDragStart = {
                localMoveOffset = DpOffset.Zero
            },
            onDrag = { change, dragAmount ->
                change.consume()
                localMoveOffset += dragAmount.toDp()
            },
            onDragEnd = {
                val spec = specForPersisting.value
                ims.lifecycleScope.launch {
                    ims.windowController.updateWindowConfig { config ->
                        when (spec) {
                            is ImeWindowSpec.Fixed -> {
                                config.copy(
                                    fixedSpecs = config.fixedSpecs.plus(config.fixedMode to spec),
                                )
                            }
                            is ImeWindowSpec.Floating -> {
                                config.copy(
                                    floatingSpecs = config.floatingSpecs.plus(config.floatingMode to spec),
                                )
                            }
                        }
                    }
                }
                localMoveOffset = DpOffset.Zero
            },
            onDragCancel = {
                localMoveOffset = DpOffset.Zero
            },
        )
    }

    SnyggBox(
        elementName = FlorisImeUi.Window.elementName,
        attributes = attributes,
        modifier = Modifier
            .align(Alignment.BottomStart)
            .then(
                when (effectiveSpec) {
                    is ImeWindowSpec.Fixed -> {
                        Modifier
                            .fillMaxWidth()
                            .padding(
                                start = effectiveSpec.paddingLeft,
                                end = effectiveSpec.paddingRight,
                                bottom = effectiveSpec.paddingBottom,
                            )
                    }
                    is ImeWindowSpec.Floating -> {
                        Modifier
                            .offset(x = effectiveSpec.offsetLeft, y = -effectiveSpec.offsetBottom)
                            .width(effectiveSpec.width)
                    }
                }
            )
            .wrapContentHeight()
            .onGloballyPositioned { coords ->
                val rootBoundsPx = coords.findRootCoordinates().boundsInRoot().roundToIntRect()
                val windowBoundsPx = coords.boundsInRoot().roundToIntRect()
                val newImeInsets = ImeInsets(
                    rootBoundsDp = with(density) { rootBoundsPx.toDpRect() },
                    rootBoundsPx = rootBoundsPx,
                    windowBoundsDp = with(density) { windowBoundsPx.toDpRect() },
                    windowBoundsPx = windowBoundsPx,
                )
                ims.windowController.updateImeInsets(newImeInsets)
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
        ProvideKeyboardRowBaseHeight {
            ImeInnerWindow(state, windowConfig, localMoveModifier)
        }
    }
}

@Composable
private fun ImeInnerWindow(state: KeyboardState, windowConfig: ImeWindowConfig, moveModifier: Modifier) {
    SnyggColumn(
        elementName = FlorisImeUi.WindowInner.elementName,
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .conditional(windowConfig.mode == ImeWindowMode.FIXED) {
                safeDrawingPadding()
            },
    ) {
        when (state.imeUiMode) {
            ImeUiMode.TEXT -> TextInputLayout()
            ImeUiMode.MEDIA -> MediaInputLayout()
            ImeUiMode.CLIPBOARD -> ClipboardInputLayout()
        }
        if (windowConfig.mode == ImeWindowMode.FLOATING) {
            FloatingSystemUiIme(moveModifier)
        }
    }
}

@Composable
private fun rememberEffectiveSpec(
    imeInsets: ImeInsets?,
    windowConfig: ImeWindowConfig,
    localMoveOffset: DpOffset = DpOffset.Zero,
): ImeWindowSpec {
    return remember(imeInsets, windowConfig, localMoveOffset) {
        val rootBounds = (imeInsets ?: ImeInsets.Indeterminate).rootBoundsDp
        when (windowConfig.mode) {
            ImeWindowMode.FIXED -> {
                val spec = windowConfig.fixedSpecs[windowConfig.fixedMode]
                    ?: ImeWindowSpec.Fixed.DefaultNormal
                // TODO
                spec
            }
            ImeWindowMode.FLOATING -> {
                val spec = windowConfig.floatingSpecs[windowConfig.floatingMode]
                    ?: ImeWindowSpec.Floating.DefaultFloating
                val rowHeight = spec.rowHeight.coerceIn(
                    minimumValue = (rootBounds.height / (3f * ImeWindowSpec.KeyboardHeightFactor)),
                    maximumValue = (rootBounds.height / (2f * ImeWindowSpec.KeyboardHeightFactor)),
                )
                val width = spec.width.coerceIn(
                    minimumValue = ImeWindowSpec.MinKeyboardWidth,
                    maximumValue = rootBounds.width,
                )
                val offsetLeft = (spec.offsetLeft + localMoveOffset.x).coerceIn(
                    minimumValue = 0.dp,
                    maximumValue = rootBounds.width - width,
                )
                val offsetBottom = (spec.offsetBottom - localMoveOffset.y).coerceIn(
                    minimumValue = 0.dp,
                    maximumValue = rootBounds.height - (rowHeight * ImeWindowSpec.KeyboardHeightFactor),
                )
                ImeWindowSpec.Floating(rowHeight, width, offsetLeft, offsetBottom)
            }
        }
    }
}
