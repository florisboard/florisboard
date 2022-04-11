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

package dev.patrickgold.florisboard.app.settings.spelling

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.app.LocalNavController
import dev.patrickgold.florisboard.extensionManager
import dev.patrickgold.florisboard.ime.spelling.SpellingExtensionEditor
import dev.patrickgold.florisboard.ime.spelling.SpellingManager
import dev.patrickgold.florisboard.lib.compose.FlorisDropdownMenu
import dev.patrickgold.florisboard.lib.compose.FlorisScreen
import dev.patrickgold.florisboard.lib.compose.FlorisStep
import dev.patrickgold.florisboard.lib.compose.FlorisStepLayout
import dev.patrickgold.florisboard.lib.compose.FlorisStepState
import dev.patrickgold.florisboard.lib.compose.stringRes
import dev.patrickgold.florisboard.spellingManager
import dev.patrickgold.jetpref.material.ui.JetPrefAlertDialog

private object Step {
    const val SelectSource: Int = 1
    const val ImportArchive: Int = 2
    const val VerifyImport: Int = 3
}

@Composable
fun ImportSpellingArchiveScreen() = FlorisScreen {
    title = stringRes(R.string.settings__spelling__import__title)
    scrollable = false

    val navController = LocalNavController.current
    val context = LocalContext.current
    val extensionManager by context.extensionManager()
    val spellingManager by context.spellingManager()

    val sources = remember { listOf("-") + SpellingManager.Config.importSources.map { it.label } }
    var sourceExpanded by remember { mutableStateOf(false) }
    var sourceSelectedIndex by rememberSaveable { mutableStateOf(0) }

    var importArchiveUri by remember { mutableStateOf<Uri?>(null) }
    var importArchiveEditor by remember { mutableStateOf<SpellingExtensionEditor?>(null) }
    var importArchiveError by remember { mutableStateOf<Throwable?>(null) }
    var writeExtError by remember { mutableStateOf<Throwable?>(null) }
    var errorDialogVisible by remember { mutableStateOf(false) }
    val importArchiveLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri ->
            // If uri is null it indicates that the selection activity
            //  was cancelled (mostly by pressing the back button), so
            //  we don't display an error message here.
            if (uri == null) return@rememberLauncherForActivityResult
            val importSource = SpellingManager.Config.importSources[sourceSelectedIndex - 1]
            spellingManager.prepareImport(importSource.id, uri).fold(
                onSuccess = {
                    importArchiveUri = uri
                    importArchiveEditor = it
                    importArchiveError = null
                    writeExtError = null
                },
                onFailure = {
                    importArchiveUri = null
                    importArchiveEditor = null
                    importArchiveError = it
                    writeExtError = null
                },
            )
        },
    )

    val stepState = rememberSaveable(saver = FlorisStepState.Saver) {
        FlorisStepState.new(init = Step.SelectSource)
    }

    content {
        LaunchedEffect(sourceSelectedIndex, importArchiveEditor) {
            stepState.setCurrentAuto(when {
                sourceSelectedIndex <= 0 -> Step.SelectSource
                importArchiveEditor == null -> Step.ImportArchive
                else -> Step.VerifyImport
            })
        }

        FlorisStepLayout(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            stepState = stepState,
            steps = listOf(
                FlorisStep(
                    id = Step.SelectSource,
                    title = if (stepState.getCurrent().value > Step.SelectSource) {
                        sources.getOrElse(sourceSelectedIndex) { "undefined" }
                    } else {
                        stringRes(R.string.settings__spelling__import_archive_s1__title)
                    },
                ) {
                    StepText(
                        modifier = Modifier.padding(bottom = 16.dp),
                        text = stringRes(R.string.settings__spelling__import_archive_s1__p1),
                    )
                    FlorisDropdownMenu(
                        items = sources,
                        expanded = sourceExpanded,
                        selectedIndex = sourceSelectedIndex,
                        onSelectItem = { sourceSelectedIndex = it },
                        onExpandRequest = { sourceExpanded = true },
                        onDismissRequest = { sourceExpanded = false },
                    )
                },
                FlorisStep(
                    id = Step.ImportArchive,
                    title = if (stepState.getCurrent().value > Step.ImportArchive && importArchiveEditor != null) {
                        importArchiveEditor?.meta?.title ?: "undefined"
                    } else {
                        stringRes(R.string.settings__spelling__import_archive_s2__title)
                    },
                ) {
                    StepText(
                        modifier = Modifier.padding(bottom = 16.dp),
                        text = stringRes(R.string.settings__spelling__import_archive_s2__p1),
                    )
                    StepText(
                        modifier = Modifier.padding(bottom = 16.dp),
                        text = importArchiveUri?.toString() ?: "No file selected.",
                        fontStyle = FontStyle.Italic,
                    )
                    if (importArchiveError != null) {
                        ErrorCard(
                            onActionClick = { errorDialogVisible = true }
                        )
                    }
                    StepButton(
                        onClick = { importArchiveLauncher.launch("*/*") },
                        label = stringRes(R.string.action__select_file),
                    )
                },
                FlorisStep(
                    id = Step.VerifyImport,
                    title = stringRes(R.string.settings__spelling__import_any_s3__title),
                ) {
                    StepText(
                        modifier = Modifier.padding(bottom = 16.dp),
                        text = stringRes(R.string.settings__spelling__import_any_s3__p1),
                    )
                    StepText(
                        modifier = Modifier.padding(bottom = 16.dp),
                        // TODO: add verify view
                        text = "TODO: add verify view",
                        fontStyle = FontStyle.Italic,
                    )
                    if (writeExtError != null) {
                        ErrorCard(
                            onActionClick = { errorDialogVisible = true }
                        )
                    }
                    StepButton(
                        onClick = {
                            runCatching {
                                extensionManager.import(importArchiveEditor!!.build())
                            }.fold(
                                onSuccess = {
                                    navController.popBackStack()
                                },
                                onFailure = {
                                    writeExtError = it
                                },
                            )
                        },
                        label = stringRes(R.string.action__import),
                    )
                },
            ),
        )

        if (errorDialogVisible) {
            JetPrefAlertDialog(
                title = "Detailed crash log",
                onDismiss = { errorDialogVisible = false },
            ) {
                if (importArchiveError != null) {
                    Text(
                        text = importArchiveError.toString(),
                        style = MaterialTheme.typography.body2,
                    )
                }
                if (writeExtError != null) {
                    Text(
                        text = writeExtError.toString(),
                        style = MaterialTheme.typography.body2,
                    )
                }
            }
        }
    }
}

@Composable
private fun ErrorCard(
    onActionClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(start = 16.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                modifier = Modifier
                    .weight(1.0f)
                    .padding(end = 8.dp),
                text = "Something went wrong!",
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
            )
            TextButton(onClick = { onActionClick() }) {
                Text(text = "Details")
            }
        }
    }
}
