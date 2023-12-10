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

package dev.patrickgold.florisboard.app.settings.localization

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.app.LocalNavController
import dev.patrickgold.florisboard.app.Routes
import dev.patrickgold.florisboard.app.ext.ExtensionImportScreenType
import dev.patrickgold.florisboard.app.florisPreferenceModel
import dev.patrickgold.florisboard.extensionManager
import dev.patrickgold.florisboard.lib.android.launchUrl
import dev.patrickgold.florisboard.lib.android.showLongToast
import dev.patrickgold.florisboard.lib.android.showShortToast
import dev.patrickgold.florisboard.lib.compose.FlorisConfirmDeleteDialog
import dev.patrickgold.florisboard.lib.compose.FlorisOutlinedBox
import dev.patrickgold.florisboard.lib.compose.FlorisScreen
import dev.patrickgold.florisboard.lib.compose.FlorisTextButton
import dev.patrickgold.florisboard.lib.compose.defaultFlorisOutlinedBox
import dev.patrickgold.florisboard.lib.compose.rippleClickable
import dev.patrickgold.florisboard.lib.compose.stringRes
import dev.patrickgold.florisboard.lib.ext.Extension
import dev.patrickgold.florisboard.lib.observeAsNonNullState
import dev.patrickgold.jetpref.datastore.ui.Preference
import dev.patrickgold.jetpref.material.ui.JetPrefListItem

@Composable
fun DictionaryManagerScreen() = FlorisScreen {
    title = "Manage installed dictionaries"

    val prefs by florisPreferenceModel()
    val navController = LocalNavController.current
    val context = LocalContext.current
    val extensionManager by context.extensionManager()

    val indexedDictionaryExtensions by extensionManager.dictionaryExtensions.observeAsNonNullState()
    var dictionaryExtensionToDelete by remember { mutableStateOf<Extension?>(null) }

    content {
        FlorisOutlinedBox(
            modifier = Modifier.defaultFlorisOutlinedBox(),
        ) {
            this@content.Preference(
                onClick = {
                    context.launchUrl("https://github.com/florisboard/nlp/blob/main/data/dicts/v0~draft1/README.md")
                },
                iconId = R.drawable.ic_input,
                title = "Download dictionaries",
            )
        }
        FlorisOutlinedBox(
            modifier = Modifier.defaultFlorisOutlinedBox(),
        ) {
            this@content.Preference(
                onClick = { navController.navigate(
                    Routes.Ext.Import(ExtensionImportScreenType.EXT_DICTIONARY, null)
                ) },
                iconId = R.drawable.ic_input,
                title = stringRes(R.string.action__import),
            )
        }
        for (ext in indexedDictionaryExtensions) key(ext.meta.id) {
            FlorisOutlinedBox(
                modifier = Modifier.defaultFlorisOutlinedBox(),
                title = ext.meta.title,
                onTitleClick = { navController.navigate(Routes.Ext.View(ext.meta.id)) },
                subtitle = ext.meta.id,
                onSubtitleClick = { navController.navigate(Routes.Ext.View(ext.meta.id)) },
            ) {
                Column(
                    // Allowing horizontal scroll to fit translations in descriptions.
                    Modifier.horizontalScroll(rememberScrollState()).width(intrinsicSize = IntrinsicSize.Max),
                ) {
                    for (dictionary in ext.dictionaries) key(ext.meta.id, dictionary.id) {
                        JetPrefListItem(
                            modifier = Modifier.rippleClickable {
                                //setLanguagePack(ext.meta.id, dictionary.id)
                            },
                            text = dictionary.label,
                        )
                    }
                }
                if (extensionManager.canDelete(ext)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 6.dp),
                    ) {
                        FlorisTextButton(
                            onClick = {
                                dictionaryExtensionToDelete = ext
                            },
                            icon = painterResource(R.drawable.ic_delete),
                            text = stringRes(R.string.action__delete),
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = MaterialTheme.colors.error,
                            ),
                        )
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        if (dictionaryExtensionToDelete != null) {
            FlorisConfirmDeleteDialog(
                onConfirm = {
                    runCatching {
                        extensionManager.delete(dictionaryExtensionToDelete!!)
                        context.showShortToast("Successfully deleted dictionary")
                    }.onFailure { error ->
                        context.showLongToast(
                            R.string.error__snackbar_message,
                            "error_message" to error.localizedMessage,
                        )
                    }
                    dictionaryExtensionToDelete = null
                },
                onDismiss = { dictionaryExtensionToDelete = null },
                what = dictionaryExtensionToDelete!!.meta.title,
            )
        }
    }
}
