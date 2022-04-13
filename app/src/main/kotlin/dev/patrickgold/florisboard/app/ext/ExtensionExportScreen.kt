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

package dev.patrickgold.florisboard.app.ext

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.app.LocalNavController
import dev.patrickgold.florisboard.extensionManager
import dev.patrickgold.florisboard.lib.android.showLongToast
import dev.patrickgold.florisboard.lib.compose.FlorisScreen
import dev.patrickgold.florisboard.lib.ext.Extension
import dev.patrickgold.florisboard.lib.ext.ExtensionDefaults

@Composable
fun ExtensionExportScreen(id: String) {
    val context = LocalContext.current
    val extensionManager by context.extensionManager()

    val ext = extensionManager.getExtensionById(id)
    if (ext != null) {
        ExportScreen(ext)
    } else {
        ExtensionNotFoundScreen(id)
    }
}

@Composable
private fun ExportScreen(ext: Extension) = FlorisScreen {
    title = ext.meta.title
    scrollable = false

    val navController = LocalNavController.current
    val context = LocalContext.current
    val extensionManager by context.extensionManager()

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(),
        onResult = { uri ->
            // If uri is null it indicates that the selection activity
            //  was cancelled (mostly by pressing the back button), so
            //  we don't display an error message here.
            if (uri == null) {
                navController.popBackStack()
                return@rememberLauncherForActivityResult
            }
            runCatching { extensionManager.export(ext, uri) }.onSuccess {
                context.showLongToast(R.string.ext__export__success)
            }.onFailure { error ->
                context.showLongToast(R.string.ext__export__failure, "error_message" to error.localizedMessage)
            }
            navController.popBackStack()
        },
    )

    content {
        exportLauncher.launch(ExtensionDefaults.createFlexName(ext.meta.id))
    }
}
