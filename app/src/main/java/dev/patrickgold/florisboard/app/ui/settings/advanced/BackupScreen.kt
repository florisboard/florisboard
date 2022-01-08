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

package dev.patrickgold.florisboard.app.ui.settings.advanced

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.Checkbox
import androidx.compose.material.RadioButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ShareCompat
import androidx.core.content.FileProvider
import dev.patrickgold.florisboard.BuildConfig
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.app.LocalNavController
import dev.patrickgold.florisboard.app.res.stringRes
import dev.patrickgold.florisboard.app.ui.components.FlorisButtonBar
import dev.patrickgold.florisboard.app.ui.components.FlorisOutlinedBox
import dev.patrickgold.florisboard.app.ui.components.FlorisScreen
import dev.patrickgold.florisboard.app.ui.components.defaultFlorisOutlinedBox
import dev.patrickgold.florisboard.app.ui.components.rippleClickable
import dev.patrickgold.florisboard.cacheManager
import dev.patrickgold.florisboard.common.android.showLongToast
import dev.patrickgold.florisboard.common.android.writeFromFile
import dev.patrickgold.florisboard.res.FileRegistry
import dev.patrickgold.florisboard.res.ZipUtils
import dev.patrickgold.florisboard.res.cache.CacheManager
import dev.patrickgold.florisboard.res.ext.ExtensionManager
import dev.patrickgold.florisboard.res.io.parentDir
import dev.patrickgold.florisboard.res.io.subDir
import dev.patrickgold.florisboard.res.io.subFile
import dev.patrickgold.florisboard.res.io.writeJson
import dev.patrickgold.jetpref.material.ui.JetPrefListItem
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


object Backup {
    const val FILE_PROVIDER_AUTHORITY = "${BuildConfig.APPLICATION_ID}.provider.file"
    const val METADATA_JSON_NAME = "backup_metadata.json"

    fun defaultFileName(metadata: Metadata): String {
        return "backup_${metadata.packageName}_${metadata.versionCode}_${metadata.timestamp}.zip"
    }

    enum class Destination {
        FILE_SYS,
        SHARE_INTENT;
    }

    class FilesSelector {
        var jetprefDatastore by mutableStateOf(true)
        var imeKeyboard by mutableStateOf(true)
        var imeSpelling by mutableStateOf(true)
        var imeTheme by mutableStateOf(true)

        fun atLeastOneSelected(): Boolean {
            return jetprefDatastore || imeKeyboard || imeSpelling || imeTheme
        }
    }

    @Serializable
    data class Metadata(
        @SerialName("package")
        val packageName: String,
        val versionCode: Int,
        val versionName: String,
        val timestamp: Long,
    )
}

