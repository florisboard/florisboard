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

package dev.patrickgold.florisboard.app.settings.theme

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Input
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Shop
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.app.LocalNavController
import dev.patrickgold.florisboard.app.Routes
import dev.patrickgold.florisboard.app.ext.ExtensionImportScreenType
import dev.patrickgold.florisboard.extensionManager
import dev.patrickgold.florisboard.ime.theme.ThemeExtension
import dev.patrickgold.florisboard.ime.theme.ThemeManager
import dev.patrickgold.florisboard.ime.theme.ThemeMode
import dev.patrickgold.florisboard.lib.android.launchUrl
import dev.patrickgold.florisboard.lib.compose.FlorisInfoCard
import dev.patrickgold.florisboard.lib.compose.FlorisOutlinedBox
import dev.patrickgold.florisboard.lib.compose.FlorisScreen
import dev.patrickgold.florisboard.lib.compose.FlorisTextButton
import dev.patrickgold.florisboard.lib.compose.defaultFlorisOutlinedBox
import dev.patrickgold.florisboard.lib.compose.stringRes
import dev.patrickgold.florisboard.lib.ext.ExtensionComponentName
import dev.patrickgold.florisboard.lib.observeAsNonNullState
import dev.patrickgold.florisboard.themeManager
import dev.patrickgold.jetpref.datastore.model.observeAsState
import dev.patrickgold.jetpref.datastore.ui.ListPreference
import dev.patrickgold.jetpref.datastore.ui.Preference
import dev.patrickgold.jetpref.datastore.ui.PreferenceGroup

@Composable
fun ThemeScreen() = FlorisScreen {
    title = stringRes(R.string.settings__theme__title)
    previewFieldVisible = true

    val context = LocalContext.current
    val navController = LocalNavController.current
    val extensionManager by context.extensionManager()
    val themeManager by context.themeManager()

    @Composable
    fun ThemeManager.getThemeLabel(id: ExtensionComponentName): String {
        val configs by indexedThemeConfigs.observeAsState()
        configs?.get(id)?.let { return it.label }
        return id.toString()
    }

    content {
        val themeMode by prefs.theme.mode.observeAsState()
        val dayThemeId by prefs.theme.dayThemeId.observeAsState()
        val nightThemeId by prefs.theme.nightThemeId.observeAsState()

        /*Card(modifier = Modifier.padding(8.dp)) {
            Column(modifier = Modifier.padding(8.dp)) {
                Text("If you want to give feedback on the new stylesheet editor and theme engine, please do so in below linked feedback thread:\n")
                Button(onClick = {
                    context.launchUrl("https://github.com/florisboard/florisboard/discussions/1531")
                }) {
                    Text("Open Feedback Thread")
                }
            }
        }*/

        ListPreference(
            prefs.theme.mode,
            icon = Icons.Default.BrightnessAuto,
            title = stringRes(R.string.pref__theme__mode__label),
            entries = ThemeMode.listEntries(),
        )
        if (themeMode == ThemeMode.FOLLOW_TIME) {
            FlorisInfoCard(
                modifier = Modifier.padding(8.dp),
                text = """
                The theme mode "Follow time" is not available in this beta release.
            """.trimIndent()
            )
        }
        Preference(
            icon = Icons.Default.LightMode,
            title = stringRes(R.string.pref__theme__day),
            summary = themeManager.getThemeLabel(dayThemeId),
            enabledIf = { prefs.theme.mode isNotEqualTo ThemeMode.ALWAYS_NIGHT },
            onClick = {
                navController.navigate(Routes.Settings.ThemeManager(ThemeManagerScreenAction.SELECT_DAY))
            },
        )
        Preference(
            icon = Icons.Default.DarkMode,
            title = stringRes(R.string.pref__theme__night),
            summary = themeManager.getThemeLabel(nightThemeId),
            enabledIf = { prefs.theme.mode isNotEqualTo ThemeMode.ALWAYS_DAY },
            onClick = {
                navController.navigate(Routes.Settings.ThemeManager(ThemeManagerScreenAction.SELECT_NIGHT))
            },
        )

        PreferenceGroup(title = stringRes(R.string.settings__theme_manager__title_manage), iconSpaceReserved = false) {
            FlorisOutlinedBox(
                modifier = Modifier.defaultFlorisOutlinedBox(),
            ) {
                Text(
                    modifier = Modifier.padding(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 4.dp),
                    text = "You can download and install additional themes from the FlorisBoard Addons Store or import any theme extension file you have downloaded from the internet.",
                    style = MaterialTheme.typography.bodySmall,
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp),
                ) {
                    FlorisTextButton(
                        onClick = {
                            context.launchUrl("https://beta.addons.florisboard.org")
                        },
                        icon = Icons.Default.Shop,
                        text = "Visit Addons Store",//stringRes(R.string.action__edit),
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    FlorisTextButton(
                        onClick = {
                            navController.navigate(Routes.Ext.Import(ExtensionImportScreenType.EXT_THEME, null))
                        },
                        icon = Icons.AutoMirrored.Filled.Input,
                        text = stringRes(R.string.action__import),
                    )
                }
            }
            FlorisOutlinedBox(
                modifier = Modifier.defaultFlorisOutlinedBox(),
            ) {
                Text(
                    modifier = Modifier.padding(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 4.dp),
                    text = "If you are a more advanced user you can also choose to create and share your own theme extension using the stylesheet editor.",
                    style = MaterialTheme.typography.bodySmall,
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp),
                ) {
                    FlorisTextButton(
                        onClick = {
                            navController.navigate(Routes.Ext.Edit("null", ThemeExtension.SERIAL_TYPE))
                        },
                        icon = Icons.Default.Add,
                        text = stringRes(R.string.ext__editor__title_create_theme),
                    )
                }
            }
        }

        PreferenceGroup(title = "All installed themes", iconSpaceReserved = false) {
            val indexedThemeExtensions by extensionManager.themes.observeAsNonNullState()
            for (ext in indexedThemeExtensions) key(ext.meta.id) {
                FlorisOutlinedBox(
                    modifier = Modifier.defaultFlorisOutlinedBox(),
                    title = ext.meta.title,
                    subtitle = ext.meta.id,
                ) {
                    Text(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                        text = ext.meta.description ?: "",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 6.dp),
                    ) {
                        FlorisTextButton(
                            onClick = {
                                navController.navigate(Routes.Ext.View(ext.meta.id))
                            },
                            icon = Icons.Outlined.Info,
                            text = "View details",//stringRes(R.string.action__add),
                            colors = ButtonDefaults.textButtonColors(),
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        FlorisTextButton(
                            onClick = {
                                navController.navigate(Routes.Ext.Edit(ext.meta.id))
                            },
                            icon = Icons.Default.Edit,
                            text = stringRes(R.string.action__edit),
                            enabled = extensionManager.canDelete(ext),
                        )
                    }
                }
            }
        }
    }
}
