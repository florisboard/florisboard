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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.app.LocalNavController
import dev.patrickgold.florisboard.app.res.stringRes
import dev.patrickgold.florisboard.app.ui.components.FlorisButtonBar
import dev.patrickgold.florisboard.app.ui.components.FlorisDropdownMenu
import dev.patrickgold.florisboard.app.ui.components.FlorisScreen
import dev.patrickgold.florisboard.common.FlorisLocale
import dev.patrickgold.florisboard.ime.core.Subtype
import dev.patrickgold.florisboard.ime.core.SubtypeJsonConfig
import dev.patrickgold.florisboard.ime.core.SubtypeLayoutMap
import dev.patrickgold.florisboard.ime.core.SubtypePreset
import dev.patrickgold.florisboard.ime.keyboard.LayoutArrangementComponent
import dev.patrickgold.florisboard.ime.keyboard.LayoutType
import dev.patrickgold.florisboard.ime.text.composing.Appender
import dev.patrickgold.florisboard.keyboardManager
import dev.patrickgold.florisboard.res.ext.ExtensionComponentName
import dev.patrickgold.florisboard.subtypeManager
import dev.patrickgold.jetpref.ui.compose.JetPrefAlertDialog
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString

private val SelectComponentName = ExtensionComponentName("-", "-")
private val SelectLayoutMap = SubtypeLayoutMap(
    characters = SelectComponentName,
    symbols = SelectComponentName,
    symbols2 = SelectComponentName,
    numeric = SelectComponentName,
    numericAdvanced = SelectComponentName,
    numericRow = SelectComponentName,
    phone = SelectComponentName,
    phone2 = SelectComponentName,
)
private val SelectLocale = FlorisLocale.from("00", "00")
private val SelectListKeys = listOf(SelectComponentName)
private val SelectListValues = listOf("- select -")

private class SubtypeEditorState(init: Subtype?) {
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

    val id: MutableState<Long> = mutableStateOf(init?.id ?: -1)
    val primaryLocale: MutableState<FlorisLocale> = mutableStateOf(init?.primaryLocale ?: SelectLocale)
    val secondaryLocales: MutableState<List<FlorisLocale>> = mutableStateOf(init?.secondaryLocales ?: listOf())
    val composer: MutableState<ExtensionComponentName> = mutableStateOf(init?.composer ?: Appender.name)
    val currencySet: MutableState<ExtensionComponentName> = mutableStateOf(init?.currencySet ?: SelectComponentName)
    val popupMapping: MutableState<ExtensionComponentName> = mutableStateOf(init?.popupMapping ?: SelectComponentName)
    val layoutMap: MutableState<SubtypeLayoutMap> = mutableStateOf(init?.layoutMap ?: SelectLayoutMap)

    fun toSubtype() = runCatching<Subtype> {
        check(primaryLocale.value != SelectLocale)
        check(composer.value != SelectComponentName)
        check(currencySet.value != SelectComponentName)
        check(popupMapping.value != SelectComponentName)
        check(layoutMap.value.characters != SelectComponentName)
        check(layoutMap.value.symbols != SelectComponentName)
        check(layoutMap.value.symbols2 != SelectComponentName)
        check(layoutMap.value.numeric != SelectComponentName)
        check(layoutMap.value.numericAdvanced != SelectComponentName)
        check(layoutMap.value.numericRow != SelectComponentName)
        check(layoutMap.value.phone != SelectComponentName)
        check(layoutMap.value.phone2 != SelectComponentName)
        Subtype(
            id.value, primaryLocale.value, secondaryLocales.value, composer.value,
            currencySet.value, popupMapping.value, layoutMap.value,
        )
    }
}

