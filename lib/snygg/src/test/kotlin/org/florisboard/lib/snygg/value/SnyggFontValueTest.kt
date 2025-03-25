package org.florisboard.lib.snygg.value

import androidx.compose.ui.text.font.FontStyle
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.assertAll
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class SnyggFontValueTest {
    @Nested
    inner class FontStyleTest {
        private val encoder = SnyggFontStyleValue

        @Test
        fun `deserialize font style values`() {
            val pairs = listOf(
                // valid
                "normal" to SnyggFontStyleValue(FontStyle.Normal),
                "italic" to SnyggFontStyleValue(FontStyle.Italic),
                 // invalid
                "invalid" to null,
                "monospaced" to null,
                "level 3000" to null,
            )
            assertAll(pairs.map { (raw, expected) -> {
                assertEquals(expected, encoder.deserialize(raw).getOrNull())
            } })
        }

        @Test
        fun `serialize font style values`() {
            val pairs = listOf(
                // valid
                SnyggFontStyleValue(FontStyle.Normal) to "normal",
                SnyggFontStyleValue(FontStyle.Italic) to "italic",
                // invalid
                SnyggDefinedVarValue("shenanigans") to null
            )
            assertAll(pairs.map { (snyggValue, expected) -> {
                assertEquals(expected, encoder.serialize(snyggValue).getOrNull())
            } })
        }

        @Test
        fun `check class of default value`() {
            assertIs<SnyggFontStyleValue>(encoder.defaultValue())
        }
    }

    @Nested
    inner class FontWeightTest {
        private val encoder = SnyggFontWeightValue

        @Test
        fun `deserialize font weight values`() {
            val validNamedPairs = SnyggFontWeightValue.Weights.map { (name, fontWeight) ->
                name to SnyggFontWeightValue(fontWeight)
            }
            val validNumericPairs = SnyggFontWeightValue.Weights.map { (_, fontWeight) ->
                fontWeight.weight.toString() to SnyggFontWeightValue(fontWeight)
            }
            val pairs = validNamedPairs + validNumericPairs + listOf(
                // invalid
                "invalid" to null,
                "monospaced" to null,
                "level 3000" to null,
            )
            assertAll(pairs.map { (raw, expected) -> {
                assertEquals(expected, encoder.deserialize(raw).getOrNull())
            } })
        }

        @Test
        fun `serialize font weight values`() {
            val validNamedPairs = SnyggFontWeightValue.Weights.map { (name, fontWeight) ->
                SnyggFontWeightValue(fontWeight) to name
            }
            val pairs = validNamedPairs + listOf(
                // invalid
                SnyggDefinedVarValue("shenanigans") to null
            )
            assertAll(pairs.map { (snyggValue, expected) -> {
                assertEquals(expected, encoder.serialize(snyggValue).getOrNull())
            } })
        }

        @Test
        fun `check class of default value`() {
            assertIs<SnyggFontWeightValue>(encoder.defaultValue())
        }
    }
}
