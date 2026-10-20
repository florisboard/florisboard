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

package dev.patrickgold.florisboard.app.ext

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Style
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.app.LocalNavController
import dev.patrickgold.florisboard.app.components.ScrollableScreenColumn
import dev.patrickgold.florisboard.app.components.SegmentedListColumn
import dev.patrickgold.florisboard.app.components.segmentedErrorColors
import dev.patrickgold.florisboard.ime.extension.ExtensionController
import dev.patrickgold.florisboard.ime.extension.ExtensionDefaults
import dev.patrickgold.florisboard.ime.extension.ExtensionManifest
import dev.patrickgold.florisboard.ime.extension.Keyboard3Extension
import dev.patrickgold.florisboard.ime.extension.LocalExtensionController
import dev.patrickgold.florisboard.ime.extension.ThemeExtension
import dev.patrickgold.florisboard.ime.io.AndroidStorage
import dev.patrickgold.florisboard.ime.io.FlorisRef
import dev.patrickgold.florisboard.ime.io.LocalStorageController
import dev.patrickgold.florisboard.ime.io.createWorkspace
import dev.patrickgold.florisboard.ime.io.queryFileName
import dev.patrickgold.florisboard.ime.io.readFromUri
import dev.patrickgold.florisboard.ime.io.readJson
import dev.patrickgold.florisboard.ime.io.unzip
import dev.patrickgold.florisboard.ime.keyboard3.interaction.LocalInteractionController
import dev.patrickgold.florisboard.ime.keyboard3.interaction.showLongToast
import dev.patrickgold.florisboard.lib.compose.FlorisScreenNg
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okio.IOException
import dev.patrickgold.florisboard.app.components.FlorisBottomBarPrimaryButton
import dev.patrickgold.florisboard.app.components.FlorisBottomBarSecondaryButton
import org.florisboard.lib.compose.FlorisButton
import dev.patrickgold.florisboard.app.components.FlorisBottomBar
import org.florisboard.lib.compose.stringRes
import kotlin.concurrent.atomics.AtomicInt
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.concurrent.atomics.fetchAndIncrement
import kotlin.time.Duration.Companion.seconds

enum class ExtensionImportScreenType(
    val id: String,
    @StringRes val titleResId: Int,
    val acceptManifest: (ExtensionManifest) -> Boolean,
) {
    EXT_ANY(
        id = "ext-any",
        titleResId = R.string.ext__import__ext_any,
        acceptManifest = { true },
    ),
    EXT_KEYBOARD3(
        id = "ext-keyboard3",
        titleResId = R.string.ext__import__ext_keyboard,
        acceptManifest = { it is Keyboard3Extension.Manifest },
    ),
    EXT_THEME(
        id = "ext-theme",
        titleResId = R.string.ext__import__ext_theme,
        acceptManifest = { it is ThemeExtension.Manifest },
    ),
    EXT_LANGUAGEPACK(
        id = "ext-languagepack",
        titleResId = R.string.ext__import__ext_languagepack,
        acceptManifest = { false }, // TODO
    );
}

private class FileNameQueryFailed : Exception() {
    override fun fillInStackTrace(): Throwable = this
}

private class WorkspaceCreationFailed(cause: IOException) : Exception(cause) {
    override fun fillInStackTrace(): Throwable = this
}

private sealed interface ImportFile {
    val uri: Uri
    val fileName: String?
    val workspaceRef: FlorisRef?

    data class Processing(
        override val uri: Uri,
        override val fileName: String?,
        override val workspaceRef: FlorisRef?,
    ) : ImportFile

    data class Failure(
        override val uri: Uri,
        override val fileName: String?,
        override val workspaceRef: FlorisRef?,
        val zippedRef: FlorisRef?,
        val unzippedRef: FlorisRef?,
        val cause: Exception,
    ) : ImportFile

    data class Success(
        override val uri: Uri,
        override val fileName: String,
        override val workspaceRef: FlorisRef?,
        val zippedRef: FlorisRef,
        val unzippedRef: FlorisRef,
        val manifest: ExtensionManifest,
    ) : ImportFile
}

