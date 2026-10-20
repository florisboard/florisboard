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

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.Crossfade
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.outlined.FileOpen
import androidx.compose.material.icons.outlined.FolderZip
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSerializable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.patrickgold.florisboard.BuildConfig
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.app.FlorisPreferenceModel
import dev.patrickgold.florisboard.app.FlorisPreferenceStore
import dev.patrickgold.florisboard.app.LocalNavController
import dev.patrickgold.florisboard.app.Routes
import dev.patrickgold.florisboard.app.apptheme.SegmentedListColumnVerticalPadding
import dev.patrickgold.florisboard.app.components.FlorisBottomBar
import dev.patrickgold.florisboard.app.components.FlorisBottomBarPrimaryButton
import dev.patrickgold.florisboard.app.components.FlorisBottomBarSecondaryButton
import dev.patrickgold.florisboard.app.components.FlorisScreenNg
import dev.patrickgold.florisboard.app.components.ScrollableScreenColumn
import dev.patrickgold.florisboard.app.components.SegmentedListColumn
import dev.patrickgold.florisboard.app.components.SegmentedRadioListItem
import dev.patrickgold.florisboard.ime.clipboard.provider.ClipboardFileStorage
import dev.patrickgold.florisboard.ime.clipboard.provider.ClipboardItem
import dev.patrickgold.florisboard.ime.clipboard.provider.ItemType
import dev.patrickgold.florisboard.ime.extension.LocalExtensionController
import dev.patrickgold.florisboard.ime.io.AndroidStorage
import dev.patrickgold.florisboard.ime.io.FlorisRef
import dev.patrickgold.florisboard.ime.io.LocalStorageController
import dev.patrickgold.florisboard.ime.io.createWorkspace
import dev.patrickgold.florisboard.ime.io.filterLinesInPlace
import dev.patrickgold.florisboard.ime.io.queryFileName
import dev.patrickgold.florisboard.ime.io.readJson
import dev.patrickgold.florisboard.ime.io.unzip
import dev.patrickgold.florisboard.ime.keyboard3.interaction.LocalInteractionController
import dev.patrickgold.florisboard.ime.keyboard3.interaction.showLongToast
import dev.patrickgold.florisboard.inferFlorisApplication
import dev.patrickgold.jetpref.datastore.runtime.AndroidAppDataStorage
import dev.patrickgold.jetpref.datastore.runtime.FileBasedStorage
import dev.patrickgold.jetpref.datastore.runtime.ImportStrategy
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import okio.IOException
import org.florisboard.lib.compose.stringRes
import org.florisboard.lib.kotlin.tryOrNull
import java.text.DateFormat
import java.util.*
import kotlin.time.Duration.Companion.milliseconds

@Serializable
sealed interface RestoreScreenState {
    val fileName: String
        get() = ""

    val workspaceRef: FlorisRef?
        get() = null

    @Serializable
    data object Idle : RestoreScreenState

    @Serializable
    data class Parsing(
        override val fileName: String,
    ) : RestoreScreenState

    @Serializable
    data class Ready(
        override val fileName: String,
        override val workspaceRef: FlorisRef,
        val backupDirRef: FlorisRef,
        val metadata: BackupArchiveMetadata,
        val isSetup: Boolean,
        val importStrategy: ImportStrategy,
        val filter: BackupArchiveFilter = BackupArchiveFilter(),
    ) : RestoreScreenState

    @Serializable
    data class Error(
        override val fileName: String,
        override val workspaceRef: FlorisRef?,
        val restoreErrorId: Int? = null,
        val restoreErrorMsg: String? = null,
    ) : RestoreScreenState
}

