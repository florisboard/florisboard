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

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.ExtendedFloatingActionButton
import androidx.compose.material.Icon
import androidx.compose.material.LocalContentColor
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Switch
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.google.accompanist.insets.LocalWindowInsets
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.app.ext.ExtensionComponentView
import dev.patrickgold.florisboard.app.florisPreferenceModel
import dev.patrickgold.florisboard.ime.theme.FlorisImeUiSpec
import dev.patrickgold.florisboard.ime.theme.ThemeExtensionComponent
import dev.patrickgold.florisboard.ime.theme.ThemeExtensionComponentEditor
import dev.patrickgold.florisboard.ime.theme.ThemeExtensionEditor
import dev.patrickgold.florisboard.ime.theme.ThemeManager
import dev.patrickgold.florisboard.lib.android.showLongToast
import dev.patrickgold.florisboard.lib.cache.CacheManager
import dev.patrickgold.florisboard.lib.compose.FlorisIconButton
import dev.patrickgold.florisboard.lib.compose.FlorisOutlinedBox
import dev.patrickgold.florisboard.lib.compose.FlorisOutlinedTextField
import dev.patrickgold.florisboard.lib.compose.FlorisScreen
import dev.patrickgold.florisboard.lib.compose.PreviewKeyboardField
import dev.patrickgold.florisboard.lib.compose.defaultFlorisOutlinedBox
import dev.patrickgold.florisboard.lib.compose.florisVerticalScroll
import dev.patrickgold.florisboard.lib.compose.rememberPreviewFieldController
import dev.patrickgold.florisboard.lib.compose.rippleClickable
import dev.patrickgold.florisboard.lib.compose.stringRes
import dev.patrickgold.florisboard.lib.ext.ExtensionValidation
import dev.patrickgold.florisboard.lib.io.readJson
import dev.patrickgold.florisboard.lib.io.subFile
import dev.patrickgold.florisboard.lib.rememberValidationResult
import dev.patrickgold.florisboard.lib.snygg.SnyggLevel
import dev.patrickgold.florisboard.lib.snygg.SnyggPropertySetEditor
import dev.patrickgold.florisboard.lib.snygg.SnyggPropertySetSpec
import dev.patrickgold.florisboard.lib.snygg.SnyggRule
import dev.patrickgold.florisboard.lib.snygg.SnyggStylesheet
import dev.patrickgold.florisboard.lib.snygg.SnyggStylesheetEditor
import dev.patrickgold.florisboard.lib.snygg.SnyggStylesheetJsonConfig
import dev.patrickgold.florisboard.lib.snygg.definedVariablesRule
import dev.patrickgold.florisboard.lib.snygg.isDefinedVariablesRule
import dev.patrickgold.florisboard.themeManager
import dev.patrickgold.jetpref.datastore.model.observeAsState
import dev.patrickgold.jetpref.material.ui.JetPrefAlertDialog
import dev.patrickgold.jetpref.material.ui.JetPrefListItem
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal val IntListSaver = Saver<SnapshotStateList<Int>, ArrayList<Int>>(
    save = { ArrayList(it) },
    restore = { it.toMutableStateList() },
)

