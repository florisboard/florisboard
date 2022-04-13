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

package dev.patrickgold.florisboard.app.settings.advanced

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.LocalContentColor
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import dev.patrickgold.florisboard.BuildConfig
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.app.LocalNavController
import dev.patrickgold.florisboard.app.florisPreferenceModel
import dev.patrickgold.florisboard.cacheManager
import dev.patrickgold.florisboard.lib.android.readToFile
import dev.patrickgold.florisboard.lib.android.showLongToast
import dev.patrickgold.florisboard.lib.cache.CacheManager
import dev.patrickgold.florisboard.lib.compose.CardDefaults
import dev.patrickgold.florisboard.lib.compose.FlorisButtonBar
import dev.patrickgold.florisboard.lib.compose.FlorisOutlinedBox
import dev.patrickgold.florisboard.lib.compose.FlorisOutlinedButton
import dev.patrickgold.florisboard.lib.compose.FlorisScreen
import dev.patrickgold.florisboard.lib.compose.defaultFlorisOutlinedBox
import dev.patrickgold.florisboard.lib.compose.stringRes
import dev.patrickgold.florisboard.lib.ext.ExtensionManager
import dev.patrickgold.florisboard.lib.io.ZipUtils
import dev.patrickgold.florisboard.lib.io.deleteContentsRecursively
import dev.patrickgold.florisboard.lib.io.readJson
import dev.patrickgold.florisboard.lib.io.subDir
import dev.patrickgold.florisboard.lib.io.subFile
import dev.patrickgold.jetpref.datastore.JetPref
import dev.patrickgold.jetpref.datastore.ui.Preference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.FileNotFoundException
import java.text.DateFormat
import java.util.*

object Restore {
    const val MIN_VERSION_CODE = 64
    const val PACKAGE_NAME = "dev.patrickgold.florisboard"
    const val BACKUP_ARCHIVE_FILE_NAME = "backup.zip"

    enum class Mode {
        MERGE,
        ERASE_AND_OVERWRITE;
    }
}

