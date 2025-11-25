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

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

/**
 * Result class for AI generation operations.
 */
sealed class GptResult {
    data class Success(val text: String) : GptResult()
    data class Error(val message: String, val throwable: Throwable? = null) : GptResult()
    data object MissingApiKey : GptResult()
}

/**
 * Represents an image attachment for vision AI.
 */
data class ImageAttachment(
    val base64Data: String,
    val mimeType: String = "image/jpeg"
)

/**
 * Controller for generating AI responses using various language models.
 */
class GptController {
    companion object {
        private val json = Json { ignoreUnknownKeys = true }
    }

    /**
     * Generate a response from the AI model.
     *
     * @param config The language model configuration
     * @param prompt The user's prompt
     * @param systemMessage Optional custom system message (overrides config.systemPrompt)
     * @param contextHistory Optional context history to include with the prompt
     * @param imageAttachment Optional image for vision models
     * @return Flow emitting GptResult
     */
    fun generateResponse(
        config: LanguageModelConfig,
        prompt: String,
        systemMessage: String? = null,
        contextHistory: String? = null,
        imageAttachment: ImageAttachment? = null
    ): Flow<GptResult> = flow {
        if (!config.hasValidApiKey()) {
            emit(GptResult.MissingApiKey)
            return@flow
        }

        try {
            // Build the effective prompt with context if provided
            val effectivePrompt = if (contextHistory.isNullOrBlank()) {
                prompt
            } else {
                "Context:\n$contextHistory\n\nUser request:\n$prompt"
            }
            
            val effectiveSystemMessage = systemMessage ?: config.getEffectiveSystemPrompt()
            
            val result = when (config.model) {
                LanguageModel.Gemini -> generateGeminiResponse(config, effectivePrompt, effectiveSystemMessage, imageAttachment)
                LanguageModel.ChatGPT,
                LanguageModel.Groq,
                LanguageModel.OpenRouter,
                LanguageModel.Mistral -> generateOpenAICompatibleResponse(config, effectivePrompt, effectiveSystemMessage, imageAttachment)
                LanguageModel.Claude -> generateClaudeResponse(config, effectivePrompt, effectiveSystemMessage, imageAttachment)
            }
            emit(result)
        } catch (e: Exception) {
            emit(GptResult.Error("Failed to generate response: ${e.message}", e))
        }
    }.flowOn(Dispatchers.IO)

    private fun generateGeminiResponse(
        config: LanguageModelConfig,
        prompt: String,
        systemMessage: String,
        imageAttachment: ImageAttachment? = null
    ): GptResult {
        val url = "${config.getEffectiveBaseUrl()}/models/${config.getEffectiveSubModel()}:generateContent"

        val requestBody = buildJsonObject {
            put("contents", buildJsonArray {
                add(buildJsonObject {
                    put("role", "user")
                    put("parts", buildJsonArray {
                        add(buildJsonObject { put("text", "[$systemMessage]") })
                    })
                })
                add(buildJsonObject {
                    put("role", "user")
                    put("parts", buildJsonArray {
                        // Add image if provided
                        if (imageAttachment != null) {
                            add(buildJsonObject {
                                put("inline_data", buildJsonObject {
                                    put("mime_type", imageAttachment.mimeType)
                                    put("data", imageAttachment.base64Data)
                                })
                            })
                        }
                        add(buildJsonObject { put("text", prompt) })
                    })
                })
            })
            put("generationConfig", buildJsonObject {
                put("maxOutputTokens", config.maxTokens)
                put("temperature", config.temperature)
                put("topP", config.topP)
            })
            put("safetySettings", buildJsonArray {
                listOf(
                    "HARM_CATEGORY_SEXUALLY_EXPLICIT",
                    "HARM_CATEGORY_HATE_SPEECH",
                    "HARM_CATEGORY_HARASSMENT",
                    "HARM_CATEGORY_DANGEROUS_CONTENT"
                ).forEach { category ->
                    add(buildJsonObject {
                        put("category", category)
                        put("threshold", "BLOCK_NONE")
                    })
                }
            })
        }

        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("x-goog-api-key", config.apiKey)
            doOutput = true
        }

