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

package dev.patrickgold.florisboard.app.settings.advanced

import android.content.ContentUris
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSerializable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.state.ToggleableState
import androidx.core.app.ShareCompat
import androidx.core.content.FileProvider
import dev.patrickgold.florisboard.BuildConfig
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.app.FlorisPreferenceModel
import dev.patrickgold.florisboard.app.FlorisPreferenceStore
import dev.patrickgold.florisboard.app.LocalNavController
import dev.patrickgold.florisboard.app.components.FlorisBottomBar
import dev.patrickgold.florisboard.app.components.FlorisBottomBarPrimaryButton
import dev.patrickgold.florisboard.app.components.FlorisBottomBarSecondaryButton
import dev.patrickgold.florisboard.app.components.ScrollableScreenColumn
import dev.patrickgold.florisboard.app.components.SegmentedListColumn
import dev.patrickgold.florisboard.app.components.SegmentedRadioListItem
import dev.patrickgold.florisboard.ime.clipboard.provider.ClipboardFileStorage
import dev.patrickgold.florisboard.ime.clipboard.provider.ItemType
import dev.patrickgold.florisboard.ime.extension.LocalExtensionController
import dev.patrickgold.florisboard.ime.io.FileRegistry
import dev.patrickgold.florisboard.ime.io.FlorisRef
import dev.patrickgold.florisboard.ime.io.LocalStorageController
import dev.patrickgold.florisboard.ime.io.createWorkspace
import dev.patrickgold.florisboard.ime.io.writeJson
import dev.patrickgold.florisboard.ime.io.zip
import dev.patrickgold.florisboard.ime.keyboard3.interaction.LocalInteractionController
import dev.patrickgold.florisboard.ime.keyboard3.interaction.showLongToast
import dev.patrickgold.florisboard.inferFlorisApplication
import dev.patrickgold.florisboard.lib.compose.FlorisScreenNg
import dev.patrickgold.jetpref.datastore.runtime.AndroidAppDataStorage
import dev.patrickgold.jetpref.datastore.runtime.FileBasedStorage
import kotlinx.coroutines.launch
import org.florisboard.lib.android.writeFromFile
import org.florisboard.lib.compose.stringRes

private enum class BackupDestination {
    FILE_SYS,
    SHARE_INTENT;
}

private data class BackupWorkspace(
    val workspaceRef: FlorisRef,
    val archiveRef: FlorisRef,
    val metadata: BackupArchiveMetadata,
)

