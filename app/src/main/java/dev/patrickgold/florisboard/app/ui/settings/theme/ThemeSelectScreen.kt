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

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.RadioButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.app.LocalNavController
import dev.patrickgold.florisboard.app.prefs.florisPreferenceModel
import dev.patrickgold.florisboard.app.res.stringRes
import dev.patrickgold.florisboard.app.ui.Routes
import dev.patrickgold.florisboard.app.ui.components.FlorisScreen
import dev.patrickgold.florisboard.app.ui.components.rippleClickable
import dev.patrickgold.florisboard.common.observeAsNonNullState
import dev.patrickgold.florisboard.ime.theme.ThemeConfig
import dev.patrickgold.florisboard.res.ext.ExtensionComponentName
import dev.patrickgold.florisboard.themeManager
import dev.patrickgold.jetpref.datastore.model.observeAsState
import dev.patrickgold.jetpref.datastore.ui.ExperimentalJetPrefDatastoreUi
import dev.patrickgold.jetpref.material.ui.JetPrefListItem

enum class ThemeSelectScreenType(val id: String) {
    DAY("day"),
    NIGHT("night");
}

@OptIn(ExperimentalJetPrefDatastoreUi::class)
@Composable
fun ThemeSelectScreen(type: ThemeSelectScreenType?) = FlorisScreen {
    title = stringRes(when (type) {
        ThemeSelectScreenType.DAY -> R.string.settings__theme_manager__title_day
        else -> R.string.settings__theme_manager__title_night
    })

    val prefs by florisPreferenceModel()
    val navController = LocalNavController.current
    val context = LocalContext.current
    val themeManager by context.themeManager()

    fun getThemeIdPref() = when (type) {
        ThemeSelectScreenType.DAY -> prefs.theme.dayThemeId
        else -> prefs.theme.nightThemeId
    }

    fun setTheme(extId: String, componentId: String) {
        val extComponentName = ExtensionComponentName(extId, componentId)
        getThemeIdPref().set(extComponentName)
    }

    val indexedThemeConfigs by themeManager.indexedThemeConfigs.observeAsNonNullState()
    val activeThemeId by getThemeIdPref().observeAsState()

    val extGroupedThemes = remember(indexedThemeConfigs) {
        buildMap<String, MutableList<ThemeConfig>> {
            for ((componentName, config) in indexedThemeConfigs) {
                if (!containsKey(componentName.extensionId)) {
                    put(componentName.extensionId, mutableListOf())
                }
                get(componentName.extensionId)!!.add(config)
            }
        }.mapValues { (_, configs) -> configs.sortedBy { it.label } }
    }

    content {
        DisposableEffect(activeThemeId) {
            themeManager.previewThemeId = activeThemeId
            onDispose {
                themeManager.previewThemeId = null
            }
        }
        for ((extensionId, configs) in extGroupedThemes) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .rippleClickable {
                            navController.navigate(Routes.Ext.View(extensionId))
                        }
                        .padding(16.dp),
                    text = extensionId,
                    color = MaterialTheme.colors.onBackground,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                for (config in configs) {
                    key(extensionId, config.id) {
                        JetPrefListItem(
                            modifier = Modifier.rippleClickable {
                                setTheme(extensionId, config.id)
                            },
                            icon = {
                                RadioButton(
                                    selected = activeThemeId.extensionId == extensionId &&
                                        activeThemeId.componentId == config.id,
                                    onClick = null,
                                )
                            },
                            text = config.label,
                        )
                    }
                }
            }
        }
    }
}
