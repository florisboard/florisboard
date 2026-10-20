/*
 * Copyright (C) 2021-2025 The FlorisBoard Contributors
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

import androidx.compose.runtime.Composable

@Composable
fun BackupScreen() {} /*= FlorisScreen {
    title = stringRes(R.string.backup_and_restore__back_up__title)
    previewFieldVisible = false

    val navController = LocalNavController.current
    val context = LocalContext.current
    val cacheManager by context.cacheManager()
    val scope = rememberCoroutineScope()

    var backupDestination by remember { mutableStateOf(Backup.Destination.FILE_SYS) }
    val backupFilesSelector = remember { Backup.FilesSelector() }
    var backupWorkspace: CacheManager.BackupAndRestoreWorkspace? = null

    val backUpToFileSystemLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip"),
        onResult = { uri ->
            if (uri == null) {
                // User can modify checkboxes between cancellation and second
                // trigger, so we make sure to clear out the previous workspace
                backupWorkspace?.close()
                backupWorkspace = null
                return@rememberLauncherForActivityResult
            }
            runCatching {
                context.contentResolver.writeFromFile(uri, backupWorkspace!!.zipFile)
                backupWorkspace!!.close()
            }.onSuccess {
                context.showLongToastSync(R.string.backup_and_restore__back_up__success)
                navController.popBackStack()
            }.onFailure { error ->
                flogError { error.stackTraceToString() }
                context.showLongToastSync(R.string.backup_and_restore__back_up__failure, "error_message" to error.message)
                backupWorkspace = null
            }
        },
    )

    suspend fun prepareBackupWorkspace() {
        val workspace = cacheManager.backupAndRestore.new()
        if (backupFilesSelector.jetprefDatastore) {
            val fileBasedStorage = workspace.inputDir
                .subDir(AndroidAppDataStorage.JETPREF_DIR_NAME)
                .subFile("${FlorisPreferenceModel.NAME}.${AndroidAppDataStorage.JETPREF_FILE_EXT}")
                .let { FileBasedStorage(it.path) }
            FlorisPreferenceStore.export(fileBasedStorage).getOrThrow()
        }
        val workspaceFilesDir = workspace.inputDir.subDir("files")
        if (backupFilesSelector.imeKeyboard) {
            context.filesDir.subDir(ExtensionManager.IME_KEYBOARD_PATH).let { dir ->
                dir.copyRecursively(workspaceFilesDir.subDir(ExtensionManager.IME_KEYBOARD_PATH))
            }
        }
        if (backupFilesSelector.imeTheme) {
            context.filesDir.subDir(ExtensionManager.IME_THEME_PATH).let { dir ->
                dir.copyRecursively(workspaceFilesDir.subDir(ExtensionManager.IME_THEME_PATH))
            }
        }

        if (backupFilesSelector.provideClipboardItems()) {
            val clipboardManager by context.clipboardManager()
            val clipboardHistory = clipboardManager.currentHistory.all
            val clipboardFilesDir = workspace.inputDir.subDir("clipboard")
            clipboardFilesDir.mkdir()
            if (backupFilesSelector.clipboardTextItems) {
                clipboardFilesDir.subFile(Backup.CLIPBOARD_TEXT_ITEMS_JSON_NAME)
                    .writeJson(clipboardHistory.filter { it.type == ItemType.TEXT })
            }
            if (backupFilesSelector.clipboardImageItems) {
                clipboardFilesDir.subFile(Backup.CLIPBOARD_IMAGES_JSON_NAME)
                    .writeJson(clipboardHistory.filter { it.type == ItemType.IMAGE })
                for (item in clipboardHistory.filter { it.type == ItemType.IMAGE }) {
                    val id = ContentUris.parseId(item.uri!!)
                    ClipboardFileStorage.getFileForId(context, id).copyTo(
                        clipboardFilesDir.subFile("${ClipboardFileStorage.CLIPBOARD_FILES_PATH}/$id")
                    )
                }
            }
            if (backupFilesSelector.clipboardVideoItems) {
                clipboardFilesDir.subFile(Backup.CLIPBOARD_VIDEO_JSON_NAME)
                    .writeJson(clipboardHistory.filter { it.type == ItemType.VIDEO })
                for (item in clipboardHistory.filter { it.type == ItemType.VIDEO }) {
                    val id = ContentUris.parseId(item.uri!!)
                    ClipboardFileStorage.getFileForId(context, id).copyTo(
                        clipboardFilesDir.subFile("${ClipboardFileStorage.CLIPBOARD_FILES_PATH}/$id")
                    )
                }
            }
        }
        workspace.metadata = Backup.Metadata(
            packageName = BuildConfig.APPLICATION_ID,
            versionCode = BuildConfig.VERSION_CODE,
            versionName = BuildConfig.VERSION_NAME,
            timestamp = System.currentTimeMillis(),
        )
        workspace.inputDir.subFile(Backup.METADATA_JSON_NAME).writeJson(workspace.metadata)
        workspace.zipFile = workspace.outputDir.subFile(Backup.defaultFileName(workspace.metadata))
        ZipUtils.zip(workspace.inputDir, workspace.zipFile)
        backupWorkspace = workspace
    }

    suspend fun prepareAndPerformBackup() {
        runCatching {
            if (backupWorkspace == null || backupWorkspace!!.isClosed()) {
                prepareBackupWorkspace()
            }
            when (backupDestination) {
                Backup.Destination.FILE_SYS -> {
                    backUpToFileSystemLauncher.launch(backupWorkspace!!.zipFile.name)
                }

                Backup.Destination.SHARE_INTENT -> {
                    val uri =
                        FileProvider.getUriForFile(context, Backup.FILE_PROVIDER_AUTHORITY, backupWorkspace!!.zipFile)
                    val shareIntent = ShareCompat.IntentBuilder(context)
                        .setStream(uri)
                        .setType(FileRegistry.BackupArchive.mediaType)
                        .createChooserIntent()
                        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    context.startActivity(shareIntent)
                }
            }
        }.onFailure { error ->
            flogError { error.stackTraceToString() }
            context.showLongToast(R.string.backup_and_restore__back_up__failure, "error_message" to error.message)
            backupWorkspace = null
        }
    }

    bottomBar {
        FlorisButtonBar {
            ButtonBarSpacer()
            ButtonBarTextButton(
                onClick = {
                    backupWorkspace?.close()
                    navController.popBackStack()
                },
                text = stringRes(R.string.action__cancel),
            )
            ButtonBarButton(
                onClick = {
                    scope.launch { prepareAndPerformBackup() }
                },
                text = stringRes(R.string.action__back_up),
                enabled = backupFilesSelector.atLeastOneSelected(),
            )
        }
    }

    content {
        FlorisOutlinedBox(
            modifier = Modifier.defaultFlorisOutlinedBox(),
            title = stringRes(R.string.backup_and_restore__back_up__destination),
        ) {
            RadioListItem(
                onClick = {
                    backupDestination = Backup.Destination.FILE_SYS
                },
                selected = backupDestination == Backup.Destination.FILE_SYS,
                text = stringRes(R.string.backup_and_restore__back_up__destination_file_sys),
            )
            RadioListItem(
                onClick = {
                    backupDestination = Backup.Destination.SHARE_INTENT
                },
                selected = backupDestination == Backup.Destination.SHARE_INTENT,
                text = stringRes(R.string.backup_and_restore__back_up__destination_share_intent),
            )
        }
        BackupFilesSelector(
            filesSelector = backupFilesSelector,
            title = stringRes(R.string.backup_and_restore__back_up__files),
        )
    }
}*/
