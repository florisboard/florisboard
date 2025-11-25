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

package org.florisboard.lib.gpt

import kotlinx.serialization.Serializable

/**
 * Configuration for a language model including API settings and parameters.
 */
@Serializable
data class LanguageModelConfig(
    val model: LanguageModel = LanguageModel.Gemini,
    val apiKey: String = "",
    val subModel: String = "",
    val baseUrl: String = "",
    val systemPrompt: String = DEFAULT_SYSTEM_PROMPT,
    val maxTokens: Int = LanguageModel.DEFAULT_MAX_TOKENS,
    val temperature: Double = LanguageModel.DEFAULT_TEMPERATURE,
    val topP: Double = LanguageModel.DEFAULT_TOP_P,
    val includeClipboardHistory: Boolean = false,
    val includeConversationHistory: Boolean = false,
) {
    companion object {
        const val DEFAULT_SYSTEM_PROMPT = "You are a helpful assistant integrated into a keyboard. Keep responses concise and directly useful."
    }

    fun getEffectiveSubModel(): String = subModel.ifEmpty { model.defaultSubModel }
    fun getEffectiveBaseUrl(): String = baseUrl.ifEmpty { model.defaultBaseUrl }
    fun getEffectiveSystemPrompt(): String = systemPrompt.ifEmpty { DEFAULT_SYSTEM_PROMPT }

    fun hasValidApiKey(): Boolean = apiKey.isNotBlank()
}
