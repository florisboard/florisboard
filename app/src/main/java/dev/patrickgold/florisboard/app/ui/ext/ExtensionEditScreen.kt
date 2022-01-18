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

package dev.patrickgold.florisboard.app.ui.ext

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.app.LocalNavController
import dev.patrickgold.florisboard.app.res.stringRes
import dev.patrickgold.florisboard.app.ui.components.FlorisButtonBar
import dev.patrickgold.florisboard.app.ui.components.FlorisIconButton
import dev.patrickgold.florisboard.app.ui.components.FlorisInfoCard
import dev.patrickgold.florisboard.app.ui.components.FlorisOutlinedBox
import dev.patrickgold.florisboard.app.ui.components.FlorisScreen
import dev.patrickgold.florisboard.app.ui.components.FlorisUnsavedChangesDialog
import dev.patrickgold.florisboard.app.ui.components.defaultFlorisOutlinedBox
import dev.patrickgold.florisboard.cacheManager
import dev.patrickgold.florisboard.extensionManager
import dev.patrickgold.florisboard.ime.keyboard.KeyboardExtension
import dev.patrickgold.florisboard.ime.spelling.SpellingExtension
import dev.patrickgold.florisboard.ime.theme.ThemeExtension
import dev.patrickgold.florisboard.ime.theme.ThemeExtensionEditor
import dev.patrickgold.florisboard.res.ZipUtils
import dev.patrickgold.florisboard.res.cache.CacheManager
import dev.patrickgold.florisboard.res.ext.Extension
import dev.patrickgold.florisboard.res.ext.ExtensionComponent
import dev.patrickgold.florisboard.res.ext.ExtensionEditor
import dev.patrickgold.florisboard.res.ext.ExtensionMaintainer
import dev.patrickgold.jetpref.datastore.ui.Preference
import java.util.*

private val TextFieldVerticalPadding = 8.dp
private val MetaDataContentPadding = PaddingValues(vertical = 8.dp, horizontal = 16.dp)

private const val AnimationDuration = 300

private val ActionScreenEnterTransition = fadeIn(tween(AnimationDuration))
private val ActionScreenExitTransition = fadeOut(tween(AnimationDuration))

private sealed class EditorAction {
    object ManageMetaData : EditorAction()

    object ManageDependencies : EditorAction()

    object ManageFiles : EditorAction()

    data class ManageComponent(val editor: ExtensionComponent) : EditorAction()
}

private class EditorState(val ext: Extension, val workspace: CacheManager.ExtEditorWorkspace<*>) {
    var currentAction by mutableStateOf<EditorAction?>(null)
}

@Composable
fun ExtensionEditScreen(id: String) {
    val context = LocalContext.current
    val cacheManager by context.cacheManager()
    val extensionManager by context.extensionManager()

    @Suppress("unchecked_cast")
    fun <W : CacheManager.ExtEditorWorkspace<T>, T : ExtensionEditor> getOrCreateWorkspace(
        uuid: String,
        container: CacheManager.WorkspacesContainer<W>,
        ext: Extension,
    ): W {
        val workspace = container.getWorkspaceByUuid(uuid)
        return workspace ?: container.new(uuid).also { newWorkspace ->
            val sourceRef = ext.sourceRef
            checkNotNull(sourceRef) { "Extension source ref must not be null" }
            newWorkspace.editor = ext.edit() as? T
            ZipUtils.unzip(context, sourceRef, newWorkspace.dir)
        }
    }

    val ext = extensionManager.getExtensionById(id)
    if (ext != null) {
        val uuid = rememberSaveable { UUID.randomUUID().toString() }
        val cacheWorkspace = remember {
            runCatching {
                when (ext) {
                    is ThemeExtension -> {
                        getOrCreateWorkspace(uuid, cacheManager.themeExtEditor, ext)
                    }
                    else -> null
                }
            }
        }
        cacheWorkspace.onSuccess { workspace ->
            if (workspace?.editor != null) {
                ExtensionEditScreenSheetSwitcher(ext, workspace)
            } else {
                ExtensionNotFoundScreen(id = id)
            }
        }.onFailure { error ->
            Text(text = remember(error) { error.stackTraceToString() })
        }
    } else {
        ExtensionNotFoundScreen(id)
    }
}

