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

import android.graphics.Rect
import android.text.InputType
import android.view.inputmethod.EditorInfo
import android.widget.EditText
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import dev.patrickgold.florisboard.FlorisImeService
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

@Composable
fun FlorisInternalTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
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
    val imeOptions: Int = when (keyboardOptions.imeAction) {
        ImeAction.Done -> EditorInfo.IME_ACTION_DONE
        ImeAction.Go -> EditorInfo.IME_ACTION_GO
        ImeAction.Next -> EditorInfo.IME_ACTION_NEXT
        ImeAction.Previous -> EditorInfo.IME_ACTION_PREVIOUS
        ImeAction.Search -> EditorInfo.IME_ACTION_SEARCH
        ImeAction.Send -> EditorInfo.IME_ACTION_SEND
        else -> EditorInfo.IME_ACTION_NONE
    }
    val inputType: Int = when (keyboardOptions.keyboardType) {
        KeyboardType.Text -> InputType.TYPE_CLASS_TEXT
        KeyboardType.Number -> InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_NORMAL
        KeyboardType.Phone -> InputType.TYPE_CLASS_PHONE
        KeyboardType.Uri -> InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI
        KeyboardType.Email -> InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
        KeyboardType.Password -> InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        KeyboardType.NumberPassword -> InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
        else -> InputType.TYPE_NULL
    }
    val inputCapitalization: Int = when (keyboardOptions.capitalization) {
        KeyboardCapitalization.Characters -> InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS
        KeyboardCapitalization.Words -> InputType.TYPE_TEXT_FLAG_CAP_WORDS
        KeyboardCapitalization.Sentences -> InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
        else -> 0
    }
    val mergedTextStyle = textStyle.copy(color = textColor, textDirection = TextDirection.Content)
    val isFocused by interactionSource.collectIsFocusedAsState()
    val isErrorState = isError || (showValidationError && validationResult?.isInvalid() == true)

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Surface(
            modifier = modifier,
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
                    AndroidView(
                        modifier = Modifier.fillMaxWidth(), // Occupy the max size in the Compose UI tree
                        factory = { context ->
                            object : EditText(context) {
                                override fun onTextChanged(
                                    text: CharSequence?,
                                    start: Int,
                                    lengthBefore: Int,
                                    lengthAfter: Int
                                ) {
                                    super.onTextChanged(text, start, lengthBefore, lengthAfter)
                                    if (text != null) {
                                        onValueChange(text.toString())
                                    }
                                }

                                override fun onFocusChanged(
                                    focused: Boolean,
                                    direction: Int,
                                    previouslyFocusedRect: Rect?
                                ) {
                                    super.onFocusChanged(focused, direction, previouslyFocusedRect)
                                    if (focused)
                                        FlorisImeService.setCurrentInputConnection(onCreateInputConnection(EditorInfo()))
                                    else
                                        FlorisImeService.setCurrentInputConnection(null)
                                }

                                override fun onEditorAction(actionCode: Int) {
                                    super.onEditorAction(actionCode)
                                    when (actionCode) {
                                        EditorInfo.IME_ACTION_DONE -> keyboardActions.onDone
                                        EditorInfo.IME_ACTION_GO -> keyboardActions.onGo
                                        EditorInfo.IME_ACTION_NEXT -> keyboardActions.onNext
                                        EditorInfo.IME_ACTION_PREVIOUS -> keyboardActions.onPrevious
                                        EditorInfo.IME_ACTION_SEARCH -> keyboardActions.onSearch
                                        EditorInfo.IME_ACTION_SEND -> keyboardActions.onSend
                                    }
                                }
                            }.apply {
                                this.isEnabled = enabled
                                this.isSingleLine = singleLine
                                this.maxLines = maxLines
                                this.setTextColor(textColor.value.toInt())

                                this.imeOptions = imeOptions
                                this.inputType =
                                    inputType or if (keyboardOptions.autoCorrect) InputType.TYPE_TEXT_FLAG_AUTO_CORRECT else 0 or inputCapitalization
                            }
                        },
                        update = { it.setText(value) }
                    )
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