@Composable
fun RestoreScreen() = FlorisScreen {
    title = stringRes(R.string.backup_and_restore__restore__title)
    previewFieldVisible = false

    val prefs by florisPreferenceModel()
    val navController = LocalNavController.current
    val context = LocalContext.current
    val cacheManager by context.cacheManager()

    val restoreFilesSelector = remember { Backup.FilesSelector() }
    var restoreMode by remember { mutableStateOf(Restore.Mode.MERGE) }
    // TODO: rememberCoroutineScope() is unusable because it provides the scope in a cancelled state, which does
    //  not make sense at all. I suspect that this is a bug and once it is resolved we can use it here again.
    val restoreScope = remember { CoroutineScope(Dispatchers.Main) }
    var restoreWorkspace by remember {
        mutableStateOf<CacheManager.BackupAndRestoreWorkspace?>(null)
    }

    val restoreDataFromFileSystemLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri ->
            if (uri == null) return@rememberLauncherForActivityResult
            runCatching {
                restoreWorkspace?.close()
                restoreWorkspace = null
                val workspace = cacheManager.backupAndRestore.new()
                workspace.zipFile = workspace.inputDir.subFile(Restore.BACKUP_ARCHIVE_FILE_NAME)
                context.contentResolver.readToFile(uri, workspace.zipFile)
                ZipUtils.unzip(workspace.zipFile, workspace.outputDir)
                workspace.metadata = try {
                    workspace.outputDir.subFile(Backup.METADATA_JSON_NAME).readJson()
                } catch (e: FileNotFoundException) {
                    error("Invalid archive: either backup_metadata.json is missing or file is not a ZIP archive.")
                }
                workspace.restoreWarningId = when {
                    workspace.metadata.versionCode != BuildConfig.VERSION_CODE -> {
                        R.string.backup_and_restore__restore__metadata_warn_different_version
                    }
                    !workspace.metadata.packageName.startsWith(Restore.PACKAGE_NAME) -> {
                        R.string.backup_and_restore__restore__metadata_warn_different_vendor
                    }
                    else -> null
                }
                workspace.restoreErrorId = when {
                    workspace.metadata.packageName.isBlank() || workspace.metadata.versionCode < Restore.MIN_VERSION_CODE -> {
                        R.string.backup_and_restore__restore__metadata_error_invalid_metadata
                    }
                    else -> null
                }
                restoreWorkspace = workspace
            }.onFailure { error ->
                context.showLongToast(R.string.backup_and_restore__restore__failure, "error_message" to error.localizedMessage)
            }
        },
    )

    suspend fun performRestore() {
        val workspace = restoreWorkspace!!
        val shouldReset = restoreMode == Restore.Mode.ERASE_AND_OVERWRITE
        if (restoreFilesSelector.jetprefDatastore) {
            val datastoreFile = workspace.outputDir
                .subDir(JetPref.JETPREF_DIR_NAME)
                .subFile("${prefs.name}.${JetPref.JETPREF_FILE_EXT}")
            if (datastoreFile.exists()) {
                prefs.datastorePersistenceHandler?.loadPrefs(datastoreFile, shouldReset)
                prefs.datastorePersistenceHandler?.persistPrefs()
            }
        }
        val workspaceFilesDir = workspace.outputDir.subDir("files")
        if (restoreFilesSelector.imeKeyboard) {
            val srcDir = workspaceFilesDir.subDir(ExtensionManager.IME_KEYBOARD_PATH)
            val dstDir = context.filesDir.subDir(ExtensionManager.IME_KEYBOARD_PATH)
            if (shouldReset) {
                dstDir.deleteContentsRecursively()
            }
            if (srcDir.exists()) {
                srcDir.copyRecursively(dstDir, overwrite = true)
            }
        }
        if (restoreFilesSelector.imeSpelling) {
            val srcDir = workspaceFilesDir.subDir(ExtensionManager.IME_SPELLING_PATH)
            val dstDir = context.filesDir.subDir(ExtensionManager.IME_SPELLING_PATH)
            if (shouldReset) {
                dstDir.deleteContentsRecursively()
            }
            if (srcDir.exists()) {
                srcDir.copyRecursively(dstDir, overwrite = true)
            }
        }
        if (restoreFilesSelector.imeTheme) {
            val srcDir = workspaceFilesDir.subDir(ExtensionManager.IME_THEME_PATH)
            val dstDir = context.filesDir.subDir(ExtensionManager.IME_THEME_PATH)
            if (shouldReset) {
                dstDir.deleteContentsRecursively()
            }
            if (srcDir.exists()) {
                srcDir.copyRecursively(dstDir, overwrite = true)
            }
        }
    }

    bottomBar {
        FlorisButtonBar {
            ButtonBarSpacer()
            ButtonBarTextButton(
                onClick = {
                    restoreWorkspace?.close()
                    navController.popBackStack()
                },
                text = stringRes(R.string.action__cancel),
            )
            ButtonBarButton(
                onClick = {
                    restoreScope.launch(Dispatchers.Main) {
                        try {
                            performRestore()
                            context.showLongToast(R.string.backup_and_restore__restore__success)
                            navController.popBackStack()
                        } catch (e: Throwable) {
                            context.showLongToast(R.string.backup_and_restore__restore__failure, "error_message" to e.localizedMessage)
                        }
                    }
                },
                text = stringRes(R.string.action__restore),
                enabled = restoreWorkspace != null && restoreWorkspace?.restoreErrorId == null,
            )
        }
    }

    content {
        FlorisOutlinedBox(
            modifier = Modifier.defaultFlorisOutlinedBox(),
            title = stringRes(R.string.backup_and_restore__restore__mode),
        ) {
            RadioListItem(
                onClick = {
                    restoreMode = Restore.Mode.MERGE
                },
                selected = restoreMode == Restore.Mode.MERGE,
                text = stringRes(R.string.backup_and_restore__restore__mode_merge),
            )
            RadioListItem(
                onClick = {
                    restoreMode = Restore.Mode.ERASE_AND_OVERWRITE
                },
                selected = restoreMode == Restore.Mode.ERASE_AND_OVERWRITE,
                text = stringRes(R.string.backup_and_restore__restore__mode_erase_and_overwrite),
            )
        }
        FlorisOutlinedButton(
            onClick = {
                runCatching {
                    restoreDataFromFileSystemLauncher.launch("*/*")
                }.onFailure { error ->
                    context.showLongToast(R.string.backup_and_restore__restore__failure, "error_message" to error.localizedMessage)
                }
            },
            modifier = Modifier
                .padding(vertical = 16.dp)
                .align(Alignment.CenterHorizontally),
            text = stringRes(R.string.action__select_file),
        )
        val workspace = restoreWorkspace
        if (workspace == null) {
            Text(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(horizontal = 16.dp),
                text = stringRes(R.string.state__no_file_selected),
                fontStyle = FontStyle.Italic,
            )
        } else {
            FlorisOutlinedBox(
                modifier = Modifier.defaultFlorisOutlinedBox(),
                title = stringRes(R.string.backup_and_restore__restore__metadata),
            ) {
                this@content.Preference(
                    iconId = R.drawable.ic_code,
                    title = workspace.metadata.packageName,
                )
                this@content.Preference(
                    iconId = R.drawable.ic_info,
                    title = "${workspace.metadata.versionName} (${workspace.metadata.versionCode})",
                )
                this@content.Preference(
                    iconId = R.drawable.ic_schedule,
                    title = remember(workspace.metadata.timestamp) {
                        val formatter = DateFormat.getDateTimeInstance()
                        val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
                        calendar.timeInMillis = workspace.metadata.timestamp
                        formatter.format(calendar.time)
                    },
                )
                if (workspace.restoreErrorId != null) {
                    Column(modifier = Modifier.padding(CardDefaults.ContentPadding)) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(9.dp)
                                .padding(bottom = 8.dp)
                                .background(MaterialTheme.colors.error.copy(alpha = 0.56f))
                        )
                        Text(
                            text = stringRes(workspace.restoreErrorId!!),
                            style = MaterialTheme.typography.body2,
                            color = MaterialTheme.colors.error,
                            fontStyle = FontStyle.Italic,
                        )
                    }
                } else if (workspace.restoreWarningId != null) {
                    Column(modifier = Modifier.padding(CardDefaults.ContentPadding)) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(9.dp)
                                .padding(bottom = 8.dp)
                                .background(LocalContentColor.current.copy(alpha = LocalContentAlpha.current))
                        )
                        Text(
                            text = stringRes(workspace.restoreWarningId!!),
                            style = MaterialTheme.typography.body2,
                            color = LocalContentColor.current.copy(alpha = LocalContentAlpha.current),
                            fontStyle = FontStyle.Italic,
                        )
                    }
                }
            }
            if (workspace.restoreErrorId == null) {
                BackupFilesSelector(
                    filesSelector = restoreFilesSelector,
                    title = stringRes(R.string.backup_and_restore__restore__files),
                )
            }
        }
    }
}
