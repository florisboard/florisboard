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

import android.text.format.Formatter
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.app.LocalNavController
import dev.patrickgold.florisboard.app.res.stringRes
import dev.patrickgold.florisboard.app.ui.components.FlorisButtonBar
import dev.patrickgold.florisboard.app.ui.components.FlorisOutlinedBox
import dev.patrickgold.florisboard.app.ui.components.FlorisScreen
import dev.patrickgold.florisboard.app.ui.components.florisHorizontalScroll
import dev.patrickgold.florisboard.cacheManager
import dev.patrickgold.florisboard.common.kotlin.resultOk
import dev.patrickgold.florisboard.extensionManager
import dev.patrickgold.florisboard.res.FileRegistry
import dev.patrickgold.florisboard.res.cache.CacheManager

enum class ExtensionImportScreenType(
    val id: String,
    @StringRes val titleResId: Int,
    val supportedFiles: List<FileRegistry.Entry>,
) {
    EXT_ANY(
        id = "ext-any",
        titleResId = R.string.importer__ext_any,
        supportedFiles = listOf(FileRegistry.FlexExtension),
    ),
    EXT_KEYBOARD(
        id = "ext-keyboard",
        titleResId = R.string.importer__ext_keyboard,
        supportedFiles = listOf(FileRegistry.FlexExtension),
    ),
    EXT_SPELLING(
        id = "ext-spelling",
        titleResId = R.string.importer__ext_spelling,
        supportedFiles = listOf(FileRegistry.FlexExtension),
    ),
    EXT_THEME(
        id = "ext-theme",
        titleResId = R.string.importer__ext_theme,
        supportedFiles = listOf(FileRegistry.FlexExtension),
    );
}

@Composable
fun ExtensionImportScreen(type: ExtensionImportScreenType, initUuid: String?) = FlorisScreen {
    title = stringRes(type.titleResId)

    val navController = LocalNavController.current
    val context = LocalContext.current
    val cacheManager by context.cacheManager()
    val extensionManager by context.extensionManager()

    val initWsUuid by rememberSaveable { mutableStateOf(initUuid) }
    var importResult by remember {
        val workspace = initWsUuid?.let { cacheManager.importer.getWorkspaceByUuid(it) }?.let { resultOk(it) }
        mutableStateOf(workspace)
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents(),
        onResult = { uriList ->
            // If uri is null it indicates that the selection activity
            //  was cancelled (mostly by pressing the back button), so
            //  we don't display an error message here.
            if (uriList.isNullOrEmpty()) return@rememberLauncherForActivityResult
            importResult?.getOrNull()?.close()
            importResult = cacheManager.readFromUriIntoCache(uriList)
        },
    )

    bottomBar {
        FlorisButtonBar {
            ButtonBarSpacer()
            ButtonBarTextButton(
                text = stringRes(R.string.assets__action__cancel),
            ) {
                importResult?.getOrNull()?.close()
                navController.popBackStack()
            }
            ButtonBarButton(
                text = stringRes(R.string.assets__action__import),
                enabled = importResult?.getOrNull() != null,
            ) {
                // TODO
            }
        }
    }

    content {
        OutlinedButton(
            modifier = Modifier
                .padding(vertical = 16.dp)
                .align(Alignment.CenterHorizontally),
            onClick = {
                importLauncher.launch("*/*")
            },
        ) {
            Text(text = stringRes(R.string.assets__action__select_files))
        }

        val result = importResult
        when {
            result == null -> {
                Text(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(horizontal = 16.dp),
                    text = stringRes(R.string.importer__no_files_selected),
                    fontStyle = FontStyle.Italic,
                )
            }
            result.isSuccess -> {
                val workspace = result.getOrThrow()
                for (fileInfo in workspace.inputFileInfos) {
                    FileInfoView(type, fileInfo)
                }
            }
            result.isFailure -> {
                Text(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    text = stringRes(R.string.importer__error_unexpected_exception),
                    style = MaterialTheme.typography.body2,
                    color = MaterialTheme.colors.error,
                )
                Text(
                    modifier = Modifier
                        .florisHorizontalScroll()
                        .padding(horizontal = 16.dp),
                    text = result.exceptionOrNull()?.stackTraceToString() ?: "null",
                    style = MaterialTheme.typography.body2,
                    color = MaterialTheme.colors.error,
                    fontStyle = FontStyle.Italic,
                )
            }
        }
    }
}

@Composable
private fun FileInfoView(
    type: ExtensionImportScreenType,
    fileInfo: CacheManager.FileInfo,
) {
    FlorisOutlinedBox(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        title = fileInfo.file.name,
        subtitle = fileInfo.mediaType ?: "application/unknown",
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Text(
                text = Formatter.formatShortFileSize(LocalContext.current, fileInfo.size),
                style = MaterialTheme.typography.body2,
            )
            val reasonStrId = remember(fileInfo) {
                if (!FileRegistry.matchesFileFilter(fileInfo.file, fileInfo.mediaType, type.supportedFiles)) {
                    R.string.importer__file_skip_unsupported
                } else {
                    null
                }
            }
            if (reasonStrId != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringRes(R.string.importer__file_skip),
                    style = MaterialTheme.typography.body2,
                    color = MaterialTheme.colors.error,
                )
                Text(
                    text = stringRes(reasonStrId),
                    style = MaterialTheme.typography.body2,
                    color = MaterialTheme.colors.error,
                    fontStyle = FontStyle.Italic,
                )
            }
        }
    }
}
