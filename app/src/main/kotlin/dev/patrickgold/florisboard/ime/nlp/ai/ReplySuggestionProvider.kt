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
