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
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("FlorisLocale Language Properties")
class FlorisLocaleLanguagePropertiesTest {

    @Test
    @DisplayName("English locale supports capitalization")
    fun testEnglishSupportsCapitalization() {
        val locale = FlorisLocale.from("en")
        assertTrue(locale.supportsCapitalization, "English should support capitalization")
    }

    @Test
    @DisplayName("English locale supports auto-space")
    fun testEnglishSupportsAutoSpace() {
        val locale = FlorisLocale.from("en")
        assertTrue(locale.supportsAutoSpace, "English should support auto-space")
    }

    @Test
    @DisplayName("Chinese locale does not support capitalization")
    fun testChineseNoCapitalization() {
        val locale = FlorisLocale.from("zh")
        assertFalse(locale.supportsCapitalization, "Chinese should not support capitalization")
    }

    @Test
    @DisplayName("Chinese locale does not support auto-space")
    fun testChineseNoAutoSpace() {
        val locale = FlorisLocale.from("zh")
        assertFalse(locale.supportsAutoSpace, "Chinese should not support auto-space")
    }

    @Test
    @DisplayName("Japanese locale does not support capitalization")
    fun testJapaneseNoCapitalization() {
        val locale = FlorisLocale.from("ja")
        assertFalse(locale.supportsCapitalization, "Japanese should not support capitalization")
    }

    @Test
    @DisplayName("Korean locale does not support auto-space")
    fun testKoreanNoAutoSpace() {
        val locale = FlorisLocale.from("ko")
        assertFalse(locale.supportsAutoSpace, "Korean should not support auto-space")
    }

    @Test
    @DisplayName("Thai locale does not support capitalization or auto-space")
    fun testThaiLanguageProperties() {
        val locale = FlorisLocale.from("th")
        assertFalse(locale.supportsCapitalization, "Thai should not support capitalization")
        assertFalse(locale.supportsAutoSpace, "Thai should not support auto-space")
    }

    @Test
    @DisplayName("Hindi locale supports auto-space but not capitalization")
    fun testHindiLanguageProperties() {
        val locale = FlorisLocale.from("hi")
        assertFalse(locale.supportsCapitalization, "Hindi should not support capitalization")
        assertTrue(locale.supportsAutoSpace, "Hindi should support auto-space")
    }

    @Test
    @DisplayName("Multiple locales with same language share properties")
    fun testLocaleCaching() {
        val locale1 = FlorisLocale.from("en", "US")
        val locale2 = FlorisLocale.from("en", "GB")
        
        // Both should have the same language properties since they're both English
        assertEquals(
            locale1.supportsCapitalization,
            locale2.supportsCapitalization,
            "Same language should have same capitalization support"
        )
        assertEquals(
            locale1.supportsAutoSpace,
            locale2.supportsAutoSpace,
            "Same language should have same auto-space support"
        )
    }

    @Test
    @DisplayName("Batch processing of locales works correctly")
    fun testBatchProcessing() {
        val locales = listOf(
            FlorisLocale.from("en"),
            FlorisLocale.from("zh"),
            FlorisLocale.from("ja"),
            FlorisLocale.from("fr"),
            FlorisLocale.from("de")
        )
        
        val batchProps = FlorisLocale.getBatchLanguageProperties(locales)
        
        assertEquals(5, batchProps.size, "Batch should return properties for all locales")
        
        // Verify English properties
        assertTrue(batchProps[locales[0]]!!.supportsCapitalization)
        assertTrue(batchProps[locales[0]]!!.supportsAutoSpace)
        
        // Verify Chinese properties
        assertFalse(batchProps[locales[1]]!!.supportsCapitalization)
        assertFalse(batchProps[locales[1]]!!.supportsAutoSpace)
    }

    @Test
    @DisplayName("Language properties are consistent across multiple calls")
    fun testConsistency() {
        val locale = FlorisLocale.from("es")
        
        val cap1 = locale.supportsCapitalization
        val cap2 = locale.supportsCapitalization
        val space1 = locale.supportsAutoSpace
        val space2 = locale.supportsAutoSpace
        
        assertEquals(cap1, cap2, "Capitalization support should be consistent")
        assertEquals(space1, space2, "Auto-space support should be consistent")
    }

    @Test
    @DisplayName("Support for various Indian languages")
    fun testIndianLanguages() {
        val indianLanguages = listOf("hi", "bn", "ta", "te", "kn", "ml", "gu", "mr", "pa")
        
        indianLanguages.forEach { lang ->
            val locale = FlorisLocale.from(lang)
            assertFalse(
                locale.supportsCapitalization,
                "Indian language '$lang' should not support capitalization"
            )
            assertTrue(
                locale.supportsAutoSpace,
                "Indian language '$lang' should support auto-space"
            )
        }
    }

    @Test
    @DisplayName("Support for various European languages")
    fun testEuropeanLanguages() {
        val europeanLanguages = listOf("en", "es", "fr", "de", "it", "pt", "nl", "sv", "no", "da")
        
        europeanLanguages.forEach { lang ->
            val locale = FlorisLocale.from(lang)
            assertTrue(
                locale.supportsCapitalization,
                "European language '$lang' should support capitalization"
            )
            assertTrue(
                locale.supportsAutoSpace,
                "European language '$lang' should support auto-space"
            )
        }
    }
}
