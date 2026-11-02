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

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Pageview
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.app.settings.theme.DialogProperty
import dev.patrickgold.florisboard.ime.keyboard3.LocalImeController
import dev.patrickgold.florisboard.ime.keyboard3.interaction.InteractionController
import dev.patrickgold.florisboard.ime.keyboard3.interaction.LocalInteractionController
import dev.patrickgold.florisboard.ime.keyboard3.interaction.showShortToast
import dev.patrickgold.florisboard.ime.keyboard3.ui.asAttrValue
import dev.patrickgold.florisboard.ime.text.key.KeyCode
import dev.patrickgold.florisboard.ime.theme.ImeUiAttr
import dev.patrickgold.florisboard.lib.NATIVE_NULLPTR
import dev.patrickgold.florisboard.lib.util.InputMethodUtils
import dev.patrickgold.jetpref.material.ui.JetPrefAlertDialog
import dev.patrickgold.jetpref.material.ui.JetPrefTextField
import dev.patrickgold.jetpref.material.ui.JetPrefTextFieldDefaults
import kotlinx.coroutines.launch
import org.florisboard.lib.compose.FlorisChip
import org.florisboard.lib.compose.FlorisIconButton
import org.florisboard.lib.compose.stringRes
import org.florisboard.lib.snygg.SnyggAttributes
import org.k3lp.lib.text.K3StringOrDescriptor

@Composable
fun EditOutputAttrBox(
    attrSpec: ImeUiAttr<K3StringOrDescriptor>,
    attributes: SnyggAttributes,
    updateAttributes: (SnyggAttributes) -> Unit,
) {
    val values = remember(attrSpec.name, attributes) {
        (attributes[attrSpec.name] ?: emptyList()).toMutableStateList()
    }
    var valueToEdit by rememberSaveable { mutableStateOf<String?>(null) }
    val initValueToEdit = valueToEdit
    if (initValueToEdit != null) {
        EditOutputValueDialog(
            initValue = initValueToEdit,
            checkExisting = { values.contains(it) },
            onAdd = {
                updateAttributes(attributes.including(attrSpec.name to it))
            },
            onDelete = {
                updateAttributes(attributes.excluding(attrSpec.name to it))
            },
            onDismiss = { valueToEdit = null },
        )
    }
    DialogProperty(
        text = "Output",
        trailingIconTitle = {
            FlorisIconButton(
                onClick = { valueToEdit = KeyCode.UNSPECIFIED.toString() },
                modifier = Modifier.offset(x = 12.dp),
                icon = Icons.Default.Add,
            )
        },
    ) {
        if (values.isEmpty()) {
            Text(
                text = stringRes(R.string.settings__theme_editor__no_values_defined),
                fontStyle = FontStyle.Italic,
            )
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            for (value in values) {
                FlorisChip(
                    onClick = { valueToEdit = value },
                    text = value,
                    selected = valueToEdit == value,
                    shape = MaterialTheme.shapes.medium,
                )
            }
        }
    }
}

