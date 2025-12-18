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
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.findRootCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.roundToIntRect
import dev.patrickgold.florisboard.app.devtools.DevtoolsOverlay
import dev.patrickgold.florisboard.ime.keyboard.KeyboardState
import dev.patrickgold.florisboard.ime.keyboard.ProvideKeyboardRowBaseHeight
import dev.patrickgold.florisboard.ime.sheet.BottomSheetWindow
import dev.patrickgold.florisboard.ime.sheet.isAnyBottomSheetVisible
import dev.patrickgold.florisboard.lib.compose.SystemUiIme

@Composable
fun FlorisImeRootWindow() {
    Box(Modifier.fillMaxSize()) {
        DevtoolsOverlay()
        FlorisImeWindow()
        BottomSheetWindow()
        SystemUiIme()
    }
}

@Composable
fun BoxScope.FlorisImeWindow() {
    val ime = LocalFlorisImeService.current
    val windowMode = FlorisImeWindowMode.FULL // TODO mode impl, for now always FULL

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .align(Alignment.BottomCenter)
            .onGloballyPositioned { coords ->
                val rootBounds = coords.findRootCoordinates().boundsInRoot()
                val windowBounds = coords.boundsInRoot()
                ime.activeImeInsets = FlorisImeInsets(
                    rootBounds.roundToIntRect(),
                    windowBounds.roundToIntRect(),
                    windowMode,
                )
            },
    ) {
        // TODO rework this
        ProvideKeyboardRowBaseHeight {
            ime.ImeUi()
        }
    }
}

fun KeyboardState.isFullscreenInputRequired(): Boolean {
    return isAnyBottomSheetVisible()
}
