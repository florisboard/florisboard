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

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.app.LocalNavController
import dev.patrickgold.florisboard.app.res.stringRes
import dev.patrickgold.florisboard.app.ui.components.FlorisScreen
import dev.patrickgold.florisboard.extensionManager
import dev.patrickgold.florisboard.res.importer.Importer
import dev.patrickgold.florisboard.res.importer.ImportWorkspace

@Composable
fun ExtensionImportScreen(wsUuid: String?) = FlorisScreen {
    title = stringRes(R.string.assets__action__import)
    scrollable = false

    val navController = LocalNavController.current
    val context = LocalContext.current
    val extensionManager by context.extensionManager()
    var importResult by remember { mutableStateOf<Result<ImportWorkspace>?>(null) }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents(),
        onResult = { uriList ->
            // If uri is null it indicates that the selection activity
            //  was cancelled (mostly by pressing the back button), so
            //  we don't display an error message here.
            if (uriList.isNullOrEmpty()) return@rememberLauncherForActivityResult
            importResult?.getOrNull()?.close()
            importResult = Importer.readFromUriIntoCache(context, uriList)
        },
    )

    content {
        Button(
            onClick = {
                importLauncher.launch("*/*")
            },
        ) {
            Text(text = "select files")
        }
        when {
            importResult == null -> {
                Text(text = "No files selected")
            }
            importResult!!.isSuccess -> {
                val workspace = importResult!!.getOrThrow()
                for (fileInfo in workspace.wsInputFiles) {
                    Text(text = fileInfo.file.name)
                    Text(text = fileInfo.file.extension)
                    Text(text = fileInfo.mediaType ?: "unspecified")
                    Text(text = "${fileInfo.size}bytes")
                }
            }
            importResult!!.isFailure -> {
                val error = importResult!!.exceptionOrNull()!!
                Text(text = "error: $error")

            }
        }
    }
}
