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

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

@DisplayName("LanguagePropertiesDatabase")
class LanguagePropertiesDatabaseTest {

    @BeforeEach
    fun setUp() {
        // Clear cache before each test to ensure independence
        LanguagePropertiesDatabase.clearCache()
    }

    @Test
    @DisplayName("Languages without capitalization support")
    fun testLanguagesWithoutCapitalization() {
        val noCapLanguages = listOf("zh", "ko", "ja", "th", "bn", "hi", "ta", "te", "kn", "ml")
        
        noCapLanguages.forEach { lang ->
            val props = LanguagePropertiesDatabase.getProperties(lang)
            assertFalse(
                props.supportsCapitalization,
                "Language '$lang' should not support capitalization"
            )
        }
    }

    @Test
    @DisplayName("Languages with capitalization support")
    fun testLanguagesWithCapitalization() {
        val capLanguages = listOf("en", "es", "fr", "de", "it", "pt", "ru", "ar", "tr")
        
        capLanguages.forEach { lang ->
            val props = LanguagePropertiesDatabase.getProperties(lang)
            assertTrue(
                props.supportsCapitalization,
                "Language '$lang' should support capitalization"
            )
        }
    }

    @Test
    @DisplayName("Languages without auto-space support")
    fun testLanguagesWithoutAutoSpace() {
        val noSpaceLanguages = listOf("zh", "ja", "ko", "th", "lo", "km", "my", "bo")
        
        noSpaceLanguages.forEach { lang ->
            val props = LanguagePropertiesDatabase.getProperties(lang)
            assertFalse(
                props.supportsAutoSpace,
                "Language '$lang' should not support auto-space"
            )
        }
    }

    @Test
    @DisplayName("Languages with auto-space support")
    fun testLanguagesWithAutoSpace() {
        val spaceLanguages = listOf("en", "es", "fr", "de", "it", "pt", "ru", "ar", "tr", "hi")
        
        spaceLanguages.forEach { lang ->
            val props = LanguagePropertiesDatabase.getProperties(lang)
            assertTrue(
                props.supportsAutoSpace,
                "Language '$lang' should support auto-space"
            )
        }
    }

    @Test
    @DisplayName("Caching mechanism works correctly")
    fun testCachingMechanism() {
        val lang = "en"
        
        // First call - should compute and cache
        val props1 = LanguagePropertiesDatabase.getProperties(lang)
        
        // Second call - should return cached result
        val props2 = LanguagePropertiesDatabase.getProperties(lang)
        
        // Both should be the same instance (reference equality) due to caching
        assertTrue(props1 === props2, "Cached properties should be the same instance")
    }

    @Test
    @DisplayName("Batch retrieval returns correct properties")
    fun testBatchRetrieval() {
        val languages = listOf("en", "zh", "ja", "fr", "es")
        val batchProps = LanguagePropertiesDatabase.getBatchProperties(languages)
        
        assertEquals(5, batchProps.size, "Batch should return properties for all languages")
        
        // Verify some specific properties
        assertTrue(batchProps["en"]!!.supportsCapitalization)
        assertTrue(batchProps["en"]!!.supportsAutoSpace)
        
        assertFalse(batchProps["zh"]!!.supportsCapitalization)
        assertFalse(batchProps["zh"]!!.supportsAutoSpace)
        
        assertTrue(batchProps["fr"]!!.supportsCapitalization)
        assertTrue(batchProps["fr"]!!.supportsAutoSpace)
    }

    @Test
    @DisplayName("Unknown languages get default properties")
    fun testUnknownLanguages() {
        // Made-up language codes
        val unknownLanguages = listOf("xyz", "abc", "zzz")
        
        unknownLanguages.forEach { lang ->
            val props = LanguagePropertiesDatabase.getProperties(lang)
            assertTrue(
                props.supportsCapitalization,
                "Unknown language '$lang' should have default capitalization support"
            )
            assertTrue(
                props.supportsAutoSpace,
                "Unknown language '$lang' should have default auto-space support"
            )
        }
    }

    @ParameterizedTest
    @ValueSource(strings = ["en", "es", "fr", "de", "it", "pt", "nl", "sv", "no", "da"])
    @DisplayName("European languages support both capitalization and auto-space")
    fun testEuropeanLanguages(lang: String) {
        val props = LanguagePropertiesDatabase.getProperties(lang)
        assertTrue(props.supportsCapitalization, "European language '$lang' should support capitalization")
        assertTrue(props.supportsAutoSpace, "European language '$lang' should support auto-space")
    }

    @ParameterizedTest
    @ValueSource(strings = ["zh", "ja", "ko"])
    @DisplayName("CJK languages do not support capitalization or auto-space")
    fun testCJKLanguages(lang: String) {
        val props = LanguagePropertiesDatabase.getProperties(lang)
        assertFalse(props.supportsCapitalization, "CJK language '$lang' should not support capitalization")
        assertFalse(props.supportsAutoSpace, "CJK language '$lang' should not support auto-space")
    }

    @Test
    @DisplayName("Batch retrieval performance optimization")
    fun testBatchPerformance() {
        // Create a large list of language codes
        val languages = (1..50).map { "lang$it" }
        
        // Batch retrieval should handle large lists efficiently
        val batchProps = LanguagePropertiesDatabase.getBatchProperties(languages)
        
        assertEquals(50, batchProps.size, "Batch should handle 50 languages")
        
        // All should have default properties since they're unknown
        batchProps.values.forEach { props ->
            assertTrue(props.supportsCapitalization)
            assertTrue(props.supportsAutoSpace)
        }
    }
}
