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
 * Enum representing supported language models for AI text generation.
 */
@Serializable
enum class LanguageModel(
    val label: String,
    val defaultSubModel: String,
    val defaultBaseUrl: String,
) {
    Gemini(
        label = "Gemini",
        defaultSubModel = "gemini-2.0-flash",
        defaultBaseUrl = "https://generativelanguage.googleapis.com/v1beta"
    ),
    ChatGPT(
        label = "ChatGPT",
        defaultSubModel = "gpt-4o-mini",
        defaultBaseUrl = "https://api.openai.com/v1"
    ),
    Groq(
        label = "Groq",
        defaultSubModel = "llama3-8b-8192",
        defaultBaseUrl = "https://api.groq.com/openai/v1"
    ),
    OpenRouter(
        label = "OpenRouter",
        defaultSubModel = "openai/gpt-4o-mini",
        defaultBaseUrl = "https://openrouter.ai/api/v1"
    ),
    Claude(
        label = "Claude",
        defaultSubModel = "claude-3-5-sonnet-20240620",
        defaultBaseUrl = "https://api.anthropic.com/v1"
    ),
    Mistral(
        label = "Mistral",
        defaultSubModel = "mistral-small-latest",
        defaultBaseUrl = "https://api.mistral.ai/v1"
    );

    companion object {
        const val DEFAULT_MAX_TOKENS = 4096
        const val DEFAULT_TEMPERATURE = 1.0
        const val DEFAULT_TOP_P = 1.0
    }
}
