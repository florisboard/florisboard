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

package dev.patrickgold.florisboard.app.ext

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
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.MaterialTheme
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
import androidx.compose.ui.unit.dp
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.app.LocalNavController
import dev.patrickgold.florisboard.app.apptheme.outline
import dev.patrickgold.florisboard.app.settings.advanced.RadioListItem
import dev.patrickgold.florisboard.app.settings.theme.DialogProperty
import dev.patrickgold.florisboard.app.settings.theme.ThemeEditorScreen
import dev.patrickgold.florisboard.cacheManager
import dev.patrickgold.florisboard.extensionManager
import dev.patrickgold.florisboard.ime.keyboard.KeyboardExtension
import dev.patrickgold.florisboard.ime.spelling.SpellingExtension
import dev.patrickgold.florisboard.ime.theme.ThemeExtension
import dev.patrickgold.florisboard.ime.theme.ThemeExtensionComponent
import dev.patrickgold.florisboard.ime.theme.ThemeExtensionComponentEditor
import dev.patrickgold.florisboard.ime.theme.ThemeExtensionComponentImpl
import dev.patrickgold.florisboard.ime.theme.ThemeExtensionEditor
import dev.patrickgold.florisboard.lib.ValidationResult
import dev.patrickgold.florisboard.lib.android.showLongToast
import dev.patrickgold.florisboard.lib.cache.CacheManager
import dev.patrickgold.florisboard.lib.compose.FlorisButtonBar
import dev.patrickgold.florisboard.lib.compose.FlorisIconButton
import dev.patrickgold.florisboard.lib.compose.FlorisInfoCard
import dev.patrickgold.florisboard.lib.compose.FlorisOutlinedBox
import dev.patrickgold.florisboard.lib.compose.FlorisOutlinedTextField
import dev.patrickgold.florisboard.lib.compose.FlorisScreen
import dev.patrickgold.florisboard.lib.compose.FlorisUnsavedChangesDialog
import dev.patrickgold.florisboard.lib.compose.autoMirrorForRtl
import dev.patrickgold.florisboard.lib.compose.defaultFlorisOutlinedBox
import dev.patrickgold.florisboard.lib.compose.stringRes
import dev.patrickgold.florisboard.lib.ext.Extension
import dev.patrickgold.florisboard.lib.ext.ExtensionComponent
import dev.patrickgold.florisboard.lib.ext.ExtensionComponentName
import dev.patrickgold.florisboard.lib.ext.ExtensionDefaults
import dev.patrickgold.florisboard.lib.ext.ExtensionEditor
import dev.patrickgold.florisboard.lib.ext.ExtensionJsonConfig
import dev.patrickgold.florisboard.lib.ext.ExtensionMaintainer
import dev.patrickgold.florisboard.lib.ext.ExtensionManager
import dev.patrickgold.florisboard.lib.ext.ExtensionMeta
import dev.patrickgold.florisboard.lib.ext.ExtensionValidation
import dev.patrickgold.florisboard.lib.ext.validate
import dev.patrickgold.florisboard.lib.io.FlorisRef
import dev.patrickgold.florisboard.lib.io.ZipUtils
import dev.patrickgold.florisboard.lib.io.subFile
import dev.patrickgold.florisboard.lib.io.writeJson
import dev.patrickgold.florisboard.lib.rememberValidationResult
import dev.patrickgold.florisboard.lib.snygg.SnyggStylesheetJsonConfig
import dev.patrickgold.florisboard.themeManager
import dev.patrickgold.jetpref.datastore.ui.Preference
import dev.patrickgold.jetpref.material.ui.JetPrefAlertDialog
import java.util.*
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
                    ManageFilesScreen(workspace)
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

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun EditScreen(
    workspace: CacheManager.ExtEditorWorkspace<*>,
    isCreateExt: Boolean,
) = FlorisScreen {
    title = stringRes(if (isCreateExt) {
        when (workspace.ext) {
            is KeyboardExtension -> R.string.ext__editor__title_create_keyboard
            is SpellingExtension -> R.string.ext__editor__title_create_spelling
            is ThemeExtension -> R.string.ext__editor__title_create_theme
            else -> R.string.ext__editor__title_create_any
        }
    } else {
        when (workspace.ext) {
            is KeyboardExtension -> R.string.ext__editor__title_edit_keyboard
            is SpellingExtension -> R.string.ext__editor__title_edit_spelling
            is ThemeExtension -> R.string.ext__editor__title_edit_theme
            else -> R.string.ext__editor__title_edit_any
        }
    })

    val context = LocalContext.current
    val navController = LocalNavController.current

    val extEditor = workspace.editor ?: return@FlorisScreen
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
        val manifestFile = workspace.saverDir.subFile(ExtensionDefaults.MANIFEST_FILE_NAME)
        manifestFile.writeJson(manifest, ExtensionJsonConfig)
        when (extEditor) {
            is ThemeExtensionEditor -> {
                for (theme in extEditor.themes) {
                    val stylesheetFile = workspace.saverDir.subFile(theme.stylesheetPath())
                    stylesheetFile.parentFile?.mkdirs()
                    val stylesheetEditor = theme.stylesheetEditor
                    if (stylesheetEditor != null) {
                        val stylesheet = stylesheetEditor.build()
                        stylesheetFile.writeJson(stylesheet, SnyggStylesheetJsonConfig)
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
            FlorisRef.internal(ExtensionManager.IME_THEME_PATH).subRef(flexArchiveName)
        } else {
            workspace.ext!!.sourceRef!!
        }
        flexArchiveFile.copyTo(sourceRef.absoluteFile(context), overwrite = true)
        workspace.close()
        navController.popBackStack()
    }

    navigationIcon {
        FlorisIconButton(
            onClick = { handleBackPress() },
            modifier = Modifier.autoMirrorForRtl(),
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
                onClick = { workspace.currentAction = EditorAction.ManageMetaData },
                iconId = R.drawable.ic_code,
                title = stringRes(R.string.ext__editor__metadata__title),
            )
            this@content.Preference(
                onClick = { workspace.currentAction = EditorAction.ManageDependencies },
                iconId = R.drawable.ic_library_books,
                title = stringRes(R.string.ext__editor__dependencies__title),
            )
            this@content.Preference(
                onClick = { workspace.currentAction = EditorAction.ManageFiles },
                iconId = R.drawable.ic_file_blank,
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
                        modifier = Modifier.defaultFlorisOutlinedBox(),
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
}

@Composable
private fun ManageMetaDataScreen(
    workspace: CacheManager.ExtEditorWorkspace<*>,
    isCreateExt: Boolean,
) = FlorisScreen {
    title = stringRes(R.string.ext__editor__metadata__title)

    val meta = workspace.editor?.meta ?: return@FlorisScreen
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
        FlorisIconButton(
            onClick = { handleBackPress() },
            icon = painterResource(R.drawable.ic_close),
        )
    }

    bottomBar {
        FlorisButtonBar {
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
private fun ManageDependenciesScreen(workspace: CacheManager.ExtEditorWorkspace<*>) = FlorisScreen {
    title = stringRes(R.string.ext__editor__dependencies__title)

    val dependencyList = workspace.editor?.dependencies ?: return@FlorisScreen

    fun handleBackPress() {
        workspace.currentAction = null
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
private fun ManageFilesScreen(workspace: CacheManager.ExtEditorWorkspace<*>) = FlorisScreen {
    title = stringRes(R.string.ext__editor__files__title)

    fun handleBackPress() {
        workspace.currentAction = null
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

private enum class CreateFrom {
    EMPTY,
    EXISTING;
}

@Composable
private fun <T : ExtensionComponent> CreateComponentScreen(
    workspace: CacheManager.ExtEditorWorkspace<*>,
    type: KClass<T>,
) = FlorisScreen {
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
                for ((componentName, theme) in themeManager.indexedThemeConfigs.value ?: emptyMap()) {
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
                                context.showLongToast("A theme with this ID already exists!")
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
                                        componentId, c.label, c.authors, c.isNightTheme, c.isBorderless,
                                        c.isMaterialYouAware, stylesheetPath = "",
                                    ).also { it.stylesheetEditor = c.stylesheetEditor }
                                }
                                if (componentEditor.stylesheetEditor != null) {
                                    val stylesheet = componentEditor.stylesheetEditor!!.build()
                                    val stylesheetFile = workspace.extDir.subFile(componentEditor.stylesheetPath())
                                    stylesheetFile.parentFile?.mkdirs()
                                    stylesheetFile.writeJson(stylesheet, SnyggStylesheetJsonConfig)
                                    componentEditor.stylesheetEditor = null
                                } else {
                                    val srcStylesheetFile = workspace.extDir.subFile(component.stylesheetPath())
                                    val dstStylesheetFile = workspace.extDir.subFile(componentEditor.stylesheetPath())
                                    dstStylesheetFile.parentFile?.mkdirs()
                                    srcStylesheetFile.copyTo(dstStylesheetFile, overwrite = true)
                                }
                                editor.themes.add(componentEditor)
                            } else {
                                val component = themeManager.indexedThemeConfigs.value?.get(componentName) ?: return
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
        FlorisIconButton(
            onClick = { handleBackPress() },
            icon = painterResource(R.drawable.ic_close),
        )
    }

    bottomBar {
        FlorisButtonBar {
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

        FlorisOutlinedBox(
            modifier = Modifier.defaultFlorisOutlinedBox(),
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
            FlorisOutlinedBox(
                modifier = Modifier.defaultFlorisOutlinedBox(),
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
            FlorisInfoCard(
                modifier = Modifier.defaultFlorisOutlinedBox(),
                text = stringRes(R.string.ext__editor__create_component__from_empty_warning),
            )
            DialogProperty(
                modifier = Modifier.padding(horizontal = 16.dp),
                text = stringRes(R.string.ext__meta__id),
            ) {
                FlorisOutlinedTextField(
                    value = newId,
                    onValueChange = { newId = it },
                    singleLine = true,
                    showValidationError = showValidationErrors,
                    validationResult = newIdValidation,
                )
            }
            DialogProperty(
                modifier = Modifier.padding(horizontal = 16.dp),
                text = stringRes(R.string.ext__meta__label),
            ) {
                FlorisOutlinedTextField(
                    value = newLabel,
                    onValueChange = { newLabel = it },
                    singleLine = true,
                    showValidationError = showValidationErrors,
                    validationResult = newLabelValidation,
                )
            }
            DialogProperty(
                modifier = Modifier.padding(horizontal = 16.dp),
                text = stringRes(R.string.ext__meta__authors),
            ) {
                FlorisOutlinedTextField(
                    value = newAuthors,
                    onValueChange = { newAuthors = it },
                    showValidationError = showValidationErrors,
                    validationResult = newAuthorsValidation,
                )
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
    val borderColor = MaterialTheme.colors.outline
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
        FlorisOutlinedTextField(
            modifier = modifier.fillMaxWidth(),
            enabled = enabled,
            value = value,
            onValueChange = onValueChange,
            singleLine = singleLine,
            showValidationError = showValidationError,
            validationResult = validationResult,
            colors = TextFieldDefaults.outlinedTextFieldColors(
                unfocusedBorderColor = borderColor,
                disabledBorderColor = borderColor,
            )
        )
    }
}