@Composable
private fun ExtensionEditScreenSheetSwitcher(ext: Extension, workspace: CacheManager.ExtEditorWorkspace<*>) {
    val state = remember(ext, workspace) { EditorState(ext, workspace) }

    Box(modifier = Modifier.fillMaxSize()) {
        EditScreen(state)
        AnimatedVisibility(
            visible = state.currentAction != null,
            enter = ActionScreenEnterTransition,
            exit = ActionScreenExitTransition,
        ) {
            when (state.currentAction) {
                EditorAction.ManageMetaData -> {
                    ManageMetaDataScreen(state)
                }
                EditorAction.ManageDependencies -> {
                    ManageDependenciesScreen(state)
                }
                EditorAction.ManageFiles -> {
                    ManageFilesScreen(state)
                }
                else -> {
                    // Render nothing
                    Box(modifier = Modifier.fillMaxSize())
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun EditScreen(state: EditorState) = FlorisScreen {
    title = stringRes(when (state.ext) {
        is KeyboardExtension -> R.string.ext__editor__title_keyboard
        is SpellingExtension -> R.string.ext__editor__title_spelling
        is ThemeExtension -> R.string.ext__editor__title_theme
        else -> R.string.ext__editor__title_any
    })

    val navController = LocalNavController.current
    val context = LocalContext.current
    val extensionManager by context.extensionManager()

    val workspace = state.workspace
    val extEditor = state.workspace.editor ?: return@FlorisScreen
    var showUnsavedChangesDialog by remember { mutableStateOf(false) }

    fun handleBackPress() {
        if (workspace.isModified) {
            showUnsavedChangesDialog = true
        } else {
            navController.popBackStack()
        }
    }

    fun handleSave() {
        /*TODO*/
    }

    navigationIcon {
        FlorisIconButton(
            onClick = { handleBackPress() },
            icon = painterResource(R.drawable.ic_arrow_back),
        )
    }

    bottomBar {
        FlorisButtonBar {
            ButtonBarSpacer()
            ButtonBarTextButton(text = stringRes(R.string.action__cancel)) {
                handleBackPress()
            }
            ButtonBarButton(text = stringRes(R.string.action__save)) {
                handleSave()
            }
        }
    }

    content {
        BackHandler {
            handleBackPress()
        }

        FlorisOutlinedBox(
            modifier = Modifier.defaultFlorisOutlinedBox(),
        ) {
            this@content.Preference(
                onClick = { state.currentAction = EditorAction.ManageMetaData },
                iconId = R.drawable.ic_code,
                title = stringRes(R.string.ext__editor__metadata__title),
            )
            this@content.Preference(
                onClick = { state.currentAction = EditorAction.ManageDependencies },
                iconId = R.drawable.ic_library_books,
                title = stringRes(R.string.ext__editor__dependencies__title),
            )
            this@content.Preference(
                onClick = { state.currentAction = EditorAction.ManageFiles },
                iconId = R.drawable.ic_file_blank,
                title = stringRes(R.string.ext__editor__files__title),
            )
        }

        when (extEditor) {
            is ThemeExtensionEditor -> {
                ExtensionComponentListView(
                    title = stringRes(R.string.ext__meta__components_theme),
                    components = extEditor.themes,
                ) { component ->
                    ExtensionComponentView(
                        modifier = Modifier.defaultFlorisOutlinedBox(),
                        meta = extEditor.meta,
                        component = component,
                        onDeleteBtnClick = { workspace.update { extEditor.themes.remove(component) } },
                        onEditBtnClick = { state.currentAction = EditorAction.ManageComponent(component) },
                    )
                }
            }
            else -> {
                // Render nothing
            }
        }

        if (showUnsavedChangesDialog) {
            FlorisUnsavedChangesDialog(
                onSave = {
                    handleSave()
                },
                onDiscard = {
                    navController.popBackStack()
                    showUnsavedChangesDialog = false
                },
                onDismiss = {
                    showUnsavedChangesDialog = false
                },
            )
        }
    }
}

@Composable
private fun ManageMetaDataScreen(state: EditorState) = FlorisScreen {
    title = stringRes(R.string.ext__editor__metadata__title)

    val workspace = state.workspace
    val editor = state.workspace.editor?.meta ?: return@FlorisScreen

    fun handleBackPress() {
        state.currentAction = null
    }

    navigationIcon {
        FlorisIconButton(
            onClick = { handleBackPress() },
            icon = painterResource(R.drawable.ic_close),
        )
    }

    content {
        BackHandler {
            handleBackPress()
        }

        Column(modifier = Modifier.padding(MetaDataContentPadding)) {
            EditorSheetTextField(
                enabled = false,
                isRequired = true,
                value = editor.id,
                onValueChange = { },
                label = stringRes(R.string.ext__meta__id),
            )
            EditorSheetTextField(
                isRequired = true,
                value = editor.version,
                onValueChange = { workspace.update { editor.version = it } },
                label = stringRes(R.string.ext__meta__version),
            )
            EditorSheetTextField(
                isRequired = true,
                value = editor.title,
                onValueChange = { workspace.update { editor.title = it } },
                label = stringRes(R.string.ext__meta__title),
            )
            EditorSheetTextField(
                value = editor.description,
                onValueChange = { workspace.update { editor.description = it } },
                label = stringRes(R.string.ext__meta__description),
            )
            // TODO: make this list editing experience better
            EditorSheetTextField(
                value = editor.keywords.joinToString(","),
                onValueChange = { v ->
                    workspace.update {
                        editor.keywords = v.split(",").map { it.trim() }
                    }
                },
                label = stringRes(R.string.ext__meta__keywords),
            )
            EditorSheetTextField(
                value = editor.homepage,
                onValueChange = { workspace.update { editor.homepage = it } },
                label = stringRes(R.string.ext__meta__homepage),
            )
            EditorSheetTextField(
                value = editor.issueTracker,
                onValueChange = { workspace.update { editor.issueTracker = it } },
                label = stringRes(R.string.ext__meta__issue_tracker),
            )
            // TODO: make this list editing experience better
            EditorSheetTextField(
                isRequired = true,
                value = editor.maintainers.joinToString(","),
                onValueChange = { v ->
                    workspace.update {
                        editor.maintainers = v
                            .split(",")
                            .map { ExtensionMaintainer.fromOrTakeRaw(it.trim()) }
                    }
                },
                label = stringRes(R.string.ext__meta__maintainers),
            )
            EditorSheetTextField(
                isRequired = true,
                value = editor.license,
                onValueChange = { workspace.update { editor.license = it } },
                label = stringRes(R.string.ext__meta__license),
            )
        }
    }
}

@Composable
private fun ManageDependenciesScreen(state: EditorState) = FlorisScreen {
    title = stringRes(R.string.ext__editor__dependencies__title)

    val workspace = state.workspace
    val dependencyList = state.workspace.editor?.dependencies ?: return@FlorisScreen

    fun handleBackPress() {
        state.currentAction = null
    }

    navigationIcon {
        FlorisIconButton(
            onClick = { handleBackPress() },
            icon = painterResource(R.drawable.ic_close),
        )
    }

    content {
        BackHandler {
            handleBackPress()
        }

        FlorisInfoCard(
            modifier = Modifier.padding(all = 8.dp),
            text = """
                Dependencies are currently not implemented, but are already somewhat
                integrated as a placeholder for the future.
                """.trimIndent().replace('\n', ' '),
        )
        if (dependencyList.isEmpty()) {
            Text(text = "no deps found")
        } else {
            for (dependency in dependencyList) {
                Text(text = dependency)
            }
        }
    }
}

@Composable
private fun ManageFilesScreen(state: EditorState) = FlorisScreen {
    title = stringRes(R.string.ext__editor__files__title)

    val workspace = state.workspace

    fun handleBackPress() {
        state.currentAction = null
    }

    navigationIcon {
        FlorisIconButton(
            onClick = { handleBackPress() },
            icon = painterResource(R.drawable.ic_close),
        )
    }

    content {
        BackHandler {
            handleBackPress()
        }

        FlorisInfoCard(
            modifier = Modifier.padding(all = 8.dp),
            text = """
                Managing archive files is currently not supported.
                """.trimIndent().replace('\n', ' '),
        )
    }
}

@Composable
private fun EditorSheetTextField(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isRequired: Boolean = false,
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    isError: Boolean = false,
) {
    val borderColor = MaterialTheme.colors.onSurface.copy(alpha = ButtonDefaults.OutlinedBorderOpacity)
    Column(modifier = Modifier.padding(vertical = TextFieldVerticalPadding)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = TextFieldVerticalPadding),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.subtitle2,
            )
            if (isRequired) {
                Text(
                    modifier = Modifier.padding(start = 2.dp),
                    text = "*",
                    style = MaterialTheme.typography.subtitle2,
                    color = MaterialTheme.colors.error,
                )
            }
        }
        OutlinedTextField(
            modifier = modifier.fillMaxWidth(),
            enabled = enabled,
            value = value,
            onValueChange = onValueChange,
            isError = isError,
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            colors = TextFieldDefaults.outlinedTextFieldColors(
                unfocusedBorderColor = borderColor,
                disabledBorderColor = borderColor,
            )
        )
    }
}
