/*
 * Copyright (C) 2022-2025 The FlorisBoard Contributors
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

package dev.patrickgold.florisboard.app.settings.theme

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ManageSearch
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isUnspecified
import androidx.compose.ui.unit.sp
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.app.ext.FONTS
import dev.patrickgold.florisboard.app.ext.IMAGES
import dev.patrickgold.florisboard.lib.ValidationResult
import dev.patrickgold.florisboard.lib.cache.CacheManager
import dev.patrickgold.florisboard.lib.compose.DpSizeSaver
import dev.patrickgold.florisboard.lib.compose.FlorisChip
import dev.patrickgold.florisboard.lib.compose.FlorisIconButton
import dev.patrickgold.florisboard.lib.compose.FlorisTextButton
import dev.patrickgold.florisboard.lib.compose.Validation
import dev.patrickgold.florisboard.lib.compose.florisVerticalScroll
import dev.patrickgold.florisboard.lib.compose.stringRes
import dev.patrickgold.florisboard.lib.ext.ExtensionValidation
import dev.patrickgold.florisboard.lib.rememberValidationResult
import dev.patrickgold.jetpref.material.ui.ColorRepresentation
import dev.patrickgold.jetpref.material.ui.ExperimentalJetPrefMaterial3Ui
import dev.patrickgold.jetpref.material.ui.JetPrefAlertDialog
import dev.patrickgold.jetpref.material.ui.JetPrefColorPicker
import dev.patrickgold.jetpref.material.ui.JetPrefDropdown
import dev.patrickgold.jetpref.material.ui.JetPrefListItem
import dev.patrickgold.jetpref.material.ui.JetPrefTextField
import dev.patrickgold.jetpref.material.ui.rememberJetPrefColorPickerState
import org.florisboard.lib.color.ColorPalette
import org.florisboard.lib.kotlin.curlyFormat
import org.florisboard.lib.kotlin.io.subDir
import org.florisboard.lib.kotlin.toStringWithoutDotZero
import org.florisboard.lib.snygg.SnyggAnnotationRule
import org.florisboard.lib.snygg.SnyggRule
import org.florisboard.lib.snygg.SnyggSpec
import org.florisboard.lib.snygg.value.SnyggContentScaleValue
import org.florisboard.lib.snygg.value.SnyggCustomFontFamilyValue
import org.florisboard.lib.snygg.value.SnyggCutCornerDpShapeValue
import org.florisboard.lib.snygg.value.SnyggCutCornerPercentShapeValue
import org.florisboard.lib.snygg.value.SnyggDefinedVarValue
import org.florisboard.lib.snygg.value.SnyggDpShapeValue
import org.florisboard.lib.snygg.value.SnyggDpSizeValue
import org.florisboard.lib.snygg.value.SnyggDynamicColorValue
import org.florisboard.lib.snygg.value.SnyggDynamicDarkColorValue
import org.florisboard.lib.snygg.value.SnyggDynamicLightColorValue
import org.florisboard.lib.snygg.value.SnyggEnumLikeValueEncoder
import org.florisboard.lib.snygg.value.SnyggFontStyleValue
import org.florisboard.lib.snygg.value.SnyggFontWeightValue
import org.florisboard.lib.snygg.value.SnyggGenericFontFamilyValue
import org.florisboard.lib.snygg.value.SnyggPaddingValue
import org.florisboard.lib.snygg.value.SnyggPercentShapeValue
import org.florisboard.lib.snygg.value.SnyggPercentageSizeValue
import org.florisboard.lib.snygg.value.SnyggRoundedCornerDpShapeValue
import org.florisboard.lib.snygg.value.SnyggRoundedCornerPercentShapeValue
import org.florisboard.lib.snygg.value.SnyggShapeValue
import org.florisboard.lib.snygg.value.SnyggSpSizeValue
import org.florisboard.lib.snygg.value.SnyggStaticColorValue
import org.florisboard.lib.snygg.value.SnyggTextAlignValue
import org.florisboard.lib.snygg.value.SnyggTextDecorationLineValue
import org.florisboard.lib.snygg.value.SnyggTextMaxLinesValue
import org.florisboard.lib.snygg.value.SnyggTextOverflowValue
import org.florisboard.lib.snygg.value.SnyggUndefinedValue
import org.florisboard.lib.snygg.value.SnyggUriValue
import org.florisboard.lib.snygg.value.SnyggValue
import org.florisboard.lib.snygg.value.SnyggValueEncoder
import java.io.File

internal val SnyggEmptyPropertyInfoForAdding = PropertyInfo(
    rule = SnyggEmptyRuleForAdding,
    name = "--select--",
    value = SnyggUndefinedValue,
)

data class PropertyInfo(
    val rule: SnyggRule,
    val name: String,
    val value: SnyggValue,
)

private enum class ShapeCorner {
    TOP_START,
    TOP_END,
    BOTTOM_END,
    BOTTOM_START;

    @Composable
    fun label(): String {
        return stringRes(
            when (this) {
                TOP_START -> R.string.enum__shape_corner__top_start
                TOP_END -> R.string.enum__shape_corner__top_end
                BOTTOM_END -> R.string.enum__shape_corner__bottom_end
                BOTTOM_START -> R.string.enum__shape_corner__bottom_start
            }
        )
    }
}

private enum class PaddingValue {
    TOP,
    BOTTOM,
    START,
    END;

    @Composable
    fun label(): String {
        return when (this) {
            TOP -> "Top"
            BOTTOM -> "Bottom"
            START -> "Start"
            END -> "End"
        }
    }
}

@Composable
internal fun EditPropertyDialog(
    initProperty: PropertyInfo,
    level: SnyggLevel,
    colorRepresentation: ColorRepresentation,
    definedVariables: Map<String, SnyggValue>,
    fontNames: List<String>,
    workspace: CacheManager.ExtEditorWorkspace<*>,
    onConfirmNewValue: (String, SnyggValue) -> Boolean,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    val isAddPropertyDialog = initProperty.name == SnyggEmptyPropertyInfoForAdding.name
    var showSelectAsError by rememberSaveable { mutableStateOf(false) }
    var showAlreadyExistsError by rememberSaveable { mutableStateOf(false) }

    var propertyName by rememberSaveable {
        mutableStateOf(
            if (isAddPropertyDialog) {
                ""
            } else {
                initProperty.name
            }
        )
    }
    val propertyNameValidation = rememberValidationResult(ExtensionValidation.ThemeComponentVariableName, propertyName)

    val encoders = remember(initProperty.rule, propertyName) { SnyggSpec.encodersOf(initProperty.rule, propertyName) }
    var propertyValueEncoder by remember {
        mutableStateOf(
            if (isAddPropertyDialog && encoders == null) {
                SnyggUndefinedValue
            } else {
                initProperty.value.encoder()
            }
        )
    }
    var propertyValue by remember {
        mutableStateOf(
            if (isAddPropertyDialog && encoders == null) {
                SnyggUndefinedValue
            } else {
                initProperty.value
            }
        )
    }

    fun isPropertyNameValid(): Boolean {
        return when (initProperty.rule) {
            is SnyggAnnotationRule.Defines -> {
                propertyNameValidation.isValid() && propertyName != SnyggEmptyPropertyInfoForAdding.name
            }

            else -> propertyName.isNotEmpty() && propertyName != SnyggEmptyPropertyInfoForAdding.name
        }
    }

    fun isPropertyValueValid(): Boolean {
        return propertyValue.let { it.encoder().serialize(it).isSuccess }
    }

    JetPrefAlertDialog(
        title = stringRes(
            if (isAddPropertyDialog) {
                R.string.settings__theme_editor__add_property
            } else {
                R.string.settings__theme_editor__edit_property
            }
        ),
        confirmLabel = stringRes(
            if (isAddPropertyDialog) {
                R.string.action__add
            } else {
                R.string.action__apply
            }
        ),
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
        neutralLabel = if (!isAddPropertyDialog) {
            stringRes(R.string.action__delete)
        } else {
            null
        },
        onNeutral = onDelete,
        neutralColors = ButtonDefaults.textButtonColors(
            contentColor = MaterialTheme.colorScheme.error,
        ),
    ) {
        Column {
            AnimatedVisibility(visible = showAlreadyExistsError) {
                Text(
                    modifier = Modifier.padding(bottom = 16.dp),
                    text = stringRes(R.string.settings__theme_editor__property_already_exists),
                    color = MaterialTheme.colorScheme.error,
                )
            }

            DialogProperty(text = stringRes(R.string.settings__theme_editor__property_name)) {
                PropertyNameInput(
                    rule = initProperty.rule,
                    name = propertyName,
                    nameValidation = propertyNameValidation,
                    onNameChange = { name ->
                        if (encoders != null) {
                            propertyValueEncoder = SnyggUndefinedValue
                            propertyValue = SnyggUndefinedValue
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
                    supportedEncoders = encoders.orEmpty(),
                    encoder = propertyValueEncoder,
                    onEncoderChange = { encoder ->
                        propertyValueEncoder = encoder
                        propertyValue = encoder.defaultValue()
                    },
                    enabled = isPropertyNameValid(),
                    isError = showSelectAsError && propertyValueEncoder == SnyggUndefinedValue,
                )

                PropertyValueEditor(
                    modifier = Modifier.padding(top = 8.dp),
                    value = propertyValue,
                    onValueChange = { propertyValue = it },
                    level = level,
                    colorRepresentation = colorRepresentation,
                    definedVariables = definedVariables,
                    fontNames = fontNames,
                    workspace = workspace,
                    isError = !isPropertyValueValid(),
                )
            }
        }
    }
}

@Composable
private fun PropertyNameInput(
    rule: SnyggRule,
    name: String,
    nameValidation: ValidationResult,
    onNameChange: (String) -> Unit,
    level: SnyggLevel,
    isAddPropertyDialog: Boolean,
    showSelectAsError: Boolean,
) {
    val context = LocalContext.current
    if (rule !is SnyggAnnotationRule.Defines) {
        val possiblePropertyNames = buildList {
            add(SnyggEmptyPropertyInfoForAdding.name)
            addAll(SnyggSpec.propertiesOf(rule))
        }
        val possiblePropertyLabels = possiblePropertyNames.map { context.translatePropertyName(it, level) }
        val propertiesSelectedIndex = remember(name) {
            possiblePropertyNames.indexOf(name).coerceIn(possiblePropertyNames.indices)
        }
        JetPrefDropdown(
            options = possiblePropertyLabels,
            selectedOptionIndex = propertiesSelectedIndex,
            onSelectOption = { index ->
                onNameChange(possiblePropertyNames[index])
            },
            enabled = isAddPropertyDialog,
            isError = showSelectAsError && propertiesSelectedIndex == 0,
        )
    } else {
        val focusManager = LocalFocusManager.current
        JetPrefTextField(
            value = name,
            onValueChange = onNameChange,
            enabled = isAddPropertyDialog,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
            singleLine = true,
            isError = showSelectAsError
        )
        Validation(
            showValidationErrors = isAddPropertyDialog && showSelectAsError,
            validationResult = nameValidation
        )

    }
}

@Composable
private fun PropertyValueEncoderDropdown(
    supportedEncoders: Set<SnyggValueEncoder>,
    encoder: SnyggValueEncoder,
    onEncoderChange: (SnyggValueEncoder) -> Unit,
    enabled: Boolean = true,
    isError: Boolean = false,
) {
    val encoders = remember(supportedEncoders) {
        listOf(SnyggUndefinedValue) + supportedEncoders
    }
    val selectedIndex = remember(encoder) {
        encoders.indexOf(encoder).coerceIn(encoders.indices)
    }
    val context = LocalContext.current
    JetPrefDropdown(
        options = encoders,
        selectedOptionIndex = selectedIndex,
        onSelectOption = { index ->
            onEncoderChange(encoders[index])
        },
        enabled = enabled,
        isError = isError,
        optionsLabelProvider = { context.translatePropertyValueEncoderName(it) },
    )
}

@OptIn(ExperimentalJetPrefMaterial3Ui::class)
@Composable
private fun PropertyValueEditor(
    value: SnyggValue,
    onValueChange: (SnyggValue) -> Unit,
    level: SnyggLevel,
    colorRepresentation: ColorRepresentation,
    definedVariables: Map<String, SnyggValue>,
    fontNames: List<String>,
    workspace: CacheManager.ExtEditorWorkspace<*>,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
) {
    val context = LocalContext.current
    when (value) {
        is SnyggDefinedVarValue -> {
            val variableKeys = remember(definedVariables) {
                listOf("") + definedVariables.keys.toList()
            }
            val selectedIndex = remember(variableKeys, value.key) {
                variableKeys.indexOf(value.key).coerceIn(variableKeys.indices)
            }
            Row(modifier, verticalAlignment = Alignment.CenterVertically) {
                JetPrefDropdown(
                    modifier = Modifier
                        .padding(end = 12.dp)
                        .weight(1f),
                    options = variableKeys,
                    optionsLabelProvider = { context.translatePropertyName(it, level) },
                    selectedOptionIndex = selectedIndex,
                    isError = isError,
                    onSelectOption = { index ->
                        onValueChange(SnyggDefinedVarValue(variableKeys[index]))
                    },
                )
                SnyggValueIcon(
                    value = value,
                    definedVariables = definedVariables,
                )
            }
        }

        is SnyggStaticColorValue -> {
            val colorPickerState = rememberJetPrefColorPickerState(initColor = value.color)
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                JetPrefColorPicker(
                    modifier = Modifier.padding(top = 8.dp),
                    onColorChange = { onValueChange(SnyggStaticColorValue(it)) },
                    initialRepresentation = colorRepresentation,
                    state = colorPickerState,
                )
            }
        }

        is SnyggDynamicColorValue -> {
            val onSelectItem: (Int) -> Unit = when (value) {
                is SnyggDynamicDarkColorValue -> { index ->
                    onValueChange(SnyggDynamicDarkColorValue(ColorPalette.colorNames[index]))
                }

                is SnyggDynamicLightColorValue -> { index ->
                    onValueChange(SnyggDynamicLightColorValue(ColorPalette.colorNames[index]))
                }
            }

            val selectedIndex = remember(value.colorName) {
                ColorPalette.colorNames.indexOf(value.colorName).coerceIn(ColorPalette.colorNames.indices)
            }
            Row(modifier, verticalAlignment = Alignment.CenterVertically) {
                JetPrefDropdown(
                    modifier = Modifier
                        .padding(end = 12.dp)
                        .weight(1f),
                    options = ColorPalette.colorNames,
                    selectedOptionIndex = selectedIndex,
                    onSelectOption = onSelectItem,
                    isError = isError,
                    optionsLabelProvider = { context.translatePropertyName(it, level) },
                )
                SnyggValueIcon(
                    value = value,
                    definedVariables = definedVariables,
                )
            }
        }

        is SnyggGenericFontFamilyValue -> {
            EnumLikeValueEditor(value.encoder(), value, onValueChange, modifier)
        }

        is SnyggCustomFontFamilyValue -> {
            CustomFontFamilyValueEditor(value, onValueChange, fontNames, isError, modifier)
        }

        is SnyggFontStyleValue -> {
            EnumLikeValueEditor(value.encoder(), value, onValueChange, modifier)
        }

        is SnyggFontWeightValue -> {
            EnumLikeValueEditor(value.encoder(), value, onValueChange, modifier)
        }

        is SnyggPaddingValue -> {
            PaddingValueEditor(value, onValueChange, modifier)
        }

        is SnyggShapeValue -> {
            ShapeValueEditor(value, onValueChange, modifier)
        }

        is SnyggDpSizeValue -> {
            var sizeStr by remember {
                val dp = value.dp.takeUnless { it.isUnspecified } ?: SnyggDpSizeValue.defaultValue().dp
                mutableStateOf(dp.value.toStringWithoutDotZero())
            }
            Row(modifier, verticalAlignment = Alignment.CenterVertically) {
                JetPrefTextField(
                    modifier = Modifier.weight(1f),
                    value = sizeStr,
                    onValueChange = { value ->
                        sizeStr = value
                        val size = sizeStr.toFloatOrNull()?.let { SnyggDpSizeValue(it.dp) }
                        onValueChange(size ?: SnyggDpSizeValue(Dp.Unspecified))
                    },
                    isError = value.dp.isUnspecified || value.dp.value < 0f,
                )
                Text(
                    modifier = Modifier.padding(start = 8.dp),
                    text = "dp",
                    fontFamily = FontFamily.Monospace,
                )
            }
        }

        is SnyggSpSizeValue -> {
            var sizeStr by remember {
                val sp = value.sp.takeUnless { it.isUnspecified } ?: SnyggSpSizeValue.defaultValue().sp
                mutableStateOf(sp.value.toStringWithoutDotZero())
            }
            Row(modifier, verticalAlignment = Alignment.CenterVertically) {
                JetPrefTextField(
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

        is SnyggPercentageSizeValue -> {
            var sizeStr by remember {
                mutableStateOf(value.percentage.toString())
            }
            Row(modifier, verticalAlignment = Alignment.CenterVertically) {
                JetPrefTextField(
                    modifier = Modifier.weight(1f),
                    value = sizeStr,
                    onValueChange = { value ->
                        sizeStr = value
                        val size = sizeStr.toFloatOrNull()?.let { SnyggPercentageSizeValue(it) }
                        onValueChange(size ?: SnyggPercentageSizeValue(0f))
                    },
                    isError = value.percentage < 0f || value.percentage > 1f,
                )
                Text(
                    modifier = Modifier.padding(start = 8.dp),
                    text = "%",
                    fontFamily = FontFamily.Monospace,
                )
            }
        }

        is SnyggContentScaleValue -> {
            EnumLikeValueEditor(value.encoder(), value, onValueChange, modifier)
        }

        is SnyggTextAlignValue -> {
            EnumLikeValueEditor(value.encoder(), value, onValueChange, modifier)
        }

        is SnyggTextDecorationLineValue -> {
            EnumLikeValueEditor(value.encoder(), value, onValueChange, modifier)
        }

        is SnyggTextMaxLinesValue -> {
            var inputStr by rememberSaveable {
                mutableStateOf(value.encoder().serialize(value).getOrDefault("???"))
            }
            JetPrefTextField(
                value = inputStr,
                onValueChange = { newInputStr ->
                    inputStr = newInputStr
                    onValueChange(
                        value.encoder().deserialize(newInputStr)
                            .getOrDefault(SnyggTextMaxLinesValue(-1))
                    )
                },
                modifier = modifier,
                isError = isError,
            )
        }

        is SnyggTextOverflowValue -> {
            EnumLikeValueEditor(value.encoder(), value, onValueChange, modifier)
        }

        is SnyggUriValue -> {
            var inputStr by rememberSaveable {
                mutableStateOf(value.uri)
            }
            Column(modifier) {
                val fontFiles = remember {
                    workspace.extDir.subDir(FONTS).listFiles { it.isFile }.orEmpty().asList()
                }
                val imageFiles = remember {
                    workspace.extDir.subDir(IMAGES).listFiles { it.isFile }.orEmpty().asList()
                }
                var showSelectFileDialog by rememberSaveable { mutableStateOf(false) }
                if (showSelectFileDialog) {
                    @Composable
                    fun ClickableFilesWithHeading(title: String, files: List<File>) {
                        ListItem(
                            headlineContent = {
                                Text(
                                    text = title,
                                    color = MaterialTheme.colorScheme.secondary,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            },
                            colors = ListItemDefaults.colors(
                                containerColor = AlertDialogDefaults.containerColor
                            ),
                        )
                        files.forEach { file ->
                            JetPrefListItem(
                                modifier = Modifier.clickable {
                                    val relPath = file.path.removePrefix(workspace.extDir.path)
                                    inputStr = "flex:$relPath"
                                    onValueChange(SnyggUriValue(inputStr))
                                    showSelectFileDialog = false
                                },
                                text = file.name,
                                colors = ListItemDefaults.colors(
                                    containerColor = AlertDialogDefaults.containerColor
                                ),
                            )
                        }
                    }
                    JetPrefAlertDialog(
                        title = stringRes(R.string.settings__theme_editor__file_selector_dialog_title),
                        dismissLabel = stringRes(R.string.action__cancel),
                        onDismiss = {
                            showSelectFileDialog = false
                        },
                        contentPadding = PaddingValues(horizontal = 8.dp),
                        scrollModifier = Modifier.florisVerticalScroll(),
                    ) {
                        Column {
                            if (fontFiles.isNotEmpty()) {
                                ClickableFilesWithHeading(stringRes(R.string.ext__editor__files__type_fonts), fontFiles)
                            }
                            if (imageFiles.isNotEmpty()) {
                                ClickableFilesWithHeading(stringRes(R.string.ext__editor__files__type_images), imageFiles)
                            }
                            if (fontFiles.isEmpty() && imageFiles.isEmpty()) {
                                Text(
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    text = stringRes(R.string.settings__theme_editor__file_selector_no_files_text,
                                        "action_title" to stringRes(R.string.ext__editor__files__title)),
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        }
                    }
                }
                JetPrefTextField(
                    value = inputStr,
                    onValueChange = {},
                    isError = isError,
                    enabled = false,
                    trailingIcon = {
                        FlorisIconButton(
                            onClick = { showSelectFileDialog = true },
                            icon = Icons.AutoMirrored.Filled.ManageSearch,
                        )
                    },
                )
            }
        }

        else -> {
            // Render nothing
        }
    }
}

@Composable
private fun <T> EnumLikeValueEditor(
    encoder: SnyggEnumLikeValueEncoder<T>,
    value: SnyggValue,
    onValueChange: (SnyggValue) -> Unit,
    modifier: Modifier = Modifier,
) {
    val options = remember(encoder.serializationId) { encoder.serializationMapping.keys.toList() }
    val selectedIndex = remember(value) {
        encoder.serializationMapping.values.indexOf(encoder.destruct(value))
    }

    JetPrefDropdown(
        modifier = modifier,
        options = options,
        selectedOptionIndex = selectedIndex,
        onSelectOption = { index ->
            val innerValue = encoder.serializationMapping.values.elementAt(index)
            val value = encoder.construct(innerValue)
            onValueChange(value)
        },
    )
}

@Composable
private fun CustomFontFamilyValueEditor(
    value: SnyggCustomFontFamilyValue,
    onValueChange: (SnyggValue) -> Unit,
    fontNames: List<String>,
    isError: Boolean,
    modifier: Modifier = Modifier,
) {
    val options = remember(fontNames) {
        listOf("-select-") + fontNames
    }
    val selectedIndex = remember(value) {
        val index = fontNames.indexOf(value.fontName)
        if (index == -1) {
            0
        } else {
            index + 1
        }
    }

    JetPrefDropdown(
        modifier = modifier,
        options = options,
        selectedOptionIndex = selectedIndex,
        onSelectOption = { index ->
            if (index == 0) {
                val value = SnyggCustomFontFamilyValue("```")
                onValueChange(value)
            } else {
                val fontName = fontNames.elementAt(index - 1)
                val value = SnyggCustomFontFamilyValue(fontName)
                onValueChange(value)
            }
        },
        isError = isError,
    )
}

@Composable
private fun PaddingValueEditor(
    value: SnyggPaddingValue,
    onValueChange: (SnyggValue) -> Unit,
    modifier: Modifier = Modifier,
) {
    val layoutDirection = LocalLayoutDirection.current
    val paddingValue = value.values
    var showDialogInitDp by rememberSaveable(stateSaver = DpSizeSaver) {
        mutableStateOf(0.dp)
    }
    var showDialogForPaddingValue by rememberSaveable {
        mutableStateOf<PaddingValue?>(null)
    }
    var start by rememberSaveable(stateSaver = DpSizeSaver) {
        mutableStateOf(paddingValue.calculateStartPadding(layoutDirection))
    }
    var end by rememberSaveable(stateSaver = DpSizeSaver) {
        mutableStateOf(paddingValue.calculateEndPadding(layoutDirection))
    }
    var top by rememberSaveable(stateSaver = DpSizeSaver) {
        mutableStateOf(paddingValue.calculateTopPadding())
    }
    var bottom by rememberSaveable(stateSaver = DpSizeSaver) {
        mutableStateOf(paddingValue.calculateBottomPadding())
    }
    val paddingValues = remember(start, end, top, bottom) {
        PaddingValues(start, top, end, bottom)
    }

    LaunchedEffect(paddingValues) {
        onValueChange(
            SnyggPaddingValue(paddingValues)
        )
    }

    @Composable
    fun DpChip(
        onClick: () -> Unit,
        text: String,
        alignment: Alignment,
    ) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = alignment,
        ) {
            FlorisChip(
                onClick = onClick,
                text = text,
                shape = MaterialTheme.shapes.medium,
            )
        }
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            DpChip(
                onClick = {
                    showDialogInitDp = start
                    showDialogForPaddingValue = PaddingValue.START
                },
                text = stringRes(R.string.unit__display_pixel__symbol).curlyFormat("v" to start.value.toStringWithoutDotZero()),
                alignment = Alignment.CenterEnd,
            )
        }
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            DpChip(
                onClick = {
                    showDialogInitDp = top
                    showDialogForPaddingValue = PaddingValue.TOP
                },
                text = stringRes(R.string.unit__display_pixel__symbol).curlyFormat("v" to top.value.toStringWithoutDotZero()),
                alignment = Alignment.Center,
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(MaterialTheme.shapes.small)
                    .background(MaterialTheme.colorScheme.primary)
                    .padding(paddingValues)
                    .background(MaterialTheme.colorScheme.inversePrimary),
            )
            DpChip(
                onClick = {
                    showDialogInitDp = bottom
                    showDialogForPaddingValue = PaddingValue.BOTTOM
                },
                text = stringRes(R.string.unit__display_pixel__symbol).curlyFormat("v" to bottom.value.toStringWithoutDotZero()),
                alignment = Alignment.Center,
            )
        }
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            DpChip(
                onClick = {
                    showDialogInitDp = end
                    showDialogForPaddingValue = PaddingValue.END
                },
                text = stringRes(R.string.unit__display_pixel__symbol).curlyFormat("v" to end.value.toStringWithoutDotZero()),
                alignment = Alignment.CenterStart,
            )
        }
    }

    val dialogForPaddingValue = showDialogForPaddingValue
    if (dialogForPaddingValue != null) {
        var showValidationErrors by rememberSaveable { mutableStateOf(false) }
        var size by rememberSaveable {
            mutableStateOf(showDialogInitDp.value.toStringWithoutDotZero())
        }
        val sizeValidation = rememberValidationResult(ExtensionValidation.SnyggDpShapeValue, size)
        JetPrefAlertDialog(
            title = dialogForPaddingValue.label(),
            confirmLabel = stringRes(R.string.action__apply),
            onConfirm = {
                if (sizeValidation.isInvalid()) {
                    showValidationErrors = true
                } else {
                    val sizeDp = size.toFloat().dp
                    when (dialogForPaddingValue) {
                        PaddingValue.TOP -> top = sizeDp
                        PaddingValue.BOTTOM -> bottom = sizeDp
                        PaddingValue.START -> start = sizeDp
                        PaddingValue.END -> end = sizeDp
                    }
                    showDialogForPaddingValue = null
                }
            },
            dismissLabel = stringRes(R.string.action__cancel),
            onDismiss = {
                showDialogForPaddingValue = null
            },
        ) {
            Column {
                JetPrefTextField(
                    value = size,
                    onValueChange = { size = it },
                )
                Validation(showValidationErrors, sizeValidation)
                FlorisTextButton(
                    onClick = {
                        if (sizeValidation.isInvalid()) {
                            showValidationErrors = true
                        } else {
                            val sizeDp = size.toFloat().dp
                            top = sizeDp
                            bottom = sizeDp
                            start = sizeDp
                            end = sizeDp
                            showDialogForPaddingValue = null
                        }
                    },
                    modifier = Modifier.align(Alignment.End),
                    text = "Apply for all",
                )
            }
        }
    }
}

@Composable
private fun ShapeValueEditor(
    value: SnyggShapeValue,
    onValueChange: (SnyggValue) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (value) {
        is SnyggDpShapeValue -> {
            var showDialogInitDp by rememberSaveable(stateSaver = DpSizeSaver) {
                mutableStateOf(0.dp)
            }
            var showDialogForCorner by rememberSaveable {
                mutableStateOf<ShapeCorner?>(null)
            }
            var topStart by rememberSaveable(stateSaver = DpSizeSaver) {
                mutableStateOf(value.topStart)
            }
            var topEnd by rememberSaveable(stateSaver = DpSizeSaver) {
                mutableStateOf(value.topEnd)
            }
            var bottomEnd by rememberSaveable(stateSaver = DpSizeSaver) {
                mutableStateOf(value.bottomEnd)
            }
            var bottomStart by rememberSaveable(stateSaver = DpSizeSaver) {
                mutableStateOf(value.bottomStart)
            }
            val shape = remember(topStart, topEnd, bottomEnd, bottomStart) {
                when (value) {
                    is SnyggCutCornerDpShapeValue -> {
                        CutCornerShape(topStart, topEnd, bottomEnd, bottomStart)
                    }

                    is SnyggRoundedCornerDpShapeValue -> {
                        RoundedCornerShape(topStart, topEnd, bottomEnd, bottomStart)
                    }
                }
            }
            LaunchedEffect(shape) {
                onValueChange(
                    when (value) {
                        is SnyggCutCornerDpShapeValue -> {
                            SnyggCutCornerDpShapeValue(topStart, topEnd, bottomEnd, bottomStart)
                        }

                        is SnyggRoundedCornerDpShapeValue -> {
                            SnyggRoundedCornerDpShapeValue(topStart, topEnd, bottomEnd, bottomStart)
                        }
                    }
                )
            }
            Row(
                modifier = modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceAround,
            ) {
                Column {
                    FlorisChip(
                        onClick = {
                            showDialogInitDp = topStart
                            showDialogForCorner = ShapeCorner.TOP_START
                        },
                        text = stringRes(R.string.unit__display_pixel__symbol).curlyFormat("v" to topStart.value.toStringWithoutDotZero()),
                        shape = MaterialTheme.shapes.medium,
                    )
                    FlorisChip(
                        onClick = {
                            showDialogInitDp = bottomStart
                            showDialogForCorner = ShapeCorner.BOTTOM_START
                        },
                        text = stringRes(R.string.unit__display_pixel__symbol).curlyFormat("v" to bottomStart.value.toStringWithoutDotZero()),
                        shape = MaterialTheme.shapes.medium,
                    )
                }
                Box(
                    modifier = Modifier
                        .requiredSize(40.dp)
                        .border(1.dp, MaterialTheme.colorScheme.onBackground, shape),
                )
                Column {
                    FlorisChip(
                        onClick = {
                            showDialogInitDp = topEnd
                            showDialogForCorner = ShapeCorner.TOP_END
                        },
                        text = stringRes(R.string.unit__display_pixel__symbol).curlyFormat("v" to topEnd.value.toStringWithoutDotZero()),
                        shape = MaterialTheme.shapes.medium,
                    )
                    FlorisChip(
                        onClick = {
                            showDialogInitDp = bottomEnd
                            showDialogForCorner = ShapeCorner.BOTTOM_END
                        },
                        text = stringRes(R.string.unit__display_pixel__symbol).curlyFormat("v" to bottomEnd.value.toStringWithoutDotZero()),
                        shape = MaterialTheme.shapes.medium,
                    )
                }
            }
            val dialogForCorner = showDialogForCorner
            if (dialogForCorner != null) {
                var showValidationErrors by rememberSaveable { mutableStateOf(false) }
                var size by rememberSaveable {
                    mutableStateOf(showDialogInitDp.value.toStringWithoutDotZero())
                }
                val sizeValidation = rememberValidationResult(ExtensionValidation.SnyggDpShapeValue, size)
                JetPrefAlertDialog(
                    title = dialogForCorner.label(),
                    confirmLabel = stringRes(R.string.action__apply),
                    onConfirm = {
                        if (sizeValidation.isInvalid()) {
                            showValidationErrors = true
                        } else {
                            val sizeDp = size.toFloat().dp
                            when (dialogForCorner) {
                                ShapeCorner.TOP_START -> topStart = sizeDp
                                ShapeCorner.TOP_END -> topEnd = sizeDp
                                ShapeCorner.BOTTOM_END -> bottomEnd = sizeDp
                                ShapeCorner.BOTTOM_START -> bottomStart = sizeDp
                            }
                            showDialogForCorner = null
                        }
                    },
                    dismissLabel = stringRes(R.string.action__cancel),
                    onDismiss = {
                        showDialogForCorner = null
                    },
                ) {
                    Column {
                        JetPrefTextField(
                            value = size,
                            onValueChange = { size = it },
                        )
                        Validation(showValidationErrors, sizeValidation)
                        FlorisTextButton(
                            onClick = {
                                if (sizeValidation.isInvalid()) {
                                    showValidationErrors = true
                                } else {
                                    val sizeDp = size.toFloat().dp
                                    topStart = sizeDp
                                    topEnd = sizeDp
                                    bottomEnd = sizeDp
                                    bottomStart = sizeDp
                                    showDialogForCorner = null
                                }
                            },
                            modifier = Modifier.align(Alignment.End),
                            text = stringRes(R.string.settings__theme_editor__property_value_shape_apply_for_all_corners),
                        )
                    }
                }
            }
        }

        is SnyggPercentShapeValue -> {
            var showDialogInitPercentage by rememberSaveable {
                mutableIntStateOf(0)
            }
            var showDialogForCorner by rememberSaveable {
                mutableStateOf<ShapeCorner?>(null)
            }
            var topStart by rememberSaveable {
                mutableIntStateOf(value.topStart)
            }
            var topEnd by rememberSaveable {
                mutableIntStateOf(value.topEnd)
            }
            var bottomEnd by rememberSaveable {
                mutableIntStateOf(value.bottomEnd)
            }
            var bottomStart by rememberSaveable {
                mutableIntStateOf(value.bottomStart)
            }
            val shape = remember(topStart, topEnd, bottomEnd, bottomStart) {
                when (value) {
                    is SnyggCutCornerPercentShapeValue -> {
                        CutCornerShape(topStart, topEnd, bottomEnd, bottomStart)
                    }

                    is SnyggRoundedCornerPercentShapeValue -> {
                        RoundedCornerShape(topStart, topEnd, bottomEnd, bottomStart)
                    }
                }
            }
            LaunchedEffect(shape) {
                onValueChange(
                    when (value) {
                        is SnyggCutCornerPercentShapeValue -> {
                            SnyggCutCornerPercentShapeValue(topStart, topEnd, bottomEnd, bottomStart)
                        }

                        is SnyggRoundedCornerPercentShapeValue -> {
                            SnyggRoundedCornerPercentShapeValue(topStart, topEnd, bottomEnd, bottomStart)
                        }
                    }
                )
            }
            Row(
                modifier = modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceAround,
            ) {
                Column {
                    FlorisChip(
                        onClick = {
                            showDialogInitPercentage = topStart
                            showDialogForCorner = ShapeCorner.TOP_START
                        },
                        text = stringRes(R.string.unit__percent__symbol).curlyFormat("v" to topStart),
                        shape = MaterialTheme.shapes.medium,
                    )
                    FlorisChip(
                        onClick = {
                            showDialogInitPercentage = bottomStart
                            showDialogForCorner = ShapeCorner.BOTTOM_START
                        },
                        text = stringRes(R.string.unit__percent__symbol).curlyFormat("v" to bottomStart),
                        shape = MaterialTheme.shapes.medium,
                    )
                }
                Box(
                    modifier = Modifier
                        .requiredSize(40.dp)
                        .border(1.dp, MaterialTheme.colorScheme.onBackground, shape),
                )
                Column {
                    FlorisChip(
                        onClick = {
                            showDialogInitPercentage = topEnd
                            showDialogForCorner = ShapeCorner.TOP_END
                        },
                        text = stringRes(R.string.unit__percent__symbol).curlyFormat("v" to topEnd),
                        shape = MaterialTheme.shapes.medium,
                    )
                    FlorisChip(
                        onClick = {
                            showDialogInitPercentage = bottomEnd
                            showDialogForCorner = ShapeCorner.BOTTOM_END
                        },
                        text = stringRes(R.string.unit__percent__symbol).curlyFormat("v" to bottomEnd),
                        shape = MaterialTheme.shapes.medium,
                    )
                }
            }
            val dialogForCorner = showDialogForCorner
            if (dialogForCorner != null) {
                var showValidationErrors by rememberSaveable { mutableStateOf(false) }
                var size by rememberSaveable {
                    mutableStateOf(showDialogInitPercentage.toString())
                }
                val sizeValidation = rememberValidationResult(ExtensionValidation.SnyggPercentShapeValue, size)
                JetPrefAlertDialog(
                    title = dialogForCorner.label(),
                    confirmLabel = stringRes(R.string.action__apply),
                    onConfirm = {
                        if (sizeValidation.isInvalid()) {
                            showValidationErrors = true
                        } else {
                            val sizePercentage = size.toInt()
                            when (showDialogForCorner) {
                                ShapeCorner.TOP_START -> topStart = sizePercentage
                                ShapeCorner.TOP_END -> topEnd = sizePercentage
                                ShapeCorner.BOTTOM_END -> bottomEnd = sizePercentage
                                ShapeCorner.BOTTOM_START -> bottomStart = sizePercentage
                                else -> {}
                            }
                            showDialogForCorner = null
                        }
                    },
                    dismissLabel = stringRes(R.string.action__cancel),
                    onDismiss = {
                        showDialogForCorner = null
                    },
                ) {
                    Column {
                        JetPrefTextField(
                            value = size,
                            onValueChange = { size = it },
                        )
                        Validation(showValidationErrors, sizeValidation)
                        FlorisTextButton(
                            onClick = {
                                if (sizeValidation.isInvalid()) {
                                    showValidationErrors = true
                                } else {
                                    val sizePercentage = size.toInt()
                                    topStart = sizePercentage
                                    topEnd = sizePercentage
                                    bottomEnd = sizePercentage
                                    bottomStart = sizePercentage
                                    showDialogForCorner = null
                                }
                            },
                            modifier = Modifier.align(Alignment.End),
                            text = stringRes(R.string.settings__theme_editor__property_value_shape_apply_for_all_corners),
                        )
                    }
                }
            }
        }

        else -> {
            Row(
                modifier = modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
            ) {
                Box(
                    modifier = Modifier
                        .requiredSize(40.dp)
                        .border(1.dp, MaterialTheme.colorScheme.onBackground, value.shape),
                )
            }
        }
    }
}
