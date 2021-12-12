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

package dev.patrickgold.florisboard.app.ui.settings.dictionary

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.app.res.stringRes
import dev.patrickgold.florisboard.app.ui.components.FlorisScreen
import dev.patrickgold.florisboard.common.android.launchActivity
import dev.patrickgold.florisboard.oldsettings.UdmActivity
import dev.patrickgold.jetpref.datastore.ui.Preference
import dev.patrickgold.jetpref.datastore.ui.SwitchPreference

@Composable
fun DictionaryScreen() = FlorisScreen {
    title = stringRes(R.string.settings__dictionary__title)

    val context = LocalContext.current

    content {
        SwitchPreference(
            prefs.dictionary.enableSystemUserDictionary,
            title = stringRes(R.string.pref__dictionary__enable_system_user_dictionary__label),
            summary = stringRes(R.string.pref__dictionary__enable_system_user_dictionary__summary),
        )
        Preference(
            title = stringRes(R.string.pref__dictionary__manage_system_user_dictionary__label),
            summary = stringRes(R.string.pref__dictionary__manage_system_user_dictionary__summary),
            onClick = { context.launchActivity(UdmActivity::class) {
                it.putExtra(UdmActivity.EXTRA_USER_DICTIONARY_TYPE, UdmActivity.USER_DICTIONARY_TYPE_SYSTEM)
            } },
            enabledIf = { prefs.dictionary.enableSystemUserDictionary isEqualTo true },
        )
        SwitchPreference(
            prefs.dictionary.enableFlorisUserDictionary,
            title = stringRes(R.string.pref__dictionary__enable_internal_user_dictionary__label),
            summary = stringRes(R.string.pref__dictionary__enable_internal_user_dictionary__summary),
        )
        Preference(
            title = stringRes(R.string.pref__dictionary__manage_floris_user_dictionary__label),
            summary = stringRes(R.string.pref__dictionary__manage_floris_user_dictionary__summary),
            onClick = { context.launchActivity(UdmActivity::class) {
                it.putExtra(UdmActivity.EXTRA_USER_DICTIONARY_TYPE, UdmActivity.USER_DICTIONARY_TYPE_FLORIS)
            } },
            enabledIf = { prefs.dictionary.enableFlorisUserDictionary isEqualTo true },
        )
    }
}
