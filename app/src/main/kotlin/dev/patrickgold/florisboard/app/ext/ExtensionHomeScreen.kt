/*
 * Copyright (C) 2025 The OmniBoard Contributors
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

package dev.silo.omniboard.app.ext

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Palette
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import dev.silo.omniboard.R
import dev.silo.omniboard.app.LocalNavController
import dev.silo.omniboard.app.Routes
import dev.silo.omniboard.extensionManager
import dev.silo.omniboard.lib.compose.OmniScreen
import dev.silo.jetpref.datastore.ui.Preference
import org.omniboard.lib.compose.stringRes

@Composable
fun ExtensionHomeScreen() = OmniScreen {
    title = stringRes(R.string.ext__home__title)
    previewFieldVisible = false

    val context = LocalContext.current
    val navController = LocalNavController.current
    val extensionManager by context.extensionManager()
    val extensionIndex = extensionManager.combinedExtensionList()

    content {
        ImportExtensionBox(navController)

        UpdateBox(extensionIndex = extensionIndex)

        Preference(
            icon = Icons.Default.Palette,
            title = stringRes(R.string.ext__list__ext_theme),
            onClick = {
                navController.navigate(Routes.Ext.List(ExtensionListScreenType.EXT_THEME, false))
            },
        )
        Preference(
            icon = Icons.Default.Keyboard,
            title = stringRes(R.string.ext__list__ext_keyboard),
            onClick = {
                navController.navigate(Routes.Ext.List(ExtensionListScreenType.EXT_KEYBOARD, false))
            },
        )
        Preference(
            icon = Icons.Default.Language,
            title = stringRes(R.string.ext__list__ext_languagepack),
            onClick = {
                navController.navigate(Routes.Ext.List(ExtensionListScreenType.EXT_LANGUAGEPACK, false))
            },
        )
    }
}
