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

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.app.LocalNavController
import dev.patrickgold.florisboard.app.res.stringRes
import dev.patrickgold.florisboard.app.ui.components.FlorisFullscreenDialog
import dev.patrickgold.florisboard.app.ui.components.FlorisScreen
import dev.patrickgold.florisboard.app.ui.components.florisVerticalScroll
import dev.patrickgold.florisboard.cacheManager
import dev.patrickgold.florisboard.extensionManager
import dev.patrickgold.florisboard.ime.keyboard.KeyboardExtension
import dev.patrickgold.florisboard.ime.spelling.SpellingExtension
import dev.patrickgold.florisboard.ime.theme.ThemeExtension
import dev.patrickgold.florisboard.res.cache.CacheManager
import dev.patrickgold.florisboard.res.ext.Extension
import dev.patrickgold.florisboard.res.ext.ExtensionEditor
import dev.patrickgold.florisboard.res.ext.ExtensionMetaEditor
import dev.patrickgold.jetpref.datastore.ui.Preference
import java.util.*

private val ComponentCardShape = RoundedCornerShape(8.dp)
private val TextFieldVerticalPadding = 8.dp

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

@Composable
private fun EditScreen(ext: Extension, workspace: CacheManager.ExtEditorWorkspace<*>) = FlorisScreen {
    title = stringRes(when (ext) {
        is KeyboardExtension -> R.string.ext__editor__title_keyboard
        is SpellingExtension -> R.string.ext__editor__title_spelling
        is ThemeExtension -> R.string.ext__editor__title_theme
        else -> R.string.ext__editor__title_any
    })
    iconSpaceReserved = false

    val navController = LocalNavController.current
    val context = LocalContext.current
    val extensionManager by context.extensionManager()

    val extensionEditor = workspace.editor ?: return@FlorisScreen
    var dialogMetaEditor by remember { mutableStateOf<ExtensionMetaEditor?>(null) }

    content {
        Preference(
            onClick = { dialogMetaEditor = extensionEditor.meta },
            title = stringRes(R.string.ext__editor__dialog_meta),
            summary = extensionEditor.meta.id,
        )
        Preference(
            title = stringRes(R.string.ext__editor__dialog_dependencies),
            summary = extensionEditor.dependencies.joinToString(),
        )
        Preference(
            title = stringRes(R.string.ext__editor__dialog_files),
            summary = "0",
        )

        Spacer(modifier = Modifier.height(16.dp))
        ExtensionComponentListTitleView(ext)

        if (dialogMetaEditor != null) {
            FlorisFullscreenDialog(
                title = stringRes(R.string.ext__editor__dialog_meta),
                onDismiss = { dialogMetaEditor = null },
            ) {
                val editor = dialogMetaEditor
                if (editor != null) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(horizontal = 8.dp)
                            .florisVerticalScroll(),
                    ) {
                        EditorDialogTextField(
                            value = editor.id,
                            onValueChange = { workspace.update { editor.id = it } },
                            label = stringRes(R.string.ext__meta__id),
                        )
                        EditorDialogTextField(
                            value = editor.version,
                            onValueChange = { workspace.update { editor.version = it } },
                            label = stringRes(R.string.ext__meta__version),
                        )
                        EditorDialogTextField(
                            value = editor.title,
                            onValueChange = { workspace.update { editor.title = it } },
                            label = stringRes(R.string.ext__meta__title),
                        )
                        EditorDialogTextField(
                            value = editor.description,
                            onValueChange = { workspace.update { editor.description = it } },
                            label = stringRes(R.string.ext__meta__description),
                        )
                        EditorDialogTextField(
                            value = editor.homepage,
                            onValueChange = { workspace.update { editor.homepage = it } },
                            label = stringRes(R.string.ext__meta__homepage),
                        )
                        EditorDialogTextField(
                            value = editor.issueTracker,
                            onValueChange = { workspace.update { editor.issueTracker = it } },
                            label = stringRes(R.string.ext__meta__issue_tracker),
                        )
                        EditorDialogTextField(
                            value = editor.license,
                            onValueChange = { workspace.update { editor.license = it } },
                            label = stringRes(R.string.ext__meta__license),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EditorDialogTextField(
    modifier: Modifier = Modifier,
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    isError: Boolean = false,
) {
    TextField(
        modifier = Modifier
            .padding(vertical = TextFieldVerticalPadding)
            .fillMaxWidth(),
        value = value,
        onValueChange = onValueChange,
        label = { Text(text = label) },
        isError = isError,
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
    )
}
