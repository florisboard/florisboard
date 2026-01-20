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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.app.LocalNavController
import dev.patrickgold.florisboard.app.Routes
import dev.patrickgold.florisboard.app.enumDisplayEntriesOf
import dev.patrickgold.florisboard.ime.keyboard.IncognitoMode
import dev.patrickgold.florisboard.ime.nlp.SpellingLanguageMode
import dev.patrickgold.florisboard.lib.compose.FlorisHyperlinkText
import dev.patrickgold.florisboard.lib.compose.FlorisScreen
import dev.patrickgold.jetpref.datastore.model.observeAsState
import dev.patrickgold.jetpref.datastore.ui.ExperimentalJetPrefDatastoreUi
import dev.patrickgold.jetpref.datastore.ui.ListPreference
import dev.patrickgold.jetpref.datastore.ui.Preference
import dev.patrickgold.jetpref.datastore.ui.PreferenceGroup
import dev.patrickgold.jetpref.datastore.ui.SwitchPreference
import androidx.compose.material3.OutlinedTextField
import dev.patrickgold.jetpref.material.ui.JetPrefAlertDialog
import org.florisboard.lib.android.AndroidVersion
import org.florisboard.lib.compose.FlorisErrorCard
import org.florisboard.lib.compose.stringRes

import dev.patrickgold.jetpref.datastore.ui.DialogSliderPreference

@OptIn(ExperimentalJetPrefDatastoreUi::class)
@Composable
fun TypingScreen() = FlorisScreen {
    title = stringRes(R.string.settings__typing__title)
    previewFieldVisible = true

    val navController = LocalNavController.current
    var showApiKeyDialog by remember { mutableStateOf(false) }
    var tempApiKey by remember { mutableStateOf(prefs.aiIntegration.geminiApiKey.get()) }

    content {
        // This card is temporary and is therefore not using a string resource (not so temporary as we thought...)
        FlorisErrorCard(
            modifier = Modifier.padding(8.dp),
            text = """
                Suggestions (except system autofill) and spell checking are not available in this release. All
                preferences in the "Corrections" group are properly implemented though.
            """.trimIndent().replace('\n', ' '),
        )

        PreferenceGroup(title = stringRes(R.string.pref__language_detection__title)) {
            SwitchPreference(
                prefs.languageDetection.enabled,
                title = stringRes(R.string.pref__language_detection__enabled__label),
                summary = stringRes(R.string.pref__language_detection__enabled__summary),
            )
            SwitchPreference(
                prefs.languageDetection.autoSwitchLayout,
                title = stringRes(R.string.pref__language_detection__auto_switch_layout__label),
                summary = stringRes(R.string.pref__language_detection__auto_switch_layout__summary),
                enabledIf = { prefs.languageDetection.enabled isEqualTo true },
            )
            SwitchPreference(
                prefs.languageDetection.showVisualIndicator,
                title = stringRes(R.string.pref__language_detection__show_visual_indicator__label),
                summary = stringRes(R.string.pref__language_detection__show_visual_indicator__summary),
                enabledIf = { prefs.languageDetection.enabled isEqualTo true },
            )
            DialogSliderPreference(
                prefs.languageDetection.detectionSensitivity,
                title = stringRes(R.string.pref__language_detection__detection_sensitivity__label),
                valueLabel = { "$it%" },
                min = 0,
                max = 100,
                stepIncrement = 5,
                enabledIf = { prefs.languageDetection.enabled isEqualTo true },
            )
            DialogSliderPreference(
                prefs.languageDetection.minWordLength,
                title = stringRes(R.string.pref__language_detection__min_word_length__label),
                valueLabel = { it.toString() },
                min = 1,
                max = 10,
                stepIncrement = 1,
                enabledIf = { prefs.languageDetection.enabled isEqualTo true },
            )
        }

        PreferenceGroup(title = stringRes(R.string.pref__ai_integration__title)) {
            SwitchPreference(
                prefs.aiIntegration.enabled,
                title = stringRes(R.string.pref__ai_integration__enabled__label),
                summary = stringRes(R.string.pref__ai_integration__enabled__summary),
            )
            val isAiEnabled by prefs.aiIntegration.enabled.observeAsState()
            if (isAiEnabled) {
                Card(modifier = Modifier.padding(8.dp)) {
                    Text(
                        modifier = Modifier.padding(8.dp),
                        text = stringRes(R.string.pref__ai_integration__privacy_disclaimer),
                    )
                }
                Preference(
                    title = stringRes(R.string.pref__ai_integration__gemini_api_key__label),
                    summary = stringRes(R.string.pref__ai_integration__gemini_api_key__summary),
                    enabledIf = { prefs.aiIntegration.enabled isEqualTo true },
                    onClick = { 
                        tempApiKey = prefs.aiIntegration.geminiApiKey.get()
                        showApiKeyDialog = true
                    }
                )

                if (showApiKeyDialog) {
                    JetPrefAlertDialog(
                        title = stringRes(R.string.pref__ai_integration__gemini_api_key__label),
                        confirmLabel = stringRes(android.R.string.ok),
                        onConfirm = {
                            prefs.aiIntegration.geminiApiKey.set(tempApiKey)
                            showApiKeyDialog = false
                        },
                        dismissLabel = stringRes(android.R.string.cancel),
                        onDismiss = { showApiKeyDialog = false },
                    ) {
                        Column {
                            OutlinedTextField(
                                value = tempApiKey,
                                onValueChange = { tempApiKey = it },
                                label = { Text("API Key") },
                                modifier = Modifier.padding(top = 16.dp)
                            )
                        }
                    }
                }
                SwitchPreference(
                    prefs.aiIntegration.provideReplySuggestions,
                    title = stringRes(R.string.pref__ai_integration__provide_reply_suggestions__label),
                    enabledIf = { prefs.aiIntegration.enabled isEqualTo true },
                )
                SwitchPreference(
                    prefs.aiIntegration.enableTextRewrite,
                    title = stringRes(R.string.pref__ai_integration__enable_text_rewrite__label),
                    enabledIf = { prefs.aiIntegration.enabled isEqualTo true },
                )
                SwitchPreference(
                    prefs.aiIntegration.enableToneAdjustment,
                    title = stringRes(R.string.pref__ai_integration__enable_tone_adjustment__label),
                    enabledIf = { prefs.aiIntegration.enabled isEqualTo true },
                )
            }
        }

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
                icon = ImageVector.vectorResource(id = R.drawable.ic_incognito),
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
