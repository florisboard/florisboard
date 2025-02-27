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

package dev.patrickgold.florisboard.ime.core

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.patrickgold.florisboard.ime.keyboard.KeyboardState
import dev.patrickgold.florisboard.ime.sheet.BottomSheetHostUi
import dev.patrickgold.florisboard.ime.theme.FlorisImeTheme
import dev.patrickgold.florisboard.ime.theme.FlorisImeUi
import dev.patrickgold.florisboard.keyboardManager
import dev.patrickgold.florisboard.lib.compose.FlorisIconButton
import dev.patrickgold.florisboard.subtypeManager
import dev.patrickgold.jetpref.material.ui.JetPrefListItem
import org.florisboard.lib.snygg.ui.snyggBackground
import org.florisboard.lib.snygg.ui.snyggClip
import org.florisboard.lib.snygg.ui.solidColor
import org.florisboard.lib.snygg.ui.spSize

@Composable
fun SelectSubtypePanel(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val keyboardManager by context.keyboardManager()
    val subtypeManager by context.subtypeManager()

    val state by keyboardManager.activeState.collectAsState()

    var currentlySelected by remember { mutableLongStateOf(subtypeManager.activeSubtype.id) }

    BottomSheetHostUi(
        isShowing = state.isSubtypeSelectionShowing(),
        onHide = {
            subtypeManager.switchToSubtypeById(currentlySelected)
            keyboardManager.activeState.isSubtypeSelectionVisible = false
        },
    ) {
        val panelStyle = FlorisImeTheme.style.get(FlorisImeUi.SmartbarActionsEditor)
        val headerStyle = FlorisImeTheme.style.get(FlorisImeUi.SmartbarActionsEditorHeader)
        val subheaderStyle = FlorisImeTheme.style.get(FlorisImeUi.SmartbarActionsEditorSubheader)
        Column(
            modifier = Modifier
                .snyggBackground(context, panelStyle, fallbackColor = FlorisImeTheme.fallbackSurfaceColor())
                .snyggClip(panelStyle),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .snyggBackground(context, headerStyle),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FlorisIconButton(
                    onClick = {
                        subtypeManager.switchToSubtypeById(currentlySelected)
                        keyboardManager.activeState.isSubtypeSelectionVisible = false
                    },
                    icon = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    iconColor = headerStyle.foreground.solidColor(context, default = FlorisImeTheme.fallbackContentColor()),
                )
                Text(
                    modifier = Modifier.weight(1f),
                    text = "Select subtype",
                    color = headerStyle.foreground.solidColor(context, default = FlorisImeTheme.fallbackContentColor()),
                    fontSize = headerStyle.fontSize.spSize(),
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.size(48.dp).clickable{})
            }

            Box {
                LazyColumn {
                    items(
                        subtypeManager.subtypes,
                        key = {
                            it.id
                        }
                    ) {
                        JetPrefListItem(
                            modifier = Modifier.clickable {
                                currentlySelected = it.id
                            },
                            icon = {
                                if (currentlySelected == it.id) {
                                    Icon(Icons.Default.RadioButtonChecked, null)
                                } else {
                                    Icon(Icons.Default.RadioButtonUnchecked, null)
                                }
                            },
                            text = it.primaryLocale.displayName(),
                            colors = ListItemDefaults.colors(
                                leadingIconColor = subheaderStyle.foreground.solidColor(context),
                                headlineColor = subheaderStyle.foreground.solidColor(context),
                                containerColor = subheaderStyle.background.solidColor(context),
                            )
                        )
                    }
                }
            }
        }
    }
}


fun KeyboardState.isSubtypeSelectionShowing(): Boolean {
    return isSubtypeSelectionVisible
}