@Composable
fun ThemeEditorScreen(
    workspace: CacheManager.ExtEditorWorkspace<*>,
    editor: ThemeExtensionComponentEditor,
) = FlorisScreen {
    title = stringRes(R.string.ext__editor__edit_component__title_theme)
    scrollable = false

    val prefs by florisPreferenceModel()
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val themeManager by context.themeManager()

    val scope = rememberCoroutineScope()
    val previewFieldController = rememberPreviewFieldController().also { it.isVisible = true }

    val stylesheetEditor = remember {
        editor.stylesheetEditor ?: run {
            val stylesheetPath = editor.stylesheetPath()
            editor.stylesheetPathOnLoad = stylesheetPath
            val stylesheetFile = workspace.extDir.subFile(stylesheetPath)
            val stylesheetEditor = if (stylesheetFile.exists()) {
                try {
                    stylesheetFile.readJson<SnyggStylesheet>(SnyggStylesheetJsonConfig).edit()
                } catch (e: Throwable) {
                    SnyggStylesheetEditor()
                }
            } else {
                SnyggStylesheetEditor()
            }
            if (stylesheetEditor.rules.none { (rule, _) -> rule.isDefinedVariablesRule() }) {
                stylesheetEditor.rules[SnyggRule.definedVariablesRule()] = SnyggPropertySetEditor()
            }
            stylesheetEditor
        }.also { editor.stylesheetEditor = it }
    }

    val snyggLevel by prefs.theme.editorLevel.observeAsState()
    val displayColorsAs by prefs.theme.editorDisplayColorsAs.observeAsState()
    val displayKbdAfterDialogs by prefs.theme.editorDisplayKbdAfterDialogs.observeAsState()
    var oldFocusState by remember { mutableStateOf(false) }
    var snyggRuleToEdit by rememberSaveable(stateSaver = SnyggRule.Saver) { mutableStateOf(null) }
    var snyggPropertyToEdit by remember { mutableStateOf<PropertyInfo?>(null) }
    var snyggPropertySetForEditing = remember<SnyggPropertySetEditor?> { null }
    var snyggPropertySetSpecForEditing = remember<SnyggPropertySetSpec?> { null }
    var showEditComponentMetaDialog by rememberSaveable { mutableStateOf(false) }
    var showFineTuneDialog by rememberSaveable { mutableStateOf(false) }

    fun handleBackPress() {
        workspace.currentAction = null
    }

    navigationIcon {
        FlorisIconButton(
            onClick = { handleBackPress() },
            icon = painterResource(R.drawable.ic_close),
        )
    }

    actions {
        FlorisIconButton(
            onClick = { showFineTuneDialog = true },
            icon = painterResource(R.drawable.ic_tune),
        )
    }

    floatingActionButton {
        ExtendedFloatingActionButton(
            icon = { Icon(
                painter = painterResource(R.drawable.ic_add),
                contentDescription = null,
            ) },
            text = { Text(
                text = stringRes(R.string.settings__theme_editor__add_rule),
            ) },
            onClick = { snyggRuleToEdit = SnyggEmptyRuleForAdding },
        )
    }

    bottomBar {
        PreviewKeyboardField(previewFieldController)
    }

    content {
        BackHandler {
            handleBackPress()
        }

        val isImeVisible = LocalWindowInsets.current.ime.isVisible
        LaunchedEffect(showEditComponentMetaDialog, showFineTuneDialog, snyggRuleToEdit, snyggPropertyToEdit) {
            val visible = showEditComponentMetaDialog || showFineTuneDialog ||
                snyggRuleToEdit != null || snyggPropertyToEdit != null
            if (visible) {
                oldFocusState = isImeVisible
                focusManager.clearFocus()
            } else {
                delay(250)
                when (displayKbdAfterDialogs) {
                    DisplayKbdAfterDialogs.ALWAYS -> {
                        previewFieldController.focusRequester.requestFocus()
                    }
                    DisplayKbdAfterDialogs.NEVER -> {
                        // Do nothing
                    }
                    DisplayKbdAfterDialogs.REMEMBER -> {
                        if (oldFocusState) {
                            previewFieldController.focusRequester.requestFocus()
                        }
                    }
                }
            }
        }

        DisposableEffect(workspace.version) {
            themeManager.previewThemeInfo = ThemeManager.ThemeInfo.DEFAULT.copy(
                stylesheet = stylesheetEditor.build().compileToFullyQualified(FlorisImeUiSpec),
            )
            onDispose {
                themeManager.previewThemeInfo = null
            }
        }

        val definedVariables = remember(stylesheetEditor.rules) {
            stylesheetEditor.rules.firstNotNullOfOrNull { (rule, propertySet) ->
                if (rule.isDefinedVariablesRule()) {
                    propertySet.properties
                } else {
                    null
                }
            } ?: emptyMap()
        }

        // TODO: (priority = low)
        //  Floris scrollbar does not like lazy lists with non-constant item heights.
        //  Consider building a custom scrollbar tailored for this list specifically.
        val lazyListState = rememberLazyListState()
        LazyColumn(
            //modifier = Modifier.florisScrollbar(lazyListState, isVertical = true),
            state = lazyListState,
        ) {
            item {
                Column {
                    ExtensionComponentView(
                        modifier = Modifier.defaultFlorisOutlinedBox(),
                        meta = workspace.editor!!.meta,
                        component = editor,
                        onEditBtnClick = { showEditComponentMetaDialog = true },
                    )
                    if (stylesheetEditor.rules.isEmpty() ||
                        (stylesheetEditor.rules.size == 1 && stylesheetEditor.rules.keys.all { it.isDefinedVariablesRule() })
                    ) {
                        Text(
                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp),
                            text = stringRes(R.string.settings__theme_editor__no_rules_defined),
                            fontStyle = FontStyle.Italic,
                        )
                    }
                }
            }

            items(stylesheetEditor.rules.entries.toList()) { (rule, propertySet) -> key(rule) {
                val isVariablesRule = rule.isDefinedVariablesRule()
                val propertySetSpec = FlorisImeUiSpec.propertySetSpec(rule.element)
                FlorisOutlinedBox(
                    modifier = Modifier
                        .padding(vertical = 8.dp, horizontal = 16.dp)
                        .fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        SnyggRuleRow(
                            rule = rule,
                            level = snyggLevel,
                            showEditBtn = !isVariablesRule,
                            onEditRuleBtnClick = {
                                snyggRuleToEdit = rule
                            },
                            onAddPropertyBtnClick = {
                                snyggPropertySetForEditing = propertySet
                                snyggPropertySetSpecForEditing = propertySetSpec
                                snyggPropertyToEdit = SnyggEmptyPropertyInfoForAdding
                            },
                        )
                        if (isVariablesRule) {
                            Text(
                                modifier = Modifier.padding(bottom = 8.dp, start = 16.dp, end = 16.dp),
                                text = stringRes(R.string.snygg__rule_element__defines_description),
                                style = MaterialTheme.typography.body2,
                                fontStyle = FontStyle.Italic,
                            )
                        }
                        for ((propertyName, propertyValue) in propertySet.properties) {
                            val propertySpec = propertySetSpec?.propertySpec(propertyName)
                            if (propertySpec != null && propertySpec.level <= snyggLevel || isVariablesRule) {
                                JetPrefListItem(
                                    modifier = Modifier.rippleClickable {
                                        snyggPropertySetForEditing = propertySet
                                        snyggPropertySetSpecForEditing = propertySetSpec
                                        snyggPropertyToEdit = PropertyInfo(propertyName, propertyValue)
                                    },
                                    text = translatePropertyName(propertyName, snyggLevel),
                                    secondaryText = translatePropertyValue(propertyValue, snyggLevel, displayColorsAs),
                                    singleLineSecondaryText = true,
                                    trailing = { SnyggValueIcon(propertyValue, definedVariables) },
                                )
                            }
                        }
                    }
                }
            } }

            item {
                Spacer(modifier = Modifier.height(72.dp))
            }
        }

        if (showEditComponentMetaDialog) {
            ComponentMetaEditorDialog(
                workspace = workspace,
                editor = editor,
                onConfirm = { showEditComponentMetaDialog = false },
                onDismiss = { showEditComponentMetaDialog = false },
            )
        }

        if (showFineTuneDialog) {
            FineTuneDialog(onDismiss = { showFineTuneDialog = false })
        }

        val ruleToEdit = snyggRuleToEdit
        if (ruleToEdit != null) {
            EditRuleDialog(
                initRule = ruleToEdit,
                level = snyggLevel,
                onConfirmRule = { oldRule, newRule ->
                    val rules = stylesheetEditor.rules
                    when {
                        oldRule == newRule -> {
                            snyggRuleToEdit = null
                            true
                        }
                        rules.contains(newRule) -> {
                            false
                        }
                        else -> workspace.update {
                            val set = rules.remove(oldRule)
                            when {
                                set != null -> {
                                    rules[newRule] = set
                                    snyggRuleToEdit = null
                                    scope.launch {
                                        lazyListState.animateScrollToItem(index = rules.keys.indexOf(newRule))
                                    }
                                    true
                                }
                                oldRule == SnyggEmptyRuleForAdding -> {
                                    rules[newRule] = SnyggPropertySetEditor()
                                    snyggRuleToEdit = null
                                    scope.launch {
                                        lazyListState.animateScrollToItem(index = rules.keys.indexOf(newRule))
                                    }
                                    true
                                }
                                else -> {
                                    false
                                }
                            }
                        }
                    }
                },
                onDeleteRule = { rule ->
                    workspace.update {
                        stylesheetEditor.rules.remove(rule)
                    }
                    snyggRuleToEdit = null
                },
                onDismiss = { snyggRuleToEdit = null },
            )
        }

        val propertyToEdit = snyggPropertyToEdit
        if (propertyToEdit != null) {
            EditPropertyDialog(
                propertySetSpec = snyggPropertySetSpecForEditing,
                initProperty = propertyToEdit,
                level = snyggLevel,
                displayColorsAs = displayColorsAs,
                definedVariables = definedVariables,
                onConfirmNewValue = { name, value ->
                    val properties = snyggPropertySetForEditing?.properties ?: return@EditPropertyDialog false
                    if (propertyToEdit == SnyggEmptyPropertyInfoForAdding && properties.containsKey(name)) {
                        return@EditPropertyDialog false
                    }
                    workspace.update {
                        properties[name] = value
                    }
                    snyggPropertyToEdit = null
                    true
                },
                onDelete = {
                    workspace.update {
                        snyggPropertySetForEditing?.properties?.remove(propertyToEdit.name)
                    }
                    snyggPropertyToEdit = null
                },
                onDismiss = { snyggPropertyToEdit = null },
            )
        }
    }
}

