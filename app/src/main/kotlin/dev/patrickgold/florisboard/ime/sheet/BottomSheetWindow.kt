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

package dev.patrickgold.florisboard.ime.sheet

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import dev.patrickgold.florisboard.ime.core.SelectSubtypePanel
import dev.patrickgold.florisboard.ime.keyboard.KeyboardState
import dev.patrickgold.florisboard.ime.smartbar.quickaction.QuickActionsEditorPanel
import dev.patrickgold.florisboard.keyboardManager
import kotlin.getValue

@Composable
fun BottomSheetWindow() {
    val context = LocalContext.current
    val keyboardManager by context.keyboardManager()
    val state by keyboardManager.activeState.collectAsState()

    BottomSheetHostUi(
        isShowing = state.isAnyBottomSheetVisible(),
        onHide = {
            if (state.isActionsEditorVisible) {
                keyboardManager.activeState.isActionsEditorVisible = false
            }
            if (state.isSubtypeSelectionVisible) {
                keyboardManager.activeState.isSubtypeSelectionVisible = false
            }
        },
    ) {
        if (state.isActionsEditorVisible) {
            QuickActionsEditorPanel()
        }
        if (state.isSubtypeSelectionVisible) {
            SelectSubtypePanel()
        }
    }
}

fun KeyboardState.isAnyBottomSheetVisible(): Boolean {
    return isActionsEditorVisible || isSubtypeSelectionVisible
}
