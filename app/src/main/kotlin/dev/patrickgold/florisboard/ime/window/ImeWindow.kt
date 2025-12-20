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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.findRootCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.roundToIntRect
import dev.patrickgold.florisboard.app.devtools.DevtoolsOverlay
import dev.patrickgold.florisboard.ime.ImeUiMode
import dev.patrickgold.florisboard.ime.clipboard.ClipboardInputLayout
import dev.patrickgold.florisboard.ime.keyboard.KeyboardState
import dev.patrickgold.florisboard.ime.keyboard.ProvideKeyboardRowBaseHeight
import dev.patrickgold.florisboard.ime.media.MediaInputLayout
import dev.patrickgold.florisboard.ime.sheet.BottomSheetWindow
import dev.patrickgold.florisboard.ime.sheet.isAnyBottomSheetVisible
import dev.patrickgold.florisboard.ime.text.TextInputLayout
import dev.patrickgold.florisboard.ime.theme.FlorisImeUi
import dev.patrickgold.florisboard.keyboardManager
import dev.patrickgold.florisboard.lib.compose.SystemUiIme
import org.florisboard.lib.android.AndroidVersion
import org.florisboard.lib.compose.conditional
import org.florisboard.lib.snygg.ui.SnyggBox
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

    val attributes = remember(state.keyboardMode, state.inputShiftState) {
        mapOf(
            FlorisImeUi.Attr.Mode to state.keyboardMode.toString(),
            FlorisImeUi.Attr.ShiftState to state.inputShiftState.toString(),
        )
    }

    val layoutDirection = LocalLayoutDirection.current
    LaunchedEffect(layoutDirection) {
        keyboardManager.activeState.layoutDirection = layoutDirection
    }

    val windowConfig = ImeWindowConfig.DefaultPortrait // TODO mode impl, for now always FULL

    SnyggBox(
        elementName = FlorisImeUi.Window.elementName,
        attributes = attributes,
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .align(Alignment.BottomCenter)
            .onGloballyPositioned { coords ->
                val rootBounds = coords.findRootCoordinates().boundsInRoot()
                val windowBounds = coords.boundsInRoot()
                ims.windowController.updateImeInsets(
                    rootBounds.roundToIntRect(),
                    windowBounds.roundToIntRect(),
                )
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
            ImeInnerWindow(state, windowConfig)
        }
    }
}

@Composable
private fun ImeInnerWindow(state: KeyboardState, windowConfig: ImeWindowConfig) {
    SnyggBox(
        elementName = FlorisImeUi.WindowInner.elementName,
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .conditional(windowConfig.mode == ImeWindowMode.FIXED) {
                safeDrawingPadding()
            },
        allowClip = false,
    ) {
        when (state.imeUiMode) {
            ImeUiMode.TEXT -> TextInputLayout()
            ImeUiMode.MEDIA -> MediaInputLayout()
            ImeUiMode.CLIPBOARD -> ClipboardInputLayout()
        }
    }
}

fun KeyboardState.isFullscreenInputRequired(): Boolean {
    return isAnyBottomSheetVisible()
}