@Composable
fun BackupScreen() = FlorisScreen {
    title = stringRes(R.string.backup_and_restore__back_up__title)
    previewFieldVisible = false

    val navController = LocalNavController.current
    val context = LocalContext.current
    val cacheManager by context.cacheManager()

    var backupDestination by remember { mutableStateOf(Backup.Destination.FILE_SYS) }
    val backupFilesSelector = remember { Backup.FilesSelector() }
    var backupWorkspace: CacheManager.BackupAndRestoreWorkspace? = null

    val backUpToFileSystemLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(),
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
                context.showLongToast(R.string.backup_and_restore__back_up__success)
                navController.popBackStack()
            }.onFailure { error ->
                context.showLongToast(R.string.backup_and_restore__back_up__failure, "error_message" to error.localizedMessage)
                backupWorkspace = null
            }
        },
    )

    fun prepareBackupWorkspace() {
        val workspace = cacheManager.backupAndRestore.new()
        if (backupFilesSelector.jetprefDatastore) {
            context.filesDir.parentDir!!.subDir("jetpref_datastore").let { dir ->
                dir.copyRecursively(workspace.inputDir.subDir("jetpref_datastore"))
            }
        }
        val workspaceFilesDir = workspace.inputDir.subDir("files")
        if (backupFilesSelector.imeKeyboard) {
            context.filesDir.subDir(ExtensionManager.IME_KEYBOARD_PATH).let { dir ->
                dir.copyRecursively(workspaceFilesDir.subDir(ExtensionManager.IME_KEYBOARD_PATH))
            }
        }
        if (backupFilesSelector.imeSpelling) {
            context.filesDir.subDir(ExtensionManager.IME_SPELLING_PATH).let { dir ->
                dir.copyRecursively(workspaceFilesDir.subDir(ExtensionManager.IME_SPELLING_PATH))
            }
        }
        if (backupFilesSelector.imeTheme) {
            context.filesDir.subDir(ExtensionManager.IME_THEME_PATH).let { dir ->
                dir.copyRecursively(workspaceFilesDir.subDir(ExtensionManager.IME_THEME_PATH))
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

    fun prepareAndPerformBackup() {
        runCatching {
            if (backupWorkspace == null || backupWorkspace!!.isClosed()) {
                prepareBackupWorkspace()
            }
            when (backupDestination) {
                Backup.Destination.FILE_SYS -> {
                    backUpToFileSystemLauncher.launch(backupWorkspace!!.zipFile.name)
                }
                Backup.Destination.SHARE_INTENT -> {
                    val uri = FileProvider.getUriForFile(context, Backup.FILE_PROVIDER_AUTHORITY, backupWorkspace!!.zipFile)
                    val shareIntent = ShareCompat.IntentBuilder(context)
                        .setStream(uri)
                        .setType(FileRegistry.BackupArchive.mediaType)
                        .createChooserIntent()
                        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    context.startActivity(shareIntent)
                }
            }
        }.onFailure { error ->
            context.showLongToast(R.string.backup_and_restore__back_up__failure, "error_message" to error.localizedMessage)
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
                    prepareAndPerformBackup()
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
        FlorisOutlinedBox(
            modifier = Modifier.defaultFlorisOutlinedBox(),
            title = stringRes(R.string.backup_and_restore__back_up__files),
        ) {
            CheckboxListItem(
                onClick = { backupFilesSelector.jetprefDatastore = !backupFilesSelector.jetprefDatastore },
                checked = backupFilesSelector.jetprefDatastore,
                text = stringRes(R.string.backup_and_restore__back_up__files_jetpref_datastore),
            )
            CheckboxListItem(
                onClick = { backupFilesSelector.imeKeyboard = !backupFilesSelector.imeKeyboard },
                checked = backupFilesSelector.imeKeyboard,
                text = stringRes(R.string.backup_and_restore__back_up__files_ime_keyboard),
            )
            CheckboxListItem(
                onClick = { backupFilesSelector.imeSpelling = !backupFilesSelector.imeSpelling },
                checked = backupFilesSelector.imeSpelling,
                text = stringRes(R.string.backup_and_restore__back_up__files_ime_spelling),
            )
            CheckboxListItem(
                onClick = { backupFilesSelector.imeTheme = !backupFilesSelector.imeTheme },
                checked = backupFilesSelector.imeTheme,
                text = stringRes(R.string.backup_and_restore__back_up__files_ime_theme),
            )
        }
    }
}

@Composable
internal fun CheckboxListItem(
    onClick: () -> Unit,
    checked: Boolean,
    text: String,
) {
    JetPrefListItem(
        modifier = Modifier.rippleClickable(onClick = onClick),
        icon = {
            Checkbox(
                checked = checked,
                onCheckedChange = null,
            )
        },
        text = text,
    )
}

@Composable
internal fun RadioListItem(
    onClick: () -> Unit,
    selected: Boolean,
    text: String,
) {
    JetPrefListItem(
        modifier = Modifier.rippleClickable(onClick = onClick),
        icon = {
            RadioButton(
                selected = selected,
                onClick = null,
            )
        },
        text = text,
    )
}
