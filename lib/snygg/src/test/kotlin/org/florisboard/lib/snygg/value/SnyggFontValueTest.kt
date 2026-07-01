package org.florisboard.lib.snygg.value

import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
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
                SnyggDefinedVarValue("shenanigans") to null,
                SnyggUndefinedValue to null,
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
            val pairs = listOf(
                // valid
                "thin" to SnyggFontWeightValue(FontWeight(100)),
                "extra-light" to SnyggFontWeightValue(FontWeight(200)),
                "light" to SnyggFontWeightValue(FontWeight(300)),
                "normal" to SnyggFontWeightValue(FontWeight(400)),
                "medium" to SnyggFontWeightValue(FontWeight(500)),
                "semi-bold" to SnyggFontWeightValue(FontWeight(600)),
                "bold" to SnyggFontWeightValue(FontWeight(700)),
                "extra-bold" to SnyggFontWeightValue(FontWeight(800)),
                "black" to SnyggFontWeightValue(FontWeight(900)),
                "100" to SnyggFontWeightValue(FontWeight(100)),
                "200" to SnyggFontWeightValue(FontWeight(200)),
                "300" to SnyggFontWeightValue(FontWeight(300)),
                "400" to SnyggFontWeightValue(FontWeight(400)),
                "500" to SnyggFontWeightValue(FontWeight(500)),
                "600" to SnyggFontWeightValue(FontWeight(600)),
                "700" to SnyggFontWeightValue(FontWeight(700)),
                "800" to SnyggFontWeightValue(FontWeight(800)),
                "900" to SnyggFontWeightValue(FontWeight(900)),
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
            val pairs = listOf(
                SnyggFontWeightValue(FontWeight(100)) to "thin",
                SnyggFontWeightValue(FontWeight(200)) to "extra-light",
                SnyggFontWeightValue(FontWeight(300)) to "light",
                SnyggFontWeightValue(FontWeight(400)) to "normal",
                SnyggFontWeightValue(FontWeight(500)) to "medium",
                SnyggFontWeightValue(FontWeight(600)) to "semi-bold",
                SnyggFontWeightValue(FontWeight(700)) to "bold",
                SnyggFontWeightValue(FontWeight(800)) to "extra-bold",
                SnyggFontWeightValue(FontWeight(900)) to "black",
                // invalid
                SnyggDefinedVarValue("shenanigans") to null,
                SnyggUndefinedValue to null,
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