@Composable
private fun ComponentMetaEditorDialog(
    workspace: CacheManager.ExtEditorWorkspace<*>,
    editor: ThemeExtensionComponentEditor,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    var showValidationErrors by rememberSaveable { mutableStateOf(false) }

    var id by rememberSaveable { mutableStateOf(editor.id) }
    val idValidation = rememberValidationResult(ExtensionValidation.ComponentId, id)
    var label by rememberSaveable { mutableStateOf(editor.label) }
    val labelValidation = rememberValidationResult(ExtensionValidation.ComponentLabel, label)
    var authors by rememberSaveable { mutableStateOf(editor.authors.joinToString("\n")) }
    val authorsValidation = rememberValidationResult(ExtensionValidation.ComponentAuthors, authors)
    var isNightTheme by rememberSaveable { mutableStateOf(editor.isNightTheme) }
    var isBorderless by rememberSaveable { mutableStateOf(editor.isBorderless) }
    val isMaterialYouAware by rememberSaveable { mutableStateOf(editor.isMaterialYouAware) }
    var stylesheetPath by rememberSaveable { mutableStateOf(editor.stylesheetPath) }
    val stylesheetPathValidation = rememberValidationResult(ExtensionValidation.ThemeComponentStylesheetPath, stylesheetPath)

    JetPrefAlertDialog(
        title = stringRes(R.string.ext__editor__metadata__title),
        confirmLabel = stringRes(R.string.action__apply),
        onConfirm = {
            val allFieldsValid = idValidation.isValid() &&
                labelValidation.isValid() &&
                authorsValidation.isValid() &&
                stylesheetPathValidation.isValid()
            if (!allFieldsValid) {
                showValidationErrors = true
            } else if (id != editor.id && (workspace.editor as? ThemeExtensionEditor)?.themes?.find { it.id == id.trim() } != null) {
                context.showLongToast("A theme with this ID already exists!")
            } else {
                workspace.update {
                    editor.id = id.trim()
                    editor.label = label.trim()
                    editor.authors = authors.lines().map { it.trim() }.filter { it.isNotBlank() }
                    editor.isNightTheme = isNightTheme
                    editor.isBorderless = isBorderless
                    editor.isMaterialYouAware = isMaterialYouAware
                    editor.stylesheetPath = stylesheetPath.trim()
                }
                onConfirm()
            }
        },
        dismissLabel = stringRes(R.string.action__cancel),
        onDismiss = onDismiss,
        scrollModifier = Modifier.florisVerticalScroll(),
    ) {
        Column {
            DialogProperty(text = stringRes(R.string.ext__meta__id)) {
                FlorisOutlinedTextField(
                    value = id,
                    onValueChange = { id = it },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
                    singleLine = true,
                    showValidationError = showValidationErrors,
                    validationResult = idValidation,
                )
            }
            DialogProperty(text = stringRes(R.string.ext__meta__label)) {
                FlorisOutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    singleLine = true,
                    showValidationError = showValidationErrors,
                    validationResult = labelValidation,
                )
            }
            DialogProperty(text = stringRes(R.string.ext__meta__authors)) {
                FlorisOutlinedTextField(
                    value = authors,
                    onValueChange = { authors = it },
                    showValidationError = showValidationErrors,
                    validationResult = authorsValidation,
                )
            }
            JetPrefListItem(
                modifier = Modifier.toggleable(isNightTheme) { isNightTheme = it },
                text = stringRes(R.string.settings__theme_editor__component_meta_is_night_theme),
                trailing = {
                    Switch(checked = isNightTheme, onCheckedChange = null)
                },
            )
            JetPrefListItem(
                modifier = Modifier.toggleable(isBorderless) { isBorderless = it },
                text = stringRes(R.string.settings__theme_editor__component_meta_is_borderless),
                trailing = {
                    Switch(checked = isBorderless, onCheckedChange = null)
                },
            )
            DialogProperty(text = stringRes(R.string.settings__theme_editor__component_meta_stylesheet_path)) {
                FlorisOutlinedTextField(
                    value = stylesheetPath,
                    onValueChange = { stylesheetPath = it },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
                    singleLine = true,
                    placeholder = if (stylesheetPath.isEmpty()) {
                        ThemeExtensionComponent.defaultStylesheetPath(id.trim())
                    } else {
                        null
                    },
                    showValidationError = showValidationErrors,
                    validationResult = stylesheetPathValidation,
                )
            }
        }
    }
}

