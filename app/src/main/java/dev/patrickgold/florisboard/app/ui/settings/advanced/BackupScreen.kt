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

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Checkbox
import androidx.compose.material.RadioButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import dev.patrickgold.florisboard.BuildConfig
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.app.LocalNavController
import dev.patrickgold.florisboard.app.res.stringRes
import dev.patrickgold.florisboard.app.ui.components.FlorisButtonBar
import dev.patrickgold.florisboard.app.ui.components.FlorisOutlinedBox
import dev.patrickgold.florisboard.app.ui.components.FlorisScreen
import dev.patrickgold.florisboard.app.ui.components.rippleClickable
import dev.patrickgold.florisboard.cacheManager
import dev.patrickgold.florisboard.common.android.showLongToast
import dev.patrickgold.florisboard.common.android.writeFromFile
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

private enum class BackupDestination {
    FILE_SYS,
    SHARE_INTENT;
}

object BackupConstants {
    const val METADATA_JSON_NAME = "backup_metadata.json"

    fun backupName(timestamp: Long): String {
        return "backup_${BuildConfig.APPLICATION_ID}_${BuildConfig.VERSION_CODE}_$timestamp.zip"
    }
}

class BackupFiles {
    var jetprefDatastore by mutableStateOf(true)
    var imeKeyboard by mutableStateOf(true)
    var imeSpelling by mutableStateOf(true)
    var imeTheme by mutableStateOf(true)

    fun atLeastOneSelected(): Boolean {
        return jetprefDatastore || imeKeyboard || imeSpelling || imeTheme
    }
}

@Serializable
data class BackupMetadata(
    @SerialName("package")
    val packageName: String,
    val versionCode: Int,
    val versionName: String,
    val timestamp: Long,
)

@Composable
fun BackupScreen() = FlorisScreen {
    title = stringRes(R.string.back_up_and_restore__back_up__title)
    previewFieldVisible = false

    val navController = LocalNavController.current
    val context = LocalContext.current
    val cacheManager by context.cacheManager()

    var backupDestination by remember { mutableStateOf(BackupDestination.FILE_SYS) }
    val backupFiles = remember { BackupFiles() }
    var backupWorkspace: CacheManager.BackupAndRestoreWorkspace? = null

    val backUpToFileSystemLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(),
        onResult = { uri ->
            if (uri == null) return@rememberLauncherForActivityResult
            runCatching {
                context.contentResolver.writeFromFile(uri, backupWorkspace!!.zipFile!!)
                backupWorkspace!!.close()
            }.onSuccess {
                context.showLongToast(R.string.back_up_and_restore__back_up__success)
                navController.popBackStack()
            }.onFailure { error ->
                context.showLongToast(R.string.back_up_and_restore__back_up__failure, "error_message" to error.localizedMessage)
                backupWorkspace = null
            }
        },
    )
    val backUpToShareLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult(),
        onResult = { result ->
            if (result.resultCode == Activity.RESULT_CANCELED) return@rememberLauncherForActivityResult
            //
        },
    )

    fun prepareBackupWorkspace() {
        val workspace = cacheManager.backUpAndRestore.new()
        if (backupFiles.jetprefDatastore) {
            context.filesDir.parentDir!!.subDir("jetpref_datastore").let { dir ->
                dir.copyRecursively(workspace.inputDir.subDir("jetpref_datastore"))
            }
        }
        val workspaceFilesDir = workspace.inputDir.subDir("files")
        if (backupFiles.imeKeyboard) {
            context.filesDir.subDir(ExtensionManager.IME_KEYBOARD_PATH).let { dir ->
                dir.copyRecursively(workspaceFilesDir.subDir(ExtensionManager.IME_KEYBOARD_PATH))
            }
        }
        if (backupFiles.imeSpelling) {
            context.filesDir.subDir(ExtensionManager.IME_SPELLING_PATH).let { dir ->
                dir.copyRecursively(workspaceFilesDir.subDir(ExtensionManager.IME_SPELLING_PATH))
            }
        }
        if (backupFiles.imeTheme) {
            context.filesDir.subDir(ExtensionManager.IME_THEME_PATH).let { dir ->
                dir.copyRecursively(workspaceFilesDir.subDir(ExtensionManager.IME_THEME_PATH))
            }
        }
        val metadata = BackupMetadata(
            packageName = BuildConfig.APPLICATION_ID,
            versionCode = BuildConfig.VERSION_CODE,
            versionName = BuildConfig.VERSION_NAME,
            timestamp = System.currentTimeMillis(),
        )
        workspace.inputDir.subFile(BackupConstants.METADATA_JSON_NAME).writeJson(metadata)
        workspace.zipFile = workspace.outputDir.subFile(BackupConstants.backupName(metadata.timestamp))
        ZipUtils.zip(workspace.inputDir, workspace.zipFile!!)
        backupWorkspace = workspace
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
                    runCatching {
                        if (backupWorkspace == null || backupWorkspace!!.isClosed()) {
                            prepareBackupWorkspace()
                        }
                        when (backupDestination) {
                            BackupDestination.FILE_SYS -> {
                                backUpToFileSystemLauncher.launch(backupWorkspace!!.zipFile!!.name)
                            }
                        }
                    }.onFailure { error ->
                        context.showLongToast(R.string.back_up_and_restore__back_up__failure, "error_message" to error.localizedMessage)
                        backupWorkspace = null
                    }
                },
                text = stringRes(R.string.action__back_up),
                enabled = backupFiles.atLeastOneSelected(),
            )
        }
    }

    content {
        FlorisOutlinedBox(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            title = stringRes(R.string.back_up_and_restore__back_up__destination),
        ) {
            JetPrefListItem(
                modifier = Modifier.rippleClickable {
                    backupDestination = BackupDestination.FILE_SYS
                },
                icon = {
                    RadioButton(
                        selected = backupDestination == BackupDestination.FILE_SYS,
                        onClick = null,
                    )
                },
                text = stringRes(R.string.back_up_and_restore__back_up__destination_file_sys),
            )
            JetPrefListItem(
                modifier = Modifier.rippleClickable {
                    backupDestination = BackupDestination.SHARE_INTENT
                },
                icon = {
                    RadioButton(
                        selected = backupDestination == BackupDestination.SHARE_INTENT,
                        onClick = null,
                    )
                },
                text = stringRes(R.string.back_up_and_restore__back_up__destination_share_intent),
            )
        }
        FlorisOutlinedBox(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            title = stringRes(R.string.back_up_and_restore__back_up__files),
        ) {
            CheckboxListItem(
                onClick = { backupFiles.jetprefDatastore = !backupFiles.jetprefDatastore },
                checked = backupFiles.jetprefDatastore,
                text = stringRes(R.string.back_up_and_restore__back_up__files_jetpref_datastore),
            )
            CheckboxListItem(
                onClick = { backupFiles.imeKeyboard = !backupFiles.imeKeyboard },
                checked = backupFiles.imeKeyboard,
                text = stringRes(R.string.back_up_and_restore__back_up__files_ime_keyboard),
            )
            CheckboxListItem(
                onClick = { backupFiles.imeSpelling = !backupFiles.imeSpelling },
                checked = backupFiles.imeSpelling,
                text = stringRes(R.string.back_up_and_restore__back_up__files_ime_spelling),
            )
            CheckboxListItem(
                onClick = { backupFiles.imeTheme = !backupFiles.imeTheme },
                checked = backupFiles.imeTheme,
                text = stringRes(R.string.back_up_and_restore__back_up__files_ime_theme),
            )
        }
    }
}

@Composable
private fun CheckboxListItem(
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
