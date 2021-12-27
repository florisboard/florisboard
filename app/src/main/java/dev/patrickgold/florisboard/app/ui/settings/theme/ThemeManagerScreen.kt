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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.LocalContentColor
import androidx.compose.material.RadioButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.app.LocalNavController
import dev.patrickgold.florisboard.app.prefs.florisPreferenceModel
import dev.patrickgold.florisboard.app.res.stringRes
import dev.patrickgold.florisboard.app.ui.Routes
import dev.patrickgold.florisboard.app.ui.components.FlorisOutlinedBox
import dev.patrickgold.florisboard.app.ui.components.FlorisScreen
import dev.patrickgold.florisboard.app.ui.components.rippleClickable
import dev.patrickgold.florisboard.common.observeAsNonNullState
import dev.patrickgold.florisboard.extensionManager
import dev.patrickgold.florisboard.ime.theme.ThemeExtensionComponent
import dev.patrickgold.florisboard.res.ext.ExtensionComponentName
import dev.patrickgold.florisboard.themeManager
import dev.patrickgold.jetpref.datastore.model.observeAsState
import dev.patrickgold.jetpref.datastore.ui.ExperimentalJetPrefDatastoreUi
import dev.patrickgold.jetpref.datastore.ui.Preference
import dev.patrickgold.jetpref.material.ui.JetPrefListItem

enum class ThemeManagerScreenAction(val id: String) {
    SELECT_DAY("select_day"),
    SELECT_NIGHT("select_night"),
    MANAGE("manage_installed_themes");
}

@OptIn(ExperimentalJetPrefDatastoreUi::class)
@Composable
fun ThemeManagerScreen(action: ThemeManagerScreenAction?) = FlorisScreen {
    title = stringRes(when (action) {
        ThemeManagerScreenAction.SELECT_DAY -> R.string.settings__theme_manager__title_day
        ThemeManagerScreenAction.SELECT_NIGHT -> R.string.settings__theme_manager__title_night
        ThemeManagerScreenAction.MANAGE -> R.string.settings__theme_manager__title_manage
        else -> error("Theme manager screen action must not be null")
    })

    val prefs by florisPreferenceModel()
    val navController = LocalNavController.current
    val context = LocalContext.current
    val extensionManager by context.extensionManager()
    val themeManager by context.themeManager()

    val indexedThemeConfigs by themeManager.indexedThemeConfigs.observeAsNonNullState()
    val selectedManagerThemeId = remember { mutableStateOf<ExtensionComponentName?>(null) }
    val extGroupedThemes = remember(indexedThemeConfigs) {
        buildMap<String, MutableList<ThemeExtensionComponent>> {
            for ((componentName, config) in indexedThemeConfigs) {
                if (!containsKey(componentName.extensionId)) {
                    put(componentName.extensionId, mutableListOf())
                }
                get(componentName.extensionId)!!.add(config)
            }
        }.mapValues { (_, configs) -> configs.sortedBy { it.label } }
    }

    fun getThemeIdPref() = when (action) {
        ThemeManagerScreenAction.SELECT_DAY -> prefs.theme.dayThemeId
        ThemeManagerScreenAction.SELECT_NIGHT -> prefs.theme.nightThemeId
        ThemeManagerScreenAction.MANAGE -> error("internal error in manager logic")
    }

    fun setTheme(extId: String, componentId: String) {
        val extComponentName = ExtensionComponentName(extId, componentId)
        when (action) {
            ThemeManagerScreenAction.SELECT_DAY,
            ThemeManagerScreenAction.SELECT_NIGHT -> {
                getThemeIdPref().set(extComponentName)
            }
            ThemeManagerScreenAction.MANAGE -> {
                selectedManagerThemeId.value = extComponentName
            }
        }
    }

    val activeThemeId by when (action) {
        ThemeManagerScreenAction.SELECT_DAY,
        ThemeManagerScreenAction.SELECT_NIGHT -> getThemeIdPref().observeAsState()
        ThemeManagerScreenAction.MANAGE -> selectedManagerThemeId
    }

    content {
        DisposableEffect(activeThemeId) {
            themeManager.previewThemeId = activeThemeId
            onDispose {
                themeManager.previewThemeId = null
            }
        }
        val grayColor = LocalContentColor.current.copy(alpha = 0.56f)
        if (action == ThemeManagerScreenAction.MANAGE) {
            FlorisOutlinedBox(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                Column {
                    this@content.Preference(
                        onClick = { },
                        iconId = R.drawable.ic_add,
                        title = stringRes(R.string.settings__theme_manager__create_empty),
                    )
                    this@content.Preference(
                        onClick = { },
                        iconId = R.drawable.ic_add,
                        title = stringRes(R.string.settings__theme_manager__create_from_selected),
                        enabledIf = { activeThemeId != null },
                    )
                    this@content.Preference(
                        onClick = { },
                        iconId = R.drawable.ic_input,
                        title = stringRes(R.string.assets__action__import),
                    )
                }
            }
        }
        for ((extensionId, configs) in extGroupedThemes) {
            FlorisOutlinedBox(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                title = remember {
                    extensionManager.getExtensionById(extensionId)?.meta?.title ?: extensionId
                },
                onTitleClick = { navController.navigate(Routes.Ext.View(extensionId)) }
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        modifier = Modifier
                            .padding(start = 10.dp, bottom = 4.dp)
                            .rippleClickable {
                                navController.navigate(Routes.Ext.View(extensionId))
                            }
                            .padding(start = 6.dp, end = 6.dp, bottom = 4.dp),
                        text = extensionId,
                        color = grayColor,
                        fontWeight = FontWeight.Normal,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                    )
                    for (config in configs) {
                        key(extensionId, config.id) {
                            JetPrefListItem(
                                modifier = Modifier.rippleClickable {
                                    setTheme(extensionId, config.id)
                                },
                                icon = {
                                    RadioButton(
                                        selected = activeThemeId?.extensionId == extensionId &&
                                            activeThemeId?.componentId == config.id,
                                        onClick = null,
                                    )
                                },
                                text = config.label,
                                trailing = {
                                    Row {
                                        Icon(
                                            modifier = Modifier.size(18.dp),
                                            painter = painterResource(if (config.isNightTheme) {
                                                R.drawable.ic_dark_mode
                                            } else {
                                                R.drawable.ic_light_mode
                                            }),
                                            contentDescription = null,
                                            tint = grayColor,
                                        )
                                    }
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}
