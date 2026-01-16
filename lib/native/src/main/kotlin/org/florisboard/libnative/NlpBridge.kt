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

package org.florisboard.libnative

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class NativeSuggestion(
    val text: String,
    val confidence: Double,
    val is_eligible_for_auto_commit: Boolean
)

@Serializable
data class NativeSpellCheckResult(
    val is_valid: Boolean,
    val is_typo: Boolean,
    val suggestions: List<String>
)

private external fun nativeLoadDictionary(jsonData: String): Boolean
private external fun nativeSpellCheck(word: String, contextJson: String, maxSuggestions: Int): String?
private external fun nativeSuggest(prefix: String, contextJson: String, maxCount: Int): String?
private external fun nativeLearnWord(word: String, contextJson: String)
private external fun nativePenalizeWord(word: String)
private external fun nativeRemoveWord(word: String): Boolean
private external fun nativeGetFrequency(word: String): Double
private external fun nativeExportPersonalDict(): String?
private external fun nativeImportPersonalDict(jsonData: String): Boolean
private external fun nativeExportContextMap(): String?
private external fun nativeImportContextMap(jsonData: String): Boolean
private external fun nativeClear()

object NlpBridge {
    private val json = Json { ignoreUnknownKeys = true }

    fun loadDictionary(jsonData: String): Boolean = nativeLoadDictionary(jsonData)

    fun spellCheck(word: String, context: List<String>, maxSuggestions: Int): NativeSpellCheckResult? {
        val contextJson = json.encodeToString(context)
        val resultJson = nativeSpellCheck(word, contextJson, maxSuggestions) ?: return null
        return try {
            json.decodeFromString<NativeSpellCheckResult>(resultJson)
        } catch (e: Exception) {
            null
        }
    }

    fun suggest(prefix: String, context: List<String>, maxCount: Int): List<NativeSuggestion> {
        val contextJson = json.encodeToString(context)
        val resultJson = nativeSuggest(prefix, contextJson, maxCount) ?: return emptyList()
        return try {
            json.decodeFromString<List<NativeSuggestion>>(resultJson)
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun learnWord(word: String, context: List<String>) {
        val contextJson = json.encodeToString(context)
        nativeLearnWord(word, contextJson)
    }

    fun penalizeWord(word: String) = nativePenalizeWord(word)

    fun removeWord(word: String): Boolean = nativeRemoveWord(word)

    fun getFrequency(word: String): Double = nativeGetFrequency(word)

    fun exportPersonalDict(): String? = nativeExportPersonalDict()

    fun importPersonalDict(jsonData: String): Boolean = nativeImportPersonalDict(jsonData)

    fun exportContextMap(): String? = nativeExportContextMap()

    fun importContextMap(jsonData: String): Boolean = nativeImportContextMap(jsonData)

    fun clear() = nativeClear()
}
