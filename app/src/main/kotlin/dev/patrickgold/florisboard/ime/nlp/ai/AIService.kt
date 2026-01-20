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

/**
 * Base interface for all AI operations in FlorisBoard.
 */
interface AIService {
    /**
     * Whether the service is ready to handle requests (e.g., API key is set).
     */
    fun isReady(): Boolean

    /**
     * Generic result wrapper for AI operations.
     */
    sealed class Result<out T> {
        data class Success<T>(val data: T) : Result<T>()
        data class Error(val exception: Throwable, val message: String? = null) : Result<Nothing>()
        object Loading : Result<Nothing>()
    }
}

/**
 * Interface for generating reply suggestions.
 */
interface ReplySuggestionProvider : AIService {
    /**
     * Generates reply suggestions based on the provided context.
     * @param context The conversation context (last few messages).
     * @param limit Maximum number of suggestions to return.
     */
    suspend fun generateReplies(context: List<String>, limit: Int = 3): AIService.Result<List<String>>
}

/**
 * Interface for rewriting or transforming text.
 */
interface TextRewriteProvider : AIService {
    enum class RewriteMode {
        FORMAL,
        CASUAL,
        SHORTER,
        LONGER,
        FIX_GRAMMAR
    }

    /**
     * Rewrites the given text based on the specified mode.
     */
    suspend fun rewriteText(text: String, mode: RewriteMode): AIService.Result<String>
}

/**
 * Interface for adjusting the tone of text.
 */
interface ToneAdjustmentProvider : AIService {
    enum class Tone {
        PROFESSIONAL,
        FRIENDLY,
        POLITE,
        DIRECT
    }

    /**
     * Adjusts the tone of the given text.
     */
    suspend fun adjustTone(text: String, tone: Tone): AIService.Result<String>
}
