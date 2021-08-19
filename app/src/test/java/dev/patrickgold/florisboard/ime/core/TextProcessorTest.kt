package dev.patrickgold.florisboard.ime.core

import dev.patrickgold.florisboard.common.FlorisLocale
import org.junit.Test
import kotlin.test.assertEquals

/**
 * Unit test for [TextProcessor].
 */
class TextProcessorTest {
    @Test
    fun `Test isWord with samples that are words`() {
        listOf(
            "a",
            "word",
            "you're",
            "re-write",
            "CONSTANT_NAME",
            "__CONSTANT_NAME",
            "__CONSTANT_NAME__",
            "CONSTANT_NAME__",
            "Test20",
            "2021",
            "20Test",
            "Straße",
            "Ärger",
            "Österreich",
            "drüber",
        ).forEach {
            assertEquals(
                expected = true,
                actual = TextProcessor.isWord(it, FlorisLocale.ENGLISH),
                message = "Expected that '$it' is a word, but isn't."
            )
        }
    }

    @Test
    fun `Test isWord with samples that are not words`() {
        listOf(
            " ",
            "&",
            "-",
            "+",
            "*",
            "/",
            "__",
            "~",
            "~",
            "2.0",
            "2,0",
            "--word",
            "--word--",
            "word--",
        ).forEach {
            assertEquals(
                expected = false,
                actual = TextProcessor.isWord(it, FlorisLocale.ENGLISH),
                message = "Expected that '$it' is a word, but isn't."
            )
        }
    }

    @Test
    fun `Test detectWords for basic Latin`() {
        val inputStr = "A test sentence, in 2021 for KOTLIN_TEST. #Test + #Kotlin 2.0"
        assertEquals(
            expected = listOf(
                0..0, 2..5, 7..14, 17..18, 20..23, 25..27, 29..39, 43..46, 51..56, 58..58, 60..60
            ),
            actual = TextProcessor.detectWords(inputStr, FlorisLocale.ENGLISH).toList(),
            message = "Text processor word detection fails for '$inputStr'"
        )
    }
}
