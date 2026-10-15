/*
 * Copyright (C) 2021-2026 The FlorisBoard Contributors
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

package dev.patrickgold.florisboard.app.ext

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.LibraryBooks
import androidx.compose.material.icons.filled.Code
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSerializable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.app.LocalNavController
import dev.patrickgold.florisboard.app.components.ScrollableScreenColumn
import dev.patrickgold.florisboard.app.components.SegmentedListColumn
import dev.patrickgold.florisboard.ime.extension.Extension
import dev.patrickgold.florisboard.ime.extension.ExtensionComponent
import dev.patrickgold.florisboard.ime.extension.ExtensionComponentName
import dev.patrickgold.florisboard.ime.extension.ExtensionController
import dev.patrickgold.florisboard.ime.extension.ExtensionDefaults
import dev.patrickgold.florisboard.ime.extension.ExtensionDependencyDecl
import dev.patrickgold.florisboard.ime.extension.ExtensionMaintainer
import dev.patrickgold.florisboard.ime.extension.ExtensionMeta
import dev.patrickgold.florisboard.ime.extension.ExtensionType
import dev.patrickgold.florisboard.ime.extension.Keyboard3Extension
import dev.patrickgold.florisboard.ime.extension.LocalExtensionController
import dev.patrickgold.florisboard.ime.extension.ThemeExtension
import dev.patrickgold.florisboard.ime.extension.isValid
import dev.patrickgold.florisboard.ime.io.FlorisRef
import dev.patrickgold.florisboard.ime.io.LocalStorageController
import dev.patrickgold.florisboard.ime.io.createWorkspace
import dev.patrickgold.florisboard.ime.io.writeJson
import dev.patrickgold.florisboard.lib.compose.FlorisScreenNg
import dev.patrickgold.florisboard.lib.compose.FlorisUnsavedChangesDialog
import dev.patrickgold.jetpref.material.ui.JetPrefAlertDialog
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import okio.IOException
import org.florisboard.lib.compose.ButtonBarButton
import org.florisboard.lib.compose.ButtonBarTextButton
import org.florisboard.lib.compose.FlorisButtonBar
import org.florisboard.lib.compose.stringRes
import org.florisboard.lib.kotlin.tryOrNull
import kotlin.reflect.KClass

private const val AnimationDuration = 300

private val ActionScreenEnterTransition = fadeIn(tween(AnimationDuration))
private val ActionScreenExitTransition = fadeOut(tween(AnimationDuration))

@Serializable
sealed class EditorAction {
    @Serializable
    data object None : EditorAction()

    @Serializable
    data object ManageMetaData : EditorAction()

    @Serializable
    data object ManageDependencies : EditorAction()

    @Serializable
    data object ManageFiles : EditorAction()

    @Serializable
    data class CreateComponent(
        val components: List<ExtensionComponent>,
        val componentKlass: KClass<*>,
    ) : EditorAction()

    @Serializable
    data class ManageComponent(
        val component: ExtensionComponent,
    ) : EditorAction()
}

@Serializable
sealed interface EditorState {
    @Serializable
    data object Loading : EditorState

    @Serializable
    data class Workspace(
        val workspaceRef: FlorisRef,
        val meta: ExtensionMeta,
        val dependencies: ExtensionDependencyDecl,
        val components: EditorStateComponents,
    ) : EditorState

    @Serializable
    data class Error(
        val error: String,
    ) : EditorState
}

@Serializable
data class EditorStateComponents(
    val themeStylesheets: List<ThemeExtension.ThemeComponent> = emptyList(),
    val k3Imports: List<Keyboard3Extension.ImportComponent> = emptyList(),
    val k3Keyboards: List<Keyboard3Extension.KeyboardComponent> = emptyList(),
    val k3Tests: List<Keyboard3Extension.TestComponent> = emptyList(),
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ExtensionEditScreen(extensionId: String?, type: ExtensionType) {
    val extensionController = LocalExtensionController.current
    val navController = LocalNavController.current
    val storageController = LocalStorageController.current

    val scope = rememberCoroutineScope()
    val storage by storageController.activeStorage.collectAsState()
    val extensionIndex by extensionController.activeIndex.collectAsState()

    var editorState by rememberSerializable { mutableStateOf<EditorState>(EditorState.Loading) }
    var editorAction by rememberSerializable { mutableStateOf<EditorAction>(EditorAction.None) }
    var showUnsavedChangesDialog by remember { mutableStateOf(false) }
    var showInvalidMetadataDialog by remember { mutableStateOf(false) }
    val isCreateExt = extensionId == null

    LaunchedEffect(extensionId, type, storage) {
        // no key for extensionIndex, else we have an unintended re-copy during save!
        editorState = EditorState.Loading
        val workspaceRef = try {
            storage.createWorkspace()
        } catch (e: IOException) {
            editorState = EditorState.Error("failed to create workspace: ${e.message}")
            return@LaunchedEffect
        }
        if (isCreateExt) {
            editorState = EditorState.Workspace(
                workspaceRef = workspaceRef,
                meta = ExtensionMeta(
                    id = ExtensionDefaults.createLocalId("themes", System.currentTimeMillis().toString()),
                    version = "0.0.0",
                    title = "My themes",
                    maintainers = listOf(ExtensionMaintainer(name = "Local")),
                    license = "(none specified)",
                ),
                dependencies = emptyMap(),
                components = EditorStateComponents(),
            )
        } else {
            val extension = extensionIndex.extensions[extensionId]
            if (extension == null) {
                editorState = EditorState.Error("no such extension '$extensionId'")
                tryOrNull { storage.deleteRecursively(workspaceRef) }
                return@LaunchedEffect
            }
            if (extension.type != type) {
                editorState = EditorState.Error("expected type '$type', but got '${extension.type}'")
                tryOrNull { storage.deleteRecursively(workspaceRef) }
                return@LaunchedEffect
            }
            try {
                storage.copyRecursively(extension.sourceRef, workspaceRef)
            } catch (e: IOException) {
                editorState = EditorState.Error("failed to copy extension into workspace: ${e.message}")
                tryOrNull { storage.deleteRecursively(workspaceRef) }
                return@LaunchedEffect
            }
            editorState = EditorState.Workspace(
                workspaceRef = workspaceRef,
                meta = extension.manifest.meta,
                dependencies = extension.manifest.dependencies,
                components = EditorStateComponents(
                    themeStylesheets = if (extension is ThemeExtension) {
                        extension.manifest.themes
                    } else emptyList(),
                    k3Imports = if (extension is Keyboard3Extension) {
                        extension.manifest.imports
                    } else emptyList(),
                    k3Keyboards = if (extension is Keyboard3Extension) {
                        extension.manifest.keyboards
                    } else emptyList(),
                    k3Tests = if (extension is Keyboard3Extension) {
                        extension.manifest.tests
                    } else emptyList(),
                )
            )
        }
    }

    fun handleSave() {
        val state = editorState
        if (state !is EditorState.Workspace) {
            return
        }
        if (!state.meta.isValid()) {
            showUnsavedChangesDialog = false
            showInvalidMetadataDialog = true
            return
        }
        val manifest = when (type) {
            ExtensionType.THEME -> ThemeExtension.Manifest(
                meta = state.meta,
                dependencies = state.dependencies,
                themes = state.components.themeStylesheets,
            )
            ExtensionType.KEYBOARD3 -> Keyboard3Extension.Manifest(
                meta = state.meta,
                dependencies = state.dependencies,
                imports = state.components.k3Imports,
                keyboards = state.components.k3Keyboards,
                tests = state.components.k3Tests,
            )
        }
        check(extensionId == null || extensionId == manifest.meta.id)
        scope.launch {
            val manifestRef = state.workspaceRef.subRef(ExtensionDefaults.MANIFEST_FILE_NAME)
            storage.writeJson(manifestRef, manifest, ExtensionController.ExtensionJson)
            extensionController.import(state.workspaceRef)
            storage.deleteRecursively(state.workspaceRef)
            navController.popBackStack()
        }
    }

    fun handleCreateComponent(createFrom: CreateFrom, componentName: ExtensionComponentName?) {
        /*
        when (val editor = workspace.editor) {
            is ThemeExtensionEditor -> {
                when (createFrom) {
                    CreateFrom.EMPTY -> {
                        if (editor.themes.any { it.id == newId.trim() }) {
                            context.showLongToastSync("A theme with this ID already exists!")
                        } else {
                            val componentEditor = ThemeExtensionComponentEditor(
                                id = newId.trim(),
                                label = newLabel.trim(),
                                authors = newAuthors.lines().map { it.trim() }.filter { it.isNotBlank() },
                            )
                            editor.themes.add(componentEditor)
                            workspace.currentAction = null
                        }
                    }
                    CreateFrom.EXISTING -> {
                        val componentName = selectedComponentName ?: return
                        val componentId = if (editor.themes.any { it.id == componentName.componentId }) {
                            var suffix = 1
                            var tempId: String
                            do {
                                tempId = "${componentName.componentId}_${suffix++}"
                            } while (editor.themes.any { it.id == tempId })
                            tempId
                        } else {
                            componentName.componentId
                        }
                        if (componentName.extensionId == extId) {
                            val component = editor.themes.find { it.id == componentName.componentId } ?: return
                            val componentEditor = component.let { c ->
                                ThemeExtensionComponentEditor(
                                    componentId, c.label, c.authors, c.isNightTheme, stylesheetPath = "",
                                ).also { it.stylesheetEditor = c.stylesheetEditor }
                            }
                            if (componentEditor.stylesheetEditor != null) {
                                val stylesheetFile = workspace.extDir.subFile(componentEditor.stylesheetPath())
                                stylesheetFile.parentFile?.mkdirs()
                                val stylesheet = componentEditor.stylesheetEditor!!.build().toJson(PrettyPrintConfig).getOrThrow()
                                stylesheetFile.writeText(stylesheet)
                                componentEditor.stylesheetEditor = null
                            } else {
                                val srcStylesheetFile = workspace.extDir.subFile(component.stylesheetPath())
                                val dstStylesheetFile = workspace.extDir.subFile(componentEditor.stylesheetPath())
                                dstStylesheetFile.parentFile?.mkdirs()
                                srcStylesheetFile.copyTo(dstStylesheetFile, overwrite = true)
                            }
                            editor.themes.add(componentEditor)
                        } else {
                            val component = themeManager.indexedThemeConfigs.value.first.get(componentName) ?: return
                            val componentEditor = (component as? ThemeExtensionComponentImpl)?.edit() ?: return
                            componentEditor.id = componentId
                            componentEditor.stylesheetPath = ""
                            val externalExt = extensionManager.getExtensionById(componentName.extensionId) ?: return
                            val stylesheetJson = ZipUtils.readFileFromArchive(
                                context, externalExt.sourceRef!!, component.stylesheetPath()
                            ).getOrNull() ?: return
                            val dstStylesheetFile = workspace.extDir.subFile(componentEditor.stylesheetPath())
                            dstStylesheetFile.parentFile?.mkdirs()
                            dstStylesheetFile.writeText(stylesheetJson)
                            editor.themes.add(componentEditor)
                        }
                        workspace.currentAction = null
                    }
                }
            }
        }
         */
    }

    fun handleBackPress() {
        showUnsavedChangesDialog = true
    }

    BackHandler {
        handleBackPress()
    }

    val state = editorState
    val action = editorAction
    Box(modifier = Modifier.fillMaxSize()) {
        FlorisScreenNg(
            title = stringRes(if (isCreateExt) {
                when (type) {
                    ExtensionType.THEME -> R.string.ext__editor__title_create_theme
                    ExtensionType.KEYBOARD3 -> R.string.ext__editor__title_create_keyboard
                }
            } else {
                when (type) {
                    ExtensionType.THEME -> R.string.ext__editor__title_edit_theme
                    ExtensionType.KEYBOARD3 -> R.string.ext__editor__title_edit_keyboard
                }
            }),
            previewFieldVisible = false,
            navigationIconOnClick = { handleBackPress() },
            bottomBar = {
                FlorisButtonBar {
                    Spacer(modifier = Modifier.weight(1f))
                    ButtonBarTextButton(text = stringRes(R.string.action__cancel)) {
                        handleBackPress()
                    }
                    ButtonBarButton(text = stringRes(R.string.action__save), enabled = state is EditorState.Workspace) {
                        handleSave()
                    }
                }
            },
        ) { contentPadding ->
            ScrollableScreenColumn(contentPadding) {
                SegmentedListColumn {
                    val count = 3
                    SegmentedListItem(
                        onClick = { editorAction = EditorAction.ManageMetaData },
                        shapes = ListItemDefaults.segmentedShapes(0, count),
                        leadingContent = { Icon(Icons.Default.Code, null) },
                        content = { Text(stringRes(R.string.ext__editor__metadata__title)) },
                    )
                    SegmentedListItem(
                        onClick = { editorAction = EditorAction.ManageDependencies },
                        shapes = ListItemDefaults.segmentedShapes(1, count),
                        leadingContent = { Icon(Icons.AutoMirrored.Outlined.LibraryBooks, null) },
                        content = { Text(stringRes(R.string.ext__editor__dependencies__title)) },
                    )
                    SegmentedListItem(
                        onClick = { editorAction = EditorAction.ManageFiles },
                        shapes = ListItemDefaults.segmentedShapes(2, count),
                        leadingContent = { Icon(ImageVector.vectorResource(R.drawable.ic_file_blank), null) },
                        content = { Text(stringRes(R.string.ext__editor__files__title)) },
                    )
                }

                when (state) {
                    is EditorState.Loading -> {
                        LoadingIndicator()
                    }
                    is EditorState.Workspace -> when (type) {
                        ExtensionType.THEME -> {
                            ExtensionComponentListColumn(
                                title = stringRes(R.string.ext__meta__components_theme),
                                components = state.components.themeStylesheets,
                                onCreateComponent = {
                                    editorAction = EditorAction.CreateComponent(
                                        components = state.components.themeStylesheets,
                                        componentKlass = ThemeExtension.ThemeComponent::class,
                                    )
                                },
                                onEditComponent = { component ->
                                    editorAction = EditorAction.ManageComponent(component)
                                },
                                onDeleteComponent = { component ->
                                    // TODO also delete stylesheet json??
                                    editorState = state.copy(
                                        components = state.components.copy(
                                            themeStylesheets = state.components.themeStylesheets
                                                .filter { it.id != component.id },
                                        )
                                    )
                                },
                            )
                        }
                        ExtensionType.KEYBOARD3 -> {
                            //
                        }
                    }
                    is EditorState.Error -> {
                        Text(state.error)
                    }
                }
            }
        }

        if (state is EditorState.Workspace) {
            AnimatedVisibility(
                visible = action != EditorAction.None,
                enter = ActionScreenEnterTransition,
                exit = ActionScreenExitTransition,
            ) {
                when (action) {
                    is EditorAction.ManageMetaData -> {
                        ExtensionEditMetaOverlay(
                            meta = state.meta,
                            isCreateExt = isCreateExt,
                            onConfirm = { newMeta ->
                                editorState = state.copy(meta = newMeta)
                                editorAction = EditorAction.None
                            },
                            onDismiss = {
                                editorAction = EditorAction.None
                            },
                        )
                    }
                    is EditorAction.ManageDependencies -> {
                        ExtensionEditDependenciesOverlay(
                            dependencies = state.dependencies,
                            onConfirm = { newDependencies ->
                                editorState = state.copy(dependencies = newDependencies)
                                editorAction = EditorAction.None
                            },
                            onDismiss = {
                                editorAction = EditorAction.None
                            },
                        )
                    }
                    is EditorAction.ManageFiles -> {
                        ExtensionEditFilesOverlay(
                            storage = storage,
                            workspaceRef = state.workspaceRef,
                            type = type,
                            onDismiss = {
                                editorAction = EditorAction.None
                            },
                        )
                    }
                    is EditorAction.CreateComponent -> {
                        ExtensionCreateComponentOverlay(
                            extensionId = extensionId,
                            extensionIndex = extensionIndex,
                            components = action.components,
                            componentKlass = action.componentKlass,
                            onCreate = { createFrom, componentName ->
                                handleCreateComponent(
                                    createFrom,
                                    componentName
                                )
                            },
                            onDismiss = {
                                editorAction = EditorAction.None
                            },
                        )
                    }
                    is EditorAction.ManageComponent -> when (action.component) {
                        is ThemeExtension.ThemeComponent -> {
                            // TODO ThemeEditorScreen(workspace, action.editor)
                        }
                        else -> {
                            // Render nothing
                        }
                    }
                    else -> {
                        // Render nothing
                        Box(modifier = Modifier.fillMaxSize())
                    }
                }
            }
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

    if (showInvalidMetadataDialog) {
        JetPrefAlertDialog(
            title = stringRes(R.string.ext__editor__metadata__title_invalid),
            confirmLabel = stringRes(R.string.action__ok),
            onConfirm = {
                showInvalidMetadataDialog = false
            },
            onDismiss = {
                showInvalidMetadataDialog = false
            },
            content = {
                Text(text = stringRes(R.string.ext__editor__metadata__message_invalid))
            },
        )
    }
}
