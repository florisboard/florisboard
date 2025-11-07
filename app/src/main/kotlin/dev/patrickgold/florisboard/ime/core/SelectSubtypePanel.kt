/*
 * Copyright (C) 2025 The OmniBoard Contributors
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

package dev.silo.omniboard.ime.core

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import dev.silo.omniboard.R
import dev.silo.omniboard.ime.keyboard.KeyboardState
import dev.silo.omniboard.ime.theme.OmniImeUi
import dev.silo.omniboard.keyboardManager
import dev.silo.omniboard.subtypeManager
import org.omniboard.lib.compose.stringRes
import org.omniboard.lib.snygg.ui.SnyggBox
import org.omniboard.lib.snygg.ui.SnyggColumn
import org.omniboard.lib.snygg.ui.SnyggListItem
import org.omniboard.lib.snygg.ui.SnyggRow
import org.omniboard.lib.snygg.ui.SnyggText

@Composable
fun SelectSubtypePanel(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val keyboardManager by context.keyboardManager()
    val subtypeManager by context.subtypeManager()

    val listState = rememberLazyListState()
    val subtypes by subtypeManager.subtypesFlow.collectAsState()

    val currentlySelected = subtypeManager.activeSubtype.id

    SnyggColumn(OmniImeUi.SubtypePanel.elementName, modifier = modifier.safeDrawingPadding()) {
        SnyggRow(
            elementName = OmniImeUi.SubtypePanelHeader.elementName,
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SnyggText(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(false) {},
                text = stringRes(R.string.select_subtype_panel__header),
            )
        }

        SnyggBox(OmniImeUi.SubtypePanelList.elementName) {
            LazyColumn(
                state = listState,
            ) {
                items(
                    subtypes,
                    key = {
                        it.id
                    }
                ) {
                    SnyggListItem(
                        elementName = OmniImeUi.SubtypePanelListItem.elementName,
                        onClick = {
                            subtypeManager.switchToSubtypeById(it.id)
                            keyboardManager.activeState.isSubtypeSelectionVisible = false
                        },
                        leadingImageVector = when {
                            currentlySelected == it.id -> Icons.Default.RadioButtonChecked
                            else -> Icons.Default.RadioButtonUnchecked
                        },
                        text = it.primaryLocale.displayName(),
                    )
                }
            }
        }
    }
}

fun KeyboardState.isSubtypeSelectionShowing(): Boolean {
    return isSubtypeSelectionVisible
}
