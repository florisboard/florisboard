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

package dev.patrickgold.florisboard.app.ui.settings.theme

//import androidx.compose.material.LocalContentColor
//import androidx.compose.runtime.Composable
//import androidx.compose.runtime.DisposableEffect
//import androidx.compose.runtime.remember
//import androidx.compose.runtime.saveable.rememberSaveable
//import androidx.compose.ui.platform.LocalContext
//import dev.patrickgold.florisboard.R
//import dev.patrickgold.florisboard.app.LocalNavController
//import dev.patrickgold.florisboard.app.prefs.florisPreferenceModel
//import dev.patrickgold.florisboard.app.res.stringRes
//import dev.patrickgold.florisboard.app.ui.components.FlorisScreen
//import dev.patrickgold.florisboard.app.ui.ext.ExtensionNotFoundScreen
//import dev.patrickgold.florisboard.cacheManager
//import dev.patrickgold.florisboard.extensionManager
//import dev.patrickgold.florisboard.ime.theme.ThemeExtension
//import dev.patrickgold.florisboard.ime.theme.ThemeExtensionEditor
//import dev.patrickgold.florisboard.res.cache.CacheManager
//import dev.patrickgold.florisboard.themeManager
//import dev.patrickgold.jetpref.datastore.ui.ExperimentalJetPrefDatastoreUi
//import java.util.*
//
//@Composable
//fun ThemeComponentEditorScreen(uuid: String) {
//    val context = LocalContext.current
//    val cacheManager by context.cacheManager()
//    val extensionManager by context.extensionManager()
//
//    val uuid = rememberSaveable { UUID.randomUUID().toString() }
//    val cacheWorkspace = remember {
//        val workspace = cacheManager.themeExtEditor.getWorkspaceByUuid(uuid)
//        if (workspace != null) {
//            workspace
//        } else {
//            val newWorkspace = cacheManager.themeExtEditor.new(uuid)
//            extensionManager.getExtensionById(id)?.let { ext ->
//                if (ext is ThemeExtension) {
//                    newWorkspace.editor = ext.edit()
//                }
//            }
//            newWorkspace
//        }
//    }
//
//    if (cacheWorkspace.editor != null) {
//        EditorScreen(workspace = cacheWorkspace)
//    } else {
//        ExtensionNotFoundScreen(id = id)
//    }
//}
//
//@OptIn(ExperimentalJetPrefDatastoreUi::class)
//@Composable
//private fun EditorScreen(workspace: CacheManager.ExtEditorWorkspace<ThemeExtensionEditor>) = FlorisScreen {
//    title = stringRes(R.string.settings__theme_editor__title)
//    previewFieldVisible = true
//
//    val prefs by florisPreferenceModel()
//    val navController = LocalNavController.current
//    val context = LocalContext.current
//    val cacheManager by context.cacheManager()
//    val extensionManager by context.extensionManager()
//    val themeManager by context.themeManager()
//
//    val previewThemeInfo = remember { null }
//
//    content {
//        DisposableEffect(previewThemeInfo) {
//            themeManager.previewThemeInfo = previewThemeInfo
//            onDispose {
//                themeManager.previewThemeInfo = null
//            }
//        }
//        val grayColor = LocalContentColor.current.copy(alpha = 0.56f)
//    }
//}
