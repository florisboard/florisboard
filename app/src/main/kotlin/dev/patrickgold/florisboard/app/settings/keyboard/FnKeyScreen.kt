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

package dev.patrickgold.florisboard.app.settings.keyboard

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.window.DialogProperties
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.app.FlorisPreferenceStore
import dev.patrickgold.florisboard.ime.keyboard3.ImeActions
import dev.patrickgold.florisboard.ime.keyboard3.ImeIcons
import dev.patrickgold.florisboard.ime.keyboard3.LocalImeController
import dev.patrickgold.florisboard.ime.keyboard3.touch.FnKeyAction
import dev.patrickgold.florisboard.ime.keyboard3.touch.TouchModelOptions
import dev.patrickgold.florisboard.ime.keyboard3.touch.computeKeyDisplay
import dev.patrickgold.florisboard.ime.keyboard3.ui.staticIcon3
import dev.patrickgold.florisboard.lib.compose.FlorisScreen
import dev.patrickgold.jetpref.datastore.model.collectAsState
import dev.patrickgold.jetpref.datastore.ui.Preference
import dev.patrickgold.jetpref.datastore.ui.PreferenceGroup
import dev.patrickgold.jetpref.material.ui.JetPrefAlertDialog
import dev.patrickgold.jetpref.material.ui.JetPrefDropdown
import kotlinx.coroutines.launch
import org.florisboard.lib.compose.FlorisIconButton
import org.florisboard.lib.compose.stringRes
import org.k3lp.lib.text.K3Descriptor
import org.k3lp.model.K3Model
import kotlin.math.roundToInt

private const val ACTION_HEADLINE = "@fl:action/"

