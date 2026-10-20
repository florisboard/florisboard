/*
 * Copyright (C) 2025-2026 The FlorisBoard Contributors
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

import android.provider.OpenableColumns
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSerializable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.app.components.ScrollableScreenColumn
import dev.patrickgold.florisboard.app.components.SegmentedListColumn
import dev.patrickgold.florisboard.ime.extension.ExtensionType
import dev.patrickgold.florisboard.ime.io.AndroidStorage
import dev.patrickgold.florisboard.ime.io.FlorisRef
import dev.patrickgold.florisboard.ime.io.Storage
import dev.patrickgold.florisboard.ime.io.readFromUri
import dev.patrickgold.florisboard.ime.keyboard3.interaction.LocalInteractionController
import dev.patrickgold.florisboard.ime.keyboard3.interaction.showLongToast
import dev.patrickgold.florisboard.ime.keyboard3.interaction.showShortToast
import dev.patrickgold.florisboard.lib.compose.FlorisScreenNg
import dev.patrickgold.jetpref.material.ui.JetPrefAlertDialog
import dev.patrickgold.jetpref.material.ui.JetPrefTextField
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import okio.IOException
import org.florisboard.lib.android.query
import org.florisboard.lib.compose.stringRes
import org.florisboard.lib.kotlin.mimeTypeFilterOf
import java.util.*

const val FONTS = "fonts"
const val IMAGES = "images"

val MIME_TYPES = mapOf(
    FONTS to mimeTypeFilterOf(
        // Source: https://www.alienfactory.co.uk/articles/mime-types-for-web-fonts-in-bedsheet#mimeTypes
        "font/*",
        "application/font-*",
        "application/x-font-*",
        "application/vnd.ms-fontobject",
    ),
    IMAGES to mimeTypeFilterOf(
        "image/*",
    ),
)

@Composable
fun ExtensionEditFilesOverlay(
    storage: Storage,
    workspaceRef: FlorisRef,
    type: ExtensionType,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val interactionController = LocalInteractionController.current

    val scope = rememberCoroutineScope()
    var version by rememberSerializable { mutableIntStateOf(0) }
    var fontFiles by remember { mutableStateOf(emptyList<FlorisRef>()) }
    var imageFiles by remember { mutableStateOf(emptyList<FlorisRef>()) }

    LaunchedEffect(storage, version) {
        try {
            if (type == ExtensionType.THEME) {
                fontFiles = storage.listOrNull(workspaceRef.subRef(FONTS)).orEmpty()
                    .filter { storage.metadataOrNull(it)?.isRegularFile == true }
                imageFiles = storage.listOrNull(workspaceRef.subRef(IMAGES)).orEmpty()
                    .filter { storage.metadataOrNull(it)?.isRegularFile == true }
            }
        } catch (_: IOException) {
            fontFiles = emptyList()
            imageFiles = emptyList()
        }
    }

    var currentImportDest by remember { mutableStateOf<String?>(null) }
    var currentImportResult by remember { mutableStateOf<Result<Pair<FlorisRef, String>>?>(null) }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri ->
            currentImportResult = runCatching {
                checkNotNull(uri) { "" }
                val mimeType = context.contentResolver.getType(uri)
                val filter = MIME_TYPES[currentImportDest!!]!!
                check(filter.matches(mimeType)) {
                    "Given file mime type was '$mimeType', expected one of ${filter.types}"
                }
                val dirRef = workspaceRef.subRef(currentImportDest!!)
                storage.mkdirs(dirRef)
                val tempFileRef = dirRef.subRef("temp_${UUID.randomUUID()}")
                (storage as AndroidStorage).readFromUri(uri, tempFileRef)
                val fileName = context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME)).use { cursor ->
                    if (cursor == null || !cursor.moveToFirst()) return@use null
                    val name = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    cursor.getString(name)
                }
                tempFileRef to fileName.orEmpty()
            }
        },
    )

    LaunchedEffect(currentImportResult) {
        val message = currentImportResult?.exceptionOrNull()?.message
        if (!message.isNullOrBlank()) {
            interactionController.showLongToast(message)
        }
    }

    BackHandler {
        onDismiss()
    }

    FlorisScreenNg(
        title = stringRes(R.string.ext__editor__files__title),
        previewFieldVisible = false,
        navigationIcon = Icons.Default.Close,
        navigationIconOnClick = onDismiss,
    ) { contentPadding ->
        ScrollableScreenColumn(contentPadding) {
            if (type == ExtensionType.THEME) {
                FileListColumn(
                    title = stringRes(R.string.ext__editor__files__type_fonts),
                    icon = Icons.Default.TextFields,
                    fileRefs = fontFiles,
                    storage = storage,
                    scope = scope,
                    onAdd = {
                        currentImportDest = FONTS
                        importLauncher.launch("*/*")
                    },
                    onFilesChanged = { version++ },
                )
                FileListColumn(
                    title = stringRes(R.string.ext__editor__files__type_images),
                    icon = Icons.Default.Photo,
                    fileRefs = imageFiles,
                    storage = storage,
                    scope = scope,
                    onAdd = {
                        currentImportDest = IMAGES
                        importLauncher.launch("*/*")
                    },
                    onFilesChanged = { version++ },
                )
            }
        }
    }

    val dest = currentImportDest
    val result = currentImportResult?.getOrNull()
    if (dest != null && result != null) {
        var fileNameInput by rememberSerializable { mutableStateOf(result.second) }
        JetPrefAlertDialog(
            title = stringRes(R.string.action__import_file),
            confirmLabel = stringRes(R.string.action__add),
            onConfirm = {
                scope.launch {
                    val fileName = fileNameInput.trim()
                    val dirRef = workspaceRef.subRef(dest)
                    storage.mkdirs(dirRef)
                    val fileRef = dirRef.subRef(fileName)
                    if (fileRef.parentRef != workspaceRef.subRef(dest)) {
                        interactionController.showShortToast("Invalid file name")
                    } else if (storage.exists(fileRef)) {
                        interactionController.showShortToast("File already exists")
                    } else {
                        val tempFileRef = result.first
                        try {
                            storage.atomicMove(tempFileRef, fileRef)
                        } catch (_: IOException) {
                            interactionController.showShortToast("Failed to rename file")
                            storage.delete(tempFileRef)
                        }
                        currentImportDest = null
                        currentImportResult = null
                        version++
                    }
                }
            },
            dismissLabel = stringRes(R.string.action__cancel),
            onDismiss = {
                val tempFileRef = result.first
                storage.delete(tempFileRef)
                currentImportDest = null
                currentImportResult = null
            },
        ) {
            JetPrefTextField(
                value = fileNameInput,
                onValueChange = { fileNameInput = it },
                singleLine = true,
            )
        }
    }
}

