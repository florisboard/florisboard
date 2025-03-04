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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.ime.keyboard.KeyboardState
import dev.patrickgold.florisboard.ime.theme.FlorisImeTheme
import dev.patrickgold.florisboard.ime.theme.FlorisImeUi
import dev.patrickgold.florisboard.keyboardManager
import dev.patrickgold.florisboard.lib.compose.rippleClickable
import dev.patrickgold.florisboard.lib.compose.stringRes
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

    val listState = rememberLazyListState()
    val subtypes by subtypeManager.subtypesFlow.collectAsState()

    val currentlySelected = subtypeManager.activeSubtype.id

    val panelStyle = FlorisImeTheme.style.get(FlorisImeUi.SmartbarActionsEditor)
    val headerStyle = FlorisImeTheme.style.get(FlorisImeUi.SmartbarActionsEditorHeader)
    val subheaderStyle = FlorisImeTheme.style.get(FlorisImeUi.SmartbarActionsEditorSubheader)

    Column(
        modifier = modifier
            .snyggBackground(context, panelStyle, fallbackColor = FlorisImeTheme.fallbackSurfaceColor())
            .snyggClip(panelStyle),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .snyggBackground(context, headerStyle),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                modifier = Modifier
                    .weight(1f)
                    .clickable(false) {},
                text = stringRes(R.string.select_subtype_panel__header),
                color = headerStyle.foreground.solidColor(
                    context,
                    default = FlorisImeTheme.fallbackContentColor()
                ),
                fontSize = headerStyle.fontSize.spSize(),
                textAlign = TextAlign.Center,
            )
        }

        Box {
            LazyColumn(
                state = listState,
            ) {
                items(
                    subtypes,
                    key = {
                        it.id
                    }
                ) {
                    JetPrefListItem(
                        modifier = Modifier
                            .fillMaxWidth()
                            .rippleClickable {
                                subtypeManager.switchToSubtypeById(it.id)
                                keyboardManager.activeState.isSubtypeSelectionVisible = false
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
        Spacer(Modifier
            .systemBarsPadding()
            .snyggBackground(context, panelStyle))
    }
}


fun KeyboardState.isSubtypeSelectionShowing(): Boolean {
    return isSubtypeSelectionVisible
}
