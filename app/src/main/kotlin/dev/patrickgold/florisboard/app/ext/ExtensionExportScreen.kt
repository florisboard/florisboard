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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.rememberCoroutineScope
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.app.LocalNavController
import dev.patrickgold.florisboard.app.components.ScrollableScreenColumn
import dev.patrickgold.florisboard.ime.extension.ExtensionDefaults
import dev.patrickgold.florisboard.ime.extension.LocalExtensionController
import dev.patrickgold.florisboard.ime.io.FileRegistry
import dev.patrickgold.florisboard.ime.io.FlorisRef
import dev.patrickgold.florisboard.ime.keyboard3.interaction.LocalInteractionController
import dev.patrickgold.florisboard.ime.keyboard3.interaction.showLongToast
import dev.patrickgold.florisboard.lib.compose.FlorisScreenNg
import kotlinx.coroutines.launch
import org.florisboard.lib.compose.stringRes

@Composable
fun ExtensionExportScreen(extensionId: String) {
    val extensionController = LocalExtensionController.current
    val interactionController = LocalInteractionController.current
    val navController = LocalNavController.current

    val scope = rememberCoroutineScope()
    val successMsg = stringRes(R.string.ext__export__success)
    val failureMsg = stringRes(R.string.ext__export__failure)

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(FileRegistry.FlexExtension.mediaType),
        onResult = { uri ->
            // If uri is null it indicates that the selection activity
            //  was cancelled (mostly by pressing the back button), so
            //  we don't display an error message here.
            if (uri == null) {
                navController.popBackStack()
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
                navController.popBackStack()
            }
        },
    )

    SideEffect {
        exportLauncher.launch(ExtensionDefaults.createFlexName(extensionId))
    }

    FlorisScreenNg(
        title = extensionId,
        previewFieldVisible = false,
    ) { contentPadding ->
        ScrollableScreenColumn(contentPadding) {
            // no content
        }
    }
}
