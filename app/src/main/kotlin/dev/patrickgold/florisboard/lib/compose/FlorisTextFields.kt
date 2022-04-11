/*
 * Copyright (C) 2021 Patrick Goldinger
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

package dev.patrickgold.florisboard.lib.compose

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ProvideTextStyle
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextFieldColors
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import dev.patrickgold.florisboard.app.apptheme.outline
import dev.patrickgold.florisboard.lib.ValidationResult

@Composable
fun FlorisOutlinedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    textStyle: TextStyle = TextStyle.Default,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = false,
    maxLines: Int = Int.MAX_VALUE,
    placeholder: String? = null,
    isError: Boolean = false,
    showValidationHint: Boolean = true,
    showValidationError: Boolean = false,
    validationResult: ValidationResult? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    shape: Shape = MaterialTheme.shapes.small,
    colors: TextFieldColors = TextFieldDefaults.outlinedTextFieldColors(),
) {
    var textFieldValueState by remember { mutableStateOf(TextFieldValue(text = value)) }
    val textFieldValue = textFieldValueState.copy(text = value)

    FlorisOutlinedTextField(
        value = textFieldValue,
        onValueChange = {
            textFieldValueState = it
            if (value != it.text) {
                onValueChange(it.text)
            }
        },
        modifier = modifier,
        enabled = enabled,
        readOnly = readOnly,
        textStyle = textStyle,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        singleLine = singleLine,
        maxLines = maxLines,
        placeholder = placeholder,
        isError = isError,
        showValidationHint = showValidationHint,
        showValidationError = showValidationError,
        validationResult = validationResult,
        visualTransformation = visualTransformation,
        interactionSource = interactionSource,
        shape = shape,
        colors = colors,
    )
}

@Composable
fun FlorisOutlinedTextField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    textStyle: TextStyle = TextStyle.Default,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = false,
    maxLines: Int = Int.MAX_VALUE,
    placeholder: String? = null,
    isError: Boolean = false,
    showValidationHint: Boolean = true,
    showValidationError: Boolean = false,
    validationResult: ValidationResult? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    shape: Shape = MaterialTheme.shapes.small,
    colors: TextFieldColors = TextFieldDefaults.outlinedTextFieldColors(
        unfocusedBorderColor = MaterialTheme.colors.outline,
        disabledBorderColor = MaterialTheme.colors.outline,
    ),
) {
    val textColor = textStyle.color.takeOrElse {
        colors.textColor(enabled).value
    }
    val mergedTextStyle = textStyle.copy(color = textColor, textDirection = TextDirection.Content)
    val isFocused by interactionSource.collectIsFocusedAsState()
    val isErrorState = isError || (showValidationError && validationResult?.isInvalid() == true)

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        BasicTextField(
            modifier = modifier.padding(vertical = 4.dp),
            value = value,
            onValueChange = onValueChange,
            enabled = enabled,
            readOnly = readOnly,
            textStyle = mergedTextStyle,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            singleLine = singleLine,
            maxLines = maxLines,
            visualTransformation = visualTransformation,
            cursorBrush = SolidColor(colors.cursorColor(isErrorState).value),
            decorationBox = { innerTextField ->
                Surface(
                    modifier = modifier.fillMaxWidth(),
                    color = colors.backgroundColor(enabled).value,
                    border = if (isErrorState && enabled) {
                        BorderStroke(ButtonDefaults.OutlinedBorderSize, MaterialTheme.colors.error)
                    } else if (isFocused) {
                        BorderStroke(ButtonDefaults.OutlinedBorderSize, MaterialTheme.colors.primary)
                    } else {
                        ButtonDefaults.outlinedBorder
                    },
                    shape = shape,
                ) {
                    Box(
                        modifier = Modifier
                            .defaultMinSize(
                                minWidth = ButtonDefaults.MinWidth,
                                minHeight = 40.dp,
                            )
                            .padding(ButtonDefaults.ContentPadding),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        ProvideTextStyle(value = mergedTextStyle) {
                            innerTextField()
                        }
                        if (!placeholder.isNullOrBlank()) {
                            Text(
                                text = placeholder,
                                style = MaterialTheme.typography.body2,
                                color = MaterialTheme.colors.onSurface.copy(alpha = 0.56f),
                            )
                        }
                    }
                }
            },
        )
    }

    if (showValidationHint && validationResult?.isValid() == true && validationResult.hasHintMessage()) {
        Text(
            text = validationResult.hintMessage(),
            style = MaterialTheme.typography.body2,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.56f),
        )
    }

    if (showValidationError && validationResult?.isInvalid() == true && validationResult.hasErrorMessage()) {
        Text(
            text = validationResult.errorMessage(),
            style = MaterialTheme.typography.body2,
            color = MaterialTheme.colors.error,
        )
    }
}