@Composable
private fun SnyggRuleRow(
    rule: SnyggRule,
    level: SnyggLevel,
    showEditBtn: Boolean,
    onEditRuleBtnClick: () -> Unit,
    onAddPropertyBtnClick: () -> Unit,
) {
    @Composable
    fun Selector(text: String) {
        Text(
            modifier = Modifier
                .padding(end = 8.dp)
                .background(MaterialTheme.colors.primaryVariant),
            text = text,
            style = MaterialTheme.typography.body2,
            fontFamily = FontFamily.Monospace,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }

    @Composable
    fun AttributesList(text: String, list: String) {
        Text(
            text = "$text = $list",
            style = MaterialTheme.typography.body2,
            color = LocalContentColor.current.copy(alpha = 0.56f),
            fontFamily = FontFamily.Monospace,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp),
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 8.dp, horizontal = 10.dp),
        ) {
            Text(
                text = translateElementName(rule, level),
                style = MaterialTheme.typography.body2,
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(modifier = Modifier.fillMaxWidth()) {
                if (rule.pressedSelector) {
                    Selector(text = when (level) {
                        SnyggLevel.DEVELOPER -> SnyggRule.PRESSED_SELECTOR
                        else -> stringRes(R.string.snygg__rule_selector__pressed)
                    })
                }
                if (rule.focusSelector) {
                    Selector(text = when (level) {
                        SnyggLevel.DEVELOPER -> SnyggRule.FOCUS_SELECTOR
                        else -> stringRes(R.string.snygg__rule_selector__focus)
                    })
                }
                if (rule.disabledSelector) {
                    Selector(text = when (level) {
                        SnyggLevel.DEVELOPER -> SnyggRule.DISABLED_SELECTOR
                        else -> stringRes(R.string.snygg__rule_selector__disabled)
                    })
                }
            }
            if (rule.codes.isNotEmpty()) {
                AttributesList(text = "code", list = remember(rule.codes) { rule.codes.toString() })
            }
            if (rule.shiftStates.isNotEmpty()) {
                AttributesList(text = "shiftstate", list = remember(rule.shiftStates) { rule.shiftStates.toString() })
            }
        }
        if (showEditBtn) {
            FlorisIconButton(
                onClick = onEditRuleBtnClick,
                icon = painterResource(R.drawable.ic_edit),
                iconColor = MaterialTheme.colors.primary,
                iconModifier = Modifier.size(ButtonDefaults.IconSize),
            )
        }
        FlorisIconButton(
            onClick = onAddPropertyBtnClick,
            icon = painterResource(R.drawable.ic_add),
            iconColor = MaterialTheme.colors.secondary,
            iconModifier = Modifier.size(ButtonDefaults.IconSize),
        )
    }
}

@Composable
internal fun DialogProperty(
    text: String,
    modifier: Modifier = Modifier,
    trailingIconTitle: @Composable () -> Unit = { },
    content: @Composable () -> Unit,
) {
    Column(modifier = modifier.padding(bottom = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 8.dp),
                text = text,
                style = MaterialTheme.typography.subtitle2,
            )
            trailingIconTitle()
        }
        content()
    }
}
