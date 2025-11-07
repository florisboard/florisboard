/*
 * Copyright (C) 2021-2025 The OmniBoard Contributors
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

package dev.silo.omniboard.app.ext

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.LibraryBooks
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import dev.silo.omniboard.R
import dev.silo.omniboard.app.LocalNavController
import dev.silo.omniboard.app.settings.advanced.RadioListItem
import dev.silo.omniboard.app.settings.theme.DialogProperty
import dev.silo.omniboard.app.settings.theme.PrettyPrintConfig
import dev.silo.omniboard.app.settings.theme.ThemeEditorScreen
import dev.silo.omniboard.cacheManager
import dev.silo.omniboard.extensionManager
import dev.silo.omniboard.ime.keyboard.KeyboardExtension
import dev.silo.omniboard.ime.theme.ThemeExtension
import dev.silo.omniboard.ime.theme.ThemeExtensionComponent
import dev.silo.omniboard.ime.theme.ThemeExtensionComponentEditor
import dev.silo.omniboard.ime.theme.ThemeExtensionComponentImpl
import dev.silo.omniboard.ime.theme.ThemeExtensionEditor
import dev.silo.omniboard.lib.ValidationResult
import dev.silo.omniboard.lib.cache.CacheManager
import dev.silo.omniboard.lib.compose.OmniScreen
import dev.silo.omniboard.lib.compose.OmniUnsavedChangesDialog
import dev.silo.omniboard.lib.compose.Validation
import dev.silo.omniboard.lib.ext.Extension
import dev.silo.omniboard.lib.ext.ExtensionComponent
import dev.silo.omniboard.lib.ext.ExtensionComponentName
import dev.silo.omniboard.lib.ext.ExtensionDefaults
import dev.silo.omniboard.lib.ext.ExtensionEditor
import dev.silo.omniboard.lib.ext.ExtensionJsonConfig
import dev.silo.omniboard.lib.ext.ExtensionMaintainer
import dev.silo.omniboard.lib.ext.ExtensionManager
import dev.silo.omniboard.lib.ext.ExtensionMeta
import dev.silo.omniboard.lib.ext.ExtensionValidation
import dev.silo.omniboard.lib.ext.validate
import dev.silo.omniboard.lib.io.OmniRef
import dev.silo.omniboard.lib.io.ZipUtils
import dev.silo.omniboard.lib.rememberValidationResult
import dev.silo.omniboard.themeManager
import dev.silo.jetpref.datastore.ui.Preference
import dev.silo.jetpref.material.ui.JetPrefAlertDialog
import dev.silo.jetpref.material.ui.JetPrefTextField
import java.util.*
import org.omniboard.lib.compose.OmniButtonBar
import org.omniboard.lib.compose.OmniIconButton
import org.omniboard.lib.compose.OmniInfoCard
import org.omniboard.lib.compose.OmniOutlinedBox
import org.omniboard.lib.compose.defaultOmniOutlinedBox
import org.omniboard.lib.compose.stringRes
import org.omniboard.lib.android.showLongToastSync
import org.omniboard.lib.kotlin.io.deleteContentsRecursively
import org.omniboard.lib.kotlin.io.subDir
import org.omniboard.lib.kotlin.io.subFile
import org.omniboard.lib.kotlin.io.writeJson
import kotlin.reflect.KClass

private val TextFieldVerticalPadding = 8.dp
private val MetaDataContentPadding = PaddingValues(vertical = 8.dp, horizontal = 16.dp)

private const val AnimationDuration = 300

private val ActionScreenEnterTransition = fadeIn(tween(AnimationDuration))
private val ActionScreenExitTransition = fadeOut(tween(AnimationDuration))

sealed class EditorAction {
    object ManageMetaData : EditorAction()

    object ManageDependencies : EditorAction()

    object ManageFiles : EditorAction()

    data class CreateComponent<T : ExtensionComponent>(val type: KClass<T>) : EditorAction()

    data class ManageComponent(val editor: ExtensionComponent) : EditorAction()
}

@Composable
fun ExtensionEditScreen(id: String, createSerialType: String?) {
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
            if (createSerialType == null) {
                checkNotNull(sourceRef) { "Extension source ref must not be null" }
                ZipUtils.unzip(context, sourceRef, newWorkspace.extDir)
            }
            newWorkspace.ext = ext
            newWorkspace.editor = ext.edit() as? T
        }
    }

    val ext = extensionManager.getExtensionById(id) ?: remember {
        val meta = ExtensionMeta(
            id = ExtensionDefaults.createLocalId("themes", System.currentTimeMillis().toString()),
            version = "0.0.0",
            title = "My themes",
            maintainers = listOf(ExtensionMaintainer(name = "Local")),
            license = "(none specified)",
        )
        when (createSerialType) {
            ThemeExtension.SERIAL_TYPE -> ThemeExtension(meta, null, emptyList())
            else -> null
        }
    }
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
                ExtensionEditScreenSheetSwitcher(workspace, isCreateExt = createSerialType != null)
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
private fun ExtensionEditScreenSheetSwitcher(
    workspace: CacheManager.ExtEditorWorkspace<*>,
    isCreateExt: Boolean,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        EditScreen(workspace, isCreateExt)
        AnimatedVisibility(
            visible = workspace.currentAction != null,
            enter = ActionScreenEnterTransition,
            exit = ActionScreenExitTransition,
        ) {
            when (val action = workspace.currentAction) {
                is EditorAction.ManageMetaData -> {
                    ManageMetaDataScreen(workspace, isCreateExt)
                }
                is EditorAction.ManageDependencies -> {
                    ManageDependenciesScreen(workspace)
                }
                is EditorAction.ManageFiles -> {
                    ExtensionEditFilesScreen(workspace)
                }
                is EditorAction.CreateComponent<*> -> {
                    CreateComponentScreen(workspace, action.type)
                }
                is EditorAction.ManageComponent -> when (action.editor) {
                    is ThemeExtensionComponentEditor -> {
                        ThemeEditorScreen(workspace, action.editor)
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

@Composable
private fun EditScreen(
    workspace: CacheManager.ExtEditorWorkspace<*>,
    isCreateExt: Boolean,
) = OmniScreen {
    title = stringRes(if (isCreateExt) {
        when (workspace.ext) {
            is KeyboardExtension -> R.string.ext__editor__title_create_keyboard
            is ThemeExtension -> R.string.ext__editor__title_create_theme
            else -> R.string.ext__editor__title_create_any
        }
    } else {
        when (workspace.ext) {
            is KeyboardExtension -> R.string.ext__editor__title_edit_keyboard
            is ThemeExtension -> R.string.ext__editor__title_edit_theme
            else -> R.string.ext__editor__title_edit_any
        }
    })

    val context = LocalContext.current
    val navController = LocalNavController.current

    val extEditor = workspace.editor ?: return@OmniScreen
    var showUnsavedChangesDialog by remember { mutableStateOf(false) }
    var showInvalidMetadataDialog by remember { mutableStateOf(false) }

    fun handleBackPress() {
        if (workspace.isModified) {
            showUnsavedChangesDialog = true
        } else {
            workspace.close()
            navController.popBackStack()
        }
    }

    fun handleSave() {
        if (!extEditor.meta.validate()) {
            showUnsavedChangesDialog = false
            showInvalidMetadataDialog = true
            return
        }
        val manifest = extEditor.build()
        workspace.saverDir.deleteContentsRecursively()
        val manifestFile = workspace.saverDir.subFile(ExtensionDefaults.MANIFEST_FILE_NAME)
        manifestFile.writeJson(manifest, ExtensionJsonConfig)
        when (extEditor) {
            is ThemeExtensionEditor -> {
                // TODO: this is hacky
                val fonts = workspace.extDir.subDir("fonts")
                if (fonts.exists()) {
                    fonts.copyRecursively(workspace.saverDir.subDir("fonts"), overwrite = true)
                }
                val images = workspace.extDir.subDir("images")
                if (images.exists()) {
                    images.copyRecursively(workspace.saverDir.subDir("images"), overwrite = true)
                }
                for (theme in extEditor.themes) {
                    val stylesheetFile = workspace.saverDir.subFile(theme.stylesheetPath())
                    stylesheetFile.parentFile?.mkdirs()
                    val stylesheetEditor = theme.stylesheetEditor
                    if (stylesheetEditor != null) {
                        runCatching {
                            val stylesheet = stylesheetEditor.build().toJson(PrettyPrintConfig).getOrThrow()
                            stylesheetFile.writeText(stylesheet)
                        }.onFailure {
                            // TODO: better error handling
                            context.showLongToastSync(it.message.toString())
                            return
                        }
                    } else {
                        val unmodifiedStylesheetFile = workspace.extDir.subFile(theme.stylesheetPath())
                        if (unmodifiedStylesheetFile.exists()) {
                            unmodifiedStylesheetFile.copyTo(stylesheetFile, overwrite = true)
                        }
                    }
                }
            }
            else -> { }
        }
        val flexArchiveName = ExtensionDefaults.createFlexName(extEditor.meta.id)
        val flexArchiveFile = workspace.dir.subFile(flexArchiveName)
        ZipUtils.zip(workspace.saverDir, flexArchiveFile)
        val sourceRef = if (isCreateExt) {
            OmniRef.internal(ExtensionManager.IME_THEME_PATH).subRef(flexArchiveName)
        } else {
            workspace.ext!!.sourceRef!!
        }
        flexArchiveFile.copyTo(sourceRef.absoluteFile(context), overwrite = true)
        workspace.close()
        navController.popBackStack()
    }

    navigationIcon {
        OmniIconButton(
            onClick = { handleBackPress() },
            icon = Icons.AutoMirrored.Filled.ArrowBack,
        )
    }

    bottomBar {
        OmniButtonBar {
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

        OmniOutlinedBox(
            modifier = Modifier.defaultOmniOutlinedBox(),
        ) {
            Preference(
                onClick = { workspace.currentAction = EditorAction.ManageMetaData },
                icon = Icons.Default.Code,
                title = stringRes(R.string.ext__editor__metadata__title),
            )
            Preference(
                onClick = { workspace.currentAction = EditorAction.ManageDependencies },
                icon = Icons.AutoMirrored.Outlined.LibraryBooks,
                title = stringRes(R.string.ext__editor__dependencies__title),
            )
            Preference(
                onClick = { workspace.currentAction = EditorAction.ManageFiles },
                icon = ImageVector.vectorResource(R.drawable.ic_file_blank),
                title = stringRes(R.string.ext__editor__files__title),
            )
        }

        when (extEditor) {
            is ThemeExtensionEditor -> {
                ExtensionComponentListView(
                    title = stringRes(R.string.ext__meta__components_theme),
                    components = extEditor.themes,
                    onCreateBtnClick = {
                        workspace.currentAction = EditorAction.CreateComponent(ThemeExtensionComponent::class)
                    },
                ) { component ->
                    ExtensionComponentView(
                        modifier = Modifier.defaultOmniOutlinedBox(),
                        meta = extEditor.meta,
                        component = component,
                        onDeleteBtnClick = { workspace.update { extEditor.themes.remove(component) } },
                        onEditBtnClick = { workspace.currentAction = EditorAction.ManageComponent(component) },
                    )
                }
            }
            else -> {
                // Render nothing
            }
        }

        if (showUnsavedChangesDialog) {
            OmniUnsavedChangesDialog(
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
}

@Composable
private fun ManageMetaDataScreen(
    workspace: CacheManager.ExtEditorWorkspace<*>,
    isCreateExt: Boolean,
) = OmniScreen {
    title = stringRes(R.string.ext__editor__metadata__title)

    val meta = workspace.editor?.meta ?: return@OmniScreen
    var showValidationErrors by rememberSaveable { mutableStateOf(false) }

    var id by rememberSaveable { mutableStateOf(meta.id) }
    val idValidation = rememberValidationResult(ExtensionValidation.MetaId, id)
    var version by rememberSaveable { mutableStateOf(meta.version) }
    val versionValidation = rememberValidationResult(ExtensionValidation.MetaVersion, version)
    var title by rememberSaveable { mutableStateOf(meta.title) }
    val titleValidation = rememberValidationResult(ExtensionValidation.MetaTitle, title)
    var description by rememberSaveable { mutableStateOf(meta.description ?: "") }
    var keywords by rememberSaveable { mutableStateOf(meta.keywords?.joinToString("\n") ?: "") }
    var homepage by rememberSaveable { mutableStateOf(meta.homepage ?: "") }
    var issueTracker by rememberSaveable { mutableStateOf(meta.issueTracker ?: "") }
    var maintainers by rememberSaveable { mutableStateOf(meta.maintainers.joinToString("\n")) }
    val maintainersValidation = rememberValidationResult(ExtensionValidation.MetaMaintainers, maintainers)
    var license by rememberSaveable { mutableStateOf(meta.license) }
    val licenseValidation = rememberValidationResult(ExtensionValidation.MetaLicense, license)

    fun handleBackPress() {
        workspace.currentAction = null
    }

    fun handleApply() {
        val invalid = idValidation.isInvalid() ||
            versionValidation.isInvalid() ||
            titleValidation.isInvalid() ||
            maintainersValidation.isInvalid() ||
            licenseValidation.isInvalid()
        if (invalid) {
            showValidationErrors = true
        } else {
            workspace.update {
                workspace.editor?.meta = ExtensionMeta(
                    id = id.trim(),
                    version = version.trim(),
                    title = title.trim(),
                    description = description.trim().takeIf { it.isNotBlank() },
                    keywords = keywords.lines().map { it.trim() }.filter { it.isNotBlank() }.takeIf { it.isNotEmpty() },
                    homepage = homepage.trim().takeIf { it.isNotBlank() },
                    issueTracker = issueTracker.trim().takeIf { it.isNotBlank() },
                    maintainers = maintainers.lines().map { it.trim() }.filter { it.isNotBlank() }
                        .map { ExtensionMaintainer.fromOrTakeRaw(it) },
                    license = license.trim(),
                )
            }
            workspace.currentAction = null
        }
    }

    navigationIcon {
        OmniIconButton(
            onClick = { handleBackPress() },
            icon = Icons.Default.Close,
        )
    }

    bottomBar {
        OmniButtonBar {
            ButtonBarSpacer()
            ButtonBarTextButton(text = stringRes(R.string.action__cancel)) {
                handleBackPress()
            }
            ButtonBarButton(text = stringRes(R.string.action__apply)) {
                handleApply()
            }
        }
    }

    content {
        BackHandler {
            handleBackPress()
        }

        Column(modifier = Modifier.padding(MetaDataContentPadding)) {
            EditorSheetTextField(
                enabled = isCreateExt,
                isRequired = true,
                value = id,
                onValueChange = { id = it },
                label = stringRes(R.string.ext__meta__id),
                showValidationError = showValidationErrors,
                validationResult = idValidation,
            )
            EditorSheetTextField(
                isRequired = true,
                value = version,
                onValueChange = { version = it },
                label = stringRes(R.string.ext__meta__version),
                showValidationError = showValidationErrors,
                validationResult = versionValidation,
            )
            EditorSheetTextField(
                isRequired = true,
                value = title,
                onValueChange = { title = it },
                label = stringRes(R.string.ext__meta__title),
                showValidationError = showValidationErrors,
                validationResult = titleValidation,
            )
            EditorSheetTextField(
                value = description,
                onValueChange = { description = it },
                label = stringRes(R.string.ext__meta__description),
            )
            EditorSheetTextField(
                value = keywords,
                onValueChange = { keywords = it },
                label = stringRes(R.string.ext__meta__keywords),
                singleLine = false,
            )
            EditorSheetTextField(
                value = homepage,
                onValueChange = { homepage = it },
                label = stringRes(R.string.ext__meta__homepage),
            )
            EditorSheetTextField(
                value = issueTracker,
                onValueChange = { issueTracker = it },
                label = stringRes(R.string.ext__meta__issue_tracker),
            )
            EditorSheetTextField(
                isRequired = true,
                value = maintainers,
                onValueChange = { maintainers = it },
                label = stringRes(R.string.ext__meta__maintainers),
                singleLine = false,
                showValidationError = showValidationErrors,
                validationResult = maintainersValidation,
            )
            EditorSheetTextField(
                isRequired = true,
                value = license,
                onValueChange = { license = it },
                label = stringRes(R.string.ext__meta__license),
                showValidationError = showValidationErrors,
                validationResult = licenseValidation,
            )
        }
    }
}

@Composable
private fun ManageDependenciesScreen(workspace: CacheManager.ExtEditorWorkspace<*>) = OmniScreen {
    title = stringRes(R.string.ext__editor__dependencies__title)

    val dependencyList = workspace.editor?.dependencies ?: return@OmniScreen

    fun handleBackPress() {
        workspace.currentAction = null
    }

    navigationIcon {
        OmniIconButton(
            onClick = { handleBackPress() },
            icon = Icons.Default.Close,
        )
    }

    content {
        BackHandler {
            handleBackPress()
        }

        OmniInfoCard(
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

private enum class CreateFrom {
    EMPTY,
    EXISTING;
}

@Composable
private fun <T : ExtensionComponent> CreateComponentScreen(
    workspace: CacheManager.ExtEditorWorkspace<*>,
    type: KClass<T>,
) = OmniScreen {
    title = stringRes(when (type) {
        ThemeExtensionComponent::class -> R.string.ext__editor__create_component__title_theme
        else -> R.string.ext__editor__create_component__title
    })

    val context = LocalContext.current
    val extensionManager by context.extensionManager()
    val themeManager by context.themeManager()

    var createFrom by rememberSaveable { mutableStateOf(CreateFrom.EXISTING) }
    val extId = workspace.editor?.meta?.id ?: "null"
    val components = remember<Map<ExtensionComponentName, ExtensionComponent>> {
        when (val editor = workspace.editor) {
            is ThemeExtensionEditor -> buildMap {
                for (theme in editor.themes) {
                    put(ExtensionComponentName(extId, theme.id), theme)
                }
                for ((componentName, theme) in themeManager.indexedThemeConfigs.value.first) {
                    if (componentName.extensionId != extId) {
                        put(componentName, theme)
                    }
                }
            }
            else -> {
                emptyMap()
            }
        }
    }
    var selectedComponentName by rememberSaveable(stateSaver = ExtensionComponentName.Saver) {
        mutableStateOf(null)
    }
    var showValidationErrors by rememberSaveable { mutableStateOf(false) }

    var newId by rememberSaveable { mutableStateOf("") }
    val newIdValidation = rememberValidationResult(ExtensionValidation.ComponentId, newId)
    var newLabel by rememberSaveable { mutableStateOf("") }
    val newLabelValidation = rememberValidationResult(ExtensionValidation.ComponentLabel, newLabel)
    var newAuthors by rememberSaveable { mutableStateOf("") }
    val newAuthorsValidation = rememberValidationResult(ExtensionValidation.ComponentAuthors, newAuthors)

    fun handleBackPress() {
        workspace.currentAction = null
    }

    fun handleCreate() {
        val invalid = createFrom == CreateFrom.EMPTY && (newIdValidation.isInvalid() ||
            newLabelValidation.isInvalid() || newAuthorsValidation.isInvalid())
        if (invalid) {
            showValidationErrors = true
        } else {
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
        }
    }

    fun hasSufficientInfoForCreating(): Boolean {
        return when (createFrom) {
            CreateFrom.EMPTY -> newId.isNotBlank() && newLabel.isNotBlank() && newAuthors.isNotBlank()
            CreateFrom.EXISTING -> components.containsKey(selectedComponentName)
        }
    }

    navigationIcon {
        OmniIconButton(
            onClick = { handleBackPress() },
            icon = Icons.Default.Close,
        )
    }

    bottomBar {
        OmniButtonBar {
            ButtonBarSpacer()
            ButtonBarTextButton(text = stringRes(R.string.action__cancel)) {
                handleBackPress()
            }
            ButtonBarButton(
                text = stringRes(R.string.action__create),
                enabled = hasSufficientInfoForCreating(),
            ) {
                handleCreate()
            }
        }
    }

    content {
        BackHandler {
            handleBackPress()
        }

        OmniOutlinedBox(
            modifier = Modifier.defaultOmniOutlinedBox(),
        ) {
            RadioListItem(
                onClick = { createFrom = CreateFrom.EXISTING },
                selected = createFrom == CreateFrom.EXISTING,
                text = stringRes(R.string.ext__editor__create_component__from_existing),
            )
            RadioListItem(
                onClick = { createFrom = CreateFrom.EMPTY },
                selected = createFrom == CreateFrom.EMPTY,
                text = stringRes(R.string.ext__editor__create_component__from_empty),
            )
        }

        if (createFrom == CreateFrom.EXISTING) {
            OmniOutlinedBox(
                modifier = Modifier.defaultOmniOutlinedBox(),
            ) {
                for ((componentName, component) in components) {
                    RadioListItem(
                        onClick = { selectedComponentName = componentName },
                        selected = selectedComponentName == componentName,
                        text = component.label,
                        secondaryText = componentName.toString(),
                    )
                }
            }
        } else if (createFrom == CreateFrom.EMPTY) {
            OmniInfoCard(
                modifier = Modifier.defaultOmniOutlinedBox(),
                text = stringRes(R.string.ext__editor__create_component__from_empty_warning),
            )
            DialogProperty(
                modifier = Modifier.padding(horizontal = 16.dp),
                text = stringRes(R.string.ext__meta__id),
            ) {
                JetPrefTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = newId,
                    onValueChange = { newId = it },
                    singleLine = true,
                )
                Validation(showValidationErrors, newIdValidation)
            }
            DialogProperty(
                modifier = Modifier.padding(horizontal = 16.dp),
                text = stringRes(R.string.ext__meta__label),
            ) {
                JetPrefTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = newLabel,
                    onValueChange = { newLabel = it },
                    singleLine = true,
                )
                Validation(showValidationErrors, newLabelValidation)

            }
            DialogProperty(
                modifier = Modifier.padding(horizontal = 16.dp),
                text = stringRes(R.string.ext__meta__authors),
            ) {
                JetPrefTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = newAuthors,
                    onValueChange = { newAuthors = it },
                )
                Validation(showValidationErrors, newAuthorsValidation)
            }
        }
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
    singleLine: Boolean = true,
    showValidationError: Boolean = false,
    validationResult: ValidationResult? = null,
) {
    Column(modifier = Modifier.padding(vertical = TextFieldVerticalPadding)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = TextFieldVerticalPadding),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
            )
            if (isRequired) {
                Text(
                    modifier = Modifier.padding(start = 2.dp),
                    text = "*",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
        JetPrefTextField(
            modifier = modifier.fillMaxWidth(),
            enabled = enabled,
            value = value,
            onValueChange = onValueChange,
            singleLine = singleLine,
        )
        Validation(showValidationError, validationResult)
    }
}