@Composable
fun FnKeyScreen() = FlorisScreen {
    title = stringRes(R.string.settings__fn_key__title)
    previewFieldVisible = true

    val prefs by FlorisPreferenceStore
    val scope = rememberCoroutineScope()

    val imeController = LocalImeController.current
    val imeState by imeController.activeState.collectAsState()
    val model by remember { derivedStateOf { imeState.model } }

    val fnKeyEnabled by prefs.keyboard.fnKeyEnabled.collectAsState()
    val fnKeyArrangement by prefs.keyboard.fnKeyArrangement.collectAsState()
    val resetBtnEnabled by remember {
        val def = TouchModelOptions.Default
        derivedStateOf {
            fnKeyEnabled != def.fnKeyEnabled ||
                fnKeyArrangement != def.fnKeyArrangement
        }
    }
    var resetRequested by remember { mutableStateOf(false) }

    actions {
        FilledTonalIconButton(
            onClick = { resetRequested = true },
            enabled = resetBtnEnabled,
            colors = IconButtonDefaults.filledTonalIconButtonColors(
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.error,
            ),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_reset_settings),
                contentDescription = null,
            )
        }
    }

    content {
        SpotlightText(
            text = stringRes(R.string.settings__fn_key__spotlight_text),
        )

        SpotlightSwitchPreference(
            prefs.keyboard.fnKeyEnabled,
            title = stringRes(R.string.settings__fn_key__spotlight_switch_text),
        )

        var showSimpleActionEditor by remember { mutableStateOf(false) }
        PreferenceGroup(
            title = stringRes(R.string.settings__fn_key__short_press_group_title),
            enabledIf = { prefs.keyboard.fnKeyEnabled isEqualTo true },
        ) {
            ActionPreference(
                model = model,
                action = fnKeyArrangement.simpleAction,
                onClick = { showSimpleActionEditor = true },
            )
        }
        if (showSimpleActionEditor) {
            ActionEditorDialog(
                initial = fnKeyArrangement.simpleAction,
                onEdit = { newAction ->
                    scope.launch {
                        val newArrangement = fnKeyArrangement.copy(
                            simpleAction = newAction,
                        )
                        prefs.keyboard.fnKeyArrangement.set(newArrangement)
                        showSimpleActionEditor = false
                    }
                },
                onDismiss = { showSimpleActionEditor = false },
                allowExtendedConfig = false,
            )
        }

        PreferenceGroup(
            title = stringRes(R.string.settings__fn_key__long_press_group_title),
            enabledIf = { prefs.keyboard.fnKeyEnabled isEqualTo true },
        ) {
            val longPressActions = fnKeyArrangement.longPressActions
            var showAddActionDialog by remember { mutableStateOf(false) }
            var showEditActionDialog by remember { mutableStateOf<FnKeyAction?>(null) }

            for ((index, action) in longPressActions.withIndex()) key(action) {
                ActionPreference(
                    model = model,
                    action = action,
                    onClick = { showEditActionDialog = action },
                    trailing = {
                        Row {
                            FlorisIconButton(
                                onClick = {
                                    scope.launch {
                                        val newArrangement = fnKeyArrangement.copy(
                                            longPressActions = buildList {
                                                addAll(longPressActions)
                                                removeAt(index)
                                                add(index - 1, action)
                                            },
                                        )
                                        prefs.keyboard.fnKeyArrangement.set(newArrangement)
                                    }
                                },
                                icon = Icons.Default.KeyboardArrowUp,
                                iconColor = MaterialTheme.colorScheme.primary,
                                iconModifier = Modifier.size(ButtonDefaults.IconSize),
                                enabled = index > 0,
                            )
                            FlorisIconButton(
                                onClick = {
                                    scope.launch {
                                        val newArrangement = fnKeyArrangement.copy(
                                            longPressActions = buildList {
                                                addAll(longPressActions)
                                                removeAt(index)
                                                add(index + 1, action)
                                            },
                                        )
                                        prefs.keyboard.fnKeyArrangement.set(newArrangement)
                                    }
                                },
                                icon = Icons.Default.KeyboardArrowDown,
                                iconColor = MaterialTheme.colorScheme.primary,
                                iconModifier = Modifier.size(ButtonDefaults.IconSize),
                                enabled = index + 1 < longPressActions.size,
                            )
                        }
                    },
                )
            }
            Preference(
                icon = Icons.Default.Add,
                title = stringRes(R.string.settings__fn_key__long_press_add_action_btn),
                onClick = { showAddActionDialog = true },
            )

            if (showAddActionDialog) {
                ActionEditorDialog(
                    initial = null,
                    onAdd = { newAction ->
                        scope.launch {
                            val newArrangement = fnKeyArrangement.copy(
                                longPressActions = buildList {
                                    addAll(longPressActions)
                                    add(newAction)
                                }
                            )
                            prefs.keyboard.fnKeyArrangement.set(newArrangement)
                            showAddActionDialog = false
                        }
                    },
                    onDismiss = { showAddActionDialog = false },
                    allowExtendedConfig = false,
                )
            }

            showEditActionDialog?.let { actionToBeEdited ->
                ActionEditorDialog(
                    initial = actionToBeEdited,
                    onEdit = { newAction ->
                        scope.launch {
                            val newArrangement = fnKeyArrangement.copy(
                                longPressActions = buildList {
                                    addAll(longPressActions)
                                    val indexToReplace = indexOf(actionToBeEdited)
                                    if (indexToReplace != -1) {
                                        set(indexToReplace, newAction)
                                    }
                                }
                            )
                            prefs.keyboard.fnKeyArrangement.set(newArrangement)
                            showEditActionDialog = null
                        }
                    },
                    onDelete = {
                        scope.launch {
                            val newArrangement = fnKeyArrangement.copy(
                                longPressActions = buildList {
                                    addAll(longPressActions)
                                    val indexToReplace = indexOf(actionToBeEdited)
                                    if (indexToReplace != -1) {
                                        removeAt(indexToReplace)
                                    }
                                }
                            )
                            prefs.keyboard.fnKeyArrangement.set(newArrangement)
                            showEditActionDialog = null
                        }
                    },
                    onDismiss = { showEditActionDialog = null },
                    allowExtendedConfig = false,
                    allowDelete = true,
                )
            }
        }

        if (resetRequested) {
            JetPrefAlertDialog(
                title = stringRes(R.string.action__reset_confirm_title),
                confirmLabel = stringRes(R.string.action__reset),
                confirmColors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error,
                ),
                onConfirm = {
                    scope.launch {
                        prefs.keyboard.fnKeyEnabled.reset()
                        prefs.keyboard.fnKeyArrangement.reset()
                        resetRequested = false
                    }
                },
                dismissLabel = stringRes(R.string.action__cancel),
                onDismiss = { resetRequested = false },
            ) {
                Text(text = stringRes(R.string.action__reset_confirm_message, "name" to title))
            }
        }
    }
}

