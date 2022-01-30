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
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.unit.isUnspecified
import androidx.compose.ui.unit.sp
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.app.res.stringRes
import dev.patrickgold.florisboard.app.ui.components.FlorisDropdownMenu
import dev.patrickgold.florisboard.app.ui.components.FlorisOutlinedTextField
import dev.patrickgold.florisboard.common.ValidationResult
import dev.patrickgold.florisboard.common.rememberValidationResult
import dev.patrickgold.florisboard.res.ext.ExtensionValidation
import dev.patrickgold.florisboard.snygg.SnyggLevel
import dev.patrickgold.florisboard.snygg.SnyggPropertySetSpec
import dev.patrickgold.florisboard.snygg.value.SnyggDefinedVarValue
import dev.patrickgold.florisboard.snygg.value.SnyggImplicitInheritValue
import dev.patrickgold.florisboard.snygg.value.SnyggSolidColorValue
import dev.patrickgold.florisboard.snygg.value.SnyggSpSizeValue
import dev.patrickgold.florisboard.snygg.value.SnyggValue
import dev.patrickgold.florisboard.snygg.value.SnyggValueEncoder
import dev.patrickgold.florisboard.snygg.value.SnyggVarValueEncoders
import dev.patrickgold.jetpref.material.ui.ExperimentalJetPrefMaterialUi
import dev.patrickgold.jetpref.material.ui.JetPrefAlertDialog
import dev.patrickgold.jetpref.material.ui.JetPrefColorPicker
import dev.patrickgold.jetpref.material.ui.rememberJetPrefColorPickerState

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
    onConfirmNewValue: (String, SnyggValue) -> Boolean,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    val isAddPropertyDialog = initProperty == SnyggEmptyPropertyInfoForAdding
    var showSelectAsError by rememberSaveable { mutableStateOf(false) }
    var showAlreadyExistsError by rememberSaveable { mutableStateOf(false) }

    var propertyName by rememberSaveable {
        mutableStateOf(if (isAddPropertyDialog && propertySetSpec == null) { "" } else { initProperty.name })
    }
    val propertyNameValidation = rememberValidationResult(ExtensionValidation.ThemeComponentVariableName, propertyName)
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
            is SnyggSpSizeValue -> value.sp.isSpecified && value.sp.value >= 1f
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
                if (!onConfirmNewValue(propertyName, propertyValue)) {
                    showAlreadyExistsError = true
                }
            }
        },
        dismissLabel = stringRes(R.string.action__cancel),
        onDismiss = onDismiss,
        neutralLabel = if (!isAddPropertyDialog) { stringRes(R.string.action__delete) } else { null },
        onNeutral = onDelete,
        neutralColors = ButtonDefaults.textButtonColors(
            contentColor = MaterialTheme.colors.error,
        ),
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
                    nameValidation = propertyNameValidation,
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
    nameValidation: ValidationResult,
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
        FlorisOutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            enabled = isAddPropertyDialog,
            showValidationHint = isAddPropertyDialog,
            showValidationError = showSelectAsError,
            validationResult = nameValidation,
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

@OptIn(ExperimentalJetPrefMaterialUi::class)
@Composable
private fun PropertyValueEditor(
    value: SnyggValue,
    onValueChange: (SnyggValue) -> Unit,
    level: SnyggLevel,
    definedVariables: Map<String, SnyggValue>,
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
                )
            }
        }
        is SnyggSolidColorValue -> {
            Column(modifier = Modifier.padding(top = 8.dp)) {
                Row(
                    modifier = Modifier.padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .weight(1f),
                        text = value.encoder().serialize(value).getOrDefault("?"),
                    )
                    SnyggValueIcon(
                        value = value,
                        definedVariables = definedVariables,
                    )
                }
                val state = rememberJetPrefColorPickerState(initColor = value.color)
                JetPrefColorPicker(
                    onColorChange = { onValueChange(SnyggSolidColorValue(it)) },
                    state = state,
                )
            }
        }
        is SnyggSpSizeValue -> {
            var sizeStr by remember {
                val sp = value.sp.takeUnless { it.isUnspecified } ?: SnyggSpSizeValue.defaultValue().sp
                mutableStateOf(sp.value.toString())
            }
            Row(
                modifier = Modifier.padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FlorisOutlinedTextField(
                    modifier = Modifier.weight(1f),
                    value = sizeStr,
                    onValueChange = { value ->
                        sizeStr = value
                        val size = sizeStr.toFloatOrNull()?.let { SnyggSpSizeValue(it.sp) }
                        onValueChange(size ?: SnyggSpSizeValue(TextUnit.Unspecified))
                    },
                    isError = value.sp.isUnspecified || value.sp.value < 1f,
                )
                Text(
                    modifier = Modifier.padding(start = 8.dp),
                    text = "sp",
                    fontFamily = FontFamily.Monospace,
                )
            }
        }
        else -> {
            // Render nothing
        }
    }
}
