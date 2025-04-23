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
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.unit.isUnspecified
import androidx.compose.ui.unit.sp
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.lib.ValidationResult
import dev.patrickgold.florisboard.lib.compose.DpSizeSaver
import dev.patrickgold.florisboard.lib.compose.FlorisChip
import dev.patrickgold.florisboard.lib.compose.FlorisIconButton
import dev.patrickgold.florisboard.lib.compose.FlorisTextButton
import dev.patrickgold.florisboard.lib.compose.Validation
import dev.patrickgold.florisboard.lib.compose.rippleClickable
import dev.patrickgold.florisboard.lib.compose.stringRes
import dev.patrickgold.florisboard.lib.ext.ExtensionValidation
import dev.patrickgold.florisboard.lib.rememberValidationResult
import dev.patrickgold.florisboard.lib.stripUnicodeCtrlChars
import dev.patrickgold.jetpref.material.ui.ExperimentalJetPrefMaterial3Ui
import dev.patrickgold.jetpref.material.ui.JetPrefAlertDialog
import dev.patrickgold.jetpref.material.ui.JetPrefColorPicker
import dev.patrickgold.jetpref.material.ui.JetPrefDropdown
import dev.patrickgold.jetpref.material.ui.JetPrefTextField
import dev.patrickgold.jetpref.material.ui.rememberJetPrefColorPickerState
import org.florisboard.lib.color.ColorPalette
import org.florisboard.lib.kotlin.curlyFormat
import org.florisboard.lib.kotlin.toStringWithoutDotZero
import org.florisboard.lib.snygg.SnyggAnnotationRule
import org.florisboard.lib.snygg.SnyggRule
import org.florisboard.lib.snygg.SnyggSpec
import org.florisboard.lib.snygg.value.SnyggCutCornerDpShapeValue
import org.florisboard.lib.snygg.value.SnyggCutCornerPercentShapeValue
import org.florisboard.lib.snygg.value.SnyggDefinedVarValue
import org.florisboard.lib.snygg.value.SnyggDpShapeValue
import org.florisboard.lib.snygg.value.SnyggDpSizeValue
import org.florisboard.lib.snygg.value.SnyggDynamicColorValue
import org.florisboard.lib.snygg.value.SnyggDynamicDarkColorValue
import org.florisboard.lib.snygg.value.SnyggDynamicLightColorValue
import org.florisboard.lib.snygg.value.SnyggPercentShapeValue
import org.florisboard.lib.snygg.value.SnyggRoundedCornerDpShapeValue
import org.florisboard.lib.snygg.value.SnyggRoundedCornerPercentShapeValue
import org.florisboard.lib.snygg.value.SnyggShapeValue
import org.florisboard.lib.snygg.value.SnyggSpSizeValue
import org.florisboard.lib.snygg.value.SnyggStaticColorValue
import org.florisboard.lib.snygg.value.SnyggUndefinedValue
import org.florisboard.lib.snygg.value.SnyggValue
import org.florisboard.lib.snygg.value.SnyggValueEncoder

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