        return try {
            OutputStreamWriter(connection.outputStream).use { writer ->
                writer.write(requestBody.toString())
            }

            val responseCode = connection.responseCode
            if (responseCode != HttpURLConnection.HTTP_OK) {
                val errorStream = connection.errorStream?.bufferedReader()?.readText() ?: "Unknown error"
                return GptResult.Error("HTTP $responseCode: $errorStream")
            }

            val response = BufferedReader(InputStreamReader(connection.inputStream)).use { reader ->
                reader.readText()
            }

            val responseJson = json.parseToJsonElement(response).jsonObject
            val candidates = responseJson["candidates"]?.jsonArray
            if (candidates != null && candidates.isNotEmpty()) {
                val content = candidates[0].jsonObject["content"]?.jsonObject
                val parts = content?.get("parts")?.jsonArray
                if (parts != null && parts.isNotEmpty()) {
                    val text = parts[0].jsonObject["text"]?.jsonPrimitive?.content
                    if (text != null) {
                        return GptResult.Success(text)
                    }
                }
            }
            GptResult.Error("Invalid response format from Gemini API")
        } finally {
            connection.disconnect()
        }
    }

    private fun generateOpenAICompatibleResponse(
        config: LanguageModelConfig,
        prompt: String,
        systemMessage: String,
        imageAttachment: ImageAttachment? = null
    ): GptResult {
        val url = "${config.getEffectiveBaseUrl()}/chat/completions"

        val requestBody = buildJsonObject {
            put("model", config.getEffectiveSubModel())
            put("messages", buildJsonArray {
                add(buildJsonObject {
                    put("role", "system")
                    put("content", systemMessage)
                })
                add(buildJsonObject {
                    put("role", "user")
                    // For vision models, content is an array of objects
                    if (imageAttachment != null) {
                        put("content", buildJsonArray {
                            add(buildJsonObject {
                                put("type", "image_url")
                                put("image_url", buildJsonObject {
                                    put("url", "data:${imageAttachment.mimeType};base64,${imageAttachment.base64Data}")
                                })
                            })
                            add(buildJsonObject {
                                put("type", "text")
                                put("text", prompt)
                            })
                        })
                    } else {
                        put("content", prompt)
                    }
                })
            })
            put("stream", false)
            put("max_tokens", config.maxTokens)
            put("temperature", config.temperature)
            put("top_p", config.topP)
        }

        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Authorization", "Bearer ${config.apiKey}")
            doOutput = true
        }

        return try {
            OutputStreamWriter(connection.outputStream).use { writer ->
                writer.write(requestBody.toString())
            }

            val responseCode = connection.responseCode
            if (responseCode != HttpURLConnection.HTTP_OK) {
                val errorStream = connection.errorStream?.bufferedReader()?.readText() ?: "Unknown error"
                return GptResult.Error("HTTP $responseCode: $errorStream")
            }

            val response = BufferedReader(InputStreamReader(connection.inputStream)).use { reader ->
                reader.readText()
            }

            val responseJson = json.parseToJsonElement(response).jsonObject
            val choices = responseJson["choices"]?.jsonArray
            if (choices != null && choices.isNotEmpty()) {
                val message = choices[0].jsonObject["message"]?.jsonObject
                val content = message?.get("content")?.jsonPrimitive?.content
                if (content != null) {
                    return GptResult.Success(content)
                }
            }
            GptResult.Error("Invalid response format from API")
        } finally {
            connection.disconnect()
        }
    }

    private fun generateClaudeResponse(
        config: LanguageModelConfig,
        prompt: String,
        systemMessage: String,
        imageAttachment: ImageAttachment? = null
    ): GptResult {
        val url = "${config.getEffectiveBaseUrl()}/messages"

        val requestBody = buildJsonObject {
            put("model", config.getEffectiveSubModel())
            put("max_tokens", config.maxTokens)
            put("system", systemMessage)
            put("messages", buildJsonArray {
                add(buildJsonObject {
                    put("role", "user")
                    // For vision models, content is an array of objects
                    if (imageAttachment != null) {
                        put("content", buildJsonArray {
                            add(buildJsonObject {
                                put("type", "image")
                                put("source", buildJsonObject {
                                    put("type", "base64")
                                    put("media_type", imageAttachment.mimeType)
                                    put("data", imageAttachment.base64Data)
                                })
                            })
                            add(buildJsonObject {
                                put("type", "text")
                                put("text", prompt)
                            })
                        })
                    } else {
                        put("content", prompt)
                    }
                })
            })
        }

        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("x-api-key", config.apiKey)
            setRequestProperty("anthropic-version", "2023-06-01")
            doOutput = true
        }

        return try {
            OutputStreamWriter(connection.outputStream).use { writer ->
                writer.write(requestBody.toString())
            }

            val responseCode = connection.responseCode
            if (responseCode != HttpURLConnection.HTTP_OK) {
                val errorStream = connection.errorStream?.bufferedReader()?.readText() ?: "Unknown error"
                return GptResult.Error("HTTP $responseCode: $errorStream")
            }

            val response = BufferedReader(InputStreamReader(connection.inputStream)).use { reader ->
                reader.readText()
            }

            val responseJson = json.parseToJsonElement(response).jsonObject
            val content = responseJson["content"]?.jsonArray
            if (content != null && content.isNotEmpty()) {
                val text = content[0].jsonObject["text"]?.jsonPrimitive?.content
                if (text != null) {
                    return GptResult.Success(text)
                }
            }
            GptResult.Error("Invalid response format from Claude API")
        } finally {
            connection.disconnect()
        }
    }
}
