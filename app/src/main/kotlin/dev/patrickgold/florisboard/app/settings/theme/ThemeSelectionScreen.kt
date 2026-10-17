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

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.app.FlorisPreferenceStore
import dev.patrickgold.florisboard.app.components.ScrollableScreenColumn
import dev.patrickgold.florisboard.app.components.SegmentedListColumn
import dev.patrickgold.florisboard.app.components.resetColors
import dev.patrickgold.florisboard.ime.extension.ExtensionComponentName
import dev.patrickgold.florisboard.ime.theme.LocalThemeController
import dev.patrickgold.florisboard.lib.compose.FlorisScreenNg
import dev.patrickgold.jetpref.datastore.model.collectAsState
import kotlinx.coroutines.launch
import org.florisboard.lib.compose.stringRes

enum class ThemeSelectionScreenAction(val id: String) {
    SELECT_DAY("select-day"),
    SELECT_NIGHT("select-night");
}

@Composable
fun ThemeSelectionScreen(action: ThemeSelectionScreenAction?) {
    val prefs by FlorisPreferenceStore
    val themeController = LocalThemeController.current
    val scope = rememberCoroutineScope()

    val title = stringRes(when (action) {
        ThemeSelectionScreenAction.SELECT_DAY -> R.string.settings__theme_manager__title_day
        ThemeSelectionScreenAction.SELECT_NIGHT -> R.string.settings__theme_manager__title_night
        else -> error("Theme manager screen action must not be null")
    })
    val grayColor = LocalContentColor.current.copy(alpha = 0.56f)

    val themeIndex by themeController.effectiveThemeIndex.collectAsState()
    val themeIdPref = remember(action) {
        when (action) {
            ThemeSelectionScreenAction.SELECT_DAY -> prefs.theme.dayThemeId
            ThemeSelectionScreenAction.SELECT_NIGHT -> prefs.theme.nightThemeId
        }
    }

    val activeThemeId by themeIdPref.collectAsState()
    DisposableEffect(activeThemeId) {
        themeController.activePreviewThemeId.value = activeThemeId
        onDispose {
            themeController.activePreviewThemeId.value = null
        }
    }

    fun setTheme(extId: String, componentId: String) {
        val extComponentName = ExtensionComponentName(extId, componentId)
        scope.launch {
            themeIdPref.set(extComponentName)
        }
    }

    FlorisScreenNg(
        title = title,
        previewFieldVisible = true,
        actions = {
            FilledTonalIconButton(
                onClick = {
                    scope.launch {
                        themeIdPref.reset()
                    }
                },
                enabled = activeThemeId != themeIdPref.default,
                colors = IconButtonDefaults.resetColors(),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_reset_settings),
                    contentDescription = null,
                )
            }
        },
    ) { contentPadding ->
        ScrollableScreenColumn(contentPadding) {
            for ((extensionId, extension) in themeIndex.extensions) key(extensionId) {
                val components = extension.manifest.themes.sortedBy { it.name }
                SegmentedListColumn(title = extension.manifest.meta.title) {
                    for ((index, component) in components.withIndex()) key(extensionId, component.id) {
                        val selected = activeThemeId.extensionId == extensionId &&
                            activeThemeId.componentId == component.id
                        SegmentedListItem(
                            selected = selected,
                            onClick = { setTheme(extensionId, component.id) },
                            shapes = ListItemDefaults.segmentedShapes(index, components.size),
                            verticalAlignment = Alignment.CenterVertically,
                            leadingContent = {
                                RadioButton(
                                    selected = selected,
                                    onClick = null,
                                )
                            },
                            content = { Text(component.name) },
                            trailingContent = {
                                Icon(
                                    modifier = Modifier.size(ButtonDefaults.IconSize),
                                    imageVector = if (component.isNightTheme) {
                                        Icons.Default.DarkMode
                                    } else {
                                        Icons.Default.LightMode
                                    },
                                    contentDescription = null,
                                    tint = grayColor,
                                )
                            },
                        )
                    }
                }
            }
        }
    }
}
