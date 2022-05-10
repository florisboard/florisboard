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

package dev.patrickgold.florisboard.app.settings.theme

import android.icu.lang.UCharacter
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Icon
import androidx.compose.material.LocalContentColor
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.flowlayout.FlowRow
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.ime.input.InputKeyEventReceiver
import dev.patrickgold.florisboard.ime.keyboard.ComputingEvaluator
import dev.patrickgold.florisboard.ime.keyboard.DefaultComputingEvaluator
import dev.patrickgold.florisboard.ime.keyboard.Key
import dev.patrickgold.florisboard.ime.keyboard.KeyData
import dev.patrickgold.florisboard.ime.keyboard.Keyboard
import dev.patrickgold.florisboard.ime.keyboard.KeyboardMode
import dev.patrickgold.florisboard.ime.keyboard.computeIconResId
import dev.patrickgold.florisboard.ime.keyboard.computeLabel
import dev.patrickgold.florisboard.ime.nlp.NATIVE_NULLPTR
import dev.patrickgold.florisboard.ime.input.InputShiftState
import dev.patrickgold.florisboard.ime.text.key.KeyCode
import dev.patrickgold.florisboard.ime.text.keyboard.TextKeyData
import dev.patrickgold.florisboard.ime.theme.FlorisImeUiSpec
import dev.patrickgold.florisboard.keyboardManager
import dev.patrickgold.florisboard.lib.android.showShortToast
import dev.patrickgold.florisboard.lib.android.stringRes
import dev.patrickgold.florisboard.lib.compose.FlorisChip
import dev.patrickgold.florisboard.lib.compose.FlorisDropdownMenu
import dev.patrickgold.florisboard.lib.compose.FlorisHyperlinkText
import dev.patrickgold.florisboard.lib.compose.FlorisIconButton
import dev.patrickgold.florisboard.lib.compose.FlorisOutlinedTextField
import dev.patrickgold.florisboard.lib.compose.florisHorizontalScroll
import dev.patrickgold.florisboard.lib.compose.stringRes
import dev.patrickgold.florisboard.lib.kotlin.curlyFormat
import dev.patrickgold.florisboard.lib.kotlin.getKeyByValue
import dev.patrickgold.florisboard.lib.snygg.SnyggLevel
import dev.patrickgold.florisboard.lib.snygg.SnyggRule
import dev.patrickgold.florisboard.lib.util.InputMethodUtils
import dev.patrickgold.jetpref.material.ui.JetPrefAlertDialog

private val TransparentTextSelectionColors = TextSelectionColors(
    handleColor = Color.Transparent,
    backgroundColor = Color.Transparent,
)
internal val SnyggEmptyRuleForAdding = SnyggRule(element = "- select -")