@OptIn(ExperimentalAtomicApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ExtensionImportScreen(
    type: ExtensionImportScreenType,
    givenUriList: List<Uri>? = null,
) {
    val extensionController = LocalExtensionController.current
    val interactionController = LocalInteractionController.current
    val navController = LocalNavController.current
    val storageController = LocalStorageController.current

    val scope = rememberCoroutineScope()
    val extensionIndex by extensionController.activeIndex.collectAsState()
    val storage by storageController.activeStorage.collectAsState()

    val idBase = AtomicInt(0)
    val importFiles = remember { mutableStateMapOf<Int, ImportFile>() }
    val importBtnEnabled by remember {
        derivedStateOf { importFiles.isNotEmpty() && importFiles.all { (_, v) -> v !is ImportFile.Processing } }
    }

    fun launchImportJob(uri: Uri) = scope.launch {
        val id = idBase.fetchAndIncrement()
        val workspaceRef = try {
            storage.createWorkspace()
        } catch (e: IOException) {
            importFiles[id] = ImportFile.Failure(
                uri = uri,
                fileName = null,
                workspaceRef = null,
                zippedRef = null,
                unzippedRef = null,
                cause = WorkspaceCreationFailed(e),
            )
            return@launch
        }
        importFiles[id] = ImportFile.Processing(uri, null, workspaceRef)
        val fileName = (storage as AndroidStorage).queryFileName(uri)?.trim()
        if (fileName.isNullOrEmpty()) {
            importFiles[id] = ImportFile.Failure(
                uri = uri,
                fileName = null,
                workspaceRef = workspaceRef,
                zippedRef = null,
                unzippedRef = null,
                cause = FileNameQueryFailed(),
            )
            return@launch
        }
        importFiles[id] = ImportFile.Processing(uri, fileName, workspaceRef)
        delay(1.seconds)
        val zippedRef = workspaceRef.subRef(fileName)
        try {
            (storage as AndroidStorage).readFromUri(uri, zippedRef)
        } catch (e: IOException) {
            importFiles[id] = ImportFile.Failure(
                uri = uri,
                fileName = fileName,
                workspaceRef = workspaceRef,
                zippedRef = zippedRef,
                unzippedRef = null,
                cause = e,
            )
            return@launch
        }
        val unzippedRef = workspaceRef.subRef(zippedRef.pathName + ".d")
        val manifestRef = unzippedRef.subRef(ExtensionDefaults.MANIFEST_FILE_NAME)
        val manifest: ExtensionManifest
        try {
            storage.mkdirs(unzippedRef)
            storage.unzip(zippedRef, unzippedRef)
            manifest = storage.readJson<ExtensionManifest>(manifestRef, ExtensionController.ExtensionJson)
            if (!type.acceptManifest(manifest)) {
                throw IOException("incorrect or invalid extension manifest type")
            }
        } catch (e: IOException) {
            importFiles[id] = ImportFile.Failure(
                uri = uri,
                fileName = fileName,
                workspaceRef = workspaceRef,
                zippedRef = zippedRef,
                unzippedRef = unzippedRef,
                cause = e,
            )
            return@launch
        }
        importFiles[id] = ImportFile.Success(
            uri = uri,
            fileName = fileName,
            workspaceRef = workspaceRef,
            zippedRef = zippedRef,
            unzippedRef = unzippedRef,
            manifest = manifest,
        )
    }

    LaunchedEffect(Unit) {
        if (givenUriList.isNullOrEmpty()) {
            return@LaunchedEffect
        }
        for (uri in givenUriList) {
            launchImportJob(uri)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            for ((_, importFile) in importFiles) {
                importFile.workspaceRef?.let { workspaceRef ->
                    try {
                        storage.deleteRecursively(workspaceRef)
                    } catch (_: IOException) {
                        // ignore
                    }
                }
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents(),
        onResult = { uriList ->
            // If uri is null it indicates that the selection activity
            //  was cancelled (mostly by pressing the back button), so
            //  we don't display an error message here.
            if (uriList.isEmpty()) return@rememberLauncherForActivityResult
            for (uri in uriList) {
                launchImportJob(uri)
            }
        },
    )

    FlorisScreenNg(
        title = stringRes(type.titleResId),
        previewFieldVisible = false,
        bottomBar = {
            val successText = stringRes(R.string.ext__import__success)
            val failureText = stringRes(R.string.ext__import__failure)
            FlorisBottomBar {
                Spacer(modifier = Modifier.weight(1f))
                FlorisBottomBarSecondaryButton(
                    onClick = { navController.popBackStack() },
                    text = stringRes(R.string.action__cancel),
                )
                FlorisBottomBarPrimaryButton(
                    onClick = {
                        scope.launch {
                            runCatching {
                                for ((_, importFile) in importFiles) {
                                    if (importFile is ImportFile.Success) {
                                        extensionController.import(importFile.unzippedRef)
                                    }
                                }
                            }.onSuccess {
                                interactionController.showLongToast(successText)
                                navController.popBackStack()
                            }.onFailure { error ->
                                interactionController.showLongToast(failureText, "error_message" to error.localizedMessage)
                            }
                        }
                    },
                    text = stringRes(R.string.action__import),
                    enabled = importBtnEnabled,
                )
            }
        }
    ) { contentPadding ->
        ScrollableScreenColumn(contentPadding) {
            if (importFiles.isEmpty()) {
                FlorisButton(
                    onClick = {
                        importLauncher.launch("*/*")
                    },
                    modifier = Modifier
                        .padding(vertical = 16.dp)
                        .align(Alignment.CenterHorizontally),
                    text = stringRes(R.string.action__select_files),
                )
                Text(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(horizontal = 16.dp),
                    text = stringRes(R.string.state__no_files_selected),
                    fontStyle = FontStyle.Italic,
                )
            }

            for (importFile in importFiles.values) {
                SegmentedListColumn(title = importFile.fileName ?: "(file name unknown)") {
                    when (importFile) {
                        is ImportFile.Processing -> {
                            val count = 1
                            SegmentedListItem(
                                shapes = ListItemDefaults.segmentedShapes(0, count),
                                leadingContent = {
                                    LoadingIndicator(
                                        modifier = Modifier.size(24.dp),
                                    )
                                },
                                content = {
                                    Text("Processing...")
                                },
                            )
                        }
                        is ImportFile.Failure -> {
                            val count = 1
                            SegmentedListItem(
                                shapes = ListItemDefaults.segmentedShapes(0, count),
                                colors = ListItemDefaults.segmentedErrorColors(),
                                leadingContent = {
                                    Icon(Icons.Default.ErrorOutline, null)
                                },
                                content = {
                                    Text(stringRes(R.string.ext__import__file_skip))
                                },
                                supportingContent = {
                                    Text(importFile.cause.message ?: importFile.cause::class.toString())
                                },
                            )
                        }
                        is ImportFile.Success -> {
                            val manifest = importFile.manifest
                            val count = 3 + when (manifest) {
                                is ThemeExtension.Manifest -> 1
                                else -> 0
                            }
                            val maintainers = remember(manifest.meta) {
                                manifest.meta.maintainers.joinToString { it.name }
                            }
                            SegmentedListItem(
                                shapes = ListItemDefaults.segmentedShapes(0, count),
                                leadingContent = {
                                    Icon(Icons.Default.Extension, null)
                                },
                                overlineContent = {
                                    Text(manifest.meta.id)
                                },
                                content = {
                                    Text(manifest.meta.title)
                                },
                                supportingContent = manifest.meta.description?.let { description ->
                                    @Composable {
                                        Text(
                                            text = description.trim(),
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                },
                            )
                            SegmentedListItem(
                                shapes = ListItemDefaults.segmentedShapes(1, count),
                                leadingContent = {
                                    Icon(Icons.Default.Person, null)
                                },
                                content = {
                                    Text(maintainers)
                                },
                            )
                            SegmentedListItem(
                                shapes = ListItemDefaults.segmentedShapes(2, count),
                                leadingContent = {
                                    Icon(Icons.Default.Numbers, null)
                                },
                                content = {
                                    Text(manifest.meta.version)
                                },
                            )
                            when (manifest) {
                                is ThemeExtension.Manifest -> {
                                    SegmentedListItem(
                                        shapes = ListItemDefaults.segmentedShapes(3, count),
                                        leadingContent = {
                                            Icon(Icons.Outlined.Style, null)
                                        },
                                        content = {
                                            Text("${manifest.themes.size} stylesheets")
                                        },
                                    )
                                }
                                is Keyboard3Extension.Manifest -> {
                                    // TODO
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
