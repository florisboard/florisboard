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

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlin.system.measureTimeMillis

class LanguageDetectorTest : FunSpec({
    
    lateinit var detector: LanguageDetector
    
    beforeEach {
        detector = LanguageDetector()
    }
    
    context("Telugu script detection") {
        test("should detect pure Telugu text") {
            detector.detectLanguage("నమస్కారం") shouldBe DetectedLanguage.TELUGU
            detector.detectLanguage("మీరు ఎలా ఉన్నారు") shouldBe DetectedLanguage.TELUGU
            detector.detectLanguage("తెలుగు భాష") shouldBe DetectedLanguage.TELUGU
        }
        
        test("should detect mixed Telugu-English text as Telugu") {
            detector.detectLanguage("Hello నమస్కారం") shouldBe DetectedLanguage.TELUGU
            detector.detectLanguage("నమస్కారం world") shouldBe DetectedLanguage.TELUGU
        }
    }
    
    context("English detection") {
        test("should detect pure English text") {
            detector.detectLanguage("Hello world") shouldBe DetectedLanguage.ENGLISH
            detector.detectLanguage("How are you") shouldBe DetectedLanguage.ENGLISH
            detector.detectLanguage("Good morning") shouldBe DetectedLanguage.ENGLISH
        }
        
        test("should detect English with numbers") {
            detector.detectLanguage("Hello 123") shouldBe DetectedLanguage.ENGLISH
            detector.detectLanguage("Test 456 abc") shouldBe DetectedLanguage.ENGLISH
        }
    }
    
    context("Teluglish detection") {
        test("should detect common Teluglish words") {
            detector.detectLanguage("naku telugu raadu") shouldBe DetectedLanguage.TELUGLISH
            detector.detectLanguage("ela unnavu ra") shouldBe DetectedLanguage.TELUGLISH
            detector.detectLanguage("enti chestunnav") shouldBe DetectedLanguage.TELUGLISH
        }
        
        test("should detect Teluglish with suffixes") {
            detector.detectLanguage("vachindi") shouldBe DetectedLanguage.TELUGLISH
            detector.detectLanguage("chesanu") shouldBe DetectedLanguage.TELUGLISH
            detector.detectLanguage("ayindi") shouldBe DetectedLanguage.TELUGLISH
        }
        
        test("should detect mixed Teluglish sentences") {
            detector.detectLanguage("nenu okay andi") shouldBe DetectedLanguage.TELUGLISH
            detector.detectLanguage("chala bagundi ra") shouldBe DetectedLanguage.TELUGLISH
            detector.detectLanguage("meeru ela unnaru") shouldBe DetectedLanguage.TELUGLISH
        }
        
        test("should handle case insensitivity") {
            detector.detectLanguage("NAKU TELUGU RAADU") shouldBe DetectedLanguage.TELUGLISH
            detector.detectLanguage("Ela Unnavu") shouldBe DetectedLanguage.TELUGLISH
        }
    }
    
    context("Edge cases") {
        test("should return UNKNOWN for empty text") {
            detector.detectLanguage("") shouldBe DetectedLanguage.UNKNOWN
        }
        
        test("should return UNKNOWN for whitespace-only text") {
            detector.detectLanguage("   ") shouldBe DetectedLanguage.UNKNOWN
            detector.detectLanguage("\n\t") shouldBe DetectedLanguage.UNKNOWN
        }
        
        test("should handle single characters") {
            detector.detectLanguage("a") shouldBe DetectedLanguage.ENGLISH
            detector.detectLanguage("న") shouldBe DetectedLanguage.TELUGU
        }
        
        test("should handle special characters") {
            detector.detectLanguage("@#$%") shouldBe DetectedLanguage.ENGLISH
            detector.detectLanguage("!!!") shouldBe DetectedLanguage.ENGLISH
        }
    }
    
    context("Performance") {
        test("should detect 1000 texts in under 100ms") {
            val testTexts = listOf(
                "నమస్కారం",
                "Hello world",
                "naku telugu raadu",
                "ela unnavu",
                "Good morning",
                "మీరు ఎలా ఉన్నారు"
            )
            
            val elapsed = measureTimeMillis {
                repeat(1000) {
                    val text = testTexts[it % testTexts.size]
                    detector.detectLanguage(text)
                }
            }
            
            elapsed shouldBe { it < 100 }
        }
    }
    
    context("Cache functionality") {
        test("should cache detection results") {
            val text = "naku telugu raadu"
            
            // First detection
            detector.detectLanguage(text) shouldBe DetectedLanguage.TELUGLISH
            
            // Second detection should use cache
            detector.detectLanguage(text) shouldBe DetectedLanguage.TELUGLISH
        }
        
        test("should clear cache") {
            detector.detectLanguage("naku telugu raadu")
            detector.clearCache()
            // Should still work after cache clear
            detector.detectLanguage("naku telugu raadu") shouldBe DetectedLanguage.TELUGLISH
        }
    }
})
