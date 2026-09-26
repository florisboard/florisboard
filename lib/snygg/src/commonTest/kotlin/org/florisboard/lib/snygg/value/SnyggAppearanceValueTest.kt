package org.florisboard.lib.snygg.value

import androidx.compose.ui.graphics.Color
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.assertAll
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class SnyggAppearanceValueTest {
    @Nested
    inner class StaticColor {
        private val encoder = SnyggStaticColorValue

        private fun helperMakeColor(r: Int, g: Int, b: Int): SnyggStaticColorValue {
            return SnyggStaticColorValue(
                Color(r, g, b)
            )
        }

        private fun helperMakeColor(r: Int, g: Int, b: Int, a: Int): SnyggStaticColorValue {
            return SnyggStaticColorValue(
                Color(r, g, b).copy(alpha = a.toFloat() / 0xFF.toFloat())
            )
        }

        private fun helperMakeColor(r: Int, g: Int, b: Int, a: Float): SnyggStaticColorValue {
            return SnyggStaticColorValue(
                Color(r, g, b).copy(alpha = a)
            )
        }

        @Test
        fun `deserialize named colors`() {
            val pairs = listOf(
                // valid
                "transparent" to helperMakeColor(r = 0, g = 0, b = 0, a = 0),
                // invalid
                "some-color" to null,
            )
            assertAll(pairs.map { (raw, expected) -> {
                assertEquals(expected, encoder.deserialize(raw).getOrNull(), "deserialize $raw")
            } })
        }

        @Test
        fun `deserialize hex6 colors`() {
            val pairs = listOf(
                // valid
                "#000000" to helperMakeColor(r = 0x00, g = 0x00, b = 0x00),
                "#ffffff" to helperMakeColor(r = 0xff, g = 0xff, b = 0xff),
                "#FFFFFF" to helperMakeColor(r = 0xff, g = 0xff, b = 0xff),
                "#abcdef" to helperMakeColor(r = 0xab, g = 0xcd, b = 0xef),
                "#ABCDEF" to helperMakeColor(r = 0xab, g = 0xcd, b = 0xef),
                "#ff66ff" to helperMakeColor(r = 0xff, g = 0x66, b = 0xff),
                "#FF66FF" to helperMakeColor(r = 0xff, g = 0x66, b = 0xff),
                // invalid
                "ff66ff" to null,
                "##ff66ff" to null,
                "#ff 66 ff" to null,
                "#ff66zz" to null,
                "#gggggg" to null,
                "#" to null,
                "#000" to null, // we do not allow shorthand notation
                "#fff" to null, // we do not allow shorthand notation
            )
            assertAll(pairs.map { (raw, expected) -> {
                assertEquals(expected, encoder.deserialize(raw).getOrNull(), "deserialize $raw")
            } })
        }

        @Test
        fun `deserialize hex8 colors`() {
            val pairs = listOf(
                // valid
                "#000000ff" to helperMakeColor(r = 0x00, g = 0x00, b = 0x00, a = 0xff),
                "#00000000" to helperMakeColor(r = 0x00, g = 0x00, b = 0x00, a = 0x00),
                "#00000020" to helperMakeColor(r = 0x00, g = 0x00, b = 0x00, a = 0x20),
                "#ffffffff" to helperMakeColor(r = 0xff, g = 0xff, b = 0xff, a = 0xff),
                "#FFFFFFFF" to helperMakeColor(r = 0xff, g = 0xff, b = 0xff, a = 0xff),
                "#ffffff00" to helperMakeColor(r = 0xff, g = 0xff, b = 0xff, a = 0x00),
                "#FFFFFF00" to helperMakeColor(r = 0xff, g = 0xff, b = 0xff, a = 0x00),
                // invalid
                "#1234567" to null,
                "ff66ff1a" to null,
                "##ff66ff1a" to null,
                "#ff 66 ff 1a" to null,
                "#ff66zz1a" to null,
                "#gggggg1a" to null,
                "#" to null,
                "#000" to null, // we do not allow shorthand notation
                "#fff" to null, // we do not allow shorthand notation
            )
            assertAll(pairs.map { (raw, expected) -> {
                assertEquals(expected, encoder.deserialize(raw).getOrNull(), "deserialize $raw")
            } })
        }

        @Test
        fun `deserialize rgb colors`() {
            val pairs = listOf(
                // valid
                "rgb(0, 0, 0)" to helperMakeColor(r = 0, g = 0, b = 0),
                "rgb(1, 2, 3)" to helperMakeColor(r = 1, g = 2, b = 3),
                "rgb(255, 255, 255)" to helperMakeColor(r = 255, 255, 255),
                "rgb(86, 54, 23)" to helperMakeColor(r = 86, 54, 23),
                "rgb(0,0,0)" to helperMakeColor(r = 0, g = 0, b = 0),
                "rgb(   0  ,0,  0)" to helperMakeColor(r = 0, g = 0, b = 0),
                // invalid
                "rgb" to null,
                "rgb()" to null,
                "rgb( )" to null,
                "rgb(,)" to null,
                "rgb(,,)" to null,
                "rgb(,,,)" to null,
                "rgb(0 0 0)" to null, // we do not allow CSS no-comma syntax
                "RGB(0, 0, 0)" to null, // we do not allow caps function name
                "rgb(-1, -1, -1)" to null,
                "rgb(256, 256, 256)" to null, // we do not allow CSS integer wrap-around of values
                "rgb(0, 0, 0, 1.0)" to null,
            )
            assertAll(pairs.map { (raw, expected) -> {
                assertEquals(expected, encoder.deserialize(raw).getOrNull(), "deserialize $raw")
            } })
        }

        @Test
        fun `deserialize rgba colors`() {
            val pairs = listOf(
                // valid
                "rgba(0, 0, 0, 0)" to helperMakeColor(r = 0, g = 0, b = 0, a = 0.0f),
                "rgba(1, 2, 3, 1.0)" to helperMakeColor(r = 1, g = 2, b = 3, a = 1.0f),
                "rgba(255, 255, 255, 1.0)" to helperMakeColor(r = 255, g = 255, b = 255, ),
                "rgba(86, 54, 23, 0.5)" to helperMakeColor(r = 86, g = 54, b = 23, a = 0.5f),
                "rgba(0,0,0,0)" to helperMakeColor(r = 0, g = 0, b = 0, a = 0.0f),
                "rgba(   0  ,0,  0, 0.000)" to helperMakeColor(r = 0, g = 0, b = 0, a = 0.0f),
                // invalid
                "rgba" to null,
                "rgba()" to null,
                "rgba( )" to null,
                "rgba(,)" to null,
                "rgba(,,)" to null,
                "rgba(,,,)" to null,
                "rgba(0 0 0 0)" to null, // we do not allow CSS no-comma syntax
                "RGBA(0, 0, 0, 0)" to null, // we do not allow caps function name
                "rgba(-1, -1, -1, 1.0)" to null,
                "rgba(256, 256, 256, 1.0)" to null, // we do not allow CSS integer wrap-around of values
                "rgba(0, 0, 0, 1.5)" to null,
            )
            assertAll(pairs.map { (raw, expected) -> {
                assertEquals(expected, encoder.deserialize(raw).getOrNull(), "deserialize $raw")
            } })
        }

        @Test
        fun `serialize colors`() {
            val pairs = listOf(
                // valid
                helperMakeColor(r = 0, g = 0, b = 0) to "rgba(0,0,0,1)",
                // invalid
                SnyggDefinedVarValue("shenanigans") to null
            )
            assertAll(pairs.map { (solidColorValue, expected) -> {
                assertEquals(expected, encoder.serialize(solidColorValue).getOrNull(), "serialize $solidColorValue")
            } })
        }

        @Test
        fun `check class of default value`() {
            assertIs<SnyggStaticColorValue>(encoder.defaultValue())
        }
    }

    @Nested
    inner class DynamicColor {
        private val lightColorEncoder = SnyggDynamicLightColorValue
        private val darkColorEncoder = SnyggDynamicDarkColorValue

        private fun helperLightColor(id: String): SnyggDynamicLightColorValue {
            return SnyggDynamicLightColorValue(colorName = id)
        }

        private fun helperDarkColor(id: String): SnyggDynamicDarkColorValue {
            return SnyggDynamicDarkColorValue(colorName = id)
        }

        private fun helperLightColorString(id: String): String {
            return "dynamic-light-color($id)"
        }

        private fun helperDarkColorString(id: String): String {
            return "dynamic-dark-color($id)"
        }

        @Test
        fun `check class of default value`() {
            assertIs<SnyggDynamicLightColorValue>(lightColorEncoder.defaultValue())
            assertIs<SnyggDynamicDarkColorValue>(darkColorEncoder.defaultValue())
        }

        @Test
        fun `test default values`() {
            val pairs = listOf(
                SnyggDynamicLightColorValue("primary").encoder() to lightColorEncoder,
                SnyggDynamicDarkColorValue("primary").encoder() to darkColorEncoder,
            )
            assertAll(pairs.map { (expected, actual) -> {
                    assertEquals(expected, actual)
            } })
        }

        @Test
        fun `deserialize dynamic dark colors`() {
            val colors = listOf(
                // valid
                helperDarkColorString("primary") to helperDarkColor("primary"),
                helperDarkColorString("onPrimary") to helperDarkColor("onPrimary"),
                helperDarkColorString("onTertiaryContainer") to helperDarkColor("onTertiaryContainer"),

                // invalid
                helperDarkColorString("") to null,
                helperDarkColorString("invalid") to null,
                helperDarkColorString("invalid is invalid") to null,
            )
            assertAll(colors.map { (dynamicColorValue, expected) -> {
                assertEquals(expected, darkColorEncoder.deserialize(dynamicColorValue).getOrNull())
            } })
        }

        @Test
        fun `deserialize dynamic light colors`() {
            val colors = listOf(
                // valid
                helperLightColorString("primary") to helperLightColor("primary"),
                helperLightColorString("onPrimary") to helperLightColor("onPrimary"),
                helperLightColorString("onTertiaryContainer") to helperLightColor("onTertiaryContainer"),

                // invalid
                helperLightColorString("") to null,
                helperLightColorString("invalid") to null,
                helperLightColorString("invalid is invalid") to null,
            )
            assertAll(colors.map { (dynamicColorValue, expected) -> {
                assertEquals(expected, lightColorEncoder.deserialize(dynamicColorValue).getOrNull())
            } })
        }

        @Test
        fun `serialize dynamic dark colors`() {
            val pairs = listOf(
                // valid
                helperDarkColor("primary") to helperDarkColorString("primary"),
                helperDarkColor("onPrimary") to helperDarkColorString("onPrimary"),
                helperDarkColor("onTertiaryContainer") to helperDarkColorString("onTertiaryContainer"),
                // invalid
                SnyggDefinedVarValue("shenanigans") to null,
            )
            assertAll(pairs.map { (dynamicColorValue, expected) -> {
                assertEquals(expected, darkColorEncoder.serialize(dynamicColorValue).getOrNull())
            } })
        }

        @Test
        fun `serialize dynamic light colors`() {
            val pairs = listOf(
                // valid
                helperLightColor("primary") to helperLightColorString("primary"),
                helperLightColor("onPrimary") to helperLightColorString("onPrimary"),
                helperLightColor("onTertiaryContainer") to helperLightColorString("onTertiaryContainer"),

                // invalid
                SnyggDefinedVarValue("shenanigans") to null,
            )
            assertAll(pairs.map { (dynamicColorValue, expected) -> {
                assertEquals(expected, lightColorEncoder.serialize(dynamicColorValue).getOrNull())
            } })
        }

    }
}
