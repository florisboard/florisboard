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
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.app.LocalNavController
import dev.patrickgold.florisboard.app.res.stringRes
import dev.patrickgold.florisboard.app.ui.Routes
import dev.patrickgold.florisboard.app.ui.components.FlorisScreen
import dev.patrickgold.florisboard.app.ui.ext.ExtensionList
import dev.patrickgold.florisboard.common.launchActivity
import dev.patrickgold.florisboard.extensionManager
import dev.patrickgold.florisboard.util.AndroidSettingsSecure
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
    val context = LocalContext.current
    val extensionManager by context.extensionManager()

    val currentSpellCheckerId by AndroidSettingsSecure.observeAsState("selected_spell_checker")
    val spellCheckerEnabled by AndroidSettingsSecure.observeAsState("spell_checker_enabled")
    Preference(
        title = stringRes(R.string.pref__spelling__active_spellchecker__label),
        summary = when {
            spellCheckerEnabled == "0" -> {
                stringRes(R.string.pref__spelling__active_spellchecker__summary_disabled)
            }
            currentSpellCheckerId == null -> {
                stringRes(R.string.pref__spelling__active_spellchecker__summary_none)
            }
            currentSpellCheckerId!!.startsWith("${context.packageName}/") -> {
                stringRes(R.string.floris_app_name)
            }
            else -> {
                stringRes(R.string.pref__spelling__active_spellchecker__summary_non_fb_set, "spell_checker_name" to currentSpellCheckerId)
            }
        },
        onClick = {
            val componentToLaunch = ComponentName(
                "com.android.settings",
                "com.android.settings.Settings\$SpellCheckersSettingsActivity"
            )
            launchActivity(context) {
                it.addCategory(Intent.CATEGORY_LAUNCHER)
                it.component = componentToLaunch
                it.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        },
    )

    PreferenceGroup(title = stringRes(R.string.pref__spelling__group_fine_adjustment__title)) {
        SwitchPreference(
            prefs.spelling.useContacts,
            title = stringRes(R.string.pref__spelling__use_contacts__label),
            summary = stringRes(R.string.pref__spelling__use_contacts__summary),
        )
        SwitchPreference(
            prefs.spelling.useUdmEntries,
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
