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

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.app.LocalNavController
import dev.patrickgold.florisboard.app.Routes
import dev.patrickgold.florisboard.ime.core.DisplayLanguageNamesIn
import dev.patrickgold.florisboard.ime.keyboard.LayoutType
import dev.patrickgold.florisboard.keyboardManager
import dev.patrickgold.florisboard.lib.compose.FlorisChip
import dev.patrickgold.florisboard.lib.compose.FlorisOutlinedBox
import dev.patrickgold.florisboard.lib.compose.FlorisScreen
import dev.patrickgold.florisboard.lib.compose.FlorisTextButton
import dev.patrickgold.florisboard.lib.compose.defaultFlorisOutlinedBox
import dev.patrickgold.florisboard.lib.compose.florisHorizontalScroll
import dev.patrickgold.florisboard.lib.compose.stringRes
import dev.patrickgold.florisboard.lib.observeAsNonNullState
import dev.patrickgold.florisboard.nlpManager
import dev.patrickgold.florisboard.plugin.FlorisPluginFeature
import dev.patrickgold.florisboard.plugin.IndexedPlugin
import dev.patrickgold.florisboard.plugin.IndexedPluginState
import dev.patrickgold.florisboard.subtypeManager
import dev.patrickgold.jetpref.datastore.model.observeAsState
import dev.patrickgold.jetpref.datastore.ui.Preference
import dev.patrickgold.jetpref.datastore.ui.PreferenceGroup
import dev.patrickgold.jetpref.material.ui.JetPrefListItem

private val VerticalGroupMargin = 24.dp

@Composable
fun LocalizationScreen() = FlorisScreen {
    title = stringRes(R.string.settings__localization__title)
    previewFieldVisible = true
    iconSpaceReserved = false

    val navController = LocalNavController.current
    val context = LocalContext.current
    val keyboardManager by context.keyboardManager()
    val nlpManager by context.nlpManager()
    val subtypeManager by context.subtypeManager()

    content {
        PreferenceGroup(title = stringRes(R.string.settings__localization__title)) {
            FlorisOutlinedBox(
                modifier = Modifier.defaultFlorisOutlinedBox(),
                title = stringRes(R.string.settings__localization__group_subtypes__label),
            ) {
                val subtypes by subtypeManager.subtypesFlow.collectAsState()
                if (subtypes.isEmpty()) {
                    Text(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        text = stringRes(R.string.settings__localization__subtype_no_subtypes_configured_warning),
                        fontStyle = FontStyle.Italic,
                        style = MaterialTheme.typography.body2,
                    )
                } else {
                    val currencySets by keyboardManager.resources.currencySets.observeAsNonNullState()
                    val layouts by keyboardManager.resources.layouts.observeAsNonNullState()
                    val displayLanguageNamesIn by this@content.prefs.localization.displayLanguageNamesIn.observeAsState()
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
                        JetPrefListItem(
                            modifier = Modifier.clickable {
                                navController.navigate(
                                    Routes.Settings.SubtypeEdit(subtype.id)
                                )
                            },
                            text = when (displayLanguageNamesIn) {
                                DisplayLanguageNamesIn.SYSTEM_LOCALE -> subtype.primaryLocale.displayName()
                                DisplayLanguageNamesIn.NATIVE_LOCALE -> subtype.primaryLocale.displayName(subtype.primaryLocale)
                            },
                            secondaryText = summary,
                        )
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp),
                ) {
                    Spacer(modifier = Modifier.weight(1f))
                    FlorisTextButton(
                        onClick = {
                            navController.navigate(Routes.Settings.SubtypeAdd)
                        },
                        icon = painterResource(R.drawable.ic_add),
                        text = stringRes(R.string.settings__localization__subtype_add_title),
                    )
                }
            }

            FlorisOutlinedBox(
                modifier = Modifier.defaultFlorisOutlinedBox(),
                title = "Your extensions",
            ) {
                this@content.Preference(
                    onClick = {
                        navController.navigate(
                            Routes.Settings.LanguagePackManager(LanguagePackManagerScreenAction.MANAGE)
                        )
                    },
                    title = stringRes(R.string.settings__localization__language_pack_title),
                )
                this@content.Preference(
                    title = "Manage keyboard layouts (not yet implemented)",
                    enabledIf = { false },
                )
            }
        }

        PreferenceGroup(
            modifier = Modifier.padding(top = VerticalGroupMargin),
            title = "Internal plugins",
        ) {
            val plugins by nlpManager.plugins.pluginIndexFlow.collectAsState()
            val (validPlugins, invalidPlugins) = remember(plugins) {
                plugins.filter { it.isInternalPlugin() }.partition { it.isValid() }
            }
            for (plugin in validPlugins) {
                FlorisPluginBox(plugin)
            }
            for (plugin in invalidPlugins) {
                FlorisInvalidPluginBox(plugin)
            }
        }

        PreferenceGroup(
            modifier = Modifier.padding(top = VerticalGroupMargin),
            title = "External plugins",
        ) {
            FlorisOutlinedBox(modifier = Modifier.defaultFlorisOutlinedBox()) {
                Text(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    text = "External plugins are currently unsupported but planned to be supported in the next few alpha releases!",
                    fontStyle = FontStyle.Italic,
                    style = MaterialTheme.typography.body2,
                )
            }
        }

        Spacer(Modifier.height(VerticalGroupMargin))
    }
}