@Composable
private fun EditOutputValueDialog(
    initValue: String,
    checkExisting: (String) -> Boolean,
    onAdd: (String) -> Unit,
    onDelete: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val imeController = LocalImeController.current
    val interactionController = LocalInteractionController.current
    val scope = rememberCoroutineScope()

    var outputValue by rememberSaveable(initValue) {
        val str = if (initValue == KeyCode.UNSPECIFIED.toString()) "" else initValue
        mutableStateOf(str)
    }
    var showKeyCodesHelp by rememberSaveable(initValue) { mutableStateOf(false) }
    var showError by rememberSaveable(initValue) { mutableStateOf(false) }
    var errorId by rememberSaveable(initValue) { mutableIntStateOf(NATIVE_NULLPTR.toInt()) }

    val focusRequester = remember { FocusRequester() }
    val isFlorisBoardEnabled by InputMethodUtils.observeIsFlorisboardEnabled()
    val isFlorisBoardSelected by InputMethodUtils.observeIsFlorisboardSelected()

    var isRecordingKey by remember { mutableStateOf(false) }
    var lastRecordingToast by remember { mutableStateOf<InteractionController.ToastHandle?>(null) }
    val recordingKeyColor = if (isRecordingKey) {
        rememberInfiniteTransition().animateColor(
            initialValue = LocalContentColor.current,
            targetValue = MaterialTheme.colorScheme.error,
            animationSpec = infiniteRepeatable(
                tween(750),
                repeatMode = RepeatMode.Reverse,
            ),
        ).value
    } else {
        LocalContentColor.current
    }

    val startRecordingMsg = stringRes(
        R.string.settings__theme_editor__output_recording_requires_default_ime_floris,
        "app_name" to stringRes(R.string.floris_app_name),
    )
    fun requestStartRecording() {
        if (isRecordingKey) {
            isRecordingKey = false
            return
        }
        if (!isFlorisBoardEnabled || !isFlorisBoardSelected) {
            scope.launch {
                lastRecordingToast?.hide()
                lastRecordingToast = interactionController.showShortToast(startRecordingMsg)
            }
            InputMethodUtils.showImePicker(context)
            return
        }
        showError = false
        isRecordingKey = true
    }

    if (isRecordingKey) {
        DisposableEffect(Unit) {
            imeController.setEmitInterceptor { value ->
                outputValue = value.asAttrValue()
                isRecordingKey = false
            }
            focusRequester.requestFocus()
            onDispose {
                imeController.setEmitInterceptor(null)
            }
        }
    }

    JetPrefAlertDialog(
        title = stringRes(
            if (initValue == KeyCode.UNSPECIFIED.toString()) {
                R.string.settings__theme_editor__add_output
            } else {
                R.string.settings__theme_editor__edit_output
            }
        ),
        confirmLabel = stringRes(
            if (initValue == KeyCode.UNSPECIFIED.toString()) {
                R.string.action__add
            } else {
                R.string.action__apply
            }
        ),
        onConfirm = {
            when {
                checkExisting(outputValue) -> {
                    errorId = R.string.settings__theme_editor__output_already_exists
                    showError = true
                }
                else -> {
                    if (initValue != KeyCode.UNSPECIFIED.toString()) {
                        onDelete(initValue)
                    }
                    onAdd(outputValue)
                    onDismiss()
                }
            }
        },
        dismissLabel = stringRes(R.string.action__cancel),
        onDismiss = onDismiss,
        neutralLabel = if (initValue != KeyCode.UNSPECIFIED.toString()) {
            stringRes(R.string.action__delete)
        } else {
            null
        },
        neutralColors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
        onNeutral = {
            onDelete(initValue)
            onDismiss()
        },
        trailingIconTitle = {
            FlorisIconButton(
                onClick = { showKeyCodesHelp = !showKeyCodesHelp },
                modifier = Modifier.offset(x = 12.dp),
                icon = Icons.AutoMirrored.Filled.HelpOutline,
            )
        },
    ) {
        Column {
            AnimatedVisibility(visible = showKeyCodesHelp) {
                Text(
                    modifier = Modifier.padding(bottom = 16.dp),
                    text = stringRes(R.string.settings__theme_editor__output_recording_help_text),
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val spaceVisualTransformation = VisualTransformation { text ->
                    val transformedText = text.replace(" ".toRegex(), "·")
                    TransformedText(AnnotatedString(transformedText), OffsetMapping.Identity)
                }
                JetPrefTextField(
                    modifier = Modifier
                        .focusRequester(focusRequester)
                        .weight(1f),
                    value = if (isRecordingKey) "" else outputValue,
                    onValueChange = { v ->
                        if (!isRecordingKey) {
                            outputValue = v
                            showError = false
                        }
                    },
                    placeholderText = when {
                        isRecordingKey -> {
                            stringRes(R.string.settings__theme_editor__output_recording_placeholder)
                        }
                        outputValue.isEmpty() -> {
                            stringRes(R.string.settings__theme_editor__output_placeholder)
                        }
                        else -> {
                            null
                        }
                    },
                    isError = showError,
                    singleLine = true,
                    appearance = JetPrefTextFieldDefaults.filled(
                        colors = if (isRecordingKey) {
                            TextFieldDefaults.colors(
                                focusedTextColor = Color.Transparent,
                                cursorColor = Color.Transparent,
                            )
                        } else {
                            TextFieldDefaults.colors()
                        }
                    ),
                    visualTransformation = spaceVisualTransformation,
                    trailingIcon = {
                        FlorisIconButton(
                            onClick = { requestStartRecording() },
                            icon = Icons.Default.Pageview,
                            iconColor = recordingKeyColor,
                        )
                    }
                )
            }
            AnimatedVisibility(visible = showError) {
                Text(
                    modifier = Modifier.padding(top = 4.dp),
                    text = stringRes(errorId),
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}
