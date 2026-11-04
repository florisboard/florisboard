/*
 * Copyright (C) 2025-2026 The FlorisBoard Contributors
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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.ime.keyboard3.LocalImeController
import dev.patrickgold.florisboard.ime.theme.FlorisImeUi
import kotlinx.coroutines.launch
import org.florisboard.lib.compose.stringRes
import org.florisboard.lib.snygg.ui.SnyggBox
import org.florisboard.lib.snygg.ui.SnyggColumn
import org.florisboard.lib.snygg.ui.SnyggListItem
import org.florisboard.lib.snygg.ui.SnyggRow
import org.florisboard.lib.snygg.ui.SnyggText

@Composable
fun SelectSubtypePanel(modifier: Modifier = Modifier) {
    val imeController = LocalImeController.current

    val imeState by imeController.activeState.collectAsState()
    val subtypes by remember { derivedStateOf { imeState.subtypes } }
    val activeSubtype by remember { derivedStateOf { imeState.activeSubtype } }

    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    SnyggColumn(FlorisImeUi.SubtypePanel.elementName, modifier = modifier.safeDrawingPadding()) {
        SnyggRow(
            elementName = FlorisImeUi.SubtypePanelHeader.elementName,
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

        SnyggBox(FlorisImeUi.SubtypePanelList.elementName) {
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
                        elementName = FlorisImeUi.SubtypePanelListItem.elementName,
                        onClick = {
                            scope.launch {
                                imeController.switchToSubtypeById(it.id)
                                imeController.updateState {
                                    state = state.copy(
                                        flags = state.flags.withSubtypeSelectionVisible(false),
                                    )
                                }
                            }
                        },
                        leadingImageVector = when {
                            activeSubtype.id == it.id -> Icons.Default.RadioButtonChecked
                            else -> Icons.Default.RadioButtonUnchecked
                        },
                        text = it.primaryLocale.displayName(),
                    )
                }
            }
        }
    }
}
