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

package dev.patrickgold.florisboard.app.settings.theme

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Input
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Brightness2
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Shop
import androidx.compose.material.icons.filled.WbTwilight
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import dev.patrickgold.florisboard.BuildConfig
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.app.FlorisPreferenceStore
import dev.patrickgold.florisboard.app.LocalNavController
import dev.patrickgold.florisboard.app.Routes
import dev.patrickgold.florisboard.app.components.ScrollableScreenColumn
import dev.patrickgold.florisboard.app.components.SegmentedListColumn
import dev.patrickgold.florisboard.app.enumDisplayEntriesOf
import dev.patrickgold.florisboard.app.ext.ExtensionImportScreenType
import dev.patrickgold.florisboard.ime.extension.ExtensionComponentName
import dev.patrickgold.florisboard.ime.extension.ExtensionType
import dev.patrickgold.florisboard.ime.theme.LocalThemeController
import dev.patrickgold.florisboard.ime.theme.PreferredThemeMode
import dev.patrickgold.florisboard.lib.compose.FlorisScreenNg
import dev.patrickgold.florisboard.lib.util.launchUrl
import dev.patrickgold.jetpref.datastore.model.collectAsState
import dev.patrickgold.jetpref.datastore.ui.ColorPickerPreference
import dev.patrickgold.jetpref.datastore.ui.ListPreference
import dev.patrickgold.jetpref.datastore.ui.LocalTimePickerPreference
import dev.patrickgold.jetpref.datastore.ui.Preference
import dev.patrickgold.jetpref.datastore.ui.isMaterialYou
import org.florisboard.lib.compose.stringRes
import org.florisboard.lib.snygg.color.ColorMappings