@Composable
fun SubtypeEditorScreen(id: Long?) = FlorisScreen {
    title = stringRes(if (id == null) {
        R.string.settings__localization__subtype_add_title
    } else {
        R.string.settings__localization__subtype_edit_title
    })

    val navController = LocalNavController.current
    val context = LocalContext.current
    val keyboardManager by context.keyboardManager()
    val subtypeManager by context.subtypeManager()

    val currencySets by keyboardManager.resources.currencySets.observeAsState()
    val layoutExtensions by keyboardManager.resources.layouts.observeAsState()
    val popupMappings by keyboardManager.resources.popupMappings.observeAsState()
    val subtypePresets by keyboardManager.resources.subtypePresets.observeAsState()

    val subtypeEditor = rememberSaveable(saver = SubtypeEditorState.Saver) {
        val subtype = id?.let { subtypeManager.getSubtypeById(id) }
        SubtypeEditorState(subtype)
    }
    var primaryLocale by subtypeEditor.primaryLocale
    var secondaryLocales by subtypeEditor.secondaryLocales
    var composer by subtypeEditor.composer
    var currencySet by subtypeEditor.currencySet
    var popupMapping by subtypeEditor.popupMapping
    var layoutMap by subtypeEditor.layoutMap

    var showSelectAsError by rememberSaveable { mutableStateOf(false) }
    var errorDialogStrId by rememberSaveable { mutableStateOf<Int?>(null) }

    actions {
        if (id != null) {
            IconButton(onClick = {
                val subtype = subtypeManager.getSubtypeById(id)
                if (subtype != null) {
                    subtypeManager.removeSubtype(subtype)
                    navController.popBackStack()
                }
            }) {
                Icon(
                    painter = painterResource(R.drawable.ic_delete),
                    contentDescription = null,
                )
            }
        }
    }

    bottomBar {
        FlorisButtonBar {
            ButtonBarSpacer()
            ButtonBarButton(text = stringRes(R.string.assets__action__cancel)) {
                navController.popBackStack()
            }
            ButtonBarButton(text = stringRes(R.string.assets__action__save)) {
                subtypeEditor.toSubtype().onSuccess { subtype ->
                    if (id == null) {
                        if (!subtypeManager.addSubtype(subtype)) {
                            errorDialogStrId = R.string.settings__localization__subtype_error_already_exists
                            return@ButtonBarButton
                        }
                    } else {
                        subtypeManager.modifySubtypeWithSameId(subtype)
                    }
                    navController.popBackStack()
                }.onFailure {
                    showSelectAsError = true
                    errorDialogStrId = R.string.settings__localization__subtype_error_fields_no_value
                }
            }
        }
    }

    content {
        Column(modifier = Modifier.padding(8.dp)) {
            Card(modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            ) {
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
                    listOf(SelectLocale.languageTag()) + (subtypePresets?.map { it.locale.languageTag() } ?: listOf())
                }
                val languageNames = remember(subtypePresets) {
                    SelectListValues + (subtypePresets?.map { it.locale.displayName() } ?: listOf())
                }
                var expanded by remember { mutableStateOf(false) }
                val selectedIndex = languageCodes.indexOf(primaryLocale.languageTag()).coerceAtLeast(0)
                FlorisDropdownMenu(
                    items = languageNames,
                    expanded = expanded,
                    selectedIndex = selectedIndex,
                    isError = showSelectAsError && selectedIndex == 0,
                    onSelectItem = { index ->
                        val locale = FlorisLocale.fromTag(languageCodes[index])
                        primaryLocale = locale
                        subtypeManager.getSubtypePresetForLocale(locale)?.let { preset ->
                            preset.popupMapping?.let { popupMapping = it }
                        }
                    },
                    onExpandRequest = { expanded = true },
                    onDismissRequest = { expanded = false },
                )
            }
            SubtypeProperty(stringRes(R.string.settings__localization__subtype_popup_mapping)) {
                val popupMappingIds = remember(popupMappings) {
                    SelectListKeys + (popupMappings?.keys ?: listOf())
                }
                val popupMappingLabels = remember(popupMappings) {
                    SelectListValues + (popupMappings?.values?.map { it.label } ?: listOf())
                }
                var expanded by remember { mutableStateOf(false) }
                val selectedIndex = popupMappingIds.indexOf(popupMapping).coerceAtLeast(0)
                FlorisDropdownMenu(
                    items = popupMappingLabels,
                    expanded = expanded,
                    selectedIndex = selectedIndex,
                    isError = showSelectAsError && selectedIndex == 0,
                    onSelectItem = { popupMapping = popupMappingIds[it] },
                    onExpandRequest = { expanded = true },
                    onDismissRequest = { expanded = false },
                )
            }
            SubtypeProperty(stringRes(R.string.settings__localization__subtype_characters_layout)) {
                val layoutType = LayoutType.CHARACTERS
                SubtypeLayoutDropdown(
                    layoutType = layoutType,
                    layouts = layoutExtensions?.get(layoutType) ?: mapOf(),
                    showSelectAsError = showSelectAsError,
                    layoutMap = layoutMap,
                    onLayoutMapChanged = { layoutMap = it },
                )
            }

            SubtypeGroupSpacer()

            SubtypeProperty(stringRes(R.string.settings__localization__subtype_symbols_layout)) {
                val layoutType = LayoutType.SYMBOLS
                SubtypeLayoutDropdown(
                    layoutType = layoutType,
                    layouts = layoutExtensions?.get(layoutType) ?: mapOf(),
                    showSelectAsError = showSelectAsError,
                    layoutMap = layoutMap,
                    onLayoutMapChanged = { layoutMap = it },
                )
            }
            SubtypeProperty(stringRes(R.string.settings__localization__subtype_symbols2_layout)) {
                val layoutType = LayoutType.SYMBOLS2
                SubtypeLayoutDropdown(
                    layoutType = layoutType,
                    layouts = layoutExtensions?.get(layoutType) ?: mapOf(),
                    showSelectAsError = showSelectAsError,
                    layoutMap = layoutMap,
                    onLayoutMapChanged = { layoutMap = it },
                )
            }
            SubtypeProperty(stringRes(R.string.settings__localization__subtype_currency_set)) {
                val currencySetIds = remember(currencySets) {
                    SelectListKeys + (currencySets?.keys ?: listOf())
                }
                val currencySetNames = remember(currencySets) {
                    SelectListValues + (currencySets?.values?.map { it.label } ?: listOf())
                }
                var expanded by remember { mutableStateOf(false) }
                FlorisDropdownMenu(
                    items = currencySetNames,
                    expanded = expanded,
                    selectedIndex = currencySetIds.indexOf(currencySet).coerceAtLeast(0),
                    isError = showSelectAsError && currencySet == SelectComponentName,
                    onSelectItem = { currencySet = currencySetIds[it] },
                    onExpandRequest = { expanded = true },
                    onDismissRequest = { expanded = false },
                )
            }

            SubtypeGroupSpacer()

            SubtypeProperty(stringRes(R.string.settings__localization__subtype_numeric_layout)) {
                val layoutType = LayoutType.NUMERIC
                SubtypeLayoutDropdown(
                    layoutType = layoutType,
                    layouts = layoutExtensions?.get(layoutType) ?: mapOf(),
                    showSelectAsError = showSelectAsError,
                    layoutMap = layoutMap,
                    onLayoutMapChanged = { layoutMap = it },
                )
            }
            SubtypeProperty(stringRes(R.string.settings__localization__subtype_numeric_advanced_layout)) {
                val layoutType = LayoutType.NUMERIC_ADVANCED
                SubtypeLayoutDropdown(
                    layoutType = layoutType,
                    layouts = layoutExtensions?.get(layoutType) ?: mapOf(),
                    showSelectAsError = showSelectAsError,
                    layoutMap = layoutMap,
                    onLayoutMapChanged = { layoutMap = it },
                )
            }
            SubtypeProperty(stringRes(R.string.settings__localization__subtype_numeric_row_layout)) {
                val layoutType = LayoutType.NUMERIC_ROW
                SubtypeLayoutDropdown(
                    layoutType = layoutType,
                    layouts = layoutExtensions?.get(layoutType) ?: mapOf(),
                    showSelectAsError = showSelectAsError,
                    layoutMap = layoutMap,
                    onLayoutMapChanged = { layoutMap = it },
                )
            }

            SubtypeGroupSpacer()

            SubtypeProperty(stringRes(R.string.settings__localization__subtype_phone_layout)) {
                val layoutType = LayoutType.PHONE
                SubtypeLayoutDropdown(
                    layoutType = layoutType,
                    layouts = layoutExtensions?.get(layoutType) ?: mapOf(),
                    showSelectAsError = showSelectAsError,
                    layoutMap = layoutMap,
                    onLayoutMapChanged = { layoutMap = it },
                )
            }
            SubtypeProperty(stringRes(R.string.settings__localization__subtype_phone2_layout)) {
                val layoutType = LayoutType.PHONE2
                SubtypeLayoutDropdown(
                    layoutType = layoutType,
                    layouts = layoutExtensions?.get(layoutType) ?: mapOf(),
                    showSelectAsError = showSelectAsError,
                    layoutMap = layoutMap,
                    onLayoutMapChanged = { layoutMap = it },
                )
            }
        }

        errorDialogStrId?.let { strId ->
            JetPrefAlertDialog(
                title = stringRes(R.string.assets__error__title),
                confirmLabel = stringRes(android.R.string.ok),
                onConfirm = {
                    errorDialogStrId = null
                },
                onDismiss = {
                    errorDialogStrId = null
                },
            ) {
                Text(text = stringRes(strId))
            }
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

@Composable
private fun SubtypeLayoutDropdown(
    layoutType: LayoutType,
    layouts: Map<ExtensionComponentName, LayoutArrangementComponent>,
    showSelectAsError: Boolean,
    layoutMap: SubtypeLayoutMap,
    onLayoutMapChanged: (SubtypeLayoutMap) -> Unit,
) {
    val layoutIds = remember(layouts) { SelectListKeys + layouts.keys.toList() }
    val layoutLabels = remember(layouts) { SelectListValues + layouts.values.map { it.label } }
    val layoutId = remember(layoutMap) { layoutMap[layoutType] }
    var expanded by remember { mutableStateOf(false) }
    val selectedIndex = layoutIds.indexOf(layoutId).coerceAtLeast(0)
    FlorisDropdownMenu(
        items = layoutLabels,
        expanded = expanded,
        selectedIndex = selectedIndex,
        isError = showSelectAsError && selectedIndex == 0,
        onSelectItem = { onLayoutMapChanged(layoutMap.copy(layoutType, layoutIds[it])!!) },
        onExpandRequest = { expanded = true },
        onDismissRequest = { expanded = false },
    )
}

@Composable
private fun SubtypeGroupSpacer() {
    Spacer(modifier = Modifier
        .fillMaxWidth()
        .height(32.dp))
}