@Composable
fun RestoreScreen(isSetup: Boolean) {
    val context = LocalContext.current
    val extensionController = LocalExtensionController.current
    val interactionController = LocalInteractionController.current
    val navController = LocalNavController.current
    val storageController = LocalStorageController.current

    val storage by storageController.activeStorage.collectAsState()
    val scope = rememberCoroutineScope()
    var restoreScreenState by rememberSerializable {
        mutableStateOf<RestoreScreenState>(RestoreScreenState.Idle)
    }

    DisposableEffect(restoreScreenState.workspaceRef) {
        val workspaceRef = restoreScreenState.workspaceRef
        onDispose {
            workspaceRef?.let { storage.deleteRecursively(it, mustExist = false) }
        }
    }

    val restoreDataFromFileSystemLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri ->
            if (uri == null) return@rememberLauncherForActivityResult
            scope.launch {
                val fileName = tryOrNull { (storage as AndroidStorage).queryFileName(uri) }
                if (fileName == null) {
                    restoreScreenState = RestoreScreenState.Error(
                        fileName = "???",
                        workspaceRef = null,
                        restoreErrorId = R.string.backup_and_restore__restore__file_name_query_error,
                    )
                    return@launch
                }
                restoreScreenState = RestoreScreenState.Parsing(fileName)
                delay(500.milliseconds) // to give the animations breathing room
                var workspaceRef: FlorisRef? = null
                try {
                    workspaceRef = storage.createWorkspace()
                    val zippedRef = workspaceRef.subRef(BackupDefaults.ARCHIVE_FILE_NAME)
                    storage.copy(FlorisRef.from(uri), zippedRef)
                    val unzippedRef = workspaceRef.subRef(BackupDefaults.ARCHIVE_DIR_NAME)
                    storage.unzip(zippedRef, unzippedRef)
                    val metadata = storage.readJson<BackupArchiveMetadata>(unzippedRef.subRef(BackupDefaults.METADATA_JSON_NAME))
                    if (metadata.packageName.isBlank() || metadata.versionCode < BackupDefaults.RESTORE_MIN_VERSION_CODE) {
                        restoreScreenState = RestoreScreenState.Error(
                            fileName = fileName,
                            workspaceRef = workspaceRef,
                            restoreErrorId = R.string.backup_and_restore__restore__metadata_error_invalid_metadata,
                        )
                    }
                    restoreScreenState = RestoreScreenState.Ready(
                        fileName = fileName,
                        workspaceRef = workspaceRef,
                        backupDirRef = unzippedRef,
                        metadata = metadata,
                        isSetup = isSetup,
                        importStrategy = when {
                            isSetup -> ImportStrategy.Erase
                            else -> ImportStrategy.Merge
                        },
                    )
                } catch (e: IOException) {
                    restoreScreenState = RestoreScreenState.Error(
                        fileName = fileName,
                        workspaceRef = workspaceRef,
                        restoreErrorMsg = "Invalid archive: either backup_metadata.json is missing or file is not a ZIP archive. Details: ${e.message}",
                    )
                }
            }
        },
    )

    suspend fun performRestore(state: RestoreScreenState.Ready) {
        val shouldReset = state.importStrategy == ImportStrategy.Erase
        if (state.filter.inclPrefs) {
            val prefRef = state.backupDirRef
                .subRef(AndroidAppDataStorage.JETPREF_DIR_NAME)
                .subRef("${FlorisPreferenceModel.NAME}.${AndroidAppDataStorage.JETPREF_FILE_EXT}")
            if (storage.exists(prefRef)) {
                val (prefFs, prefPath) = storage.resolve(prefRef)
                prefFs.filterLinesInPlace(prefPath) { line ->
                    !line.startsWith("b;internal__is_ime_set_up;")
                }
                val fileBasedStorage = FileBasedStorage(prefPath.toString())
                FlorisPreferenceStore.import(state.importStrategy, fileBasedStorage).getOrThrow()
            }
        }

        if (state.filter.includesAnyExtension()) {
            extensionController.restore(
                backupDirRef = state.backupDirRef,
                importStrategy = state.importStrategy,
                filter = state.filter,
                isLegacyMode = state.metadata.versionCode <= BackupDefaults.OLD_EXT_VERSION_CODE_CUTOFF,
            )
        }

        val clipboardManager = context.inferFlorisApplication().clipboardManager.value
        if (shouldReset) {
            clipboardManager.clearFullHistory()
            ClipboardFileStorage.resetClipboardFileStorage(context)
        }
        if (state.filter.inclClipboard() != ToggleableState.Off) {
            val clipboardFilesRef = state.backupDirRef.subRef("clipboard")

            if (state.filter.inclClipboardTextItems) {
                val clipboardItemsRef = clipboardFilesRef.subRef(BackupDefaults.CLIPBOARD_TEXT_ITEMS_JSON_NAME)
                if (storage.exists(clipboardItemsRef)) {
                    val clipboardItemsList = storage.readJson<List<ClipboardItem>>(clipboardItemsRef)
                    clipboardManager.restoreHistory(items = clipboardItemsList.filter { it.type == ItemType.TEXT })
                }
            }
            if (state.filter.inclClipboardImageItems) {
                val clipboardItemsRef = clipboardFilesRef.subRef(BackupDefaults.CLIPBOARD_IMAGES_JSON_NAME)
                if (storage.exists(clipboardItemsRef)) {
                    val clipboardItemsList = storage.readJson<List<ClipboardItem>>(clipboardItemsRef)
                    for (item in clipboardItemsList.filter { it.type == ItemType.IMAGE }) {
                        ClipboardFileStorage.insertFileFromBackupIfNotExisting(
                            context,
                            clipboardFilesRef.subRef(
                                "${ClipboardFileStorage.CLIPBOARD_FILES_PATH}/${
                                    item.uri!!.path!!.split(
                                        '/'
                                    ).last()
                                }"
                            ).let { ref ->
                                val (_, path) = storage.resolve(ref)
                                path.toFile()
                            },
                        )
                    }
                    clipboardManager.restoreHistory(items = clipboardItemsList.filter { it.type == ItemType.IMAGE })
                }
            }
            if (state.filter.inclClipboardVideoItems) {
                val clipboardItemsRef = clipboardFilesRef.subRef(BackupDefaults.CLIPBOARD_VIDEO_JSON_NAME)
                if (storage.exists(clipboardItemsRef)) {
                    val clipboardItemsList = storage.readJson<List<ClipboardItem>>(clipboardItemsRef)
                    for (item in clipboardItemsList.filter { it.type == ItemType.VIDEO }) {
                        ClipboardFileStorage.insertFileFromBackupIfNotExisting(
                            context,
                            clipboardFilesRef.subRef(
                                "${ClipboardFileStorage.CLIPBOARD_FILES_PATH}/${
                                    item.uri!!.path!!.split(
                                        '/'
                                    ).last()
                                }"
                            ).let { ref ->
                                val (_, path) = storage.resolve(ref)
                                path.toFile()
                            },
                        )
                    }
                    clipboardManager.restoreHistory(items = clipboardItemsList.filter { it.type == ItemType.VIDEO })
                }
            }
        }
    }

    fun triggerSelectFile() {
        restoreDataFromFileSystemLauncher.launch("application/zip")
    }

    val state = restoreScreenState
    FlorisScreenNg(
        title = stringRes(R.string.backup_and_restore__restore__title),
        previewFieldVisible = false,
        bottomBar = {
            FlorisBottomBar {
                Spacer(Modifier.weight(1f))
                FlorisBottomBarSecondaryButton(
                    onClick = { navController.navigateUp() },
                    text = stringRes(R.string.action__cancel),
                )
                val successMsg = stringRes(R.string.backup_and_restore__restore__success)
                val failureMsg = stringRes(R.string.backup_and_restore__restore__failure)
                FlorisBottomBarPrimaryButton(
                    onClick = {
                        if (state is RestoreScreenState.Ready) {
                            scope.launch {
                                try {
                                    performRestore(state)
                                    interactionController.showLongToast(successMsg)
                                    if (isSetup) {
                                        navController.navigate(Routes.Setup.Screen(hasRestoredBackup = true)) {
                                            popUpTo(Routes.Setup.Screen::class) { inclusive = true }
                                        }
                                    } else {
                                        navController.navigateUp()
                                    }
                                } catch (e: Throwable) {
                                    interactionController.showLongToast(
                                        failureMsg,
                                        "error_message" to e.message,
                                    )
                                }
                            }
                        }
                    },
                    text = stringRes(R.string.action__restore),
                    enabled = state is RestoreScreenState.Ready && state.filter.atLeastOneSelected(),
                )
            }
        },
    ) { contentPadding ->
        SharedTransitionLayout(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding),
        ) {
            AnimatedContent(
                targetState = state is RestoreScreenState.Idle,
                transitionSpec = {
                    fadeIn(spring(stiffness = Spring.StiffnessMediumLow)) togetherWith
                        fadeOut(spring(stiffness = Spring.StiffnessMediumLow))
                },
                label = "restoreLayout",
            ) { isIdle ->
                if (isIdle) {
                    IdleLayout(
                        onSelectFile = { triggerSelectFile() },
                        animatedVisibilityScope = this,
                    )
                } else {
                    ActiveLayout(
                        state = state,
                        onUpdateState = { restoreScreenState = it },
                        onSelectFile = { triggerSelectFile() },
                        animatedVisibilityScope = this,
                    )
                }
            }
        }
    }
}

