/*
 * Copyright (C) 2022-2025 The FlorisBoard Contributors
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

package dev.patrickgold.florisboard.ime.smartbar.quickaction

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import dev.patrickgold.compose.tooltip.PlainTooltip
import dev.patrickgold.florisboard.ime.keyboard3.ImeActions
import dev.patrickgold.florisboard.ime.keyboard3.LocalImeController
import dev.patrickgold.florisboard.ime.keyboard3.ui.Display3
import dev.patrickgold.florisboard.ime.keyboard3.ui.ImeKeyButton
import dev.patrickgold.florisboard.ime.keyboard3.ui.rememberDerivedEnabledState
import dev.patrickgold.florisboard.ime.theme.FlorisImeUi
import org.florisboard.lib.snygg.ui.SnyggText

enum class QuickActionBarType {
    INTERACTIVE_BUTTON,
    INTERACTIVE_TILE,
    EDITOR_TILE;
}

@Composable
fun QuickActionButton(
    action: QuickAction,
    modifier: Modifier = Modifier,
    type: QuickActionBarType = QuickActionBarType.INTERACTIVE_BUTTON,
) {
    val imeController = LocalImeController.current
    val imeState by imeController.activeState.collectAsState()
    val descriptor  = remember(action) {
        if (action is QuickAction.InsertK3Descriptor) {
            action.descriptor
        } else {
            ImeActions.NoopSpacer
        }
    }
    val derivedEnabledState by rememberDerivedEnabledState(descriptor)
    val isEnabled = type != QuickActionBarType.EDITOR_TILE && derivedEnabledState
    val elementName = when (type) {
        QuickActionBarType.INTERACTIVE_BUTTON -> FlorisImeUi.SmartbarActionKey
        QuickActionBarType.INTERACTIVE_TILE -> FlorisImeUi.SmartbarActionTile
        QuickActionBarType.EDITOR_TILE -> FlorisImeUi.SmartbarActionsEditorTile
    }.elementName

    PlainTooltip(action.computeTooltip(imeState), enabled = type == QuickActionBarType.INTERACTIVE_BUTTON) {
        ImeKeyButton(
            elementName = elementName,
            output = descriptor,
            isEnabled = isEnabled,
            modifier = modifier.aspectRatio(1f),
        ) { display ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // Render foreground
                Display3(
                    display = display,
                    elementName = "$elementName-icon",
                )
                // Render additional info if this is a tile
                if (type != QuickActionBarType.INTERACTIVE_BUTTON) {
                    SnyggText(
                        elementName = "$elementName-text",
                        text = action.computeDisplayName(imeState),
                    )
                }
            }
        }
    }
}
