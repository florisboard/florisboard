package org.florisboard.lib.snygg

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.florisboard.lib.color.schemes.yellowDarkScheme
import org.florisboard.lib.color.schemes.yellowLightScheme
import org.florisboard.lib.snygg.value.SnyggCircleShapeValue
import org.florisboard.lib.snygg.value.SnyggDpSizeValue
import org.florisboard.lib.snygg.value.SnyggFontStyleValue
import org.florisboard.lib.snygg.value.SnyggFontWeightValue
import org.florisboard.lib.snygg.value.SnyggPaddingValue
import org.florisboard.lib.snygg.value.SnyggUndefinedValue
import org.florisboard.lib.snygg.value.SnyggRectangleShapeValue
import org.florisboard.lib.snygg.value.SnyggSpSizeValue
import org.florisboard.lib.snygg.value.SnyggStaticColorValue
import org.junit.jupiter.api.Nested
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class SnyggThemeTest {
    private fun SnyggTheme.helperQuery(
        elementName: String,
        attributes: SnyggQueryAttributes = emptyMap(),
        selectors: SnyggQuerySelectors = SnyggQuerySelectors(),
        parentStyle: SnyggPropertySet = SnyggPropertySet(),
    ): SnyggPropertySet {
        return this.query(
            elementName,
            attributes,
            selectors,
            parentStyle,
            dynamicLightColorScheme = yellowLightScheme,
            dynamicDarkColorScheme = yellowDarkScheme,
        )
    }

    @Test
    fun `basic theme compilation`() {
        val stylesheet = SnyggStylesheet.v2 {
            defines {
                "--primary" to dynamicDarkColor("primary")
                "--secondary" to dynamicLightColor("secondary")
                "--shape" to size(12.dp)
            }
            "keyboard" {
                background = rgbaColor(255, 255, 255)
                foreground = `var`("--primary")
                shape = `var`("--shape")
            }
            "key" {
                background = rgbaColor(255, 255, 255)
                foreground = `var`("--primary")
                borderColor = `var`("--secondary")
            }
            "key"(pressed = true) {
                foreground = rgbaColor(255, 255, 255)
            }
            "key"(focus = true) {
                borderColor = `var`("--primary")
            }
            "key"(disabled = true) {
                shadowElevation = size(2.dp)
            }
        }
        val theme = SnyggTheme.compileFrom(stylesheet)

        val key = theme.helperQuery("key")
        val keyBackground = assertIs<SnyggStaticColorValue>(key.background)
        assertEquals(Color.White, keyBackground.color)
        val keyForeground = assertIs<SnyggStaticColorValue>(key.foreground)
        assertEquals(yellowDarkScheme.primary, keyForeground.color)
        val keyBorderColor = assertIs<SnyggStaticColorValue>(key.borderColor)
        assertEquals(yellowLightScheme.secondary, keyBorderColor.color)

        val keyPressed = theme.helperQuery("key", selectors = selectorsOf(pressed = true))
        assertEquals(keyBackground, keyPressed.background)
        assertNotEquals(keyForeground, keyPressed.foreground)
        assertEquals(keyBorderColor, keyPressed.borderColor)
        val keyPressedForeground = assertIs<SnyggStaticColorValue>(keyPressed.foreground)
        assertEquals(Color(255, 255, 255), keyPressedForeground.color)

        val keyFocus = theme.helperQuery("key", selectors = selectorsOf(focus = true))
        assertEquals(keyBackground, keyFocus.background)
        assertEquals(keyForeground, keyFocus.foreground)
        assertNotEquals(keyBorderColor, keyFocus.borderColor)
        val keyFocusBorderColor = assertIs<SnyggStaticColorValue>(keyFocus.borderColor)
        assertEquals(yellowDarkScheme.primary, keyFocusBorderColor.color)

        val keyDisabled = theme.helperQuery("key", selectors = selectorsOf(disabled = true))
        assertEquals(keyBackground, keyDisabled.background)
        assertEquals(keyForeground, keyDisabled.foreground)
        assertEquals(keyBorderColor, keyDisabled.borderColor)
        val keyDisabledShadowElevation = assertIs<SnyggDpSizeValue>(keyDisabled.shadowElevation)
        assertEquals(2.dp, keyDisabledShadowElevation.dp)
    }

    @Test
    fun `empty theme compilation`() {
        val stylesheet = SnyggStylesheet.v2 {
            // empty
        }
        val theme = SnyggTheme.compileFrom(stylesheet)

        val key = theme.helperQuery("key")
        assertTrue { key.properties.isEmpty() }
    }

    @Test
    fun `theme with attributes`() {
        val stylesheet = SnyggStylesheet.v2 {
            "key" {
                background = rgbaColor(0, 0, 0)
            }
            "key"("code" to listOf(1)) {
                foreground = rgbaColor(255, 255, 255)
            }
            "key"("group" to listOf(2)) {
                shape = rectangleShape()
            }
            "key"("code" to listOf(1), "group" to listOf(2)) {
                shape = circleShape()
            }
        }
        val theme = SnyggTheme.compileFrom(stylesheet)

        val key = theme.helperQuery("key")
        assertIs<SnyggStaticColorValue>(key.background)
        assertIs<SnyggUndefinedValue>(key.foreground)
        assertIs<SnyggUndefinedValue>(key.shape)

        val keyCode0 = theme.helperQuery("key", attributes = mapOf("code" to 0))
        assertEquals(key, keyCode0)

        val keyCode1 = theme.helperQuery("key", attributes = mapOf("code" to 1))
        assertIs<SnyggStaticColorValue>(keyCode1.background)
        assertIs<SnyggStaticColorValue>(keyCode1.foreground)
        assertIs<SnyggUndefinedValue>(keyCode1.shape)

        val keyGroup2 = theme.helperQuery("key", attributes = mapOf("group" to 2))
        assertIs<SnyggStaticColorValue>(keyGroup2.background)
        assertIs<SnyggUndefinedValue>(keyGroup2.foreground)
        assertIs<SnyggRectangleShapeValue>(keyGroup2.shape)

        val keyCode1Group2 = theme.helperQuery("key", attributes = mapOf("code" to 1, "group" to 2))
        assertIs<SnyggStaticColorValue>(keyCode1Group2.background)
        assertIs<SnyggStaticColorValue>(keyCode1Group2.foreground)
        assertIs<SnyggCircleShapeValue>(keyCode1Group2.shape)
    }

    @Test
    fun `theme with broken vars`() {
        val stylesheet = SnyggStylesheet.v2 {
            "key" {
                background = `var`("--not-existing")
            }
        }
        val theme = SnyggTheme.compileFrom(stylesheet)

        val key = theme.helperQuery("key")
        assertIs<SnyggUndefinedValue>(key.background)
    }

    @Nested
    inner class InheritTests {
        @Test
        fun `single-level inherit behavior`() {
            val stylesheet = SnyggStylesheet.v2 {
                "parent" {
                    background = rgbaColor(255, 0, 0)
                    foreground = rgbaColor(0, 0, 255)
                    borderColor = rgbaColor(255, 255, 255)
                    // borderStyle
                    borderWidth = size(2.dp)
                    // fontFamily
                    fontSize = fontSize(12.sp)
                    fontStyle = fontStyle(FontStyle.Italic)
                    fontWeight = fontWeight(FontWeight.Bold)
                    margin = padding(0.dp)
                    padding = padding(4.dp, 2.dp)
                    shadowColor = rgbaColor(255, 255, 255)
                    shadowElevation = size(2.dp)
                    shape = circleShape()
                }
                "child-inherits-implicitly" {
                    // inherits implicitly
                }
                "child-inherits-explicitly" {
                    background = inherit()
                    foreground = inherit()
                    borderColor = inherit()
                    // borderStyle
                    borderWidth = inherit()
                    // fontFamily
                    fontSize = inherit()
                    fontStyle = inherit()
                    fontWeight = inherit()
                    margin = inherit()
                    padding = inherit()
                    shadowColor = inherit()
                    shadowElevation = inherit()
                    shape = inherit()
                }
            }
            val theme = SnyggTheme.compileFrom(stylesheet)

            val parentStyle = theme.helperQuery("parent")

            val childImplicit = theme.helperQuery("child-inherits-implicitly", parentStyle = parentStyle)
            assertIs<SnyggUndefinedValue>(childImplicit.background)
            assertIs<SnyggStaticColorValue>(childImplicit.foreground)
            assertIs<SnyggUndefinedValue>(childImplicit.borderColor)
            // assertIs<SnyggUndefinedValue>(childImplicit.borderStyle)
            assertIs<SnyggUndefinedValue>(childImplicit.borderWidth)
            // assertIs<???>(childImplicit.fontFamily)
            assertIs<SnyggSpSizeValue>(childImplicit.fontSize)
            assertIs<SnyggFontStyleValue>(childImplicit.fontStyle)
            assertIs<SnyggFontWeightValue>(childImplicit.fontWeight)
            assertIs<SnyggUndefinedValue>(childImplicit.margin)
            assertIs<SnyggUndefinedValue>(childImplicit.padding)
            assertIs<SnyggUndefinedValue>(childImplicit.shadowColor)
            assertIs<SnyggUndefinedValue>(childImplicit.shadowElevation)
            assertIs<SnyggUndefinedValue>(childImplicit.shape)

            val childExplicit = theme.helperQuery("child-inherits-explicitly", parentStyle = parentStyle)
            assertIs<SnyggStaticColorValue>(childExplicit.background)
            assertIs<SnyggStaticColorValue>(childExplicit.foreground)
            assertIs<SnyggStaticColorValue>(childExplicit.borderColor)
            // assertIs<>(childExplicit.borderStyle)
            assertIs<SnyggDpSizeValue>(childExplicit.borderWidth)
            // assertIs<>(childExplicit.fontFamily)
            assertIs<SnyggSpSizeValue>(childExplicit.fontSize)
            assertIs<SnyggFontStyleValue>(childExplicit.fontStyle)
            assertIs<SnyggFontWeightValue>(childExplicit.fontWeight)
            assertIs<SnyggPaddingValue>(childExplicit.margin)
            assertIs<SnyggPaddingValue>(childExplicit.padding)
            assertIs<SnyggStaticColorValue>(childExplicit.shadowColor)
            assertIs<SnyggDpSizeValue>(childExplicit.shadowElevation)
            assertIs<SnyggCircleShapeValue>(childExplicit.shape)
        }

        @Test
        fun `multi-level inherit behavior`() {
            val stylesheet = SnyggStylesheet.v2 {
                "parent" {
                    background = rgbaColor(255, 0, 0)
                    foreground = rgbaColor(0, 0, 255)
                    borderColor = rgbaColor(255, 255, 255)
                    // borderStyle
                    borderWidth = size(2.dp)
                    // fontFamily
                    fontSize = fontSize(12.sp)
                    fontStyle = fontStyle(FontStyle.Italic)
                    fontWeight = fontWeight(FontWeight.Bold)
                    margin = padding(0.dp)
                    padding = padding(4.dp, 2.dp)
                    shadowColor = rgbaColor(255, 255, 255)
                    shadowElevation = size(2.dp)
                    shape = circleShape()
                }
                "middle-one-inherits-implicitly" {
                    // inherits implicitly
                }
                "middle-two-inherits-implicitly" {
                    // inherits implicitly
                }
                "child-inherits-implicitly" {
                    // inherits implicitly
                }
                "middle-one-inherits-explicitly" {
                    background = inherit()
                    foreground = inherit()
                    borderColor = inherit()
                    // borderStyle
                    borderWidth = inherit()
                    // fontFamily
                    fontSize = inherit()
                    fontStyle = inherit()
                    fontWeight = inherit()
                    margin = inherit()
                    padding = inherit()
                    shadowColor = inherit()
                    shadowElevation = inherit()
                    shape = inherit()
                }
                "middle-two-inherits-explicitly" {
                    background = inherit()
                    foreground = inherit()
                    borderColor = inherit()
                    // borderStyle
                    borderWidth = inherit()
                    // fontFamily
                    fontSize = inherit()
                    fontStyle = inherit()
                    fontWeight = inherit()
                    margin = inherit()
                    padding = inherit()
                    shadowColor = inherit()
                    shadowElevation = inherit()
                    shape = inherit()
                }
                "child-inherits-explicitly" {
                    background = inherit()
                    foreground = inherit()
                    borderColor = inherit()
                    // borderStyle
                    borderWidth = inherit()
                    // fontFamily
                    fontSize = inherit()
                    fontStyle = inherit()
                    fontWeight = inherit()
                    margin = inherit()
                    padding = inherit()
                    shadowColor = inherit()
                    shadowElevation = inherit()
                    shape = inherit()
                }
            }
            val theme = SnyggTheme.compileFrom(stylesheet)
            val parentStyle = theme.helperQuery("parent")
            val middleOneInheritsImplicitly = theme.helperQuery("middle-one-inherits-implicitly", parentStyle = parentStyle)
            val middleTwoInheritsImplicitly = theme.helperQuery("middle-two-inherits-implicitly", parentStyle = middleOneInheritsImplicitly)
            val childImplicit = theme.helperQuery("child-inherits-implicitly", parentStyle = middleTwoInheritsImplicitly)
            assertIs<SnyggUndefinedValue>(childImplicit.background)
            assertIs<SnyggStaticColorValue>(childImplicit.foreground)
            assertIs<SnyggUndefinedValue>(childImplicit.borderColor)
            // assertIs<SnyggUndefinedValue>(childImplicit.borderStyle)
            assertIs<SnyggUndefinedValue>(childImplicit.borderWidth)
            // assertIs<???>(childImplicit.fontFamily)
            assertIs<SnyggSpSizeValue>(childImplicit.fontSize)
            assertIs<SnyggFontStyleValue>(childImplicit.fontStyle)
            assertIs<SnyggFontWeightValue>(childImplicit.fontWeight)
            assertIs<SnyggUndefinedValue>(childImplicit.margin)
            assertIs<SnyggUndefinedValue>(childImplicit.padding)
            assertIs<SnyggUndefinedValue>(childImplicit.shadowColor)
            assertIs<SnyggUndefinedValue>(childImplicit.shadowElevation)
            assertIs<SnyggUndefinedValue>(childImplicit.shape)

            val middleOneInheritsExplicitly = theme.helperQuery("middle-one-inherits-explicitly", parentStyle = parentStyle)
            val middleTwoInheritsExplicitly = theme.helperQuery("middle-two-inherits-explicitly", parentStyle = middleOneInheritsExplicitly)
            val childExplicit = theme.helperQuery("child-inherits-explicitly", parentStyle = middleTwoInheritsExplicitly)
            assertIs<SnyggStaticColorValue>(childExplicit.background)
            assertIs<SnyggStaticColorValue>(childExplicit.foreground)
            assertIs<SnyggStaticColorValue>(childExplicit.borderColor)
            // assertIs<>(childExplicit.borderStyle)
            assertIs<SnyggDpSizeValue>(childExplicit.borderWidth)
            // assertIs<>(childExplicit.fontFamily)
            assertIs<SnyggSpSizeValue>(childExplicit.fontSize)
            assertIs<SnyggFontStyleValue>(childExplicit.fontStyle)
            assertIs<SnyggFontWeightValue>(childExplicit.fontWeight)
            assertIs<SnyggPaddingValue>(childExplicit.margin)
            assertIs<SnyggPaddingValue>(childExplicit.padding)
            assertIs<SnyggStaticColorValue>(childExplicit.shadowColor)
            assertIs<SnyggDpSizeValue>(childExplicit.shadowElevation)
            assertIs<SnyggCircleShapeValue>(childExplicit.shape)
        }
    }
}