@Composable
internal fun EditRuleDialog(
    initRule: SnyggRule,
    level: SnyggLevel,
    onConfirmRule: (oldRule: SnyggRule, newRule: SnyggRule) -> Boolean,
    onDeleteRule: (rule: SnyggRule) -> Unit,
    onDismiss: () -> Unit,
) {
    val isAddRuleDialog = initRule == SnyggEmptyRuleForAdding
    var showSelectAsError by rememberSaveable { mutableStateOf(false) }
    var showAlreadyExistsError by rememberSaveable { mutableStateOf(false) }

    val possibleElementNames = remember {
        listOf(SnyggEmptyRuleForAdding.element) + FlorisImeUiSpec.elements.keys
    }
    val possibleElementLabels = possibleElementNames.map { translateElementName(it, level) ?: it }
    var elementsExpanded by remember { mutableStateOf(false) }
    var elementsSelectedIndex by rememberSaveable {
        val index = possibleElementNames.indexOf(initRule.element).coerceIn(possibleElementNames.indices)
        mutableStateOf(index)
    }

    val codes = rememberSaveable(saver = IntListSaver) { initRule.codes.toMutableStateList() }
    var editCodeDialogValue by rememberSaveable { mutableStateOf<Int?>(null) }
    val groups = rememberSaveable(saver = IntListSaver) { initRule.groups.toMutableStateList() }
    var shiftStateUnshifted by rememberSaveable {
        mutableStateOf(initRule.shiftStates.contains(InputShiftState.UNSHIFTED.value))
    }
    var shiftStateShiftedManual by rememberSaveable {
        mutableStateOf(initRule.shiftStates.contains(InputShiftState.SHIFTED_MANUAL.value))
    }
    var shiftStateShiftedAutomatic by rememberSaveable {
        mutableStateOf(initRule.shiftStates.contains(InputShiftState.SHIFTED_AUTOMATIC.value))
    }
    var shiftStateCapsLock by rememberSaveable {
        mutableStateOf(initRule.shiftStates.contains(InputShiftState.CAPS_LOCK.value))
    }
    var pressedSelector by rememberSaveable { mutableStateOf(initRule.pressedSelector) }
    var focusSelector by rememberSaveable { mutableStateOf(initRule.focusSelector) }
    var disabledSelector by rememberSaveable { mutableStateOf(initRule.disabledSelector) }

    JetPrefAlertDialog(
        title = stringRes(if (isAddRuleDialog) {
            R.string.settings__theme_editor__add_rule
        } else {
            R.string.settings__theme_editor__edit_rule
        }),
        confirmLabel = stringRes(if (isAddRuleDialog) {
            R.string.action__add
        } else {
            R.string.action__apply
        }),
        onConfirm = {
            if (isAddRuleDialog && elementsSelectedIndex == 0) {
                showSelectAsError = true
            } else {
                val newRule = SnyggRule(
                    element = possibleElementNames[elementsSelectedIndex],
                    codes = codes.toList(),
                    groups = groups.toList(),
                    shiftStates = buildList {
                        if (shiftStateUnshifted) { add(InputShiftState.UNSHIFTED.value) }
                        if (shiftStateShiftedManual) { add(InputShiftState.SHIFTED_MANUAL.value) }
                        if (shiftStateShiftedAutomatic) { add(InputShiftState.SHIFTED_AUTOMATIC.value) }
                        if (shiftStateCapsLock) { add(InputShiftState.CAPS_LOCK.value) }
                    },
                    pressedSelector = pressedSelector,
                    focusSelector = focusSelector,
                    disabledSelector = disabledSelector,
                )
                if (!onConfirmRule(initRule, newRule)) {
                    showAlreadyExistsError = true
                }
            }
        },
        dismissLabel = stringRes(R.string.action__cancel),
        onDismiss = onDismiss,
        neutralLabel = if (!isAddRuleDialog) {
            stringRes(R.string.action__delete)
        } else {
            null
        },
        neutralColors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colors.error),
        onNeutral = { onDeleteRule(initRule) },
    ) {
        Column {
            AnimatedVisibility(visible = showAlreadyExistsError) {
                Text(
                    modifier = Modifier.padding(bottom = 16.dp),
                    text = stringRes(R.string.settings__theme_editor__rule_already_exists),
                    color = MaterialTheme.colors.error,
                )
            }

            DialogProperty(text = stringRes(R.string.settings__theme_editor__rule_element)) {
                FlorisDropdownMenu(
                    items = possibleElementLabels,
                    expanded = elementsExpanded,
                    enabled = isAddRuleDialog,
                    selectedIndex = elementsSelectedIndex,
                    isError = showSelectAsError && elementsSelectedIndex == 0,
                    onSelectItem = { elementsSelectedIndex = it },
                    onExpandRequest = { elementsExpanded = true },
                    onDismissRequest = { elementsExpanded = false },
                )
            }

            DialogProperty(text = stringRes(R.string.settings__theme_editor__rule_selectors)) {
                Row(modifier = Modifier.florisHorizontalScroll()) {
                    FlorisChip(
                        onClick = { pressedSelector = !pressedSelector },
                        modifier = Modifier.padding(end = 4.dp),
                        text = when (level) {
                            SnyggLevel.DEVELOPER -> SnyggRule.PRESSED_SELECTOR
                            else -> stringRes(R.string.snygg__rule_selector__pressed)
                        },
                        color = if (pressedSelector) MaterialTheme.colors.primaryVariant else Color.Unspecified,
                    )
                    FlorisChip(
                        onClick = { focusSelector = !focusSelector },
                        modifier = Modifier.padding( end = 4.dp),
                        text = when (level) {
                            SnyggLevel.DEVELOPER -> SnyggRule.FOCUS_SELECTOR
                            else -> stringRes(R.string.snygg__rule_selector__focus)
                        },
                        color = if (focusSelector) MaterialTheme.colors.primaryVariant else Color.Unspecified,
                    )
                    FlorisChip(
                        onClick = { disabledSelector = !disabledSelector },
                        text = when (level) {
                            SnyggLevel.DEVELOPER -> SnyggRule.DISABLED_SELECTOR
                            else -> stringRes(R.string.snygg__rule_selector__disabled)
                        },
                        color = if (disabledSelector) MaterialTheme.colors.primaryVariant else Color.Unspecified,
                    )
                }
            }

            DialogProperty(
                text = stringRes(R.string.settings__theme_editor__rule_codes),
                trailingIconTitle = {
                    FlorisIconButton(
                        onClick = { editCodeDialogValue = NATIVE_NULLPTR },
                        modifier = Modifier.offset(x = 12.dp),
                        icon = painterResource(R.drawable.ic_add),
                    )
                },
            ) {
                Text(
                    modifier = Modifier.padding(vertical = 4.dp),
                    text = stringRes(if (codes.isEmpty()) {
                        R.string.settings__theme_editor__no_codes_defined
                    } else {
                        R.string.settings__theme_editor__codes_defined
                    }),
                    fontStyle = FontStyle.Italic,
                )
                FlowRow {
                    for (code in codes) {
                        FlorisChip(
                            onClick = { editCodeDialogValue = code },
                            text = code.toString(),
                            shape = MaterialTheme.shapes.medium,
                        )
                    }
                }
            }

            DialogProperty(text = stringRes(R.string.settings__theme_editor__rule_shift_states)) {
                FlowRow(mainAxisSpacing = 4.dp) {
                    FlorisChip(
                        onClick = { shiftStateUnshifted = !shiftStateUnshifted },
                        text = when (level) {
                            SnyggLevel.DEVELOPER -> remember {
                                SnyggRule.Placeholders.getKeyByValue(InputShiftState.UNSHIFTED.value)
                            }
                            else -> stringRes(R.string.enum__input_shift_state__unshifted)
                        },
                        color = if (shiftStateUnshifted) MaterialTheme.colors.primaryVariant else Color.Unspecified,
                    )
                    FlorisChip(
                        onClick = { shiftStateShiftedManual = !shiftStateShiftedManual },
                        text = when (level) {
                            SnyggLevel.DEVELOPER -> remember {
                                SnyggRule.Placeholders.getKeyByValue(InputShiftState.SHIFTED_MANUAL.value)
                            }
                            else -> stringRes(R.string.enum__input_shift_state__shifted_manual)
                        },
                        color = if (shiftStateShiftedManual) MaterialTheme.colors.primaryVariant else Color.Unspecified,
                    )
                    FlorisChip(
                        onClick = { shiftStateShiftedAutomatic = !shiftStateShiftedAutomatic },
                        text = when (level) {
                            SnyggLevel.DEVELOPER -> remember {
                                SnyggRule.Placeholders.getKeyByValue(InputShiftState.SHIFTED_AUTOMATIC.value)
                            }
                            else -> stringRes(R.string.enum__input_shift_state__shifted_automatic)
                        },
                        color = if (shiftStateShiftedAutomatic) MaterialTheme.colors.primaryVariant else Color.Unspecified,
                    )
                    FlorisChip(
                        onClick = { shiftStateCapsLock = !shiftStateCapsLock },
                        text = when (level) {
                            SnyggLevel.DEVELOPER -> remember {
                                SnyggRule.Placeholders.getKeyByValue(InputShiftState.CAPS_LOCK.value)
                            }
                            else -> stringRes(R.string.enum__input_shift_state__caps_lock)
                        },
                        color = if (shiftStateCapsLock) MaterialTheme.colors.primaryVariant else Color.Unspecified,
                    )
                }
            }
        }
    }

    val initCodeValue = editCodeDialogValue
    if (initCodeValue != null) {
        EditCodeValueDialog(
            codeValue = initCodeValue,
            checkExisting = { codes.contains(it) },
            onAdd = { codes.add(it) },
            onDelete = { codes.remove(it) },
            onDismiss = { editCodeDialogValue = null },
        )
    }
}

