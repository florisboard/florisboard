package org.florisboard.lib.snygg

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.florisboard.lib.color.schemes.yellowDarkScheme
import org.florisboard.lib.color.schemes.yellowLightScheme
import org.florisboard.lib.snygg.value.SnyggCircleShapeValue
import org.florisboard.lib.snygg.value.SnyggDpSizeValue
import org.florisboard.lib.snygg.value.SnyggImplicitInheritValue
import org.florisboard.lib.snygg.value.SnyggRectangleShapeValue
import org.florisboard.lib.snygg.value.SnyggStaticColorValue
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
    ): SnyggPropertySet {
        return this.queryStatic(
            elementName,
            attributes,
            selectors,
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
        assertIs<SnyggImplicitInheritValue>(key.foreground)
        assertIs<SnyggImplicitInheritValue>(key.shape)

        val keyCode0 = theme.helperQuery("key", attributes = mapOf("code" to 0))
        assertEquals(key, keyCode0)

        val keyCode1 = theme.helperQuery("key", attributes = mapOf("code" to 1))
        assertIs<SnyggStaticColorValue>(keyCode1.background)
        assertIs<SnyggStaticColorValue>(keyCode1.foreground)
        assertIs<SnyggImplicitInheritValue>(keyCode1.shape)

        val keyGroup2 = theme.helperQuery("key", attributes = mapOf("group" to 2))
        assertIs<SnyggStaticColorValue>(keyGroup2.background)
        assertIs<SnyggImplicitInheritValue>(keyGroup2.foreground)
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
        assertIs<SnyggImplicitInheritValue>(key.background)
    }
}