@Composable
fun ThemeScreen() {
    val prefs by FlorisPreferenceStore
    val context = LocalContext.current
    val navController = LocalNavController.current
    val themeController = LocalThemeController.current

    val themeIndex by themeController.effectiveThemeIndex.collectAsState()
    val mode by prefs.theme.mode.collectAsState()
    val dayThemeId by prefs.theme.dayThemeId.collectAsState()
    val nightThemeId by prefs.theme.nightThemeId.collectAsState()

    @Composable
    fun rememberThemeName(themeId: ExtensionComponentName): String {
        return remember(themeId, themeIndex) {
            themeIndex.extensions[themeId.extensionId]?.manifest?.themes
                ?.first { it.id == themeId.componentId }
                ?.name
                ?: themeId.toString()
        }
    }

    FlorisScreenNg(
        title = stringRes(R.string.settings__theme__title),
        previewFieldVisible = true,
    ) { contentPadding ->
        ScrollableScreenColumn(contentPadding) {
            SegmentedListColumn {
                val countOffset = if (mode == PreferredThemeMode.FOLLOW_TIME) 2 else 0
                val count = 4 + countOffset
                ListPreference(
                    prefs.theme.mode,
                    shapes = ListItemDefaults.segmentedShapes(0, count),
                    icon = Icons.Default.BrightnessAuto,
                    title = stringRes(R.string.pref__theme__mode__label),
                    entries = enumDisplayEntriesOf(PreferredThemeMode::class),
                )
                if (mode == PreferredThemeMode.FOLLOW_TIME) {
                    LocalTimePickerPreference(
                        prefs.theme.sunriseTime,
                        shapes = ListItemDefaults.segmentedShapes(1, count),
                        title = stringRes(R.string.pref__theme__sunrise_time__label),
                        icon = Icons.Default.WbTwilight,
                    )
                    LocalTimePickerPreference(
                        prefs.theme.sunsetTime,
                        shapes = ListItemDefaults.segmentedShapes(2, count),
                        title = stringRes(R.string.pref__theme__sunset_time__label),
                        icon = Icons.Default.Brightness2,
                    )
                }
                Preference(
                    shapes = ListItemDefaults.segmentedShapes(1 + countOffset, count),
                    icon = Icons.Default.LightMode,
                    title = stringRes(R.string.pref__theme__day),
                    summary = rememberThemeName(dayThemeId),
                    enabledIf = { prefs.theme.mode isNotEqualTo PreferredThemeMode.ALWAYS_NIGHT },
                    onClick = {
                        navController.navigate(Routes.Settings.ThemeSelection(ThemeSelectionScreenAction.SELECT_DAY))
                    },
                )
                Preference(
                    shapes = ListItemDefaults.segmentedShapes(2 + countOffset, count),
                    icon = Icons.Default.DarkMode,
                    title = stringRes(R.string.pref__theme__night),
                    summary = rememberThemeName(nightThemeId),
                    enabledIf = { prefs.theme.mode isNotEqualTo PreferredThemeMode.ALWAYS_DAY },
                    onClick = {
                        navController.navigate(Routes.Settings.ThemeSelection(ThemeSelectionScreenAction.SELECT_NIGHT))
                    },
                )
                ColorPickerPreference(
                    pref = prefs.theme.accentColor,
                    shapes = ListItemDefaults.segmentedShapes(3 + countOffset, count),
                    title = stringRes(R.string.pref__theme__theme_accent_color__label),
                    defaultValueLabel = stringRes(R.string.action__default),
                    icon = Icons.Default.ColorLens,
                    defaultColors = ColorMappings.colors,
                    showAlphaSlider = false,
                    enableAdvancedLayout = true,
                    colorOverride = {
                        if (it.isMaterialYou(context)) {
                            Color.Unspecified
                        } else {
                            it
                        }
                    },
                )
            }

            SegmentedListColumn {
                val count = 3
                SegmentedListItem(
                    onClick = { context.launchUrl("https://${BuildConfig.FLADDONS_STORE_URL}/") },
                    shapes = ListItemDefaults.segmentedShapes(0, count),
                    verticalAlignment = Alignment.CenterVertically,
                    leadingContent = { Icon(Icons.Default.Shop, null) },
                    content = { Text("Need inspiration?") },
                    supportingContent = { Text("Visit the FlorisBoard Addons Store, an official place for the community to share themes.") },
                )
                SegmentedListItem(
                    onClick = { navController.navigate(Routes.Ext.Import(ExtensionImportScreenType.EXT_THEME)) },
                    shapes = ListItemDefaults.segmentedShapes(1, count),
                    verticalAlignment = Alignment.CenterVertically,
                    leadingContent = { Icon(Icons.AutoMirrored.Filled.Input, null) },
                    content = { Text("Already have a theme downloaded?") },
                    supportingContent = { Text("Click here to import it.") },
                )
                SegmentedListItem(
                    onClick = { navController.navigate(Routes.Ext.Edit(null, ExtensionType.THEME)) },
                    shapes = ListItemDefaults.segmentedShapes(2, count),
                    verticalAlignment = Alignment.CenterVertically,
                    leadingContent = { Icon(Icons.Filled.Add, null) },
                    content = { Text("Feeling creative?") },
                    supportingContent = { Text("Create your own theme with the advanced built-in theme editor.") },
                )
            }

            SegmentedListColumn(title = "Installed themes") {
                val extensions = themeIndex.extensions.values.toList()
                for ((index, extension) in extensions.withIndex()) key(extension.manifest.meta.id) {
                    SegmentedListItem(
                        onClick = { navController.navigate(Routes.Ext.Detail(extension.manifest.meta.id)) },
                        shapes = ListItemDefaults.segmentedShapes(index, extensions.size),
                        verticalAlignment = Alignment.CenterVertically,
                        leadingContent = { Icon(Icons.Default.Extension, null) },
                        overlineContent = { Text(extension.manifest.meta.id) },
                        content = { Text(extension.manifest.meta.title) },
                        supportingContent = extension.manifest.meta.description?.let { description ->
                            { Text(description, maxLines = 2, overflow = TextOverflow.Ellipsis) }
                        },
                    )
                }
            }
        }
    }
}