@Composable
private fun SharedTransitionScope.IdleLayout(
    onSelectFile: () -> Unit,
    animatedVisibilityScope: AnimatedVisibilityScope,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CookieBadge(
            size = 176.dp,
            spinning = false,
            animatedVisibilityScope = animatedVisibilityScope,
        )
        Spacer(Modifier.size(16.dp))
        Text(
            text = "Select a .zip backup to preview what will be restored.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.size(32.dp))
        SelectFileButton(
            compact = false,
            enabled = true,
            onClick = onSelectFile,
            animatedVisibilityScope = animatedVisibilityScope,
        )
    }
}

@Composable
private fun SharedTransitionScope.ActiveLayout(
    state: RestoreScreenState,
    onUpdateState: (RestoreScreenState) -> Unit,
    onSelectFile: () -> Unit,
    animatedVisibilityScope: AnimatedVisibilityScope,
) {
    ScrollableScreenColumn(PaddingValues.Zero) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = SegmentedListColumnVerticalPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            CookieBadge(
                size = 48.dp,
                spinning = state is RestoreScreenState.Parsing,
                animatedVisibilityScope = animatedVisibilityScope,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = state.fileName,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.MiddleEllipsis,
                )
                Text(
                    text = when (state) {
                        is RestoreScreenState.Ready -> "Ready"
                        is RestoreScreenState.Error -> "Failed"
                        else -> "Reading backup…"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            SelectFileButton(
                compact = true,
                enabled = state !is RestoreScreenState.Parsing,
                onClick = onSelectFile,
                animatedVisibilityScope = animatedVisibilityScope,
            )
        }

        val transition = updateTransition(
            targetState = state,
            label = "restoreBody",
        )
        transition.Crossfade(
            contentKey = { it::class },
        ) { target ->
            when (target) {
                is RestoreScreenState.Ready -> ReadyColumn(target, onUpdateState)
                is RestoreScreenState.Error -> ErrorColumn(target)
                else -> Unit
            }
        }
    }
}

