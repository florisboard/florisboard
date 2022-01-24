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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.app.res.stringRes
import dev.patrickgold.florisboard.app.ui.components.FlorisDropdownMenu
import dev.patrickgold.florisboard.snygg.SnyggLevel
import dev.patrickgold.florisboard.snygg.SnyggPropertySetSpec
import dev.patrickgold.florisboard.snygg.value.SnyggDefinedVarValue
import dev.patrickgold.florisboard.snygg.value.SnyggImplicitInheritValue
import dev.patrickgold.florisboard.snygg.value.SnyggSolidColorValue
import dev.patrickgold.florisboard.snygg.value.SnyggValue
import dev.patrickgold.florisboard.snygg.value.SnyggValueEncoder
import dev.patrickgold.florisboard.snygg.value.SnyggVarValueEncoders
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
    definedVariables: Map<String, SnyggValue>,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    val isAddPropertyDialog = initProperty == SnyggEmptyPropertyInfoForAdding
    var showSelectAsError by rememberSaveable { mutableStateOf(false) }
    var showAlreadyExistsError by rememberSaveable { mutableStateOf(false) }

    var propertyName by rememberSaveable {
        mutableStateOf(if (isAddPropertyDialog && propertySetSpec == null) { "" } else { initProperty.name })
    }
    var propertyValueEncoder by remember {
        mutableStateOf(if (isAddPropertyDialog && propertySetSpec == null) {
            SnyggImplicitInheritValue
        } else {
            initProperty.value.encoder()
        })
    }
    var propertyValue by remember {
        mutableStateOf(if (isAddPropertyDialog && propertySetSpec == null) {
            SnyggImplicitInheritValue
        } else {
            initProperty.value
        })
    }

    fun isPropertyNameValid(): Boolean {
        return propertyName.isNotBlank() && propertyName != SnyggEmptyPropertyInfoForAdding.name
    }

    fun isPropertyValueValid(): Boolean {
        return when (val value = propertyValue) {
            is SnyggImplicitInheritValue -> false
            is SnyggDefinedVarValue -> value.key.isNotBlank()
            else -> true
        }
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
            if (!isPropertyNameValid() || !isPropertyValueValid()) {
                showSelectAsError = true
            } else {
                //
            }
        },
        dismissLabel = stringRes(R.string.action__cancel),
        onDismiss = onDismiss,
        neutralLabel = if (!isAddPropertyDialog) { stringRes(R.string.action__delete) } else { null },
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
                    onNameChange = { name ->
                        if (propertySetSpec != null) {
                            propertyValueEncoder = SnyggImplicitInheritValue
                        }
                        propertyName = name
                    },
                    level = level,
                    isAddPropertyDialog = isAddPropertyDialog,
                    showSelectAsError = showSelectAsError,
                )
            }

            DialogProperty(text = stringRes(R.string.settings__theme_editor__property_value)) {
                PropertyValueEncoderDropdown(
                    supportedEncoders = remember(propertyName) {
                        propertySetSpec?.propertySpec(propertyName)?.encoders ?: SnyggVarValueEncoders
                    },
                    encoder = propertyValueEncoder,
                    onEncoderChange = { encoder ->
                        propertyValueEncoder = encoder
                        propertyValue = encoder.defaultValue()
                    },
                    enabled = isPropertyNameValid(),
                    isError = showSelectAsError && propertyValueEncoder == SnyggImplicitInheritValue,
                )

                PropertyValueEditor(
                    value = propertyValue,
                    onValueChange = { propertyValue = it },
                    level = level,
                    definedVariables = definedVariables,
                    enabled = isPropertyNameValid() && propertyValueEncoder != SnyggImplicitInheritValue,
                    isError = showSelectAsError && !isPropertyValueValid(),
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

@Composable
private fun PropertyValueEncoderDropdown(
    supportedEncoders: List<SnyggValueEncoder>,
    encoder: SnyggValueEncoder,
    onEncoderChange: (SnyggValueEncoder) -> Unit,
    enabled: Boolean = true,
    isError: Boolean = false,
) {
    val encoders = remember(supportedEncoders) {
        listOf(SnyggImplicitInheritValue) + supportedEncoders
    }
    var expanded by remember { mutableStateOf(false) }
    val selectedIndex = remember(encoder) {
        encoders.indexOf(encoder).coerceIn(encoders.indices)
    }
    FlorisDropdownMenu(
        items = encoders,
        labelProvider = { translatePropertyValueEncoderName(it) },
        expanded = expanded,
        enabled = enabled,
        selectedIndex = selectedIndex,
        isError = isError,
        onSelectItem = { index ->
            onEncoderChange(encoders[index])
        },
        onExpandRequest = { expanded = true },
        onDismissRequest = { expanded = false },
    )
}

@Composable
private fun PropertyValueEditor(
    value: SnyggValue,
    onValueChange: (SnyggValue) -> Unit,
    level: SnyggLevel,
    definedVariables: Map<String, SnyggValue>,
    enabled: Boolean = true,
    isError: Boolean = false,
) {
    when (value) {
        is SnyggDefinedVarValue -> {
            val variableKeys = remember(definedVariables) {
                listOf("") + definedVariables.keys.toList()
            }
            val selectedIndex by remember(variableKeys, value.key) {
                mutableStateOf(variableKeys.indexOf(value.key).coerceIn(variableKeys.indices))
            }
            var expanded by remember { mutableStateOf(false) }
            Row(
                modifier = Modifier.padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FlorisDropdownMenu(
                    modifier = Modifier
                        .padding(end = 12.dp)
                        .weight(1f),
                    items = variableKeys,
                    labelProvider = { translatePropertyName(it, level) },
                    expanded = expanded,
                    selectedIndex = selectedIndex,
                    enabled = enabled,
                    isError = isError,
                    onSelectItem = { index ->
                        onValueChange(SnyggDefinedVarValue(variableKeys[index]))
                    },
                    onExpandRequest = { expanded = true },
                    onDismissRequest = { expanded = false },
                )
                SnyggValueIcon(
                    value = value,
                    definedVariables = definedVariables,
                    modifier = Modifier.offset(y = (-2).dp),
                )
            }
        }
        else -> {
            // Render nothing
        }
    }
}
