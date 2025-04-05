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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.ime.keyboard.KeyboardState
import dev.patrickgold.florisboard.ime.theme.FlorisImeUi
import dev.patrickgold.florisboard.keyboardManager
import dev.patrickgold.florisboard.lib.compose.rippleClickable
import dev.patrickgold.florisboard.lib.compose.stringRes
import dev.patrickgold.florisboard.subtypeManager
import dev.patrickgold.jetpref.material.ui.JetPrefListItem
import org.florisboard.lib.snygg.ui.SnyggColumn
import org.florisboard.lib.snygg.ui.SnyggRow
import org.florisboard.lib.snygg.ui.SnyggSpacer
import org.florisboard.lib.snygg.ui.SnyggText

@Composable
fun SelectSubtypePanel(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val keyboardManager by context.keyboardManager()
    val subtypeManager by context.subtypeManager()

    val listState = rememberLazyListState()
    val subtypes by subtypeManager.subtypesFlow.collectAsState()

    val currentlySelected = subtypeManager.activeSubtype.id

    SnyggColumn(FlorisImeUi.SmartbarActionsEditor, modifier = modifier) {
        SnyggRow(
            elementName = FlorisImeUi.SmartbarActionsEditorHeader,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SnyggText(
                elementName = null,
                modifier = Modifier.clickable(false) {},
                text = stringRes(R.string.select_subtype_panel__header),
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
                        colors = ListItemDefaults.colors(),
                    )
                }
            }
        }
        SnyggSpacer(
            elementName = FlorisImeUi.SmartbarActionsEditorSpacer,
            modifier = Modifier.systemBarsPadding(),
        )
    }
}

fun KeyboardState.isSubtypeSelectionShowing(): Boolean {
    return isSubtypeSelectionVisible
}
