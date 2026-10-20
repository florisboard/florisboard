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

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonColors
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.app.LocalNavController
import dev.patrickgold.florisboard.app.Routes
import dev.patrickgold.florisboard.app.components.ScrollableScreenColumn
import dev.patrickgold.florisboard.app.components.SegmentedListColumn
import dev.patrickgold.florisboard.ime.extension.Extension
import dev.patrickgold.florisboard.ime.extension.ExtensionDefaults
import dev.patrickgold.florisboard.ime.extension.LocalExtensionController
import dev.patrickgold.florisboard.ime.extension.ThemeExtension
import dev.patrickgold.florisboard.ime.io.FileRegistry
import dev.patrickgold.florisboard.ime.io.FlorisRef
import dev.patrickgold.florisboard.ime.keyboard3.interaction.LocalInteractionController
import dev.patrickgold.florisboard.ime.keyboard3.interaction.showLongToast
import dev.patrickgold.florisboard.lib.compose.FlorisConfirmDeleteDialog
import dev.patrickgold.florisboard.lib.compose.FlorisScreenNg
import dev.patrickgold.florisboard.lib.util.launchUrl
import kotlinx.coroutines.launch
import org.florisboard.lib.compose.stringRes

@Composable
fun ExtensionDetailScreen(
    extensionId: String,
) {
    val extensionController = LocalExtensionController.current
    val interactionController = LocalInteractionController.current
    val navController = LocalNavController.current

    val scope = rememberCoroutineScope()
    val extensionIndex by extensionController.activeIndex.collectAsState()
    val extension = remember(extensionId, extensionIndex) { extensionIndex.extensions[extensionId] }
    var extToDelete by remember { mutableStateOf<Extension<*>?>(null) }

    val successMsg = stringRes(R.string.ext__export__success)
    val failureMsg = stringRes(R.string.ext__export__failure)
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(FileRegistry.FlexExtension.mediaType),
        onResult = { uri ->
            // If uri is null it indicates that the selection activity
            //  was cancelled (mostly by pressing the back button), so
            //  we don't display an error message here.
            if (uri == null) {
                return@rememberLauncherForActivityResult
            }
            val zipUriRef = FlorisRef.from(uri)
            scope.launch {
                try {
                    extensionController.export(extensionId, zipUriRef)
                    interactionController.showLongToast(successMsg)
                } catch (e: Throwable) {
                    interactionController.showLongToast(failureMsg, "error_message" to e.localizedMessage)
                }
            }
        },
    )

    FlorisScreenNg(
        title = when {
            extension != null -> extension.manifest.meta.title
            else -> extensionId
        },
        previewFieldVisible = false,
    ) { contentPadding ->
        ScrollableScreenColumn(contentPadding) {
            when {
                extension != null -> {
                    ExtensionDetails(
                        extension = extension,
                        onDelete = { extToDelete = extension },
                        onExport = { exportLauncher.launch(ExtensionDefaults.createFlexName(extensionId)) },
                        onEdit = { navController.navigate(Routes.Ext.Edit(extensionId, extension.type)) },
                    )
                }
                else -> ExtensionNotFoundCard(extensionId)
            }
        }

        val extIdToDelete = extToDelete?.manifest?.meta?.id
        if (extIdToDelete != null) {
            val snackbarMsg = stringRes(R.string.error__snackbar_message)
            FlorisConfirmDeleteDialog(
                onConfirm = {
                    scope.launch {
                        try {
                            extensionController.delete(extIdToDelete)
                            navController.popBackStack()
                        } catch (e: Throwable) {
                            interactionController.showLongToast(
                                snackbarMsg,
                                "error_message" to e.localizedMessage,
                            )
                        }
                        extToDelete = null
                    }
                },
                onDismiss = { extToDelete = null },
                what = extToDelete!!.manifest.meta.title,
            )
        }
    }
}