@Composable
private fun ReadyColumn(
    state: RestoreScreenState.Ready,
    onUpdateState: (RestoreScreenState.Ready) -> Unit,
) {
    Column {
        SegmentedListColumn(title = stringRes(R.string.backup_and_restore__restore__metadata)) {
            val count = 3
            SegmentedListItem(
                shapes = ListItemDefaults.segmentedShapes(0, count),
                leadingContent = { Icon(Icons.Default.Code, null) },
                content = { Text(state.metadata.packageName) },
                supportingContent = if (!state.metadata.packageName.startsWith(BackupDefaults.PACKAGE_NAME_PREFIX)) {
                    { Text(stringRes(R.string.backup_and_restore__restore__metadata_warn_different_vendor)) }
                } else null,
            )
            SegmentedListItem(
                shapes = ListItemDefaults.segmentedShapes(1, count),
                leadingContent = { Icon(Icons.Outlined.Info, null) },
                content = { Text("${state.metadata.versionName} (${state.metadata.versionCode})") },
                supportingContent = if (state.metadata.versionCode != BuildConfig.VERSION_CODE) {
                    { Text(stringRes(R.string.backup_and_restore__restore__metadata_warn_different_version)) }
                } else null,
            )
            SegmentedListItem(
                shapes = ListItemDefaults.segmentedShapes(2, count),
                leadingContent = { Icon(Icons.Default.Schedule, null) },
                content = {
                    val text = remember(state.metadata.timestamp) {
                        val formatter = DateFormat.getDateTimeInstance()
                        val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
                        calendar.timeInMillis = state.metadata.timestamp
                        formatter.format(calendar.time)
                    }
                    Text(text)
                },
            )
        }
        if (!state.isSetup) {
            SegmentedListColumn(title = stringRes(R.string.backup_and_restore__restore__mode)) {
                val count = 2
                SegmentedRadioListItem(
                    onClick = { onUpdateState(state.copy(importStrategy = ImportStrategy.Merge)) },
                    selected = state.importStrategy == ImportStrategy.Merge,
                    shapes = ListItemDefaults.segmentedShapes(0, count),
                    content = { Text(stringRes(R.string.backup_and_restore__restore__mode_merge)) },
                )
                SegmentedRadioListItem(
                    onClick = { onUpdateState(state.copy(importStrategy = ImportStrategy.Erase)) },
                    selected = state.importStrategy == ImportStrategy.Erase,
                    shapes = ListItemDefaults.segmentedShapes(1, count),
                    content = { Text(stringRes(R.string.backup_and_restore__restore__mode_erase_and_overwrite)) },
                )
            }
        }
        BackupArchiveFilterColumn(
            filter = state.filter,
            onUpdateFilter = { onUpdateState(state.copy(filter = it)) },
            title = stringRes(R.string.backup_and_restore__restore__files),
        )
    }
}

