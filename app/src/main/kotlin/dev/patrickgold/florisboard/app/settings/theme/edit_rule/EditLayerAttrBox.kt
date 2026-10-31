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
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
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
import org.k3lp.model.layer.K3LayerId

@Composable
fun EditLayerAttrBox(
    attrSpec: ImeUiAttr<K3LayerId>,
    attributes: SnyggAttributes,
    updateAttributes: (SnyggAttributes) -> Unit,
) {
    val values = remember(attrSpec.name, attributes) {
        (attributes[attrSpec.name] ?: emptyList()).toMutableStateList()
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
            for (value in values) {
                FlorisChip(
                    onClick = {
                        updateAttributes(attributes.excluding(attrSpec.name to value))
                    },
                    text = value,
                )
            }
            if (values.isEmpty()) {
                Text(
                    text = stringRes(R.string.settings__theme_editor__no_codes_defined),
                    fontStyle = FontStyle.Italic,
                )
            }
        }
    }

    if (showAddDialog) {
        val state = rememberTextFieldState()
        val isValid = remember(state) { attrSpec.validate(state.text.toString()) }
        var showValidation by remember { mutableStateOf(false) }
        JetPrefAlertDialog(
            title = stringRes(R.string.action__add),
            confirmLabel = stringRes(R.string.action__add),
            onConfirm = {
                val value = state.text.toString()
                if (attrSpec.validate(value)) {
                    updateAttributes(attributes.including(attrSpec.name to value))
                    showAddDialog = false
                } else {
                    showValidation = true
                }
            },
            dismissLabel = stringRes(R.string.action__cancel),
            onDismiss = { showAddDialog = false },
        ) {
            val isError = showValidation && !isValid
            TextField(
                state = state,
                isError = isError,
                label = { Text("Layer id") },
                supportingText = if (isError) {
                    {
                        Text(
                            text = "Invalid layer id",
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                } else null,
            )
        }
    }
}
