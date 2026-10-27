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

package dev.patrickgold.florisboard.ime.ai

import android.content.Context
import android.view.inputmethod.InputConnection
import dev.patrickgold.florisboard.FlorisImeService
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.app.FlorisPreferenceStore
import dev.patrickgold.florisboard.lib.devtools.flogError
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.florisboard.lib.android.showShortToastSync

object AiRewriter {
    private const val Endpoint = "https://openrouter.ai/api/v1/chat/completions"
    private const val Model = "openai/gpt-4o-mini"
    private const val MaxCharsBeforeCursor = 500
    const val DefaultPrompt = "Fix the grammar and spelling of this text."

    data class RewriteTarget(
        val inputConnection: InputConnection,
        val hadSelection: Boolean,
        val sourceText: String,
    )

    private val prefs by FlorisPreferenceStore
    private val json = Json { ignoreUnknownKeys = true }
    private val mediaType = "application/json".toMediaType()
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    fun captureTarget(context: Context): RewriteTarget? {
        val inputConnection = FlorisImeService.currentInputConnection()
        if (inputConnection == null) {
            context.showShortToastSync(R.string.ai_rewrite__no_text)
            return null
        }

        val selectedText = inputConnection.getSelectedText(0)?.toString().orEmpty()
        val sourceText = if (selectedText.isNotEmpty()) {
            selectedText
        } else {
            inputConnection.getTextBeforeCursor(MaxCharsBeforeCursor, 0)?.toString().orEmpty()
        }
        if (sourceText.isBlank()) {
            context.showShortToastSync(R.string.ai_rewrite__no_text)
            return null
        }

        return RewriteTarget(
            inputConnection = inputConnection,
            hadSelection = selectedText.isNotEmpty(),
            sourceText = sourceText,
        )
    }

    suspend fun rewrite(
        context: Context,
        prompt: String = DefaultPrompt,
        target: RewriteTarget? = null,
    ) {
        val rewriteTarget = target ?: captureTarget(context) ?: return

        val apiKey = prefs.ai.openRouterApiKey.get().trim()
        if (apiKey.isEmpty()) {
            context.showShortToastSync(R.string.ai_rewrite__missing_api_key)
            return
        }

        context.showShortToastSync(R.string.ai_rewrite__in_progress)
        runCatching {
            withContext(Dispatchers.IO) {
                requestRewrite(apiKey, rewriteTarget.sourceText, prompt.ifBlank { DefaultPrompt })
            }
        }.onSuccess { rewrittenText ->
            if (rewrittenText.isBlank()) {
                context.showShortToastSync(R.string.ai_rewrite__failed)
            } else {
                replaceText(
                    inputConnection = rewriteTarget.inputConnection,
                    hadSelection = rewriteTarget.hadSelection,
                    sourceLength = rewriteTarget.sourceText.length,
                    rewrittenText = rewrittenText,
                )
            }
        }.onFailure { error ->
            flogError { "AI rewrite failed: ${error.message}" }
            context.showShortToastSync(R.string.ai_rewrite__failed)
        }
    }

    private fun requestRewrite(apiKey: String, text: String, prompt: String): String {
        val requestBody = buildJsonObject {
            put("model", Model)
            put("messages", buildJsonArray {
                add(buildJsonObject {
                    put("role", "system")
                    put("content", prompt)
                })
                add(buildJsonObject {
                    put("role", "user")
                    put("content", text)
                })
            })
        }.toString().toRequestBody(mediaType)

        val request = Request.Builder()
            .url(Endpoint)
            .header("Authorization", "Bearer $apiKey")
            .header("Content-Type", "application/json")
            .header("HTTP-Referer", "https://github.com/florisboard/florisboard")
            .header("X-Title", "FlorisBoard AI Rewrite")
            .post(requestBody)
            .build()

        client.newCall(request).execute().use { response ->
            val responseBody = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw IOException("OpenRouter request failed with HTTP ${response.code}: $responseBody")
            }
            return json.parseToJsonElement(responseBody)
                .jsonObject["choices"]
                ?.jsonArray
                ?.firstOrNull()
                ?.jsonObject
                ?.get("message")
                ?.jsonObject
                ?.get("content")
                ?.jsonPrimitive
                ?.contentOrNull
                ?.trim()
                .orEmpty()
        }
    }

    private fun replaceText(
        inputConnection: InputConnection,
        hadSelection: Boolean,
        sourceLength: Int,
        rewrittenText: String,
    ) {
        val currentInputConnection = FlorisImeService.currentInputConnection()
        if (currentInputConnection !== inputConnection) return

        currentInputConnection.beginBatchEdit()
        try {
            currentInputConnection.finishComposingText()
            if (!hadSelection) {
                currentInputConnection.deleteSurroundingText(sourceLength, 0)
            }
            currentInputConnection.commitText(rewrittenText, 1)
        } finally {
            currentInputConnection.endBatchEdit()
        }
    }
}
