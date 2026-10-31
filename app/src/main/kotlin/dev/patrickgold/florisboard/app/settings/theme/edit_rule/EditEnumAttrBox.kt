/*
 * Copyright (C) 2026 The FlorisBoard Contributors
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

package dev.patrickgold.florisboard.app.settings.theme.edit_rule

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.offset
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.app.settings.theme.DialogProperty
import dev.patrickgold.florisboard.ime.theme.ImeUiAttr
import dev.patrickgold.jetpref.material.ui.JetPrefAlertDialog
import org.florisboard.lib.compose.FlorisChip
import org.florisboard.lib.compose.FlorisIconButton
import org.florisboard.lib.compose.stringRes
import org.florisboard.lib.snygg.SnyggAttributes

@Composable
fun EditEnumAttrBox(
    attrSpec: ImeUiAttr.Enumeration<*>,
    attributes: SnyggAttributes,
    updateAttributes: (SnyggAttributes) -> Unit,
) {
    val allEntries = attrSpec.allowedValues
    val (alreadyAddedEntries, notYetAddedEntries) = remember(attributes, attrSpec) {
        allEntries.partition { entry ->
            attributes[attrSpec.name]?.contains(entry) == true
        }
    }
    var showAddDialog by remember { mutableStateOf(false) }

    DialogProperty(
        text = attrSpec.name,
        trailingIconTitle = {
            FlorisIconButton(
                onClick = { showAddDialog = true },
                modifier = Modifier.offset(x = 12.dp),
                icon = Icons.Default.Add,
            )
        },
    ) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            for (entry in alreadyAddedEntries) {
                FlorisChip(
                    onClick = {
                        updateAttributes(attributes.excluding(attrSpec.name to entry))
                    },
                    text = entry,
                )
            }
            if (alreadyAddedEntries.isEmpty()) {
                Text(
                    text = stringRes(R.string.settings__theme_editor__no_codes_defined),
                    fontStyle = FontStyle.Italic,
                )
            }
        }
    }

    if (showAddDialog) {
        JetPrefAlertDialog(
            title = stringRes(R.string.action__add),
            dismissLabel = stringRes(R.string.action__cancel),
            onDismiss = { showAddDialog = false },
        ) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (entry in notYetAddedEntries) {
                    FlorisChip(
                        onClick = {
                            updateAttributes(attributes.including(attrSpec.name to entry))
                            showAddDialog = false
                        },
                        text = entry,
                    )
                }
            }
            if (notYetAddedEntries.isEmpty()) {
                Text(
                    text = stringRes(R.string.settings__theme_editor__no_enum_value_to_add_anymore),
                    fontStyle = FontStyle.Italic,
                )
            }
        }
    }
}