@Composable
private fun ExtensionDetails(
    extension: Extension<*>,
    onDelete: () -> Unit,
    onExport: () -> Unit,
    onEdit: () -> Unit,
) {
    val context = LocalContext.current
    val meta = extension.manifest.meta

    SegmentedListColumn {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            ExtensionActionButton(
                onClick = onDelete,
                label = stringRes(R.string.action__delete),
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                ),
                enabled = extension.canDelete(),
                content = { Icon(Icons.Default.Delete, null) },
            )
            ExtensionActionButton(
                onClick = onExport,
                colors = IconButtonDefaults.filledTonalIconButtonColors(),
                label = stringRes(R.string.action__export),
                content = { Icon(Icons.Default.Share, null) },
            )
            ExtensionActionButton(
                onClick = onEdit,
                colors = IconButtonDefaults.filledIconButtonColors(),
                label = stringRes(R.string.action__edit),
                enabled = extension.canDelete(),
                content = { Icon(Icons.Default.Edit, null) },
            )
        }
    }

    SegmentedListColumn {
        val count = 8
        SegmentedListItem(
            shapes = ListItemDefaults.segmentedShapes(0, count),
            content = { Text(stringRes(R.string.ext__meta__id)) },
            supportingContent = { Text(meta.id) },
        )
        SegmentedListItem(
            shapes = ListItemDefaults.segmentedShapes(1, count),
            content = { Text(stringRes(R.string.ext__meta__version)) },
            supportingContent = { Text(meta.version) },
        )
        SegmentedListItem(
            shapes = ListItemDefaults.segmentedShapes(2, count),
            content = { Text(stringRes(R.string.ext__meta__description)) },
            supportingContent = { Text(meta.description ?: "(no description provided)") },
        )
        SegmentedListItem(
            shapes = ListItemDefaults.segmentedShapes(3, count),
            content = { Text(stringRes(R.string.ext__meta__maintainers)) },
            supportingContent = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    for ((n, maintainer) in meta.maintainers.withIndex()) {
                        if (n > 0) {
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        ExtensionMaintainerChip(maintainer)
                    }
                }
            },
        )
        SegmentedListItem(
            shapes = ListItemDefaults.segmentedShapes(4, count),
            content = { Text(stringRes(R.string.ext__meta__keywords)) },
            supportingContent = {
                if (meta.keywords != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        for ((n, keyword) in meta.keywords.withIndex()) {
                            if (n > 0) {
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            ExtensionKeywordChip(keyword)
                        }
                    }
                } else {
                    Text("(not set)")
                }
            },
        )
        SegmentedListItem(
            onClick = {
                if (meta.homepage != null) {
                    context.launchUrl(meta.homepage)
                }
            },
            shapes = ListItemDefaults.segmentedShapes(5, count),
            content = { Text(stringRes(R.string.ext__meta__homepage)) },
            supportingContent = { Text(meta.homepage ?: "(not set)") },
        )
        SegmentedListItem(
            onClick = {
                if (meta.issueTracker != null) {
                    context.launchUrl(meta.issueTracker)
                }
            },
            shapes = ListItemDefaults.segmentedShapes(6, count),
            content = { Text(stringRes(R.string.ext__meta__issue_tracker)) },
            supportingContent = { Text(meta.issueTracker ?: "(not set)") },
        )
        // TODO: display human-readable License name instead of
        //  SPDX identifier
        SegmentedListItem(
            shapes = ListItemDefaults.segmentedShapes(7, count),
            content = { Text(stringRes(R.string.ext__meta__license)) },
            supportingContent = { Text(meta.license) },
        )
    }

    when (extension) {
        is ThemeExtension -> {
            ExtensionComponentListColumn(
                title = stringRes(R.string.ext__meta__components_theme),
                components = extension.manifest.themes,
            )
        }
        else -> {
            // Render nothing
        }
    }
}

@Composable
private fun ExtensionActionButton(
    onClick: () -> Unit,
    label: String,
    colors: IconButtonColors,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    val buttonSize = IconButtonDefaults.mediumContainerSize(
        IconButtonDefaults.IconButtonWidthOption.Wide,
    )

    Column(
        modifier = modifier.widthIn(min = buttonSize.width),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier.size(buttonSize),
            shapes = IconButtonDefaults.shapes(
                shape = IconButtonDefaults.mediumRoundShape,
                pressedShape = IconButtonDefaults.mediumPressedShape,
            ),
            colors = colors,
            enabled = enabled,
            content = content,
        )

        Text(
            text = label,
            modifier = Modifier
                .padding(top = 8.dp)
                .alpha(if (enabled) 1f else 0.38f),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