@Composable
private fun ErrorColumn(
    state: RestoreScreenState.Error,
) {
    val errorText = when {
        state.restoreErrorId != null -> stringRes(state.restoreErrorId)
        else -> state.restoreErrorMsg ?: "(no error message provided)"
    }
    SegmentedListColumn {
        val count = 1
        SegmentedListItem(
            shapes = ListItemDefaults.segmentedShapes(0, count),
            content = { Text(errorText) },
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SharedTransitionScope.CookieBadge(
    size: Dp,
    spinning: Boolean,
    animatedVisibilityScope: AnimatedVisibilityScope,
    modifier: Modifier = Modifier,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "cookieSpin")
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 3000, easing = LinearEasing)),
        label = "cookieAngle",
    )

    Box(
        modifier = modifier
            .sharedBounds(
                sharedContentState = rememberSharedContentState(key = "cookie"),
                animatedVisibilityScope = animatedVisibilityScope,
            )
            .size(size),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { rotationZ = if (spinning) angle else 0f }
                .clip(MaterialShapes.Cookie9Sided.toShape())
                .background(MaterialTheme.colorScheme.primaryContainer),
        )
        Icon(
            imageVector = Icons.Outlined.FolderZip,
            contentDescription = null,
            modifier = Modifier.size(size * 0.5f),
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }
}

/**
 * See: https://developer.android.com/reference/kotlin/androidx/compose/material3/Button.composable
 */
@Composable
private fun SharedTransitionScope.SelectFileButton(
    compact: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    animatedVisibilityScope: AnimatedVisibilityScope,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.sharedBounds(
            sharedContentState = rememberSharedContentState(key = "selectButton"),
            animatedVisibilityScope = animatedVisibilityScope,
        ),
    ) {
        if (compact) {
            FilledTonalButton(
                onClick = onClick,
                enabled = enabled,
            ) {
                Text(stringRes(R.string.action__change))
            }
        } else {
            val size = ButtonDefaults.MediumContainerHeight
            Button(
                onClick = onClick,
                enabled = enabled,
                modifier = Modifier.heightIn(size),
                contentPadding = ButtonDefaults.contentPaddingFor(size),
            ) {
                Icon(
                    imageVector = Icons.Outlined.FileOpen,
                    contentDescription = null,
                    modifier = Modifier.size(ButtonDefaults.iconSizeFor(size)),
                )
                Spacer(Modifier.width(ButtonDefaults.iconSpacingFor(size)))
                Text(
                    text = stringRes(R.string.action__select_file),
                    style = ButtonDefaults.textStyleFor(size),
                )
            }
        }
    }
}
