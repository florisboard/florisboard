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

package dev.patrickgold.florisboard.app.ui.settings.spelling

import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.app.LocalNavController
import dev.patrickgold.florisboard.app.res.stringRes
import dev.patrickgold.florisboard.app.ui.Routes
import dev.patrickgold.florisboard.app.ui.components.FlorisCanvasIcon
import dev.patrickgold.florisboard.app.ui.components.FlorisErrorCard
import dev.patrickgold.florisboard.app.ui.components.FlorisScreen
import dev.patrickgold.florisboard.app.ui.components.FlorisSimpleCard
import dev.patrickgold.florisboard.app.ui.components.FlorisWarningCard
import dev.patrickgold.florisboard.app.ui.ext.ExtensionList
import dev.patrickgold.florisboard.common.launchActivity
import dev.patrickgold.florisboard.extensionManager
import dev.patrickgold.florisboard.ime.spelling.SpellingLanguageMode
import dev.patrickgold.florisboard.util.AndroidSettings
import dev.patrickgold.jetpref.ui.compose.ListPreference
import dev.patrickgold.jetpref.ui.compose.Preference
import dev.patrickgold.jetpref.ui.compose.PreferenceGroup
import dev.patrickgold.jetpref.ui.compose.SwitchPreference
import dev.patrickgold.jetpref.ui.compose.annotations.ExperimentalJetPrefUi

@OptIn(ExperimentalJetPrefUi::class)
@Composable
fun SpellingScreen() = FlorisScreen(
    title = stringRes(R.string.settings__spelling__title),
    floatingActionButton = {
        val navController = LocalNavController.current
        FloatingActionButton(
            onClick = { navController.navigate(Routes.Settings.ImportSpellingArchive) },
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_add),
                contentDescription = "Add dictionary",
            )
        }
    },
) {
    val navController = LocalNavController.current
    val context = LocalContext.current
    val extensionManager by context.extensionManager()

    val systemSpellCheckerId by AndroidSettings.Secure.observeAsState("selected_spell_checker")
    val systemSpellCheckerEnabled by AndroidSettings.Secure.observeAsState("spell_checker_enabled")
    val systemSpellCheckerSubtypeIndex by AndroidSettings.Secure.observeAsState("selected_spell_checker_subtype")
    val systemSpellCheckerPkgName = runCatching {
        ComponentName.unflattenFromString(systemSpellCheckerId!!)!!.packageName
    }.getOrDefault("null")
    val openSystemSpellCheckerSettings = {
        val componentToLaunch = ComponentName(
            "com.android.settings",
            "com.android.settings.Settings\$SpellCheckersSettingsActivity",
        )
        launchActivity(context) {
            it.addCategory(Intent.CATEGORY_LAUNCHER)
            it.component = componentToLaunch
            it.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
    }
    PreferenceGroup(title = stringRes(R.string.pref__spelling__active_spellchecker__label)) {
        Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp)) {
            if (systemSpellCheckerEnabled == "1") {
                if (systemSpellCheckerId == null) {
                    FlorisWarningCard(
                        text = stringRes(R.string.pref__spelling__active_spellchecker__summary_none),
                        onClick = openSystemSpellCheckerSettings,
                    )
                } else {
                    var spellCheckerIcon: Drawable?
                    var spellCheckerLabel = "Unknown"
                    try {
                        val pm = context.packageManager
                        val remoteAppInfo = pm.getApplicationInfo(systemSpellCheckerPkgName, 0)
                        spellCheckerIcon = pm.getApplicationIcon(remoteAppInfo)
                        spellCheckerLabel = pm.getApplicationLabel(remoteAppInfo).toString()
                    } catch (e: PackageManager.NameNotFoundException) {
                        spellCheckerIcon = null
                    }
                    FlorisSimpleCard(
                        icon = {
                            if (spellCheckerIcon != null) {
                                FlorisCanvasIcon(
                                    modifier = Modifier.requiredSize(32.dp),
                                    drawable = spellCheckerIcon,
                                )
                            }
                        },
                        text = spellCheckerLabel,
                        secondaryText = systemSpellCheckerPkgName,
                        onClick = openSystemSpellCheckerSettings,
                    )
                    if (systemSpellCheckerPkgName == context.packageName && systemSpellCheckerSubtypeIndex == "0") {
                        FlorisWarningCard(
                            modifier = Modifier.padding(top = 8.dp),
                            text = stringRes(
                                R.string.pref__spelling__active_spellchecker__summary_use_sys_lang_set,
                                "use_floris_config" to stringRes(R.string.settings__spelling__use_floris_config),
                            ),
                            onClick = openSystemSpellCheckerSettings,
                        )
                    }
                }
            } else {
                FlorisErrorCard(
                    text = stringRes(R.string.pref__spelling__active_spellchecker__summary_disabled),
                    onClick = openSystemSpellCheckerSettings,
                )
            }
        }
    }
    Preference(
        title = stringRes(R.string.settings__spelling__dict_sources_info),
        onClick = { navController.navigate(Routes.Settings.SpellingInfo) },
    )

    PreferenceGroup(title = stringRes(R.string.pref__spelling__group_spellchecker_config__title)) {
        ListPreference(
            prefs.spelling.languageMode,
            iconId = R.drawable.ic_language,
            title = stringRes(R.string.pref__spelling__language_mode__label),
            entries = SpellingLanguageMode.listEntries(),
        )
        SwitchPreference(
            prefs.spelling.useContacts,
            iconId = R.drawable.ic_contacts,
            title = stringRes(R.string.pref__spelling__use_contacts__label),
            summary = stringRes(R.string.pref__spelling__use_contacts__summary),
        )
        SwitchPreference(
            prefs.spelling.useUdmEntries,
            iconId = R.drawable.ic_library_books,
            title = stringRes(R.string.pref__spelling__use_udm_entries__label),
            summary = stringRes(R.string.pref__spelling__use_udm_entries__summary),
        )
    }

    PreferenceGroup(title = stringRes(R.string.pref__spelling__group_installed_dictionaries__title)) {
        val spellingDicts by extensionManager.spellingDicts.observeAsState()
        if (spellingDicts != null && spellingDicts!!.isNotEmpty()) {
            ExtensionList(extList = spellingDicts!!)
        } else {
            Preference(title = "no dicts found")
        }
    }
}
