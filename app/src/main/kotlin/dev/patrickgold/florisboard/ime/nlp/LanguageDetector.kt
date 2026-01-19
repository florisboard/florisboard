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

/**
 * Detected language modes for text input.
 */
enum class DetectedLanguage {
    /** Pure Telugu script (Unicode range U+0C00 to U+0C7F) */
    TELUGU,
    
    /** Pure English text */
    ENGLISH,
    
    /** Telugu written in Latin script (Romanized Telugu) */
    TELUGLISH,
    
    /** Unknown or empty input */
    UNKNOWN
}

/**
 * Utility class for detecting the language of text input.
 * Supports Telugu script, English, and Teluglish (Telugu in Latin script).
 * 
 * Detection is performed locally with zero network latency, using:
 * - Unicode range checking for Telugu script
 * - Pattern matching for common Teluglish words and suffixes
 * - English as fallback
 */
class LanguageDetector {
    
    companion object {
        // Telugu Unicode range: U+0C00 to U+0C7F
        private val TELUGU_UNICODE_RANGE = '\u0C00'..'\u0C7F'
        
        // Common Teluglish words (Telugu words written in Latin script)
        private val TELUGLISH_WORDS = setOf(
            // Common words
            "naku", "nuvvu", "enti", "ela", "em", "cheppu", "ra", "undi", "ledu",
            "ayindi", "chesanu", "vachindi", "unnavu", "unnaru", "chesav", "chesaru",
            "kavali", "kaadu", "avunu", "ledhu", "eppudu", "ekkada", "evaru",
            "enduku", "ela", "entha", "emi", "emiti", "emaina", "evaraina",
            "akkada", "ikkada", "appudu", "ippudu", "inkoka", "inko", "inka",
            "kuda", "kani", "kaani", "ante", "ani", "annadu", "annadi", "annaru",
            "cheppadu", "cheppadi", "chepparu", "chesadu", "chesadi", "chesaru",
            "vachadu", "vachadi", "vacharu", "poyadu", "poyadi", "poyaru",
            "tinnadu", "tinnadi", "tinnaru", "paddadu", "paddadi", "paddaru",
            "nenu", "meeru", "memu", "vaadu", "vaadi", "vaaru", "adi", "idi",
            "mari", "malli", "mundu", "taruvata", "tarvata", "mundhu",
            "bagundi", "baagundi", "manchidi", "manchidhi", "baaledu", "baledu",
            "sarey", "sare", "okay", "okk", "hmm", "hmmm", "aithe", "ayithe",
            "chala", "chaala", "konchem", "konchemu", "ekkuva", "thakkuva",
            "pedda", "chinna", "peddadi", "chinnadi", "peddavi", "chinnavi"
        )
        
        // Common Teluglish suffixes
        private val TELUGLISH_SUFFIXES = listOf(
            "andi", "anu", "avu", "undi", "adi", "adi", "aru", "avu",
            "anu", "ani", "anta", "ante", "ayi", "ayyi", "ayyindi",
            "anu", "avu", "adi", "aru", "ata", "ate", "ato"
        )
        
        // Compiled regex for Teluglish detection (word boundaries)
        private val TELUGLISH_PATTERN = Regex(
            "\\b(" + TELUGLISH_WORDS.joinToString("|") + ")\\b",
            RegexOption.IGNORE_CASE
        )
        
        // Suffix pattern
        private val TELUGLISH_SUFFIX_PATTERN = Regex(
            "(" + TELUGLISH_SUFFIXES.joinToString("|") + ")\\b",
            RegexOption.IGNORE_CASE
        )
        
        // Minimum confidence threshold for Teluglish detection
        private const val TELUGLISH_CONFIDENCE_THRESHOLD = 0.3
    }
    
    // Simple cache to avoid re-detecting the same text
    private val detectionCache = LinkedHashMap<String, DetectedLanguage>(
        initialCapacity = 10,
        loadFactor = 0.75f,
        accessOrder = true
    )
    private val maxCacheSize = 10
    
    /**
     * Detects the language of the given text.
     * 
     * Detection logic:
     * 1. If text contains Telugu Unicode characters → TELUGU
     * 2. If text matches Teluglish patterns → TELUGLISH
     * 3. If text is non-empty → ENGLISH
     * 4. Otherwise → UNKNOWN
     * 
     * @param text The text to analyze
     * @return The detected language
     */
    fun detectLanguage(text: String): DetectedLanguage {
        // Handle empty or whitespace-only text
        if (text.isBlank()) {
            return DetectedLanguage.UNKNOWN
        }
        
        // Check cache first
        detectionCache[text]?.let { return it }
        
        val detected = when {
            // Check for Telugu Unicode characters
            containsTeluguScript(text) -> DetectedLanguage.TELUGU
            
            // Check for Teluglish patterns
            isTeluglish(text) -> DetectedLanguage.TELUGLISH
            
            // Default to English for non-empty text
            else -> DetectedLanguage.ENGLISH
        }
        
        // Update cache
        updateCache(text, detected)
        
        return detected
    }
    
    /**
     * Checks if the text contains Telugu Unicode characters.
     */
    private fun containsTeluguScript(text: String): Boolean {
        return text.any { it in TELUGU_UNICODE_RANGE }
    }
    
    /**
     * Checks if the text matches Teluglish patterns.
     * Uses a confidence-based approach: if enough words/suffixes match, it's Teluglish.
     */
    private fun isTeluglish(text: String): Boolean {
        val words = text.split(Regex("\\s+"))
        if (words.isEmpty()) return false
        
        var matchCount = 0
        
        // Count word matches
        matchCount += TELUGLISH_PATTERN.findAll(text).count()
        
        // Count suffix matches
        matchCount += TELUGLISH_SUFFIX_PATTERN.findAll(text).count()
        
        // Calculate confidence
        val confidence = matchCount.toDouble() / words.size
        
        return confidence >= TELUGLISH_CONFIDENCE_THRESHOLD
    }
    
    /**
     * Updates the detection cache, maintaining max size.
     */
    private fun updateCache(text: String, detected: DetectedLanguage) {
        if (detectionCache.size >= maxCacheSize) {
            // Remove oldest entry (first entry in LinkedHashMap with accessOrder=true)
            val firstKey = detectionCache.keys.iterator().next()
            detectionCache.remove(firstKey)
        }
        detectionCache[text] = detected
    }
    
    /**
     * Clears the detection cache.
     * Useful for testing or memory management.
     */
    fun clearCache() {
        detectionCache.clear()
    }
}
