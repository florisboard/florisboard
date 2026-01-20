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

package dev.patrickgold.florisboard.ime.nlp.ai

import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.google.ai.client.generativeai.type.generationConfig
import dev.patrickgold.florisboard.app.FlorisPreferenceModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Implementation of AIService interfaces using Google Gemini API.
 */
class GeminiAIService(private val prefs: FlorisPreferenceModel) : 
    ReplySuggestionProvider, 
    TextRewriteProvider, 
    ToneAdjustmentProvider {

    private fun getModel(systemInstruction: String? = null): GenerativeModel? {
        val apiKey = prefs.aiIntegration.geminiApiKey.get()
        if (apiKey.isBlank()) return null
        
        return GenerativeModel(
            modelName = "gemini-1.5-flash",
            apiKey = apiKey,
            generationConfig = generationConfig {
                temperature = 0.7f
                topK = 40
                topP = 0.95f
                maxOutputTokens = 1024
            },
            systemInstruction = systemInstruction?.let { content { text(it) } }
        )
    }

    override fun isReady(): Boolean {
        return prefs.aiIntegration.geminiApiKey.get().isNotBlank()
    }

    override suspend fun generateReplies(context: List<String>, limit: Int): AIService.Result<List<String>> = withContext(Dispatchers.IO) {
        val model = getModel(
            systemInstruction = "You are a helpful keyboard assistant. Based on the last few messages in a conversation, generate $limit short, natural-sounding reply suggestions. Keep them concise (1-5 words). Return only the suggestions, one per line."
        ) ?: return@withContext AIService.Result.Error(Exception("API Key not set"))

        try {
            val prompt = "Conversation context:\n" + context.joinToString("\n")
            val response = model.generateContent(prompt)
            val suggestions = response.text?.lines()?.filter { it.isNotBlank() }?.take(limit) ?: emptyList()
            AIService.Result.Success(suggestions)
        } catch (e: Exception) {
            AIService.Result.Error(e, "Failed to generate replies")
        }
    }

    override suspend fun rewriteText(text: String, mode: TextRewriteProvider.RewriteMode): AIService.Result<String> = withContext(Dispatchers.IO) {
        val instruction = when (mode) {
            TextRewriteProvider.RewriteMode.FORMAL -> "Rewrite the following text to be more formal and professional."
            TextRewriteProvider.RewriteMode.CASUAL -> "Rewrite the following text to be more casual and friendly."
            TextRewriteProvider.RewriteMode.SHORTER -> "Make the following text significantly shorter while keeping the core meaning."
            TextRewriteProvider.RewriteMode.LONGER -> "Expand the following text to be more descriptive."
            TextRewriteProvider.RewriteMode.FIX_GRAMMAR -> "Fix any spelling and grammar errors in the following text."
        }

        val model = getModel(systemInstruction = "You are a writing assistant. $instruction Return only the rewritten text.")
            ?: return@withContext AIService.Result.Error(Exception("API Key not set"))

        try {
            val response = model.generateContent(text)
            AIService.Result.Success(response.text ?: text)
        } catch (e: Exception) {
            AIService.Result.Error(e, "Failed to rewrite text")
        }
    }

    override suspend fun adjustTone(text: String, tone: ToneAdjustmentProvider.Tone): AIService.Result<String> = withContext(Dispatchers.IO) {
        val toneInstruction = when (tone) {
            ToneAdjustmentProvider.Tone.PROFESSIONAL -> "professional and business-like"
            ToneAdjustmentProvider.Tone.FRIENDLY -> "friendly and warm"
            ToneAdjustmentProvider.Tone.POLITE -> "very polite and respectful"
            ToneAdjustmentProvider.Tone.DIRECT -> "direct and concise"
        }

        val model = getModel(systemInstruction = "You are a communication assistant. Change the tone of the following text to be $toneInstruction. Return only the modified text.")
            ?: return@withContext AIService.Result.Error(Exception("API Key not set"))

        try {
            val response = model.generateContent(text)
            AIService.Result.Success(response.text ?: text)
        } catch (e: Exception) {
            AIService.Result.Error(e, "Failed to adjust tone")
        }
    }
}
