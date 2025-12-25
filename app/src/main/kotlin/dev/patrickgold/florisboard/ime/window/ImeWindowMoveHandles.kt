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
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.DpOffset
import org.florisboard.lib.compose.toDp

fun Modifier.imeWindowMoveHandle(
    windowController: ImeWindowController,
): Modifier = this.then(
    Modifier
        .pointerInput(Unit) {
            detectTapGestures {
                windowController.editor.toggleEnabled()
            }
        }
        .pointerInput(Unit) {
            var unconsumed = DpOffset.Zero
            detectDragGestures(
                onDragStart = {
                    windowController.editor.beginMoveGesture()
                },
                onDrag = { change, dragAmount ->
                    change.consume()
                    unconsumed += dragAmount.toDp()
                    val consumed = windowController.editor.moveBy(unconsumed)
                    unconsumed -= consumed
                },
                onDragEnd = {
                    windowController.editor.endMoveGesture()
                    unconsumed = DpOffset.Zero
                },
                onDragCancel = {
                    windowController.editor.cancelGesture()
                    unconsumed = DpOffset.Zero
                },
            )
        }
)
