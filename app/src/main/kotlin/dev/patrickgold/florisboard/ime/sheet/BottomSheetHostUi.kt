/*
 * Copyright (C) 2022 Patrick Goldinger
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

package dev.patrickgold.florisboard.ime.sheet

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import dev.patrickgold.florisboard.ime.keyboard.KeyboardState
import dev.patrickgold.florisboard.lib.compose.conditional

private val SheetOutOfBoundsBgColorInactive = Color(0x00000000)
private val SheetOutOfBoundsBgColorActive = Color(0x52000000)

private val DialogContentEnterTransition = slideInVertically { it }
private val DialogContentExitTransition = slideOutVertically { it }

@Composable
fun BottomSheetHostUi(
    isShowing: Boolean,
    onHide: () -> Unit,
    content: @Composable () -> Unit,
) {
    val bgColorOutOfBounds by animateColorAsState(
        if (isShowing) SheetOutOfBoundsBgColorActive else SheetOutOfBoundsBgColorInactive
    )

    Column(Modifier.background(bgColorOutOfBounds)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .conditional(isShowing) {
                    pointerInput(Unit) {
                        detectTapGestures {
                            onHide()
                        }
                    }
                },
        )
        AnimatedVisibility(
            visible = isShowing,
            enter = DialogContentEnterTransition,
            exit = DialogContentExitTransition,
            content = { content() },
        )
    }
}

fun KeyboardState.isBottomSheetShowing(): Boolean {
    return isActionsEditorVisible
}