@Composable
private fun FileListColumn(
    title: String,
    icon: ImageVector,
    fileRefs: List<FlorisRef>,
    storage: Storage,
    scope: CoroutineScope,
    onAdd: () -> Unit,
    onFilesChanged: () -> Unit,
) {
    val interactionController = LocalInteractionController.current
    var dialogFileRef by remember { mutableStateOf<FlorisRef?>(null) }

    SegmentedListColumn(title = title) {
        val count = fileRefs.size + 1
        for ((index, fileRef) in fileRefs.withIndex()) {
            SegmentedListItem(
                onClick = { dialogFileRef = fileRef },
                shapes = ListItemDefaults.segmentedShapes(index, count),
                leadingContent = { Icon(icon, null) },
                content = { Text(fileRef.okioPathName) },
            )
        }
        SegmentedListItem(
            onClick = onAdd,
            shapes = ListItemDefaults.segmentedShapes(count - 1, count),
            leadingContent = { Icon(Icons.Default.Add, null) },
            content = { Text(stringRes(R.string.action__add)) },
        )
    }

    dialogFileRef?.let { fileRef ->
        var fileNameInput by rememberSerializable { mutableStateOf(fileRef.okioPathName) }
        JetPrefAlertDialog(
            title = stringRes(R.string.general__properties),
            confirmLabel = stringRes(R.string.action__apply),
            dismissLabel = stringRes(R.string.action__cancel),
            neutralLabel = stringRes(R.string.action__delete),
            allowOutsideDismissal = true,
            onNeutral = {
                scope.launch {
                    try {
                        storage.delete(fileRef)
                        interactionController.showShortToast("Successfully deleted")
                    } catch (_: IOException) {
                        interactionController.showShortToast("Failed to delete")
                    }
                    dialogFileRef = null
                    onFilesChanged()
                }
            },
            onConfirm = {
                scope.launch {
                    val newFileRef = fileRef.parentRef!!.subRef(fileNameInput.trim())
                    if (newFileRef.parentRef != fileRef.parentRef) {
                        interactionController.showLongToast("Invalid file name!")
                        return@launch
                    }
                    if (storage.exists(newFileRef)) {
                        interactionController.showShortToast("Filename already exists.")
                        return@launch
                    }
                    try {
                        storage.atomicMove(fileRef, newFileRef)
                        interactionController.showShortToast("Successfully renamed")
                    } catch (_: IOException) {
                        interactionController.showShortToast("Failed to rename the file.")
                    }
                    dialogFileRef = null
                    onFilesChanged()
                }
            },
            onDismiss = {
                dialogFileRef = null
            },
        ) {
            JetPrefTextField(
                labelText = stringRes(R.string.general__file_name),
                value = fileNameInput,
                onValueChange = { fileNameInput = it },
                singleLine = true,
            )
        }
    }
}
