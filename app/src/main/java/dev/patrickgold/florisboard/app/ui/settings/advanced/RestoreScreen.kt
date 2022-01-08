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

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
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
import dev.patrickgold.florisboard.app.res.stringRes
import dev.patrickgold.florisboard.app.ui.components.CardDefaults
import dev.patrickgold.florisboard.app.ui.components.FlorisButtonBar
import dev.patrickgold.florisboard.app.ui.components.FlorisOutlinedBox
import dev.patrickgold.florisboard.app.ui.components.FlorisOutlinedButton
import dev.patrickgold.florisboard.app.ui.components.FlorisScreen
import dev.patrickgold.florisboard.app.ui.components.defaultFlorisOutlinedBox
import dev.patrickgold.florisboard.cacheManager
import dev.patrickgold.florisboard.common.android.readToFile
import dev.patrickgold.florisboard.common.android.showLongToast
import dev.patrickgold.florisboard.res.FileRegistry
import dev.patrickgold.florisboard.res.ZipUtils
import dev.patrickgold.florisboard.res.cache.CacheManager
import dev.patrickgold.florisboard.res.io.readJson
import dev.patrickgold.florisboard.res.io.subFile

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

    val navController = LocalNavController.current
    val context = LocalContext.current
    val cacheManager by context.cacheManager()

    var restoreMode by remember { mutableStateOf(Restore.Mode.MERGE) }
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
                workspace.metadata = workspace.outputDir.subFile(Backup.METADATA_JSON_NAME).readJson()
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
                    //
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
        val workspace = restoreWorkspace
        if (workspace == null) {
            FlorisOutlinedButton(
                onClick = {
                    runCatching {
                        restoreDataFromFileSystemLauncher.launch(
                            FileRegistry.BackupArchive.mediaType
                        )
                    }.onFailure { error ->
                        context.showLongToast(R.string.backup_and_restore__restore__failure, "error_message" to error.localizedMessage)
                    }
                },
                modifier = Modifier
                    .padding(vertical = 16.dp)
                    .align(Alignment.CenterHorizontally),
                text = stringRes(R.string.action__select_file),
            )
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
                contentPadding = CardDefaults.ContentPadding,
            ) {
                Text(
                    text = remember {
                        buildString {
                            append("packageName = ")
                            appendLine(workspace.metadata.packageName)
                            appendLine("version = ${workspace.metadata.versionName} (${workspace.metadata.versionCode})")
                            append("timestamp = ")
                            append(workspace.metadata.timestamp)
                        }
                    },
                    style = MaterialTheme.typography.body2,
                    color = LocalContentColor.current.copy(alpha = LocalContentAlpha.current),
                )
                if (workspace.restoreErrorId != null) {
                    Box(modifier = Modifier
                        .fillMaxWidth()
                        .height(19.dp)
                        .padding(top = 10.dp, bottom = 8.dp)
                        .background(MaterialTheme.colors.error.copy(alpha = 0.56f)))
                    Text(
                        text = stringRes(workspace.restoreErrorId!!),
                        style = MaterialTheme.typography.body2,
                        color = MaterialTheme.colors.error,
                        fontStyle = FontStyle.Italic,
                    )
                } else if (workspace.restoreWarningId != null) {
                    Box(modifier = Modifier
                        .fillMaxWidth()
                        .height(19.dp)
                        .padding(top = 10.dp, bottom = 8.dp)
                        .background(LocalContentColor.current.copy(alpha = LocalContentAlpha.current)))
                    Text(
                        text = stringRes(workspace.restoreErrorId!!),
                        style = MaterialTheme.typography.body2,
                        color = LocalContentColor.current.copy(alpha = LocalContentAlpha.current),
                        fontStyle = FontStyle.Italic,
                    )
                }
            }
        }
    }
}
