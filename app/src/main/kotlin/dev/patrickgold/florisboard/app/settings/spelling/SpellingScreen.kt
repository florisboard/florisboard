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

package dev.patrickgold.florisboard.app.settings.spelling

import android.content.ComponentName
import android.content.Intent
import android.graphics.drawable.Drawable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.app.LocalNavController
import dev.patrickgold.florisboard.app.Routes
import dev.patrickgold.florisboard.extensionManager
import dev.patrickgold.florisboard.ime.spelling.SpellingLanguageMode
import dev.patrickgold.florisboard.lib.android.AndroidSettings
import dev.patrickgold.florisboard.lib.android.launchActivity
import dev.patrickgold.florisboard.lib.compose.FlorisCanvasIcon
import dev.patrickgold.florisboard.lib.compose.FlorisErrorCard
import dev.patrickgold.florisboard.lib.compose.FlorisScreen
import dev.patrickgold.florisboard.lib.compose.FlorisSimpleCard
import dev.patrickgold.florisboard.lib.compose.FlorisWarningCard
import dev.patrickgold.florisboard.lib.compose.stringRes
import dev.patrickgold.jetpref.datastore.ui.ListPreference
import dev.patrickgold.jetpref.datastore.ui.Preference
import dev.patrickgold.jetpref.datastore.ui.PreferenceGroup
import dev.patrickgold.jetpref.datastore.ui.SwitchPreference

@Composable
fun SpellingScreen() = FlorisScreen {
    title = stringRes(R.string.settings__spelling__title)
    previewFieldVisible = true

    val navController = LocalNavController.current
    val context = LocalContext.current
    val extensionManager by context.extensionManager()

    val systemSpellCheckerId by AndroidSettings.Secure.observeAsState(
        key = "selected_spell_checker",
        foregroundOnly = true,
    )
    val systemSpellCheckerEnabled by AndroidSettings.Secure.observeAsState(
        key = "spell_checker_enabled",
        foregroundOnly = true,
    )
    val systemSpellCheckerSubtypeIndex by AndroidSettings.Secure.observeAsState(
        key = "selected_spell_checker_subtype",
        foregroundOnly = true,
    )
    val systemSpellCheckerPkgName = remember(systemSpellCheckerId) {
        runCatching {
            ComponentName.unflattenFromString(systemSpellCheckerId!!)!!.packageName
        }.getOrDefault("null")
    }
    val openSystemSpellCheckerSettings = {
        val componentToLaunch = ComponentName(
            "com.android.settings",
            "com.android.settings.Settings\$SpellCheckersSettingsActivity",
        )
        context.launchActivity {
            it.addCategory(Intent.CATEGORY_DEFAULT)
            it.component = componentToLaunch
        }
    }
    val florisSpellCheckerEnabled =
        systemSpellCheckerEnabled == "1" &&
        systemSpellCheckerPkgName == context.packageName &&
        systemSpellCheckerSubtypeIndex != "0"

    content {
        PreferenceGroup(title = stringRes(R.string.pref__spelling__active_spellchecker__label)) {
            Column(modifier = Modifier.padding(horizontal = 8.dp)) {
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
                        } catch (e: Exception) {
                            spellCheckerIcon = null
                        }
                        FlorisSimpleCard(
                            icon = {
                                if (spellCheckerIcon != null) {
                                    FlorisCanvasIcon(
                                        modifier = Modifier
                                            .padding(end = 8.dp)
                                            .requiredSize(32.dp),
                                        drawable = spellCheckerIcon,
                                    )
                                } else {
                                    Icon(
                                        modifier = Modifier
                                            .padding(end = 8.dp)
                                            .requiredSize(32.dp),
                                        painter = painterResource(R.drawable.ic_help_outline),
                                        contentDescription = null,
                                    )
                                }
                            },
                            text = spellCheckerLabel,
                            secondaryText = systemSpellCheckerPkgName,
                            contentPadding = PaddingValues(all = 8.dp),
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
        val spellingDicts by extensionManager.spellingDicts.observeAsState()
        Preference(
            iconId = R.drawable.ic_library_books,
            title = stringRes(R.string.settings__spelling__manage_dicts__title),
            summary = stringRes(
                R.string.settings__spelling__manage_dicts__n_installed,
                "n" to (spellingDicts?.size ?: 0).toString(),
            ),
            onClick = { navController.navigate(Routes.Settings.ManageSpellingDicts) },
            enabledIf = { florisSpellCheckerEnabled },
        )

        PreferenceGroup(title = stringRes(R.string.pref__spelling__group_spellchecker_config__title)) {
            ListPreference(
                prefs.spelling.languageMode,
                iconId = R.drawable.ic_language,
                title = stringRes(R.string.pref__spelling__language_mode__label),
                entries = SpellingLanguageMode.listEntries(),
                enabledIf = { florisSpellCheckerEnabled },
            )
            SwitchPreference(
                prefs.spelling.useContacts,
                iconId = R.drawable.ic_contacts,
                title = stringRes(R.string.pref__spelling__use_contacts__label),
                summary = stringRes(R.string.pref__spelling__use_contacts__summary),
                enabledIf = { florisSpellCheckerEnabled },
            )
            SwitchPreference(
                prefs.spelling.useUdmEntries,
                iconId = R.drawable.ic_library_books,
                title = stringRes(R.string.pref__spelling__use_udm_entries__label),
                summary = stringRes(R.string.pref__spelling__use_udm_entries__summary),
                enabledIf = { florisSpellCheckerEnabled },
            )
        }
    }
}
