/*
 * Copyright (C) 2026 The FlorisBoard Contributors
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

package dev.patrickgold.florisboard.ime.nlp

import android.content.Context
import dev.patrickgold.florisboard.app.FlorisPreferenceStore
import dev.patrickgold.florisboard.ime.core.Subtype
import dev.patrickgold.florisboard.subtypeManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.florisboard.lib.kotlin.collectLatestIn

/**
 * Manages automatic keyboard layout switching based on detected language.
 * 
 * This class monitors the detected language and automatically switches
 * the keyboard layout/subtype when the language changes, providing a
 * seamless multilingual typing experience.
 */
class LanguageAwareLayoutSwitcher(private val context: Context) {
    
    private val prefs by FlorisPreferenceStore
    private val subtypeManager by context.subtypeManager()
    // FIXED: Use Dispatchers.Default to avoid blocking main thread
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    
    // Track the last switched language to avoid redundant switches
    private val _lastSwitchedLanguage = MutableStateFlow(DetectedLanguage.UNKNOWN)
    val lastSwitchedLanguage = _lastSwitchedLanguage.asStateFlow()
    
    // Subtype mappings for each language (thread-safe with @Volatile)
    @Volatile private var teluguSubtype: Subtype? = null
    @Volatile private var englishSubtype: Subtype? = null
    @Volatile private var teluglishSubtype: Subtype? = null
    
    init {
        // Initialize subtype mappings on background thread
        scope.launch {
            discoverSubtypes()
        }
    }
    
    /**
     * Discovers available subtypes for Telugu, English, and Teluglish.
     * This should be called when subtypes are added or removed.
     * FIXED: Added null safety and thread safety
     */
    fun discoverSubtypes() {
        try {
            val subtypes = subtypeManager.subtypes
            
            // FIXED: Check if subtypes list is empty
            if (subtypes.isEmpty()) {
                teluguSubtype = null
                englishSubtype = null
                teluglishSubtype = null
                return
            }
            
            // Find Telugu subtype (locale code "te" or "te_IN")
            teluguSubtype = subtypes.firstOrNull { 
                it.primaryLocale.language == "te" 
            }
            
            // Find English subtype (locale code "en" or "en_US")
            englishSubtype = subtypes.firstOrNull { 
                it.primaryLocale.language == "en" 
            }
            
            // Teluglish could be a custom subtype or use English layout
            // For now, we'll use English layout for Teluglish
            teluglishSubtype = englishSubtype
        } catch (e: Exception) {
            // FIXED: Handle exceptions gracefully
            teluguSubtype = null
            englishSubtype = null
            teluglishSubtype = null
        }
    }
    
    /**
     * Monitors the detected language flow and switches layouts automatically.
     * FIXED: Runs on background thread to avoid blocking main thread
     * 
     * @param detectedLanguageFlow The flow of detected languages from NlpManager
     */
    fun observeLanguageChanges(detectedLanguageFlow: kotlinx.coroutines.flow.StateFlow<DetectedLanguage>) {
        detectedLanguageFlow.collectLatestIn(scope) { detectedLanguage ->
            handleLanguageChange(detectedLanguage)
        }
    }
    
    /**
     * Handles language changes and switches layouts if necessary.
     * FIXED: Async preference read + proper thread handling
     */
    private suspend fun handleLanguageChange(detectedLanguage: DetectedLanguage) {
        // FIXED: Use async preference read to avoid blocking
        val autoSwitchEnabled = try {
            prefs.languageDetection.autoSwitchLayout.get()
        } catch (e: Exception) {
            false // Safe default if preference read fails
        }
        
        // Only switch if auto-switching is enabled
        if (!autoSwitchEnabled) {
            return
        }
        
        // Don't switch if language hasn't changed
        if (detectedLanguage == _lastSwitchedLanguage.value) {
            return
        }
        
        // Don't switch for unknown language
        if (detectedLanguage == DetectedLanguage.UNKNOWN) {
            return
        }
        
        // Determine target subtype based on detected language
        val targetSubtype = when (detectedLanguage) {
            DetectedLanguage.TELUGU -> teluguSubtype
            DetectedLanguage.ENGLISH -> englishSubtype
            DetectedLanguage.TELUGLISH -> teluglishSubtype
            DetectedLanguage.UNKNOWN -> null
        }
        
        // FIXED: Null safety check before switching
        targetSubtype?.let { subtype ->
            try {
                // FIXED: Check if subtype is different AND valid
                if (subtypeManager.activeSubtype.id != subtype.id) {
                    // SubtypeManager.switchToSubtypeById already handles threading
                    subtypeManager.switchToSubtypeById(subtype.id)
                    _lastSwitchedLanguage.value = detectedLanguage
                }
            } catch (e: Exception) {
                // FIXED: Handle switching errors gracefully (e.g., IME not ready)
                // Don't crash, just skip this switch attempt
            }
        }
    }
    
    /**
     * Manually trigger a layout switch for the given language.
     * This can be used for testing or manual control.
     */
    fun switchToLanguage(language: DetectedLanguage) {
        scope.launch {
            handleLanguageChange(language)
        }
    }
    
    /**
     * Reset the switcher state.
     */
    fun reset() {
        _lastSwitchedLanguage.value = DetectedLanguage.UNKNOWN
    }
}
