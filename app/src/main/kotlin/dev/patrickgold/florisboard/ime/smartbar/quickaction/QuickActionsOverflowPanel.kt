/*
 * Copyright (C) 2022-2025 The OmniBoard Contributors
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

package dev.silo.omniboard.ime.smartbar.quickaction

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import dev.silo.omniboard.R
import dev.silo.omniboard.app.OmniPreferenceStore
import dev.silo.omniboard.ime.keyboard.OmniImeSizing
import dev.silo.omniboard.ime.theme.OmniImeUi
import dev.silo.omniboard.keyboardManager
import dev.silo.jetpref.datastore.model.observeAsState
import org.omniboard.lib.compose.stringRes
import org.omniboard.lib.snygg.ui.SnyggBox
import org.omniboard.lib.snygg.ui.SnyggButton
import org.omniboard.lib.snygg.ui.SnyggText

@Composable
fun QuickActionsOverflowPanel() {
    val prefs by OmniPreferenceStore
    val context = LocalContext.current
    val keyboardManager by context.keyboardManager()

    val actionArrangement by prefs.smartbar.actionArrangement.observeAsState()
    val evaluator by keyboardManager.activeSmartbarEvaluator.collectAsState()

    val dynamicActions = actionArrangement.dynamicActions
    val dynamicActionsCountToShow = when {
        dynamicActions.isEmpty() -> 0
        else -> {
            (dynamicActions.size - keyboardManager.smartbarVisibleDynamicActionsCount).coerceIn(dynamicActions.indices)
        }
    }
    val visibleActions = remember(actionArrangement, dynamicActionsCountToShow) {
        actionArrangement.dynamicActions.takeLast(dynamicActionsCountToShow)
    }

    SnyggBox(
        elementName = OmniImeUi.SmartbarActionsOverflow.elementName,
        modifier = Modifier
            .fillMaxWidth()
            .height(OmniImeSizing.keyboardUiHeight()),
    ) {
        LazyVerticalGrid(
            modifier = Modifier
                .fillMaxWidth(),
            columns = GridCells.Adaptive(OmniImeSizing.smartbarHeight * 2.2f),
        ) {
            items(visibleActions) { action ->
                QuickActionButton(
                    action = action,
                    evaluator = evaluator,
                    type = QuickActionBarType.INTERACTIVE_TILE,
                )
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                SnyggButton(
                    elementName = OmniImeUi.SmartbarActionsOverflowCustomizeButton.elementName,
                    onClick = { keyboardManager.activeState.isActionsEditorVisible = true },
                    modifier = Modifier
                        .wrapContentWidth(),
                ) {
                    SnyggText(
                        text = stringRes(R.string.quick_actions_overflow__customize_actions_button),
                    )
                }
            }
        }
    }
}