@Composable
private fun FlorisPluginBox(plugin: IndexedPlugin) {
    val navController = LocalNavController.current
    val packageContext = plugin.packageContext()
    val settingsActivityIntent = plugin.settingsActivityIntent()
    val configurationRoute = if (plugin.isInternalPlugin()) plugin.configurationRoute()?.takeIf { route ->
        navController.graph.findNode(route) != null
    } else null
    FlorisOutlinedBox(
        modifier = Modifier.defaultFlorisOutlinedBox(),
        title = plugin.metadata.title.getOrNull(packageContext).toString(),
        subtitle = plugin.metadata.id,
    ) {
        Text(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            text = plugin.metadata.shortDescription?.getOrNull(packageContext).toString(),
            style = MaterialTheme.typography.body2,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .florisHorizontalScroll(),
        ) {
            for ((n, feature) in plugin.metadata.features().withIndex()) {
                if (n > 0) {
                    Spacer(modifier = Modifier.width(8.dp))
                }
                FlorisChip(text = florisPluginFeatureToString(feature))
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp),
        ) {
            FlorisTextButton(
                onClick = {
                    navController.navigate(Routes.Plugin.View(plugin.metadata.id))
                },
                icon = painterResource(id = R.drawable.ic_info),
                text = stringRes(R.string.action__info),
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colors.onBackground,
                ),
            )
            Spacer(modifier = Modifier.weight(1f))
            FlorisTextButton(
                onClick = {
                    if (settingsActivityIntent != null) {
                        packageContext.startActivity(settingsActivityIntent)
                    } else if (configurationRoute != null) {
                        navController.navigate(configurationRoute)
                    }
                },
                icon = painterResource(R.drawable.ic_settings),
                text = stringRes(R.string.action__configure),
                enabled = configurationRoute != null || settingsActivityIntent != null,
            )
        }
    }
}

@Composable
private fun FlorisInvalidPluginBox(plugin: IndexedPlugin) {
    val reason = (plugin.state as IndexedPluginState.Error).toString()
    FlorisOutlinedBox(modifier = Modifier.defaultFlorisOutlinedBox()) {
        Text(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            text = "Unrecognised plugin with service name ${plugin.serviceName}\n\nReason: $reason",
            color = MaterialTheme.colors.error,
            fontStyle = FontStyle.Italic,
            style = MaterialTheme.typography.body2,
        )
    }
}

@Composable
private fun florisPluginFeatureToString(feature: FlorisPluginFeature): String {
    return when (feature) {
        FlorisPluginFeature.SpellingConfig -> "Spelling"
        FlorisPluginFeature.SuggestionConfig -> "Suggestion"
    }
}