private val OutputOptions by lazy {
    buildList {
        add(ImeActions.NoopSpacer)
        addAll(ImeActions.FnKeyCapable)
    }
}

@Composable
private fun ActionPreference(
    model: K3Model,
    action: FnKeyAction,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailing: @Composable (() -> Unit)? = null,
) {
    val context = LocalContext.current
    val display = remember(model, action) {
        computeKeyDisplay(model, action.output) as? K3Descriptor ?: ImeIcons.Noop
    }
    Preference(
        modifier = modifier,
        icon = staticIcon3(display, context),
        overlineText = ACTION_HEADLINE,
        title = action.output.name,
        onClick = onClick,
        trailing = trailing,
    )
}

@Composable
private fun ActionEditorDialog(
    initial: FnKeyAction?,
    onAdd: (FnKeyAction) -> Unit = {},
    onEdit: (FnKeyAction) -> Unit = {},
    onDelete: (FnKeyAction) -> Unit = {},
    onDismiss: () -> Unit,
    allowExtendedConfig: Boolean,
    allowDelete: Boolean = false,
) {
    var outputIndex by remember {
        val initialIndex = initial?.output?.let { OutputOptions.indexOf(it) }
            ?.takeIf { it != -1 }
            ?: 0
        mutableIntStateOf(initialIndex)
    }
    var width by remember { mutableDoubleStateOf(initial?.width ?: 1.0) }
    val confirmEnabled by remember { derivedStateOf { outputIndex > 0 } }

    JetPrefAlertDialog(
        title = stringRes(
            if (initial == null) {
                R.string.settings__fn_key__add_action_dialog_title
            } else {
                R.string.settings__fn_key__edit_action_dialog_title
            }
        ),
        confirmLabel = stringRes(R.string.action__apply),
        confirmEnabled = confirmEnabled,
        onConfirm = {
            val newAction = FnKeyAction(
                output = OutputOptions[outputIndex],
                width = width,
            )
            if (initial == null) {
                onAdd(newAction)
            } else {
                onEdit(newAction)
            }
        },
        dismissLabel = stringRes(R.string.action__cancel),
        onDismiss = onDismiss,
        neutralLabel = if (allowDelete && initial != null) {
            stringRes(R.string.action__delete)
        } else null,
        neutralColors = ButtonDefaults.textButtonColors(
            contentColor = MaterialTheme.colorScheme.error,
        ),
        onNeutral = {
            if (initial != null) {
                onDelete(initial)
            }
        },
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Column {
            val placeholder = stringRes(R.string.general__select_dropdown_value_placeholder)
            JetPrefDropdown(
                options = OutputOptions,
                selectedOptionIndex = outputIndex,
                onSelectOption = { outputIndex = it },
                labelText = ACTION_HEADLINE,
                optionsLabelProvider = { action ->
                    if (action == OutputOptions[0]) {
                        placeholder
                    } else {
                        action.name
                    }
                },
            )
            if (allowExtendedConfig) {
                Row(
                    modifier = Modifier,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("width")
                    val range = 0.1f..2.0f
                    Slider(
                        value = width.toFloat(),
                        onValueChange = { width = it.toDouble() },
                        valueRange = range,
                        steps = ((range.endInclusive - range.start) / 0.1f).roundToInt() + 1,
                    )
                }
            }
        }
    }
}