@Composable
fun BackupScreen() {
    val context = LocalContext.current
    val extensionController = LocalExtensionController.current
    val interactionController = LocalInteractionController.current
    val navController = LocalNavController.current
    val storageController = LocalStorageController.current

    val scope = rememberCoroutineScope()
    val storage by storageController.activeStorage.collectAsState()

    var backupDestination by rememberSerializable { mutableStateOf(BackupDestination.FILE_SYS) }
    var backupFilesFilter by rememberSerializable { mutableStateOf(BackupArchiveFilter()) }

    val successMsg = stringRes(R.string.backup_and_restore__back_up__success)
    val failureMsg = stringRes(R.string.backup_and_restore__back_up__failure)
    var backupWorkspace by remember { mutableStateOf<BackupWorkspace?>(null) }
    val backUpToFileSystemLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip"),
        onResult = { uri ->
            if (uri == null) {
                return@rememberLauncherForActivityResult
            }
            val workspace = backupWorkspace ?: return@rememberLauncherForActivityResult
            backupWorkspace = null
            scope.launch {
                try {
                    val (_, archivePath) = storage.resolve(workspace.archiveRef)
                    context.contentResolver.writeFromFile(uri, archivePath.toFile())
                    interactionController.showLongToast(successMsg)
                    navController.popBackStack()
                } catch (e: Throwable) {
                    interactionController.showLongToast(failureMsg, "error_message" to e.message)
                    backupWorkspace = null
                } finally {
                    storage.deleteRecursively(workspace.workspaceRef)
                }
            }
        },
    )

    suspend fun prepareBackupWorkspace(filter: BackupArchiveFilter): BackupWorkspace {
        val workspaceRef = storage.createWorkspace()
        val backupDirRef = workspaceRef.subRef(BackupDefaults.ARCHIVE_DIR_NAME)
        if (filter.inclPrefs) {
            val prefRef = backupDirRef
                .subRef(AndroidAppDataStorage.JETPREF_DIR_NAME)
                .subRef("${FlorisPreferenceModel.NAME}.${AndroidAppDataStorage.JETPREF_FILE_EXT}")
            val (_, prefPath) = storage.resolve(prefRef)
            FlorisPreferenceStore.export(FileBasedStorage(prefPath.toString())).getOrThrow()
        }
        if (filter.includesAnyExtension()) {
            extensionController.backup(
                backupDirRef = backupDirRef,
                filter = filter,
            )
        }

        if (backupFilesFilter.inclClipboard() != ToggleableState.Off) {
            val clipboardManager = context.inferFlorisApplication().clipboardManager.value
            val clipboardHistory = clipboardManager.currentHistory.all
            val clipboardFilesRef = backupDirRef.subRef("clipboard")
            storage.mkdirs(clipboardFilesRef)
            if (backupFilesFilter.inclClipboardTextItems) {
                val itemsRef = clipboardFilesRef.subRef(BackupDefaults.CLIPBOARD_TEXT_ITEMS_JSON_NAME)
                storage.writeJson(itemsRef, clipboardHistory.filter { it.type == ItemType.TEXT })
            }
            if (backupFilesFilter.inclClipboardImageItems) {
                val itemsRef = clipboardFilesRef.subRef(BackupDefaults.CLIPBOARD_IMAGES_JSON_NAME)
                storage.writeJson(itemsRef, clipboardHistory.filter { it.type == ItemType.IMAGE })
                for (item in clipboardHistory.filter { it.type == ItemType.IMAGE }) {
                    val id = ContentUris.parseId(item.uri!!)
                    ClipboardFileStorage.getFileForId(context, id).copyTo(
                        clipboardFilesRef.subRef("${ClipboardFileStorage.CLIPBOARD_FILES_PATH}/$id").let { ref ->
                            val (_, path) = storage.resolve(ref)
                            path.toFile()
                        }
                    )
                }
            }
            if (backupFilesFilter.inclClipboardVideoItems) {
                val itemsRef = clipboardFilesRef.subRef(BackupDefaults.CLIPBOARD_VIDEO_JSON_NAME)
                storage.writeJson(itemsRef, clipboardHistory.filter { it.type == ItemType.VIDEO })
                for (item in clipboardHistory.filter { it.type == ItemType.VIDEO }) {
                    val id = ContentUris.parseId(item.uri!!)
                    ClipboardFileStorage.getFileForId(context, id).copyTo(
                        clipboardFilesRef.subRef("${ClipboardFileStorage.CLIPBOARD_FILES_PATH}/$id").let { ref ->
                            val (_, path) = storage.resolve(ref)
                            path.toFile()
                        }
                    )
                }
            }
        }
        val metadata = BackupArchiveMetadata(
            packageName = BuildConfig.APPLICATION_ID,
            versionCode = BuildConfig.VERSION_CODE + 1,
            versionName = BuildConfig.VERSION_NAME,
            timestamp = System.currentTimeMillis(),
        )
        val metadataRef = backupDirRef.subRef(BackupDefaults.METADATA_JSON_NAME)
        storage.writeJson(metadataRef, metadata)
        val archiveRef = workspaceRef.subRef(BackupDefaults.defaultFileName(metadata))
        storage.zip(backupDirRef, archiveRef)
        return BackupWorkspace(workspaceRef, archiveRef, metadata)
    }

    suspend fun prepareAndPerformBackup() {
        try {
            val workspace = prepareBackupWorkspace(backupFilesFilter)
            when (backupDestination) {
                BackupDestination.FILE_SYS -> {
                    backupWorkspace = workspace
                    backUpToFileSystemLauncher.launch(workspace.archiveRef.pathName)
                }
                BackupDestination.SHARE_INTENT -> {
                    val (_, path) = storage.resolve(workspace.archiveRef)
                    val uri =
                        FileProvider.getUriForFile(context, BackupDefaults.FILE_PROVIDER_AUTHORITY, path.toFile())
                    val shareIntent = ShareCompat.IntentBuilder(context)
                        .setStream(uri)
                        .setType(FileRegistry.BackupArchive.mediaType)
                        .createChooserIntent()
                        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    context.startActivity(shareIntent)
                }
            }
        } catch (e: Throwable) {
            interactionController.showLongToast(failureMsg, "error_message" to e.message)
        }
    }

    FlorisScreenNg(
        title = stringRes(R.string.backup_and_restore__back_up__title),
        previewFieldVisible = false,
        bottomBar = {
            FlorisBottomBar {
                Spacer(Modifier.weight(1f))
                FlorisBottomBarSecondaryButton(
                    onClick = {
                        navController.popBackStack()
                    },
                    text = stringRes(R.string.action__cancel),
                )
                FlorisBottomBarPrimaryButton(
                    onClick = {
                        scope.launch { prepareAndPerformBackup() }
                    },
                    text = stringRes(R.string.action__back_up),
                    enabled = backupFilesFilter.atLeastOneSelected(),
                )
            }
        },
    ) { contentPadding ->
        ScrollableScreenColumn(contentPadding) {
            SegmentedListColumn(title = stringRes(R.string.backup_and_restore__back_up__destination)) {
                val count = 2
                SegmentedRadioListItem(
                    onClick = {
                        backupDestination = BackupDestination.FILE_SYS
                    },
                    selected = backupDestination == BackupDestination.FILE_SYS,
                    shapes = ListItemDefaults.segmentedShapes(0, count),
                    content = { Text(stringRes(R.string.backup_and_restore__back_up__destination_file_sys)) },
                )
                SegmentedRadioListItem(
                    onClick = {
                        backupDestination = BackupDestination.SHARE_INTENT
                    },
                    selected = backupDestination == BackupDestination.SHARE_INTENT,
                    shapes = ListItemDefaults.segmentedShapes(1, count),
                    content = { Text(stringRes(R.string.backup_and_restore__back_up__destination_share_intent)) },
                )
            }
            BackupArchiveFilterColumn(
                filter = backupFilesFilter,
                onUpdateFilter = { backupFilesFilter = it },
                title = stringRes(R.string.backup_and_restore__back_up__files),
            )
        }
    }
}
