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

package dev.patrickgold.florisboard.app.ui.settings.localization

import android.content.res.Resources
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.app.res.stringRes
import dev.patrickgold.florisboard.app.ui.components.FlorisDropdownMenu
import dev.patrickgold.florisboard.app.ui.components.FlorisScreen
import dev.patrickgold.florisboard.common.FlorisLocale
import dev.patrickgold.florisboard.ime.core.Subtype
import dev.patrickgold.florisboard.ime.core.SubtypeJsonConfig
import dev.patrickgold.florisboard.ime.core.SubtypeLayoutMap
import dev.patrickgold.florisboard.ime.core.SubtypePreset
import dev.patrickgold.florisboard.keyboardManager
import dev.patrickgold.florisboard.res.ext.ExtensionComponentName
import dev.patrickgold.florisboard.subtypeManager
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString

private val SelectList = listOf("- select -")

private class SubtypeEditorState(init: Subtype) {
    companion object {
        val Saver = Saver<SubtypeEditorState, String>(
            save = { editor ->
                val subtype = Subtype(
                    id = editor.id.value,
                    primaryLocale = editor.primaryLocale.value,
                    secondaryLocales = editor.secondaryLocales.value,
                    composer = editor.composer.value,
                    currencySet = editor.currencySet.value,
                    popupMapping = editor.popupMapping.value,
                    layoutMap = editor.layoutMap.value,
                )
                SubtypeJsonConfig.encodeToString(subtype)
            },
            restore = { str ->
                val subtype = SubtypeJsonConfig.decodeFromString<Subtype>(str)
                SubtypeEditorState(subtype)
            },
        )
    }

    val id: MutableState<Int> = mutableStateOf(init.id)
    val primaryLocale: MutableState<FlorisLocale> = mutableStateOf(init.primaryLocale)
    val secondaryLocales: MutableState<List<FlorisLocale>> = mutableStateOf(init.secondaryLocales)
    val composer: MutableState<ExtensionComponentName> = mutableStateOf(init.composer)
    val currencySet: MutableState<ExtensionComponentName> = mutableStateOf(init.currencySet)
    val popupMapping: MutableState<ExtensionComponentName> = mutableStateOf(init.popupMapping)
    val layoutMap: MutableState<SubtypeLayoutMap> = mutableStateOf(init.layoutMap)
}

@Composable
fun SubtypeEditorScreen(id: Int?) = FlorisScreen(
    title = stringRes(if (id == null) {
        R.string.settings__localization__subtype_add_title
    } else {
        R.string.settings__localization__subtype_edit_title
    }),
) {
    val context = LocalContext.current
    val keyboardManager by context.keyboardManager()
    val subtypeManager by context.subtypeManager()

    val currencySets by keyboardManager.resources.currencySets.observeAsState()
    val layoutExtensions by keyboardManager.resources.layouts.observeAsState()
    val subtypePresets by keyboardManager.resources.subtypePresets.observeAsState()

    val subtypeEditor = rememberSaveable(saver = SubtypeEditorState.Saver) {
        val subtype = id?.let { subtypeManager.getSubtypeById(id) } ?: Subtype.DEFAULT
        SubtypeEditorState(subtype)
    }
    var primaryLocale by subtypeEditor.primaryLocale
    var secondaryLocales by subtypeEditor.secondaryLocales
    var composer by subtypeEditor.composer
    var currencySet by subtypeEditor.currencySet
    var popupMapping by subtypeEditor.popupMapping
    var layoutMap by subtypeEditor.layoutMap

    Column(modifier = Modifier.padding(8.dp)) {
        Card(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
            Column(modifier = Modifier.padding(8.dp)) {
                Text(text = "Sugg Presets")
                if (id == null) {
                    val systemLocales = remember { Resources.getSystem().assets.locales }
                    val suggestedPresets = remember(subtypePresets) {
                        val presets = mutableListOf<SubtypePreset>()
                        for (systemLocaleStr in systemLocales) {
                            val systemLocale = FlorisLocale.fromTag(systemLocaleStr)
                            subtypePresets?.find { it.locale == systemLocale }?.let { presets.add(it) }
                        }
                        presets
                    }
                    if (suggestedPresets.isNotEmpty()) {
                        Text("got some")
                    }
                }
            }
        }

        SubtypeProperty(stringRes(R.string.settings__localization__subtype_locale)) {
            val languageCodes = remember(subtypePresets) {
                SelectList + (subtypePresets?.map { it.locale.languageTag() } ?: listOf())
            }
            val languageNames = remember(subtypePresets) {
                SelectList + (subtypePresets?.map { it.locale.displayName(it.locale) } ?: listOf())
            }
            var expanded by remember { mutableStateOf(false) }
            FlorisDropdownMenu(
                items = languageNames,
                expanded = expanded,
                selectedIndex = languageCodes.indexOf(primaryLocale.languageTag()).coerceAtLeast(0),
                onSelectItem = { primaryLocale = FlorisLocale.fromTag(languageCodes[it]) },
                onExpandRequest = { expanded = true },
                onDismissRequest = { expanded = false },
            )
        }
        //SubtypeProperty(stringRes(R.string.settings__localization__subtype_characters_layout)) {
        //    val layouts = layoutExtensions?.get(LayoutType.CHARACTERS) ?: mapOf()
        //    val layoutIds = remember(layouts) { layouts.keys }
        //    val layoutLabels = remember(layouts) { layouts.values.map { it.label } }
        //    var layoutId = remember(layoutMap) { layoutMap.characters }
        //    var expanded by remember { mutableStateOf(false) }
        //    FlorisDropdownMenu(
        //        items = layoutLabels,
        //        expanded = expanded,
        //        selectedIndex = layoutIds.indexOf(layoutId).coerceAtLeast(0),
        //        onSelectItem = { layoutMap.characters =  },
        //        onExpandRequest = { expanded = true },
        //        onDismissRequest = { expanded = false },
        //    )
        //}

        SubtypeProperty(stringRes(R.string.settings__localization__subtype_currency_set)) {
            val currencySetIds = remember(currencySets) {
                listOf(ExtensionComponentName("-", "-")) + (currencySets?.keys ?: listOf())
            }
            val currencySetNames = remember(currencySets) {
                SelectList + (currencySets?.values?.map { it.label } ?: listOf())
            }
            var expanded by remember { mutableStateOf(false) }
            FlorisDropdownMenu(
                items = currencySetNames,
                expanded = expanded,
                selectedIndex = currencySetIds.indexOf(currencySet).coerceAtLeast(0),
                onSelectItem = { currencySet = currencySetIds[it] },
                onExpandRequest = { expanded = true },
                onDismissRequest = { expanded = false },
            )
        }
    }
}

@Composable
private fun SubtypeProperty(text: String, content: @Composable () -> Unit) {
    Column(modifier = Modifier.padding(vertical = 8.dp, horizontal = 8.dp)) {
        Text(
            modifier = Modifier.padding(bottom = 8.dp),
            text = text,
            style = MaterialTheme.typography.subtitle2,
        )
        content()
    }
}
