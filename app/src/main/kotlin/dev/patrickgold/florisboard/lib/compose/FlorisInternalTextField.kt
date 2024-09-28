package dev.patrickgold.florisboard.lib.compose

import android.graphics.Rect
import android.text.InputType
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import dev.patrickgold.florisboard.FlorisImeService
import dev.patrickgold.florisboard.lib.ValidationResult

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
    colors: TextFieldColors = TextFieldDefaults.colors(),
) {
    val textColor = textStyle.color.takeOrElse {
        if (enabled) {
            colors.focusedTextColor
        } else {
            colors.disabledTextColor
        }
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
            color = if (enabled) { colors.focusedContainerColor} else { colors.disabledContainerColor},
            border = if (isErrorState && enabled) {
                BorderStroke(ButtonDefaults.outlinedButtonBorder.width, MaterialTheme.colorScheme.error)
            } else if (isFocused) {
                BorderStroke(ButtonDefaults.outlinedButtonBorder.width, MaterialTheme.colorScheme.primary)
            } else {
                ButtonDefaults.outlinedButtonBorder
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
                    val localTextStyle = LocalTextStyle.current
                    Row {
                        var localEditTextReference: EditText? by remember { mutableStateOf(null) }
                        AndroidView(
                            modifier = Modifier.fillMaxWidth(0.9f), // Occupy the max size in the Compose UI tree
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

                                    public override fun onFocusChanged(
                                        focused: Boolean,
                                        direction: Int,
                                        previouslyFocusedRect: Rect?
                                    ) {
                                        super.onFocusChanged(focused, direction, previouslyFocusedRect)
                                        if (focused)
                                            FlorisImeService.setCurrentInputConnection(
                                                onCreateInputConnection(
                                                    EditorInfo()
                                                )
                                            )
                                        else
                                            FlorisImeService.setCurrentInputConnection(null)
                                    }

                                    override fun onEditorAction(actionCode: Int) {
                                        super.onEditorAction(actionCode)
                                        clearFocus()
                                    }
                                }.apply {
                                    this.isEnabled = enabled
                                    this.isSingleLine = singleLine
                                    this.maxLines = maxLines
                                    this.setTextColor(localTextStyle.color.toArgb())
                                    localEditTextReference = this

                                    this.imeOptions = imeOptions
                                    this.inputType =
                                        inputType or if (keyboardOptions.autoCorrect) InputType.TYPE_TEXT_FLAG_AUTO_CORRECT else 0 or inputCapitalization
                                }
                            },
                            update = {}
                        )
                        IconButton(onClick = {
                            localEditTextReference?.clearFocus()
                        }) {
                            Icon(Icons.Default.Check, null)
                        }
                    }
                }
                if (!placeholder.isNullOrBlank()) {
                    Text(
                        text = placeholder,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.56f),
                    )
                }
            }
        }
    }

    /*if (showValidationHint && validationResult?.isValid() == true && validationResult.hasHintMessage()) {
        Text(
            text = validationResult.hintMessage(),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.56f),
        )
    }

    if (showValidationError && validationResult?.isInvalid() == true && validationResult.hasErrorMessage()) {
        Text(
            text = validationResult.errorMessage(),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
        )
    }*/
}
