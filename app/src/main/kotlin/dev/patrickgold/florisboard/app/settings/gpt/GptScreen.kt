/*
 * Copyright (C) 2025 The FlorisBoard Contributors
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

package dev.patrickgold.florisboard.app.settings.gpt

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.app.FlorisPreferenceStore
import dev.patrickgold.florisboard.lib.compose.FlorisScreen
import dev.patrickgold.jetpref.datastore.model.observeAsState
import dev.patrickgold.jetpref.datastore.ui.ExperimentalJetPrefDatastoreUi
import dev.patrickgold.jetpref.datastore.ui.PreferenceGroup
import dev.patrickgold.jetpref.datastore.ui.SwitchPreference
import org.florisboard.lib.compose.FlorisInfoCard
import org.florisboard.lib.compose.stringRes
import org.florisboard.lib.gpt.LanguageModel
import org.florisboard.lib.gpt.LanguageModelConfig

@OptIn(ExperimentalJetPrefDatastoreUi::class)
@Composable
fun GptScreen() = FlorisScreen {
    title = stringRes(R.string.settings__gpt__title)
    previewFieldVisible = true

    val prefs by FlorisPreferenceStore
    val scope = rememberCoroutineScope()

    content {
        
        FlorisInfoCard(
            modifier = Modifier.padding(8.dp),
            text = """
                AI text generation allows you to get AI-powered responses directly in the keyboard. 
                Type your prompt followed by the trigger pattern (default: !!) to generate a response.
                
                Note: You need to configure an API key for your chosen AI provider.
            """.trimIndent()
        )

        PreferenceGroup(title = stringRes(R.string.pref__gpt__general_title)) {
            SwitchPreference(
                prefs.gpt.enabled,
                title = stringRes(R.string.pref__gpt__enabled__label),
                summary = stringRes(R.string.pref__gpt__enabled__summary),
            )
        }

        PreferenceGroup(title = stringRes(R.string.pref__gpt__trigger_title)) {
            val triggerPattern by prefs.gpt.triggerPattern.observeAsState()
            var editedPattern by remember(triggerPattern) { mutableStateOf(triggerPattern) }
            
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                OutlinedTextField(
                    value = editedPattern,
                    onValueChange = { newValue ->
                        editedPattern = newValue
                        scope.launch {
                            prefs.gpt.triggerPattern.set(newValue)
                        }
                    },
                    label = { Text(stringRes(R.string.pref__gpt__trigger_pattern__label)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringRes(R.string.pref__gpt__trigger_pattern__summary),
                    modifier = Modifier.padding(horizontal = 4.dp),
                    style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                )
            }
        }

        PreferenceGroup(title = stringRes(R.string.pref__gpt__model_title)) {
            val config by prefs.gpt.config.observeAsState()
            
            // Model selection
            var selectedModel by remember(config.model) { mutableStateOf(config.model) }
            var apiKey by remember(config.apiKey) { mutableStateOf(config.apiKey) }
            var subModel by remember(config.subModel) { mutableStateOf(config.subModel) }
            var baseUrl by remember(config.baseUrl) { mutableStateOf(config.baseUrl) }
            
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                // Model type dropdown
                Text(
                    text = stringRes(R.string.pref__gpt__model__label),
                    style = androidx.compose.material3.MaterialTheme.typography.labelMedium,
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                // Simple model selection using chips
                androidx.compose.foundation.layout.FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
                ) {
                    LanguageModel.entries.forEach { model ->
                        androidx.compose.material3.FilterChip(
                            selected = selectedModel == model,
                            onClick = {
                                selectedModel = model
                                scope.launch {
                                    prefs.gpt.config.set(config.copy(model = model))
                                }
                            },
                            label = { Text(model.label) }
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // API Key input
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { newValue ->
                        apiKey = newValue
                        scope.launch {
                            prefs.gpt.config.set(config.copy(apiKey = newValue))
                        }
                    },
                    label = { Text(stringRes(R.string.pref__gpt__api_key__label)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Sub-model input
                OutlinedTextField(
                    value = subModel.ifEmpty { selectedModel.defaultSubModel },
                    onValueChange = { newValue ->
                        subModel = newValue
                        scope.launch {
                            prefs.gpt.config.set(config.copy(subModel = newValue))
                        }
                    },
                    label = { Text(stringRes(R.string.pref__gpt__sub_model__label)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringRes(R.string.pref__gpt__sub_model__summary),
                    modifier = Modifier.padding(horizontal = 4.dp),
                    style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Custom Base URL input (for OpenAI-compatible APIs like Grok)
                OutlinedTextField(
                    value = baseUrl,
                    onValueChange = { newValue ->
                        baseUrl = newValue
                        scope.launch {
                            prefs.gpt.config.set(config.copy(baseUrl = newValue))
                        }
                    },
                    label = { Text(stringRes(R.string.pref__gpt__base_url__label)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringRes(R.string.pref__gpt__base_url__summary),
                    modifier = Modifier.padding(horizontal = 4.dp),
                    style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                )
            }
        }

        PreferenceGroup(title = stringRes(R.string.pref__gpt__system_prompt_title)) {
            val config by prefs.gpt.config.observeAsState()
            var systemPrompt by remember(config.systemPrompt) { mutableStateOf(config.systemPrompt) }
            
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                OutlinedTextField(
                    value = systemPrompt,
                    onValueChange = { newValue ->
                        systemPrompt = newValue
                        scope.launch {
                            prefs.gpt.config.set(config.copy(systemPrompt = newValue))
                        }
                    },
                    label = { Text(stringRes(R.string.pref__gpt__system_prompt__label)) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 5,
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringRes(R.string.pref__gpt__system_prompt__summary),
                    modifier = Modifier.padding(horizontal = 4.dp),
                    style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                )
            }
        }

        PreferenceGroup(title = stringRes(R.string.pref__gpt__context_title)) {
            val config by prefs.gpt.config.observeAsState()
            
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                // Clipboard history toggle
                androidx.compose.foundation.layout.Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringRes(R.string.pref__gpt__include_clipboard__label),
                            style = androidx.compose.material3.MaterialTheme.typography.bodyLarge,
                        )
                        Text(
                            text = stringRes(R.string.pref__gpt__include_clipboard__summary),
                            style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                        )
                    }
                    androidx.compose.material3.Switch(
                        checked = config.includeClipboardHistory,
                        onCheckedChange = { checked ->
                            scope.launch {
                                prefs.gpt.config.set(config.copy(includeClipboardHistory = checked))
                            }
                        }
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Conversation history toggle
                androidx.compose.foundation.layout.Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringRes(R.string.pref__gpt__include_conversation__label),
                            style = androidx.compose.material3.MaterialTheme.typography.bodyLarge,
                        )
                        Text(
                            text = stringRes(R.string.pref__gpt__include_conversation__summary),
                            style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                        )
                    }
                    androidx.compose.material3.Switch(
                        checked = config.includeConversationHistory,
                        onCheckedChange = { checked ->
                            scope.launch {
                                prefs.gpt.config.set(config.copy(includeConversationHistory = checked))
                            }
                        }
                    )
                }
            }
        }
    }
}
