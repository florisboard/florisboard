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
import dev.patrickgold.florisboard.ime.core.SelectSubtypePanel
import dev.patrickgold.florisboard.ime.keyboard3.ImeState
import dev.patrickgold.florisboard.ime.keyboard3.LocalImeController
import dev.patrickgold.florisboard.ime.smartbar.quickaction.QuickActionsEditorPanel

@Composable
fun BottomSheetWindow() {
    val imeController = LocalImeController.current
    val imeState by imeController.activeState.collectAsState()

    BottomSheetHostUi(
        isShowing = imeState.isAnyBottomSheetVisible(),
        onHide = {
            imeController.updateStateBlocking {
                state = state.copy(
                    flags = state.flags
                        .withActionsEditorVisible(false)
                        .withSubtypeSelectionVisible(false),
                )
            }
        },
    ) {
        if (imeState.flags.isActionsEditorVisible) {
            QuickActionsEditorPanel()
        }
        if (imeState.flags.isSubtypeSelectionVisible) {
            SelectSubtypePanel()
        }
    }
}

fun ImeState.isAnyBottomSheetVisible(): Boolean {
    return flags.isActionsEditorVisible || flags.isSubtypeSelectionVisible
}
