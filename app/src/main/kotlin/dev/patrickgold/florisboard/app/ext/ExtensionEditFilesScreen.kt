/*
 * Copyright (C) 2025 The FlorisBoard Contributors
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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.MimeTypeFilter
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.lib.cache.CacheManager
import dev.patrickgold.florisboard.lib.compose.FlorisIconButton
import dev.patrickgold.florisboard.lib.compose.FlorisScreen
import dev.patrickgold.florisboard.lib.compose.stringRes
import dev.patrickgold.jetpref.datastore.ui.Preference
import dev.patrickgold.jetpref.material.ui.JetPrefAlertDialog
import dev.patrickgold.jetpref.material.ui.JetPrefTextField
import java.io.File
import java.util.*
import org.florisboard.lib.android.query
import org.florisboard.lib.android.readToFile
import org.florisboard.lib.android.showLongToast
import org.florisboard.lib.android.showShortToast
import org.florisboard.lib.kotlin.io.parentDir
import org.florisboard.lib.kotlin.io.subDir
import org.florisboard.lib.kotlin.io.subFile

const val FONTS = "fonts"
const val IMAGES = "images"

val MIME_TYPES = mapOf(
    FONTS to listOf(
        // Source: https://www.alienfactory.co.uk/articles/mime-types-for-web-fonts-in-bedsheet#mimeTypes
        "font/*",
        "application/vnd.ms-fontobject", // .eot
        "application/font-woff", // .woff
        "application/x-font-truetype", // .ttf
        "application/x-font-opentype", // .otf
    ),
    IMAGES to listOf(
        "image/*",
    ),
)

@Composable
fun ExtensionEditFilesScreen(workspace: CacheManager.ExtEditorWorkspace<*>) = FlorisScreen {
    title = stringRes(R.string.ext__editor__files__title)

    fun handleBackPress() {
        workspace.currentAction = null
    }

    navigationIcon {
        FlorisIconButton(
            onClick = { handleBackPress() },
            icon = Icons.Default.Close,
        )
    }

    content {
        val context = LocalContext.current
        var version by rememberSaveable { mutableIntStateOf(0) }
        val fontFiles = remember(version) {
            workspace.extDir.subDir(FONTS).listFiles { it.isFile }.orEmpty().asList()
        }
        val imageFiles = remember(version) {
            workspace.extDir.subDir(IMAGES).listFiles { it.isFile }.orEmpty().asList()
        }

        var currentImportDest by remember { mutableStateOf<String?>(null) }
        var currentImportResult by remember { mutableStateOf<Result<Pair<File, String>>?>(null) }

        val importLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent(),
            onResult = { uri ->
                currentImportResult = runCatching {
                    checkNotNull(uri) { "" }
                    val tempFile = context.cacheDir.subFile("temp_${UUID.randomUUID()}")
                    context.contentResolver.readToFile(uri, tempFile)
                    val mimeType = context.contentResolver.getType(uri)
                    val types = MIME_TYPES[currentImportDest!!]!!
                    checkNotNull(MimeTypeFilter.matches(mimeType, types.toTypedArray())) {
                        "Given file mime type was '$mimeType', expected one of $types"
                    }
                    val fileName = context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME)).use { cursor ->
                        if (cursor == null || !cursor.moveToFirst()) return@use null
                        val name = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        cursor.getString(name)
                    }
                    tempFile to fileName.orEmpty()
                }
            },
        )

        LaunchedEffect(currentImportResult) {
            val message = currentImportResult?.exceptionOrNull()?.message
            if (!message.isNullOrBlank()) {
                context.showLongToast(message)
            }
        }

        BackHandler {
            handleBackPress()
        }

        @Composable
        fun FileList(title: String, icon: ImageVector, files: List<File>, onAdd: () -> Unit) {
            var dialogFile by remember { mutableStateOf<File?>(null) }
            ListItem(
                headlineContent = {
                    Text(
                        text = title,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                leadingContent = {
                    Spacer(modifier = Modifier.width(24.dp))
                },
                trailingContent = {
                    IconButton(
                        onClick = onAdd,
                    ) {
                        Icon(Icons.Default.Add, null)
                    }
                },
            )
            for (file in files) {
                Preference(
                    onClick = {
                        dialogFile = file
                    },
                    icon = icon,
                    title = file.name,
                )
            }

            dialogFile?.let { file ->
                var fileNameInput by rememberSaveable { mutableStateOf(file.name) }
                JetPrefAlertDialog(
                    title = stringRes(R.string.general__properties),
                    confirmLabel = stringRes(R.string.action__apply),
                    dismissLabel = stringRes(R.string.action__cancel),
                    neutralLabel = stringRes(R.string.action__delete),
                    allowOutsideDismissal = true,
                    onNeutral = {
                        if (file.delete()) {
                            context.showShortToast("Successfully deleted")
                        } else {
                            context.showShortToast("Failed to delete")
                        }
                        dialogFile = null
                        version++
                    },
                    onConfirm = {
                        val newFile = file.parentFile!!.subFile(fileNameInput).canonicalFile
                        if (newFile.parentFile != file.canonicalFile.parentFile) {
                            context.showLongToast("Invalid file name!")
                            return@JetPrefAlertDialog
                        }
                        if (newFile.exists()) {
                            context.showShortToast("Filename already exists.")
                            return@JetPrefAlertDialog
                        }
                        val success = file.renameTo(newFile)
                        if (success) {
                            context.showShortToast("Successfully renamed")
                        } else {
                            context.showShortToast("Failed to rename the file.")
                        }
                        dialogFile = null
                        version++
                    },
                    onDismiss = {
                        dialogFile = null
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

        FileList(
            title = stringRes(R.string.ext__editor__files__type_fonts),
            icon = Icons.Default.TextFields,
            files = fontFiles,
        ) {
            currentImportDest = FONTS
            importLauncher.launch("*/*")
        }

        FileList(
            title = stringRes(R.string.ext__editor__files__type_images),
            icon = Icons.Default.Photo,
            files = imageFiles,
        ) {
            currentImportDest = IMAGES
            importLauncher.launch("*/*")
        }

        val dest = currentImportDest
        val result = currentImportResult?.getOrNull()
        if (dest != null && result != null) {
            var fileNameInput by rememberSaveable { mutableStateOf(result.second) }
            JetPrefAlertDialog(
                title = stringRes(R.string.action__import_file),
                confirmLabel = stringRes(R.string.action__add),
                onConfirm = {
                    val fileName = fileNameInput.trim()
                    val dir = workspace.extDir.subDir(dest)
                    dir.mkdirs()
                    val file = dir.subFile(fileName)
                    if (file.parentDir != workspace.extDir.subDir(dest)) {
                        context.showShortToast("Invalid file name")
                    } else if (file.exists()) {
                        context.showShortToast("File already exists")
                    } else {
                        val tempFile = result.first
                        if (!tempFile.renameTo(file)) {
                            context.showShortToast("Failed to rename file")
                            tempFile.delete()
                        }
                        currentImportDest = null
                        currentImportResult = null
                        version++
                    }
                },
                dismissLabel = stringRes(R.string.action__cancel),
                onDismiss = {
                    val tempFile = result.first
                    tempFile.delete()
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
}
