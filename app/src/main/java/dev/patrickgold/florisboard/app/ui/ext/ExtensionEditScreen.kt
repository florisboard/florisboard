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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.app.LocalNavController
import dev.patrickgold.florisboard.app.res.stringRes
import dev.patrickgold.florisboard.app.ui.components.FlorisScreenWithBottomSheet
import dev.patrickgold.florisboard.app.ui.components.florisVerticalScroll
import dev.patrickgold.florisboard.cacheManager
import dev.patrickgold.florisboard.extensionManager
import dev.patrickgold.florisboard.ime.keyboard.KeyboardExtension
import dev.patrickgold.florisboard.ime.spelling.SpellingExtension
import dev.patrickgold.florisboard.ime.theme.ThemeExtension
import dev.patrickgold.florisboard.res.cache.CacheManager
import dev.patrickgold.florisboard.res.ext.Extension
import dev.patrickgold.florisboard.res.ext.ExtensionEditor
import dev.patrickgold.florisboard.res.ext.ExtensionMaintainer
import dev.patrickgold.jetpref.datastore.ui.Preference
import kotlinx.coroutines.launch
import java.util.*

private val TextFieldVerticalPadding = 8.dp

private sealed class SheetAction {
    object ManageMetaData : SheetAction()

    object ManageDependencies : SheetAction()

    object ManageFiles : SheetAction()

    data class ManageComponent(val id: String) : SheetAction()
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
        return workspace ?: container.new(uuid).also { it.editor = ext.edit() as? T }
    }

    val ext = extensionManager.getExtensionById(id)
    if (ext != null) {
        val uuid = rememberSaveable { UUID.randomUUID().toString() }
        val cacheWorkspace = remember {
            when (ext) {
                is ThemeExtension -> {
                    getOrCreateWorkspace(uuid, cacheManager.themeExtEditor, ext)
                }
                else -> null
            }
        }
        if (cacheWorkspace?.editor != null) {
            EditScreen(ext, cacheWorkspace)
        } else {
            ExtensionNotFoundScreen(id = id)
        }
    } else {
        ExtensionNotFoundScreen(id)
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun EditScreen(ext: Extension, workspace: CacheManager.ExtEditorWorkspace<*>) = FlorisScreenWithBottomSheet {
    title = stringRes(when (ext) {
        is KeyboardExtension -> R.string.ext__editor__title_keyboard
        is SpellingExtension -> R.string.ext__editor__title_spelling
        is ThemeExtension -> R.string.ext__editor__title_theme
        else -> R.string.ext__editor__title_any
    })
    iconSpaceReserved = false

    val focusManager = LocalFocusManager.current
    val navController = LocalNavController.current
    val context = LocalContext.current
    val extensionManager by context.extensionManager()

    val scope = rememberCoroutineScope()
    val extensionEditor = workspace.editor ?: return@FlorisScreenWithBottomSheet
    var sheetAction by remember { mutableStateOf<SheetAction?>(null) }

    fun setSheetAction(action: SheetAction?) {
        if (action != null) {
            sheetAction = action
            scope.launch {
                bottomSheetScaffoldState.bottomSheetState.expand()
            }
        } else {
            focusManager.clearFocus(force = true)
            scope.launch {
                bottomSheetScaffoldState.bottomSheetState.collapse()
                sheetAction = null
            }
        }
    }

    content {
        BackHandler {
            if (sheetAction != null) {
                setSheetAction(null)
            } else {
                navController.popBackStack()
            }
        }

        Preference(
            onClick = { setSheetAction(SheetAction.ManageMetaData) },
            title = stringRes(R.string.ext__editor__sheet_meta),
            summary = extensionEditor.meta.id,
        )
        Preference(
            title = stringRes(R.string.ext__editor__sheet_dependencies),
            summary = extensionEditor.dependencies.joinToString(),
        )
        Preference(
            title = stringRes(R.string.ext__editor__sheet_files),
            summary = "0",
        )

        Spacer(modifier = Modifier.height(16.dp))
        ExtensionComponentListTitleView(ext)
    }

    bottomSheet {
        when (sheetAction) {
            is SheetAction.ManageMetaData -> {
                val editor = extensionEditor.meta
                EditorSheetAppBar(
                    onDismiss = { setSheetAction(null) },
                    title = stringRes(R.string.ext__editor__sheet_meta),
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .florisVerticalScroll()
                        .padding(horizontal = 8.dp),
                ) {
                    EditorSheetTextField(
                        enabled = false,
                        value = editor.id,
                        onValueChange = { },
                        label = stringRes(R.string.ext__meta__id),
                    )
                    EditorSheetTextField(
                        value = editor.version,
                        onValueChange = { workspace.update { editor.version = it } },
                        label = stringRes(R.string.ext__meta__version),
                    )
                    EditorSheetTextField(
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
                        value = editor.license,
                        onValueChange = { workspace.update { editor.license = it } },
                        label = stringRes(R.string.ext__meta__license),
                    )
                }
            }
            else -> { }
        }
    }
}

@Composable
private fun EditorSheetAppBar(
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit,
    title: String,
) {
    TopAppBar(
        navigationIcon = {
            IconButton(
                onClick = onDismiss,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_close),
                    contentDescription = "Close",
                )
            }
        },
        title = {
            Text(
                text = title,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
            )
        },
        backgroundColor = Color.Transparent,
        elevation = 0.dp,
    )
}

@Composable
private fun EditorSheetTextField(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    isError: Boolean = false,
) {
    OutlinedTextField(
        modifier = Modifier
            .padding(vertical = TextFieldVerticalPadding)
            .fillMaxWidth(),
        enabled = enabled,
        value = value,
        onValueChange = onValueChange,
        label = { Text(text = label) },
        isError = isError,
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
    )
}
