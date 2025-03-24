package org.florisboard.lib.snygg

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.florisboard.lib.color.schemes.yellowDarkScheme
import org.florisboard.lib.color.schemes.yellowLightScheme
import org.florisboard.lib.snygg.value.SnyggStaticColorValue
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

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
            }
            "key"(pressed = true) {
                foreground = rgbaColor(255, 255, 255)
            }
        }
        val theme = SnyggTheme.compileFrom(stylesheet)
        val key = theme.helperQuery("key")
        // Background was always static color
        val keyBackground = assertIs<SnyggStaticColorValue>(key.background)
        assertEquals(Color.White, keyBackground.color)
        // Foreground is double-inferred to a static color
        val keyForeground = assertIs<SnyggStaticColorValue>(key.foreground)
        assertEquals(yellowDarkScheme.primary, keyForeground.color)
    }
}
