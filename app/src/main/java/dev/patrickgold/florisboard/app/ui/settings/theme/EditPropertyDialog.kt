/*
 * Copyright (C) 2022 Patrick Goldinger
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

package dev.patrickgold.florisboard.app.ui.settings.theme

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.app.res.stringRes
import dev.patrickgold.florisboard.app.ui.components.FlorisDropdownMenu
import dev.patrickgold.florisboard.snygg.SnyggLevel
import dev.patrickgold.florisboard.snygg.SnyggPropertySetSpec
import dev.patrickgold.florisboard.snygg.value.SnyggImplicitInheritValue
import dev.patrickgold.florisboard.snygg.value.SnyggValue
import dev.patrickgold.jetpref.material.ui.JetPrefAlertDialog

internal val SnyggEmptyPropertyInfoForAdding = PropertyInfo(
    name = "- select -",
    value = SnyggImplicitInheritValue,
)

data class PropertyInfo(
    val name: String,
    val value: SnyggValue,
)

@Composable
internal fun EditPropertyDialog(
    propertySetSpec: SnyggPropertySetSpec?,
    initProperty: PropertyInfo,
    level: SnyggLevel,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    val isAddPropertyDialog = initProperty == SnyggEmptyPropertyInfoForAdding
    var showSelectAsError by rememberSaveable { mutableStateOf(false) }
    var showAlreadyExistsError by rememberSaveable { mutableStateOf(false) }

    var propertyName by rememberSaveable {
        mutableStateOf(if (isAddPropertyDialog && propertySetSpec == null) { "" } else { initProperty.name })
    }

    JetPrefAlertDialog(
        title = stringRes(if (isAddPropertyDialog) {
            R.string.settings__theme_editor__add_property
        } else {
            R.string.settings__theme_editor__edit_property
        }),
        confirmLabel = stringRes(if (isAddPropertyDialog) {
            R.string.action__add
        } else {
            R.string.action__apply
        }),
        onConfirm = {
            if (isAddPropertyDialog && (propertyName.isBlank() || propertyName == SnyggEmptyPropertyInfoForAdding.name)) {
                showSelectAsError = true
            } else {
                //
            }
        },
        dismissLabel = stringRes(R.string.action__cancel),
        onDismiss = onDismiss,
        neutralLabel = stringRes(R.string.action__delete),
        onNeutral = onDelete,
    ) {
        Column {
            AnimatedVisibility(visible = showAlreadyExistsError) {
                Text(
                    modifier = Modifier.padding(bottom = 16.dp),
                    text = stringRes(R.string.settings__theme_editor__property_already_exists),
                    color = MaterialTheme.colors.error,
                )
            }

            DialogProperty(text = stringRes(R.string.settings__theme_editor__property_name)) {
                PropertyNameInput(
                    propertySetSpec = propertySetSpec,
                    name = propertyName,
                    onNameChange = { propertyName = it },
                    level = level,
                    isAddPropertyDialog = isAddPropertyDialog,
                    showSelectAsError = showSelectAsError,
                )
            }
        }
    }
}

@Composable
private fun PropertyNameInput(
    propertySetSpec: SnyggPropertySetSpec?,
    name: String,
    onNameChange: (String) -> Unit,
    level: SnyggLevel,
    isAddPropertyDialog: Boolean,
    showSelectAsError: Boolean,
) {
    if (propertySetSpec != null) {
        val possiblePropertyNames = remember(propertySetSpec) {
            listOf(SnyggEmptyPropertyInfoForAdding.name) + propertySetSpec.supportedProperties.map { it.name }
        }
        val possiblePropertyLabels = possiblePropertyNames.map { translatePropertyName(it, level) }
        var propertiesExpanded by remember { mutableStateOf(false) }
        val propertiesSelectedIndex = remember(name) {
            possiblePropertyNames.indexOf(name).coerceIn(possiblePropertyNames.indices)
        }
        FlorisDropdownMenu(
            items = possiblePropertyLabels,
            expanded = propertiesExpanded,
            enabled = isAddPropertyDialog,
            selectedIndex = propertiesSelectedIndex,
            isError = showSelectAsError && propertiesSelectedIndex == 0,
            onSelectItem = { index ->
                onNameChange(possiblePropertyNames[index])
            },
            onExpandRequest = { propertiesExpanded = true },
            onDismissRequest = { propertiesExpanded = false },
        )
    } else {
        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            enabled = isAddPropertyDialog,
            isError = showSelectAsError && name.isBlank(),
        )
    }
}