@Composable
private fun EditCodeValueDialog(
    codeValue: Int,
    checkExisting: (Int) -> Boolean,
    onAdd: (Int) -> Unit,
    onDelete: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val keyboardManager by context.keyboardManager()

    var inputCodeString by rememberSaveable(codeValue) {
        val str = if (codeValue == 0) "" else codeValue.toString()
        mutableStateOf(str)
    }
    val textKeyData = remember(inputCodeString) {
        inputCodeString.toIntOrNull()?.let { code ->
            TextKeyData.getCodeInfoAsTextKeyData(code)
        }
    }
    var showKeyCodesHelp by rememberSaveable(codeValue) { mutableStateOf(false) }
    var showError by rememberSaveable(codeValue) { mutableStateOf(false) }
    var errorId by rememberSaveable(codeValue) { mutableStateOf(NATIVE_NULLPTR) }

    val focusRequester = remember { FocusRequester() }
    val isFlorisBoardEnabled by InputMethodUtils.observeIsFlorisboardEnabled(foregroundOnly = true)
    val isFlorisBoardSelected by InputMethodUtils.observeIsFlorisboardSelected(foregroundOnly = true)

    var isRecordingKey by remember { mutableStateOf(false) }
    var lastRecordingToast by remember { mutableStateOf<Toast?>(null) }
    val recordingKeyColor = if (isRecordingKey) {
        rememberInfiniteTransition().animateColor(
            initialValue = LocalContentColor.current,
            targetValue = MaterialTheme.colors.error,
            animationSpec = infiniteRepeatable(
                tween(750),
                repeatMode = RepeatMode.Reverse,
            ),
        ).value
    } else {
        LocalContentColor.current
    }

    fun requestStartRecording() {
        if (isRecordingKey) {
            isRecordingKey = false
            return
        }
        if (!isFlorisBoardEnabled || !isFlorisBoardSelected) {
            lastRecordingToast?.cancel()
            lastRecordingToast = context.showShortToast(
                R.string.settings__theme_editor__code_recording_requires_default_ime_floris,
                "app_name" to context.stringRes(R.string.floris_app_name),
            )
            InputMethodUtils.showImePicker(context)
            return
        }
        showError = false
        isRecordingKey = true
    }

    if (isRecordingKey) {
        DisposableEffect(Unit) {
            val receiver = object : InputKeyEventReceiver {
                override fun onInputKeyDown(data: KeyData) = Unit
                override fun onInputKeyUp(data: KeyData) {
                    inputCodeString = data.code.toString()
                    isRecordingKey = false
                }
                override fun onInputKeyRepeat(data: KeyData) = Unit
                override fun onInputKeyCancel(data: KeyData) = Unit
            }
            val defaultReceiver = keyboardManager.inputEventDispatcher.keyEventReceiver
            keyboardManager.inputEventDispatcher.keyEventReceiver = receiver
            lastRecordingToast?.cancel()
            lastRecordingToast = context.showShortToast(R.string.settings__theme_editor__code_recording_started)
            focusRequester.requestFocus()
            onDispose {
                keyboardManager.inputEventDispatcher.keyEventReceiver = defaultReceiver
                lastRecordingToast?.cancel()
                lastRecordingToast = context.showShortToast(R.string.settings__theme_editor__code_recording_stopped)
            }
        }
    }

    JetPrefAlertDialog(
        title = stringRes(if (codeValue == NATIVE_NULLPTR) {
            R.string.settings__theme_editor__add_code
        } else {
            R.string.settings__theme_editor__edit_code
        }),
        confirmLabel = stringRes(if (codeValue == NATIVE_NULLPTR) {
            R.string.action__add
        } else {
            R.string.action__apply
        }),
        onConfirm = {
            val code = inputCodeString.trim().toIntOrNull(radix = 10)
            when {
                code == null || (code !in KeyCode.Spec.CHARACTERS && code !in KeyCode.Spec.INTERNAL) -> {
                    errorId = R.string.settings__theme_editor__code_invalid
                    showError = true
                }
                code == codeValue -> {
                    onDismiss()
                }
                checkExisting(code) -> {
                    errorId = R.string.settings__theme_editor__code_already_exists
                    showError = true
                }
                else -> {
                    if (codeValue != NATIVE_NULLPTR) {
                        onDelete(codeValue)
                    }
                    onAdd(code)
                    onDismiss()
                }
            }
        },
        dismissLabel = stringRes(R.string.action__cancel),
        onDismiss = onDismiss,
        neutralLabel = if (codeValue != NATIVE_NULLPTR) {
            stringRes(R.string.action__delete)
        } else {
            null
        },
        neutralColors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colors.error),
        onNeutral = {
            onDelete(codeValue)
            onDismiss()
        },
        trailingIconTitle = {
            FlorisIconButton(
                onClick = { showKeyCodesHelp = !showKeyCodesHelp },
                modifier = Modifier.offset(x = 12.dp),
                icon = painterResource(R.drawable.ic_help_outline),
            )
        },
    ) {
        Column {
            AnimatedVisibility(visible = showKeyCodesHelp) {
                Column(modifier = Modifier.padding(bottom = 16.dp)) {
                    Text(text = stringRes(R.string.settings__theme_editor__code_recording_help_text))
                    Text(text = stringRes(R.string.settings__theme_editor__code_help_text))
                    FlorisHyperlinkText(
                        text = "Characters (unicode-table.com)",
                        url = stringRes(R.string.florisboard__character_key_codes_url),
                    )
                    FlorisHyperlinkText(
                        text = "Internal (github.com)",
                        url = stringRes(R.string.florisboard__internal_key_codes_url),
                    )
                }
            }
            TextKeyDataPreviewBox(
                modifier = Modifier.padding(bottom = 8.dp),
                textKeyData = textKeyData,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val textSelectionColors = if (isRecordingKey) {
                    TransparentTextSelectionColors
                } else {
                    LocalTextSelectionColors.current
                }
                CompositionLocalProvider(LocalTextSelectionColors provides textSelectionColors) {
                    FlorisOutlinedTextField(
                        modifier = Modifier
                            .focusRequester(focusRequester)
                            .weight(1f),
                        value = inputCodeString,
                        onValueChange = { v ->
                            inputCodeString = v
                            showError = false
                        },
                        placeholder = when {
                            isRecordingKey -> {
                                stringRes(R.string.settings__theme_editor__code_recording_placeholder)
                            }
                            inputCodeString.isEmpty() -> {
                                stringRes(R.string.settings__theme_editor__code_placeholder)
                            }
                            else -> {
                                null
                            }
                        },
                        isError = showError,
                        singleLine = true,
                        colors = if (isRecordingKey) {
                            TextFieldDefaults.outlinedTextFieldColors(
                                textColor = Color.Transparent,
                                cursorColor = Color.Transparent,
                            )
                        } else {
                            TextFieldDefaults.outlinedTextFieldColors()
                        },
                    )
                }
                FlorisIconButton(
                    onClick = { requestStartRecording() },
                    icon = painterResource(R.drawable.ic_pageview),
                    iconColor = recordingKeyColor,
                )
            }
            AnimatedVisibility(visible = showError) {
                Text(
                    modifier = Modifier.padding(top = 4.dp),
                    text = stringRes(errorId).curlyFormat(
                        "c_min" to KeyCode.Spec.CHARACTERS_MIN,
                        "c_max" to KeyCode.Spec.CHARACTERS_MAX,
                        "i_min" to KeyCode.Spec.INTERNAL_MIN,
                        "i_max" to KeyCode.Spec.INTERNAL_MAX,
                    ),
                    color = MaterialTheme.colors.error,
                )
            }
        }
    }
}

