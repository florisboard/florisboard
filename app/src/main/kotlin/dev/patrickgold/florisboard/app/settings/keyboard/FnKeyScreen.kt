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

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.Role.Companion
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.app.FlorisPreferenceModel
import dev.patrickgold.florisboard.app.FlorisPreferenceStore
import dev.patrickgold.florisboard.app.enumDisplayEntriesOf
import dev.patrickgold.florisboard.ime.keyboard3.ImeActions
import dev.patrickgold.florisboard.ime.keyboard3.touch.FnKeyAction
import dev.patrickgold.florisboard.ime.keyboard3.touch.FnKeyArrangement
import dev.patrickgold.florisboard.ime.keyboard3.touch.FnKeyType
import dev.patrickgold.florisboard.ime.keyboard3.touch.TouchModelOptions
import dev.patrickgold.florisboard.ime.keyboard3.ui.staticIcon3
import dev.patrickgold.florisboard.lib.compose.FlorisScreen
import dev.patrickgold.jetpref.datastore.model.collectAsState
import dev.patrickgold.jetpref.datastore.ui.ListPreference
import dev.patrickgold.jetpref.datastore.ui.Preference
import dev.patrickgold.jetpref.datastore.ui.PreferenceGroup
import dev.patrickgold.jetpref.datastore.ui.PreferenceUiScope
import dev.patrickgold.jetpref.datastore.ui.SwitchPreference
import dev.patrickgold.jetpref.material.ui.JetPrefAlertDialog
import dev.patrickgold.jetpref.material.ui.JetPrefDropdown
import dev.patrickgold.jetpref.material.ui.JetPrefListItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.florisboard.lib.compose.FlorisIconButton
import org.florisboard.lib.compose.stringRes
import kotlin.math.roundToInt

private const val ACTION_HEADLINE = "@fl:action/"

@Composable
fun FnKeyScreen() = FlorisScreen {
    title = stringRes(R.string.settings__fn_key__title)
    previewFieldVisible = true
    iconSpaceReserved = false

    val prefs by FlorisPreferenceStore
    val scope = rememberCoroutineScope()

    val fnKeyEnabled by prefs.keyboard.fnKeyEnabled.collectAsState()
    val fnKeyType by prefs.keyboard.fnKeyType.collectAsState()
    val fnKeyArrangement by prefs.keyboard.fnKeyArrangement.collectAsState()
    val resetBtnEnabled by remember {
        val def = TouchModelOptions.Default
        derivedStateOf {
            fnKeyEnabled != def.fnKeyEnabled ||
                fnKeyType != def.fnKeyType ||
                fnKeyArrangement != def.fnKeyArrangement
        }
    }
    var resetRequested by remember { mutableStateOf(false) }

    actions {
        TextButton(
            onClick = { resetRequested = true },
            enabled = resetBtnEnabled,
            colors = ButtonDefaults.textButtonColors(
                contentColor = MaterialTheme.colorScheme.error,
            ),
        ) {
            Icon(
                modifier = Modifier
                    .size(24.dp)
                    .padding(end = 8.dp),
                painter = painterResource(R.drawable.ic_reset_settings),
                contentDescription = null,
            )
            Text("Reset")
        }
    }

    content {
        SwitchPreference(
            prefs.keyboard.fnKeyEnabled,
            title = "enable",
            summary = "enable",
        )
        ListPreference(
            prefs.keyboard.fnKeyType,
            title = "type",
            entries = enumDisplayEntriesOf(FnKeyType::class),
            enabledIf = { prefs.keyboard.fnKeyEnabled isEqualTo true },
        )

        var showSimpleActionEditor by remember { mutableStateOf(false) }
        PreferenceGroup(
            title = "simple action",
            enabledIf = { prefs.keyboard.fnKeyEnabled isEqualTo true },
        ) {
            ActionPreference(
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

        when (fnKeyType) {
            FnKeyType.MULTI_KEY -> MultiKeyEditor(fnKeyArrangement, scope)
            FnKeyType.LAYER_KEY -> LayerKeyEditor(fnKeyArrangement, scope)
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
                        prefs.keyboard.fnKeyType.reset()
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

@Composable
private fun PreferenceUiScope<FlorisPreferenceModel>.MultiKeyEditor(
    arrangement: FnKeyArrangement,
    scope: CoroutineScope,
) {
    PreferenceGroup(
        title = "long press actions",
        enabledIf = { prefs.keyboard.fnKeyEnabled isEqualTo true },
    ) {
        val longPressActions = arrangement.longPressActions
        var showAddActionDialog by remember { mutableStateOf(false) }
        var showEditActionDialog by remember { mutableStateOf<FnKeyAction?>(null) }

        for ((index, action) in longPressActions.withIndex()) key(action) {
            ActionPreference(
                action = action,
                onClick = { showEditActionDialog = action },
                trailing = {
                    Row {
                        FlorisIconButton(
                            onClick = {
                                scope.launch {
                                    val newArrangement = arrangement.copy(
                                        longPressActions = buildList {
                                            addAll(longPressActions)
                                            removeAt(index)
                                            add(index - 1, action)
                                        },
                                    )
                                    this@MultiKeyEditor.prefs.keyboard.fnKeyArrangement.set(newArrangement)
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
                                    val newArrangement = arrangement.copy(
                                        longPressActions = buildList {
                                            addAll(longPressActions)
                                            removeAt(index)
                                            add(index + 1, action)
                                        },
                                    )
                                    this@MultiKeyEditor.prefs.keyboard.fnKeyArrangement.set(newArrangement)
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
            title = "add long press action",
            onClick = { showAddActionDialog = true },
        )

        if (showAddActionDialog) {
            ActionEditorDialog(
                initial = null,
                onAdd = { newAction ->
                    scope.launch {
                        val newArrangement = arrangement.copy(
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
                        val newArrangement = arrangement.copy(
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
                        val newArrangement = arrangement.copy(
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
}

@Composable
private fun PreferenceUiScope<FlorisPreferenceModel>.LayerKeyEditor(
    arrangement: FnKeyArrangement,
    scope: CoroutineScope,
) {
    PreferenceGroup(
        title = "layer arrangement",
        enabledIf = { prefs.keyboard.fnKeyEnabled isEqualTo true },
    ) {
        Text("TODO")
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
    action: FnKeyAction,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailing: @Composable (() -> Unit)? = null,
) {
    val context = LocalContext.current
    Preference(
        modifier = modifier,
        icon = staticIcon3(action.output, context),
        iconSpaceReserved = true,
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
    var stretch by remember { mutableStateOf(initial?.stretch ?: false) }
    var width by remember { mutableDoubleStateOf(initial?.width ?: 1.0) }
    val confirmEnabled by remember { derivedStateOf { outputIndex > 0 } }

    JetPrefAlertDialog(
        title = if (initial == null) {
            "add action"
        } else {
            "edit action"
        },
        confirmLabel = stringRes(R.string.action__apply),
        confirmEnabled = confirmEnabled,
        onConfirm = {
            val newAction = FnKeyAction(
                output = OutputOptions[outputIndex],
                stretch = stretch,
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
                    modifier = Modifier
                        .toggleable(stretch) { stretch  = it },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("stretch?")
                    Switch(
                        checked = stretch,
                        onCheckedChange = null,
                    )
                }
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