@Composable
internal fun EditPropertyDialog(
    initProperty: PropertyInfo,
    level: SnyggLevel,
    displayColorsAs: DisplayColorsAs,
    definedVariables: Map<String, SnyggValue>,
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
        return when (val value = propertyValue) {
            is SnyggUndefinedValue -> false
            is SnyggDefinedVarValue -> value.key.isNotBlank()
            is SnyggSpSizeValue -> value.sp.isSpecified && value.sp.value >= 1f
            else -> true
        }
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
                    value = propertyValue,
                    onValueChange = { propertyValue = it },
                    level = level,
                    displayColorsAs = displayColorsAs,
                    definedVariables = definedVariables,
                    isError = showSelectAsError && !isPropertyValueValid(),
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
    displayColorsAs: DisplayColorsAs,
    definedVariables: Map<String, SnyggValue>,
    isError: Boolean = false,
) {
    val context = LocalContext.current
    when (value) {
        is SnyggDefinedVarValue -> {
            val variableKeys = remember(definedVariables) {
                listOf("") + definedVariables.keys.toList()
            }
            val selectedIndex by remember(variableKeys, value.key) {
                mutableIntStateOf(variableKeys.indexOf(value.key).coerceIn(variableKeys.indices))
            }
            Row(
                modifier = Modifier.padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
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
            val colorPickerStr = context.translatePropertyValue(value, level, displayColorsAs)
            var showEditColorStrDialog by rememberSaveable { mutableStateOf(false) }
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    Row(
                        modifier = Modifier
                            .rippleClickable {
                                showEditColorStrDialog = true
                            }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            modifier = Modifier
                                .padding(end = 12.dp)
                                .weight(1f),
                            text = colorPickerStr,
                            style = MaterialTheme.typography.bodyMedium,
                            fontFamily = FontFamily.Monospace,
                        )
                        SnyggValueIcon(
                            value = value,
                            definedVariables = definedVariables,
                        )
                    }
                    JetPrefColorPicker(
                        onColorChange = { onValueChange(SnyggStaticColorValue(it)) },
                        state = colorPickerState,
                    )
                }
            }
            if (showEditColorStrDialog) {
                var showValidationErrors by rememberSaveable { mutableStateOf(false) }
                var showSyntaxHelp by rememberSaveable { mutableStateOf(false) }
                var colorStr by rememberSaveable { mutableStateOf(colorPickerStr.stripUnicodeCtrlChars()) }
                val colorStrValidation = rememberValidationResult(ExtensionValidation.SnyggStaticColorValue, colorStr)
                JetPrefAlertDialog(
                    title = stringRes(R.string.settings__theme_editor__property_value_color_dialog_title),
                    confirmLabel = stringRes(R.string.action__apply),
                    onConfirm = {
                        if (colorStrValidation.isInvalid()) {
                            showValidationErrors = true
                        } else {
                            val newValue = SnyggStaticColorValue.deserialize(colorStr.trim()).getOrThrow()
                            onValueChange(newValue)
                            colorPickerState.setColor((newValue as SnyggStaticColorValue).color)
                            showEditColorStrDialog = false
                        }
                    },
                    dismissLabel = stringRes(R.string.action__cancel),
                    onDismiss = {
                        showEditColorStrDialog = false
                    },
                    trailingIconTitle = {
                        FlorisIconButton(
                            onClick = { showSyntaxHelp = !showSyntaxHelp },
                            modifier = Modifier.offset(x = 12.dp),
                            icon = Icons.AutoMirrored.Filled.HelpOutline,
                        )
                    },
                ) {
                    Column {
                        AnimatedVisibility(visible = showSyntaxHelp) {
                            Column(modifier = Modifier.padding(bottom = 16.dp)) {
                                Text(text = "Supported color string syntaxes:")
                                Text(
                                    text = """
                                        #RRGGBBAA
                                         -> all in 00h..FFh
                                        #RRGGBB
                                         -> all in 00h..FFh
                                        rgba(r,g,b,a)
                                         -> r,g,b in 0..255
                                         -> a in 0.0..1.0
                                        rgb(r,g,b)
                                         -> r,g,b in 0..255
                                    """.trimIndent(),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontFamily = FontFamily.Monospace,
                                )
                            }
                        }
                        JetPrefTextField(
                            value = colorStr,
                            onValueChange = { colorStr = it },
                        )
                        Validation(showValidationErrors, colorStrValidation)
                    }
                }
            }
        }

        is SnyggDpSizeValue -> {
            var sizeStr by remember {
                val dp = value.dp.takeUnless { it.isUnspecified } ?: SnyggDpSizeValue.defaultValue().dp
                mutableStateOf(dp.value.toStringWithoutDotZero())
            }
            Row(
                modifier = Modifier.padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
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

        is SnyggDynamicColorValue -> {
            val onSelectItem: (Int) -> Unit = when (value) {
                is SnyggDynamicDarkColorValue -> { index ->
                    onValueChange(SnyggDynamicDarkColorValue(ColorPalette.colorNames[index]))
                }

                is SnyggDynamicLightColorValue -> { index ->
                    onValueChange(SnyggDynamicLightColorValue(ColorPalette.colorNames[index]))
                }
            }

            val selectedIndex by remember(value.colorName) {
                mutableIntStateOf(
                    ColorPalette.colorNames.indexOf(value.colorName).coerceIn(ColorPalette.colorNames.indices)
                )
            }
            Row(
                modifier = Modifier.padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
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

        is SnyggSpSizeValue -> {
            var sizeStr by remember {
                val sp = value.sp.takeUnless { it.isUnspecified } ?: SnyggSpSizeValue.defaultValue().sp
                mutableStateOf(sp.value.toStringWithoutDotZero())
            }
            Row(
                modifier = Modifier.padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
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

        is SnyggShapeValue -> when (value) {
            is SnyggDpShapeValue -> {
                var showDialogInitDp by rememberSaveable(stateSaver = DpSizeSaver) {
                    mutableStateOf(0.dp)
                }
                var showDialogForCorner by rememberSaveable {
                    mutableStateOf<ShapeCorner?>(null)
                }
                var topStart by rememberSaveable(stateSaver = DpSizeSaver) {
                    mutableStateOf(
                        when (value) {
                            is SnyggCutCornerDpShapeValue -> value.topStart
                            is SnyggRoundedCornerDpShapeValue -> value.topStart
                        }
                    )
                }
                var topEnd by rememberSaveable(stateSaver = DpSizeSaver) {
                    mutableStateOf(
                        when (value) {
                            is SnyggCutCornerDpShapeValue -> value.topEnd
                            is SnyggRoundedCornerDpShapeValue -> value.topEnd
                        }
                    )
                }
                var bottomEnd by rememberSaveable(stateSaver = DpSizeSaver) {
                    mutableStateOf(
                        when (value) {
                            is SnyggCutCornerDpShapeValue -> value.bottomEnd
                            is SnyggRoundedCornerDpShapeValue -> value.bottomEnd
                        }
                    )
                }
                var bottomStart by rememberSaveable(stateSaver = DpSizeSaver) {
                    mutableStateOf(
                        when (value) {
                            is SnyggCutCornerDpShapeValue -> value.bottomStart
                            is SnyggRoundedCornerDpShapeValue -> value.bottomStart
                        }
                    )
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
                    modifier = Modifier
                        .padding(vertical = 8.dp)
                        .fillMaxWidth(),
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
                    mutableIntStateOf(
                        when (value) {
                            is SnyggCutCornerPercentShapeValue -> value.topStart
                            is SnyggRoundedCornerPercentShapeValue -> value.topStart
                        }
                    )
                }
                var topEnd by rememberSaveable {
                    mutableIntStateOf(
                        when (value) {
                            is SnyggCutCornerPercentShapeValue -> value.topEnd
                            is SnyggRoundedCornerPercentShapeValue -> value.topEnd
                        }
                    )
                }
                var bottomEnd by rememberSaveable {
                    mutableIntStateOf(
                        when (value) {
                            is SnyggCutCornerPercentShapeValue -> value.bottomEnd
                            is SnyggRoundedCornerPercentShapeValue -> value.bottomEnd
                        }
                    )
                }
                var bottomStart by rememberSaveable {
                    mutableIntStateOf(
                        when (value) {
                            is SnyggCutCornerPercentShapeValue -> value.bottomStart
                            is SnyggRoundedCornerPercentShapeValue -> value.bottomStart
                        }
                    )
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
                    modifier = Modifier
                        .padding(vertical = 8.dp)
                        .fillMaxWidth(),
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
                    modifier = Modifier
                        .padding(vertical = 8.dp)
                        .fillMaxWidth(),
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

        else -> {
            // Render nothing
        }
    }
}