@Composable
private fun TextKeyDataPreviewBox(
    textKeyData: TextKeyData?,
    modifier: Modifier = Modifier,
) {
    val data = textKeyData ?: TextKeyData.UNSPECIFIED

    val context = LocalContext.current
    val evaluator = remember(context) {
        object : ComputingEvaluator by DefaultComputingEvaluator {
            val keyboard = object : Keyboard() {
                override val mode = KeyboardMode.NUMERIC_ADVANCED
                override fun getKeyForPos(pointerX: Float, pointerY: Float) = error("not implemented")
                override fun keys() = error("not implemented")
                override fun layout(keyboardWidth: Float, keyboardHeight: Float, desiredKey: Key,
                                    extendTouchBoundariesDownwards: Boolean) = error("not implemented")
            }
            override fun context() = context
            override fun keyboard() = keyboard
        }
    }

    val label = remember(data) { evaluator.computeLabel(data) }
    val iconId = remember(data) { evaluator.computeIconResId(data) }
    val displayName = remember(data) {
        if (data.code > 0) {
            UCharacter.getName(data.code) ?: UCharacter.getExtendedName(data.code)
        } else {
            data.label
        }
    }

    Row(modifier = modifier) {
        Box(
            modifier = Modifier
                .padding(end = 16.dp)
                .background(
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.12f),
                    shape = MaterialTheme.shapes.medium,
                )
                .height(36.dp)
                .widthIn(min = 36.dp)
                .align(Alignment.CenterVertically),
            contentAlignment = Alignment.Center,
        ) {
            if (label != null) {
                Text(
                    text = label,
                    fontSize = 16.sp,
                    maxLines = 1,
                    softWrap = false,
                )
            }
            if (iconId != null) {
                Icon(
                    modifier = Modifier.requiredSize(24.dp),
                    painter = painterResource(iconId),
                    contentDescription = null,
                )
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(text = displayName)
            Text(text = data.type.toString())
        }
    }
}
