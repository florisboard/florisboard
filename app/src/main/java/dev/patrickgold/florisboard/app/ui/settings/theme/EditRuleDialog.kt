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
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import com.google.accompanist.flowlayout.FlowRow
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.app.res.stringRes
import dev.patrickgold.florisboard.app.ui.components.FlorisChip
import dev.patrickgold.florisboard.app.ui.components.FlorisDropdownMenu
import dev.patrickgold.florisboard.app.ui.components.FlorisHyperlinkText
import dev.patrickgold.florisboard.app.ui.components.FlorisIconButton
import dev.patrickgold.florisboard.app.ui.components.FlorisOutlinedTextField
import dev.patrickgold.florisboard.app.ui.components.florisHorizontalScroll
import dev.patrickgold.florisboard.common.kotlin.curlyFormat
import dev.patrickgold.florisboard.ime.nlp.NATIVE_NULLPTR
import dev.patrickgold.florisboard.ime.text.key.InputMode
import dev.patrickgold.florisboard.ime.text.key.KeyCode
import dev.patrickgold.florisboard.ime.theme.FlorisImeUiSpec
import dev.patrickgold.florisboard.snygg.SnyggLevel
import dev.patrickgold.florisboard.snygg.SnyggRule
import dev.patrickgold.jetpref.material.ui.JetPrefAlertDialog

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
    var modeNormal by rememberSaveable { mutableStateOf(initRule.modes.contains(InputMode.NORMAL.value)) }
    var modeShiftLock by rememberSaveable { mutableStateOf(initRule.modes.contains(InputMode.SHIFT_LOCK.value)) }
    var modeCapsLock by rememberSaveable { mutableStateOf(initRule.modes.contains(InputMode.CAPS_LOCK.value)) }
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
                    modes = buildList {
                        if (modeNormal) { add(InputMode.NORMAL.value) }
                        if (modeShiftLock) { add(InputMode.SHIFT_LOCK.value) }
                        if (modeCapsLock) { add(InputMode.CAPS_LOCK.value) }
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
                if (codes.isEmpty()) {
                    Text(
                        modifier = Modifier.padding(vertical = 4.dp),
                        text = stringRes(R.string.settings__theme_editor__no_codes_defined),
                        fontStyle = FontStyle.Italic,
                    )
                }
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

            DialogProperty(text = stringRes(R.string.settings__theme_editor__rule_modes)) {
                Row(modifier = Modifier.florisHorizontalScroll()) {
                    FlorisChip(
                        onClick = { modeNormal = !modeNormal },
                        modifier = Modifier.padding(end = 4.dp),
                        text = when (level) {
                            SnyggLevel.DEVELOPER -> remember { "m:${InputMode.NORMAL.toString().lowercase()}" }
                            else -> stringRes(R.string.enum__input_mode__normal)
                        },
                        color = if (modeNormal) MaterialTheme.colors.primaryVariant else Color.Unspecified,
                    )
                    FlorisChip(
                        onClick = { modeShiftLock = !modeShiftLock },
                        modifier = Modifier.padding(end = 4.dp),
                        text = when (level) {
                            SnyggLevel.DEVELOPER -> remember { "m:${InputMode.SHIFT_LOCK.toString().lowercase()}" }
                            else -> stringRes(R.string.enum__input_mode__shift_lock)
                        },
                        color = if (modeShiftLock) MaterialTheme.colors.primaryVariant else Color.Unspecified,
                    )
                    FlorisChip(
                        onClick = { modeCapsLock = !modeCapsLock },
                        text = when (level) {
                            SnyggLevel.DEVELOPER -> remember { "m:${InputMode.CAPS_LOCK.toString().lowercase()}" }
                            else -> stringRes(R.string.enum__input_mode__caps_lock)
                        },
                        color = if (modeCapsLock) MaterialTheme.colors.primaryVariant else Color.Unspecified,
                    )
                }
            }
        }
    }

    val initCodeValue = editCodeDialogValue
    if (initCodeValue != null) {
        var inputCodeString by rememberSaveable(initCodeValue) { mutableStateOf(initCodeValue.toString()) }
        var showKeyCodesHelp by rememberSaveable(initCodeValue) { mutableStateOf(false) }
        var showError by rememberSaveable(initCodeValue) { mutableStateOf(false) }
        var errorId by rememberSaveable(initCodeValue) { mutableStateOf(NATIVE_NULLPTR) }
        JetPrefAlertDialog(
            title = stringRes(if (initCodeValue == NATIVE_NULLPTR) {
                R.string.settings__theme_editor__add_code
            } else {
                R.string.settings__theme_editor__edit_code
            }),
            confirmLabel = stringRes(if (initCodeValue == NATIVE_NULLPTR) {
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
                    code == initCodeValue -> {
                        editCodeDialogValue = null
                    }
                    codes.contains(code) -> {
                        errorId = R.string.settings__theme_editor__code_already_exists
                        showError = true
                    }
                    else -> {
                        if (initCodeValue != NATIVE_NULLPTR) {
                            codes.remove(initCodeValue)
                        }
                        codes.add(code)
                        editCodeDialogValue = null
                    }
                }
            },
            dismissLabel = stringRes(R.string.action__cancel),
            onDismiss = {
                editCodeDialogValue = null
            },
            neutralLabel = if (initCodeValue != NATIVE_NULLPTR) {
                stringRes(R.string.action__delete)
            } else {
                null
            },
            neutralColors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colors.error),
            onNeutral = {
                codes.remove(initCodeValue)
                editCodeDialogValue = null
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
                FlorisOutlinedTextField(
                    value = inputCodeString,
                    onValueChange = { v ->
                        inputCodeString = v
                        showError = false
                    },
                    isError = showError,
                    singleLine = true,
                )
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
}
