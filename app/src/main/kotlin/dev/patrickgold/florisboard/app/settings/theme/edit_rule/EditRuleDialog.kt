/*
 * Copyright (C) 2022-2026 The FlorisBoard Contributors
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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.app.settings.theme.DialogProperty
import dev.patrickgold.florisboard.app.settings.theme.SnyggLevel
import dev.patrickgold.florisboard.app.settings.theme.translateElementName
import dev.patrickgold.florisboard.ime.theme.FlorisImeUi
import dev.patrickgold.florisboard.ime.theme.ImeUiAttrs
import dev.patrickgold.jetpref.material.ui.JetPrefAlertDialog
import dev.patrickgold.jetpref.material.ui.JetPrefDropdown
import dev.patrickgold.jetpref.material.ui.JetPrefTextField
import org.florisboard.lib.compose.FlorisChip
import org.florisboard.lib.compose.florisHorizontalScroll
import org.florisboard.lib.compose.stringRes
import org.florisboard.lib.snygg.SnyggAnnotationRule
import org.florisboard.lib.snygg.SnyggAttributes
import org.florisboard.lib.snygg.SnyggElementRule
import org.florisboard.lib.snygg.SnyggRule
import org.florisboard.lib.snygg.SnyggSelector
import org.florisboard.lib.snygg.ui.NonNullSaver

val SnyggEmptyRuleForAdding = SnyggElementRule(elementName = "--select--")

@Composable
fun EditRuleDialog(
    initRule: SnyggRule,
    level: SnyggLevel,
    onConfirmRule: (oldRule: SnyggRule, newRule: SnyggRule) -> Boolean,
    onDeleteRule: (rule: SnyggRule) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val isAddRuleDialog = initRule == SnyggEmptyRuleForAdding
    var showSelectAsError by rememberSaveable { mutableStateOf(false) }
    var showAlreadyExistsError by rememberSaveable { mutableStateOf(false) }

    val possibleRuleTemplates = remember {
        buildList {
            add(SnyggEmptyRuleForAdding)
            add(SnyggAnnotationRule.Font(fontName = ""))
            FlorisImeUi.elementNames.forEach { name ->
                add(SnyggElementRule(name))
            }
        }
    }
    val possibleRuleLabels = possibleRuleTemplates.map { rule ->
        val elementName = when (rule) {
            is SnyggElementRule -> rule.elementName
            else -> rule.decl().name
        }
        context.translateElementName(elementName, level) ?: rule
    }
    var elementsSelectedIndex by rememberSaveable {
        val index = possibleRuleTemplates
            .indexOfFirst {  rule ->
                val elementName = when (rule) {
                    is SnyggElementRule -> rule.elementName
                    else -> rule.decl().name
                }
                val initElementName = when (initRule) {
                    is SnyggElementRule -> initRule.elementName
                    else -> initRule.decl().name
                }
                elementName == initElementName
            }
            .coerceIn(possibleRuleTemplates.indices)
        mutableIntStateOf(index)
    }
    var currentRule by rememberSaveable(elementsSelectedIndex, stateSaver = SnyggRule.NonNullSaver) {
        mutableStateOf(
            if (isAddRuleDialog) possibleRuleTemplates[elementsSelectedIndex] else initRule
        )
    }

    JetPrefAlertDialog(
        title = stringRes(
            if (isAddRuleDialog) {
                R.string.settings__theme_editor__add_rule
            } else {
                R.string.settings__theme_editor__edit_rule
            }
        ),
        confirmLabel = stringRes(
            if (isAddRuleDialog) {
                R.string.action__add
            } else {
                R.string.action__apply
            }
        ),
        onConfirm = {
            if (isAddRuleDialog && elementsSelectedIndex == 0) {
                showSelectAsError = true
            } else {
                if (!onConfirmRule(initRule, currentRule)) {
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
        neutralColors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
        onNeutral = { onDeleteRule(initRule) },
    ) {
        Column {
            AnimatedVisibility(visible = showAlreadyExistsError) {
                Text(
                    modifier = Modifier.padding(bottom = 16.dp),
                    text = stringRes(R.string.settings__theme_editor__rule_already_exists),
                    color = MaterialTheme.colorScheme.error,
                )
            }

            DialogProperty(text = stringRes(R.string.settings__theme_editor__rule_name)) {
                JetPrefDropdown(
                    options = possibleRuleLabels,
                    selectedOptionIndex = elementsSelectedIndex,
                    onSelectOption = { elementsSelectedIndex = it },
                    enabled = isAddRuleDialog,
                    isError = showSelectAsError && elementsSelectedIndex == 0,
                )
            }

            (currentRule as? SnyggAnnotationRule.Font)?.apply {
                DialogProperty(text = stringRes(R.string.snygg__rule_annotation__font_name)) {
                    JetPrefTextField(
                        modifier = Modifier,
                        value = fontName,
                        onValueChange = {
                            currentRule = copy(fontName = it)
                        },
                        singleLine = true,
                    )
                }
            }

            // TODO: Move to toplevel @Composable function
            (currentRule as? SnyggElementRule)?.apply {
                if (elementName == SnyggEmptyRuleForAdding.elementName) {
                    return@apply
                }
                fun updateCurrentRule(newSelector: SnyggSelector) {
                    currentRule = if (selector == newSelector) {
                        copy(selector = SnyggSelector.NONE)
                    } else {
                        copy(selector = newSelector)
                    }
                }
                DialogProperty(text = stringRes(R.string.settings__theme_editor__rule_selectors)) {
                    Row(
                        modifier = Modifier.florisHorizontalScroll(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        // TODO: avoid code duplication
                        FlorisChip(
                            onClick = { updateCurrentRule(SnyggSelector.PRESSED) },
                            text = when (level) {
                                SnyggLevel.DEVELOPER -> SnyggSelector.PRESSED.id
                                else -> stringRes(R.string.snygg__rule_selector__pressed)
                            },
                            selected = selector == SnyggSelector.PRESSED,
                        )
                        FlorisChip(
                            onClick = { updateCurrentRule(SnyggSelector.FOCUS) },
                            text = when (level) {
                                SnyggLevel.DEVELOPER -> SnyggSelector.FOCUS.id
                                else -> stringRes(R.string.snygg__rule_selector__focus)
                            },
                            selected = selector == SnyggSelector.FOCUS,
                        )
                        FlorisChip(
                            onClick = { updateCurrentRule(SnyggSelector.HOVER) },
                            text = when (level) {
                                SnyggLevel.DEVELOPER -> SnyggSelector.HOVER.id
                                else -> stringRes(R.string.snygg__rule_selector__hover)
                            },
                            selected = selector == SnyggSelector.HOVER,
                        )
                        FlorisChip(
                            onClick = { updateCurrentRule(SnyggSelector.DISABLED) },
                            text = when (level) {
                                SnyggLevel.DEVELOPER -> SnyggSelector.DISABLED.id
                                else -> stringRes(R.string.snygg__rule_selector__disabled)
                            },
                            selected = selector == SnyggSelector.DISABLED,
                        )
                    }
                }

                EditAttributesColumn(
                    elementName = elementName,
                    attributes = attributes,
                    updateAttributes = { currentRule = copy(attributes = it) },
                )
            }
        }
    }
}

@Composable
private fun EditAttributesColumn(
    elementName: String,
    attributes: SnyggAttributes,
    updateAttributes: (SnyggAttributes) -> Unit,
) {
    val attrSpecList = remember(elementName) {
        val elem = FlorisImeUi.entries.firstOrNull { it.elementName == elementName }
        elem?.attributes ?: emptyList()
    }

    Column {
        for (attrSpec in attrSpecList) {
            when (attrSpec) {
                is ImeUiAttrs.Output -> EditOutputAttrBox(attrSpec, attributes, updateAttributes)
                is ImeUiAttrs.TargetLayer -> EditLayerAttrBox(attrSpec, attributes, updateAttributes)
                is ImeUiAttrs.Layer -> EditLayerAttrBox(attrSpec, attributes, updateAttributes)
                is ImeUiAttrs.ShiftState -> EditEnumAttrBox(attrSpec, attributes, updateAttributes)
                is ImeUiAttrs.WindowMode -> EditEnumAttrBox(attrSpec, attributes, updateAttributes)
                is ImeUiAttrs.ClipboardDialogAction -> EditEnumAttrBox(attrSpec, attributes, updateAttributes)
                is ImeUiAttrs.ClipboardItemType -> EditEnumAttrBox(attrSpec, attributes, updateAttributes)
                is ImeUiAttrs.ClipboardFilterState -> EditEnumAttrBox(attrSpec, attributes, updateAttributes)
            }
        }
    }
}
