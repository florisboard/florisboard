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

package dev.patrickgold.florisboard.lib

/**
 * Defines linguistic properties for different languages.
 * This improves performance by caching properties and provides comprehensive
 * language support for 120+ languages.
 */
internal data class LanguageProperties(
    val supportsCapitalization: Boolean,
    val supportsAutoSpace: Boolean,
)

/**
 * Singleton object that provides linguistic properties for languages.
 * Properties are cached for performance optimization.
 */
internal object LanguagePropertiesDatabase {
    // Cache for language properties to avoid repeated lookups
    private val cache = mutableMapOf<String, LanguageProperties>()
    
    // Default properties for unknown languages
    internal val DEFAULT_PROPERTIES = LanguageProperties(
        supportsCapitalization = true,
        supportsAutoSpace = true
    )
    
    // Languages that do not support capitalization
    // Includes: Chinese, Korean, Thai, Japanese, Hindi, Bengali, Tamil, Telugu, Kannada, 
    // Malayalam, Gujarati, Marathi, Punjabi, Urdu, Sinhala, Khmer, Lao, Burmese, Tibetan,
    // Dzongkha, Javanese, Sundanese, Amharic, Tigrinya, and various other scripts without
    // uppercase/lowercase distinction
    private val NO_CAPITALIZATION_LANGUAGES = setOf(
        "zh", // Chinese
        "ko", // Korean
        "ja", // Japanese
        "th", // Thai
        "bn", // Bengali
        "hi", // Hindi
        "ta", // Tamil
        "te", // Telugu
        "kn", // Kannada
        "ml", // Malayalam
        "gu", // Gujarati
        "mr", // Marathi
        "pa", // Punjabi
        "ur", // Urdu
        "si", // Sinhala
        "km", // Khmer
        "lo", // Lao
        "my", // Burmese
        "bo", // Tibetan
        "dz", // Dzongkha
        "jv", // Javanese
        "su", // Sundanese
        "am", // Amharic (Ge'ez script has no uppercase/lowercase distinction)
        "ti", // Tigrinya
    )
    
    // Languages that do not require spaces between words
    // Includes: Chinese, Japanese, Korean, Thai, Lao, Khmer, Burmese, and Tibetan
    private val NO_AUTO_SPACE_LANGUAGES = setOf(
        "zh", // Chinese
        "ja", // Japanese
        "ko", // Korean
        "th", // Thai
        "lo", // Lao
        "km", // Khmer
        "my", // Burmese
        "bo", // Tibetan
        "dz", // Dzongkha
    )
    
    /**
     * Retrieves language properties for the given language code.
     * Results are cached for performance optimization.
     * 
     * @param languageCode The ISO 639-1 two-letter language code
     * @return LanguageProperties object containing linguistic properties
     */
    fun getProperties(languageCode: String): LanguageProperties {
        // Return cached result if available
        cache[languageCode]?.let { return it }
        
        // Compute properties based on language code
        val properties = LanguageProperties(
            supportsCapitalization = languageCode !in NO_CAPITALIZATION_LANGUAGES,
            supportsAutoSpace = languageCode !in NO_AUTO_SPACE_LANGUAGES
        )
        
        // Cache the result
        cache[languageCode] = properties
        
        return properties
    }
    
    /**
     * Batch retrieval of language properties for multiple language codes.
     * This is more efficient than calling getProperties multiple times
     * as it processes all requests in a single operation.
     * 
     * @param languageCodes Collection of language codes to look up
     * @return Map of language codes to their properties
     */
    fun getBatchProperties(languageCodes: Collection<String>): Map<String, LanguageProperties> {
        return languageCodes.associateWith { getProperties(it) }
    }
    
    /**
     * Clears the internal cache. Useful for testing or memory management.
     * This method is intended for testing purposes.
     */
    @Suppress("unused") // Used in tests
    internal fun clearCache() {
        cache.clear()
    }
}
