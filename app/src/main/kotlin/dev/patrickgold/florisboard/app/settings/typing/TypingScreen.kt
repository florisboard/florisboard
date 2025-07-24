/*
 * Copyright (C) 2021-2025 The FlorisBoard Contributors
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

package dev.patrickgold.florisboard.app.settings.typing

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.SpaceBar
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.app.LocalNavController
import dev.patrickgold.florisboard.app.Routes
import dev.patrickgold.florisboard.app.enumDisplayEntriesOf
import dev.patrickgold.florisboard.ime.keyboard.IncognitoMode
import dev.patrickgold.florisboard.ime.nlp.SpellingLanguageMode
import dev.patrickgold.florisboard.lib.compose.FlorisErrorCard
import dev.patrickgold.florisboard.lib.compose.FlorisHyperlinkText
import dev.patrickgold.florisboard.lib.compose.FlorisScreen
import dev.patrickgold.florisboard.lib.compose.stringRes
import dev.patrickgold.jetpref.datastore.model.observeAsState
import dev.patrickgold.jetpref.datastore.ui.DialogSliderPreference
import dev.patrickgold.jetpref.datastore.ui.ExperimentalJetPrefDatastoreUi
import dev.patrickgold.jetpref.datastore.ui.ListPreference
import dev.patrickgold.jetpref.datastore.ui.Preference
import dev.patrickgold.jetpref.datastore.ui.PreferenceGroup
import dev.patrickgold.jetpref.datastore.ui.SwitchPreference
import dev.patrickgold.jetpref.datastore.ui.vectorResource
import org.florisboard.lib.android.AndroidVersion

@OptIn(ExperimentalJetPrefDatastoreUi::class)
@Composable
fun TypingScreen() = FlorisScreen {
    title = stringRes(R.string.settings__typing__title)
    previewFieldVisible = true

    val navController = LocalNavController.current

    content {
        // This card is temporary and is therefore not using a string resource (not so temporary as we thought...)
        FlorisErrorCard(
            modifier = Modifier.padding(8.dp),
            text = """
                Suggestions (except system autofill) and spell checking are not available in this release. All
                preferences in the "Corrections" group are properly implemented though.
            """.trimIndent().replace('\n', ' '),
        )

        PreferenceGroup(title = stringRes(R.string.pref__suggestion__title)) {
            SwitchPreference(
                prefs.suggestion.enabled,
                title = stringRes(R.string.pref__suggestion__enabled__label),
                summary = stringRes(R.string.pref__suggestion__enabled__summary),
            )
            SwitchPreference(
                prefs.suggestion.blockPossiblyOffensive,
                title = stringRes(R.string.pref__suggestion__block_possibly_offensive__label),
                summary = stringRes(R.string.pref__suggestion__block_possibly_offensive__summary),
                enabledIf = { prefs.suggestion.enabled isEqualTo true },
            )
            SwitchPreference(
                prefs.suggestion.api30InlineSuggestionsEnabled,
                title = stringRes(R.string.pref__suggestion__api30_inline_suggestions_enabled__label),
                summary = stringRes(R.string.pref__suggestion__api30_inline_suggestions_enabled__summary),
                visibleIf = { AndroidVersion.ATLEAST_API30_R },
            )
            ListPreference(
                prefs.suggestion.incognitoMode,
                icon = vectorResource(id = R.drawable.ic_incognito),
                title = stringRes(R.string.pref__suggestion__incognito_mode__label),
                entries = enumDisplayEntriesOf(IncognitoMode::class),
            )
        }

        PreferenceGroup(title = stringRes(R.string.pref__correction__title)) {
            SwitchPreference(
                prefs.correction.autoCapitalization,
                title = stringRes(R.string.pref__correction__auto_capitalization__label),
                summary = stringRes(R.string.pref__correction__auto_capitalization__summary),
            )
            val isAutoSpacePunctuationEnabled by prefs.correction.autoSpacePunctuation.observeAsState()
            SwitchPreference(
                prefs.correction.autoSpacePunctuation,
                icon = Icons.Default.SpaceBar,
                title = stringRes(R.string.pref__correction__auto_space_punctuation__label),
                summary = stringRes(R.string.pref__correction__auto_space_punctuation__summary),
            )
            if (isAutoSpacePunctuationEnabled) {
                Card(modifier = Modifier.padding(8.dp)) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text(
                            text = """
                                Auto-space after punctuation is an experimental feature which may break or behave
                                unexpectedly. If you want, please give feedback about it in below linked feedback
                                thread. This helps a lot in improving this feature. Thanks!
                            """.trimIndent().replace('\n', ' '),
                        )
                        FlorisHyperlinkText(
                            text = "Feedback thread (GitHub)",
                            url = "https://github.com/florisboard/florisboard/discussions/1935",
                        )
                    }
                }
            }
            SwitchPreference(
                prefs.correction.rememberCapsLockState,
                title = stringRes(R.string.pref__correction__remember_caps_lock_state__label),
                summary = stringRes(R.string.pref__correction__remember_caps_lock_state__summary),
            )
            SwitchPreference(
                prefs.correction.doubleSpacePeriod,
                title = stringRes(R.string.pref__correction__double_space_period__label),
                summary = stringRes(R.string.pref__correction__double_space_period__summary),
            )
        }

        PreferenceGroup(title = stringRes(R.string.pref__spelling__title)) {
            val florisSpellCheckerEnabled = remember { mutableStateOf(false) }
            SpellCheckerServiceSelector(florisSpellCheckerEnabled)
            ListPreference(
                prefs.spelling.languageMode,
                icon = Icons.Default.Language,
                title = stringRes(R.string.pref__spelling__language_mode__label),
                entries = enumDisplayEntriesOf(SpellingLanguageMode::class),
                enabledIf = { florisSpellCheckerEnabled.value },
            )
            SwitchPreference(
                prefs.spelling.useContacts,
                icon = Icons.Default.Contacts,
                title = stringRes(R.string.pref__spelling__use_contacts__label),
                summary = stringRes(R.string.pref__spelling__use_contacts__summary),
                enabledIf = { florisSpellCheckerEnabled.value },
                visibleIf = { false }, // For now
            )
            SwitchPreference(
                prefs.spelling.useUdmEntries,
                icon = Icons.AutoMirrored.Filled.LibraryBooks,
                title = stringRes(R.string.pref__spelling__use_udm_entries__label),
                summary = stringRes(R.string.pref__spelling__use_udm_entries__summary),
                enabledIf = { florisSpellCheckerEnabled.value },
                visibleIf = { false }, // For now
            )
        }

        PreferenceGroup(title = stringRes(R.string.settings__dictionary__title)) {
            Preference(
                icon = Icons.AutoMirrored.Filled.LibraryBooks,
                title = stringRes(R.string.settings__dictionary__title),
                onClick = { navController.navigate(Routes.Settings.Dictionary) },
            )
        }
    }
}
