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

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ExtendedFloatingActionButton
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import dev.patrickgold.florisboard.BuildConfig
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.app.LocalNavController
import dev.patrickgold.florisboard.app.Routes
import dev.patrickgold.florisboard.app.settings.advanced.Backup
import dev.patrickgold.florisboard.app.settings.advanced.Restore
import dev.patrickgold.florisboard.appContext
import dev.patrickgold.florisboard.ime.core.DisplayLanguageNamesIn
import dev.patrickgold.florisboard.ime.keyboard.LayoutType
import dev.patrickgold.florisboard.ime.nlp.han.HanShapeBasedLanguageProvider
import dev.patrickgold.florisboard.keyboardManager
import dev.patrickgold.florisboard.lib.android.readToFile
import dev.patrickgold.florisboard.lib.android.showLongToast
import dev.patrickgold.florisboard.lib.compose.FlorisScreen
import dev.patrickgold.florisboard.lib.compose.FlorisWarningCard
import dev.patrickgold.florisboard.lib.compose.stringRes
import dev.patrickgold.florisboard.lib.io.ZipUtils
import dev.patrickgold.florisboard.lib.io.parentDir
import dev.patrickgold.florisboard.lib.io.readJson
import dev.patrickgold.florisboard.lib.io.subFile
import dev.patrickgold.florisboard.lib.observeAsNonNullState
import dev.patrickgold.florisboard.nlpManager
import dev.patrickgold.florisboard.subtypeManager
import dev.patrickgold.jetpref.datastore.model.observeAsState
import dev.patrickgold.jetpref.datastore.ui.ListPreference
import dev.patrickgold.jetpref.datastore.ui.Preference
import dev.patrickgold.jetpref.datastore.ui.PreferenceGroup
import java.io.FileNotFoundException

@Composable
fun LocalizationScreen() = FlorisScreen {
    title = stringRes(R.string.settings__localization__title)
    previewFieldVisible = true
    iconSpaceReserved = false

    val navController = LocalNavController.current
    val context = LocalContext.current
    val keyboardManager by context.keyboardManager()
    val subtypeManager by context.subtypeManager()

    floatingActionButton {
        ExtendedFloatingActionButton(
            icon = { Icon(
                painter = painterResource(R.drawable.ic_add),
                contentDescription = stringRes(R.string.settings__localization__subtype_add_title),
            ) },
            text = { Text(
                text = stringRes(R.string.settings__localization__subtype_add_title),
            ) },
            onClick = { navController.navigate(Routes.Settings.SubtypeAdd) },
        )
    }

    val loadLanguagePackFromFileSystemLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri ->
            if (uri == null) return@rememberLauncherForActivityResult
            runCatching {
                // TODO: quick and dirty. Replaces internal one, if any.
                //  Should be replaced by other mechanisms, e.g. per-addon zip files.
                val dbPath = context.filesDir.subFile(HanShapeBasedLanguageProvider.DB_PATH)
                dbPath.parentDir?.mkdirs()
                context.contentResolver.readToFile(uri, dbPath)
                context.showLongToast(R.string.settings__localization__language_pack_import_success)
            }.onFailure { error ->
                context.showLongToast(R.string.settings__localization__language_pack_import_failure, "error_message" to error.localizedMessage)
            }
        },
    )


    content {
        ListPreference(
            prefs.localization.displayLanguageNamesIn,
            title = stringRes(R.string.settings__localization__display_language_names_in__label),
            entries = DisplayLanguageNamesIn.listEntries(),
        )
        Preference(
            title = stringRes(R.string.settings__localization__language_pack_import_label),
            summary = stringRes(R.string.settings__localization__language_pack_import_summary),
            onClick = {
                runCatching {
                    loadLanguagePackFromFileSystemLauncher.launch("*/*")
                }.onFailure { error ->
                    context.showLongToast(R.string.settings__localization__language_pack_import_failure, "error_message" to error.localizedMessage)
                }
            },
        )
        PreferenceGroup(title = stringRes(R.string.settings__localization__group_subtypes__label)) {
            val subtypes by subtypeManager.subtypesFlow.collectAsState()
            if (subtypes.isEmpty()) {
                FlorisWarningCard(
                    modifier = Modifier.padding(all = 8.dp),
                    text = stringRes(R.string.settings__localization__subtype_no_subtypes_configured_warning),
                )
            } else {
                val currencySets by keyboardManager.resources.currencySets.observeAsNonNullState()
                val layouts by keyboardManager.resources.layouts.observeAsNonNullState()
                val displayLanguageNamesIn by prefs.localization.displayLanguageNamesIn.observeAsState()
                for (subtype in subtypes) {
                    val cMeta = layouts[LayoutType.CHARACTERS]?.get(subtype.layoutMap.characters)
                    val sMeta = layouts[LayoutType.SYMBOLS]?.get(subtype.layoutMap.symbols)
                    val currMeta = currencySets[subtype.currencySet]
                    val summary = stringRes(
                        id = R.string.settings__localization__subtype_summary,
                        "characters_name" to (cMeta?.label ?: "null"),
                        "symbols_name" to (sMeta?.label ?: "null"),
                        "currency_set_name" to (currMeta?.label ?: "null"),
                    )
                    Preference(
                        title = when (displayLanguageNamesIn) {
                            DisplayLanguageNamesIn.SYSTEM_LOCALE -> subtype.primaryLocale.displayName()
                            DisplayLanguageNamesIn.NATIVE_LOCALE -> subtype.primaryLocale.displayName(subtype.primaryLocale)
                        },
                        summary = summary,
                        onClick = { navController.navigate(
                            Routes.Settings.SubtypeEdit(subtype.id)
                        ) },
                    )
                }
            }
        }

        //PreferenceGroup(title = stringRes(R.string.settings__localization__group_layouts__label)) {
        //}
    }
}
