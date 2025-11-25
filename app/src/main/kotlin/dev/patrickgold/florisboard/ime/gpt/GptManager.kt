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

package dev.patrickgold.florisboard.ime.gpt

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Base64
import dev.patrickgold.florisboard.app.FlorisPreferenceStore
import dev.patrickgold.florisboard.clipboardManager
import dev.patrickgold.florisboard.editorInstance
import dev.patrickgold.florisboard.ime.clipboard.provider.ItemType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.florisboard.lib.gpt.GptController
import org.florisboard.lib.gpt.GptResult
import org.florisboard.lib.gpt.ImageAttachment
import org.florisboard.lib.gpt.LanguageModelConfig
import java.io.ByteArrayOutputStream

/**
 * State for GPT generation operations.
 */
sealed class GptState {
    data object Idle : GptState()
    data object Generating : GptState()
    data class Success(val text: String) : GptState()
    data class Error(val message: String) : GptState()
}

/**
 * Represents a conversation turn for history.
 */
data class ConversationTurn(
    val prompt: String,
    val response: String,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Manager for GPT text generation functionality.
 */
class GptManager(private val context: Context) {
    private val prefs by FlorisPreferenceStore
    private val editorInstance by context.editorInstance()
    private val clipboardManager by context.clipboardManager()

    // Using Main dispatcher so callbacks are executed on the main thread
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val controller = GptController()

    private val _state = MutableStateFlow<GptState>(GptState.Idle)
    val state = _state.asStateFlow()

    // Conversation history (last few turns)
    private val conversationHistory = mutableListOf<ConversationTurn>()
    private val maxHistorySize = 5

    // Pending image for next AI request
    private val _pendingImage = MutableStateFlow<ImageAttachment?>(null)
    val pendingImage = _pendingImage.asStateFlow()

    val isEnabled: Boolean
        get() = prefs.gpt.enabled.get()

    val triggerPattern: String
        get() = prefs.gpt.triggerPattern.get()

    val config: LanguageModelConfig
        get() = prefs.gpt.config.get()

    /**
     * Check if the given text matches the trigger pattern.
     *
     * @param text The text to check
     * @return The prompt text if trigger pattern is matched, null otherwise
     */
    fun matchesTrigger(text: String): String? {
        if (!isEnabled) return null
        val pattern = triggerPattern
        if (pattern.isEmpty()) return null

        val trimmedText = text.trimEnd()
        if (trimmedText.endsWith(pattern)) {
            // Extract prompt - everything before the trigger pattern (minus the pattern itself)
            val beforeTrigger = trimmedText.dropLast(pattern.length).trimEnd()
            // Find the last line or sentence as the prompt
            val lines = beforeTrigger.split("\n")
            val lastLine = lines.lastOrNull()?.trim() ?: return null
            if (lastLine.isNotEmpty()) {
                return lastLine
            }
        }
        return null
    }

    /**
     * Build context history string based on configuration.
     */
    private fun buildContextHistory(): String? {
        val cfg = config
        val contextParts = mutableListOf<String>()

        // Add clipboard history if enabled
        if (cfg.includeClipboardHistory) {
            val clipboardHistory = getRecentClipboardItems()
            if (clipboardHistory.isNotBlank()) {
                contextParts.add("Recent clipboard:\n$clipboardHistory")
            }
            
            // Also check for clipboard images and set as pending image if not already set
            if (_pendingImage.value == null) {
                tryLoadClipboardImage()
            }
        }

        // Add conversation history if enabled
        if (cfg.includeConversationHistory && conversationHistory.isNotEmpty()) {
            val historyText = conversationHistory.takeLast(3).joinToString("\n\n") { turn ->
                "User: ${turn.prompt}\nAssistant: ${turn.response}"
            }
            contextParts.add("Previous conversation:\n$historyText")
        }

        return if (contextParts.isEmpty()) null else contextParts.joinToString("\n\n---\n\n")
    }

    /**
     * Get recent clipboard items as text.
     */
    private fun getRecentClipboardItems(): String {
        return try {
            val history = clipboardManager.currentHistory
            history.all.take(3)
                .filter { it.type == ItemType.TEXT }
                .mapNotNull { item -> 
                    item.text?.take(200) // Limit each item to 200 chars
                }
                .joinToString("\n- ", prefix = "- ")
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * Try to load an image from clipboard history.
     */
    private fun tryLoadClipboardImage() {
        try {
            val history = clipboardManager.currentHistory
            val imageItem = history.all.firstOrNull { 
                it.type == ItemType.IMAGE 
            }
            
            if (imageItem?.uri != null) {
                setImageFromUri(imageItem.uri)
            }
        } catch (e: Exception) {
            // Ignore errors when trying to load clipboard images
        }
    }

    /**
     * Set a pending image from a URI.
     */
    fun setImageFromUri(uri: Uri): Boolean {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val bytes = inputStream?.readBytes()
            inputStream?.close()
            
            if (bytes != null) {
                val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
                _pendingImage.value = ImageAttachment(base64, mimeType)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Set a pending image from a Bitmap.
     */
    fun setImageFromBitmap(bitmap: Bitmap): Boolean {
        return try {
            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
            val bytes = outputStream.toByteArray()
            val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
            _pendingImage.value = ImageAttachment(base64, "image/jpeg")
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Clear the pending image.
     */
    fun clearPendingImage() {
        _pendingImage.value = null
    }

    /**
     * Check if there is a pending image.
     */
    fun hasPendingImage(): Boolean = _pendingImage.value != null

    /**
     * Generate a response using the configured AI model.
     *
     * @param prompt The prompt to send to the AI
     * @param onComplete Callback when generation is complete (called on Main dispatcher)
     */
    fun generateResponse(prompt: String, onComplete: ((String?) -> Unit)? = null) {
        if (!isEnabled || prompt.isBlank()) {
            onComplete?.invoke(null)
            return
        }

        _state.value = GptState.Generating

        val contextHistory = buildContextHistory()
        val imageAttachment = _pendingImage.value

        scope.launch {
            controller.generateResponse(
                config = config, 
                prompt = prompt, 
                contextHistory = contextHistory,
                imageAttachment = imageAttachment
            ).collect { result ->
                val response: String? = when (result) {
                    is GptResult.Success -> {
                        _state.value = GptState.Success(result.text)
                        // Add to conversation history
                        addToHistory(prompt, result.text)
                        // Clear the pending image after successful use
                        clearPendingImage()
                        result.text
                    }
                    is GptResult.Error -> {
                        _state.value = GptState.Error(result.message)
                        null
                    }
                    is GptResult.MissingApiKey -> {
                        _state.value = GptState.Error("API key not configured")
                        null
                    }
                }
                // Callback is already on Main dispatcher since scope uses Dispatchers.Main
                onComplete?.invoke(response)
            }
        }
    }

    /**
     * Add a conversation turn to history.
     */
    private fun addToHistory(prompt: String, response: String) {
        conversationHistory.add(ConversationTurn(prompt, response))
        // Keep only last N items
        while (conversationHistory.size > maxHistorySize) {
            conversationHistory.removeAt(0)
        }
    }

    /**
     * Clear conversation history.
     */
    fun clearHistory() {
        conversationHistory.clear()
    }

    /**
     * Reset the state to idle.
     */
    fun resetState() {
        _state.value = GptState.Idle
    }
}
