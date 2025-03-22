package org.florisboard.lib.snygg

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.serialization.json.Json
import org.florisboard.lib.snygg.value.SnyggDefinedVarValue
import org.florisboard.lib.snygg.value.SnyggDpSizeValue
import org.florisboard.lib.snygg.value.SnyggSolidColorValue
import org.florisboard.lib.snygg.value.SnyggValue
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull

val BasicStylesheetJson = """
{
  "${'$'}schema": "https://schemas.florisboard.org/snygg/v2/stylesheet",
  "@defines": {
    "--test": "transparent"
  },
  "smartbar": {
    "background": "#00000000",
    "border-color": "var(--test)",
    "shadow-elevation": "3dp"
  },
  "keyboard": {
    "background": "rgb(255,255,255)",
    "width": "20dp",
    "height": "30dp"
  }
}
""".trimIndent()

class SnyggStylesheetTest {
    @Test
    fun `test basic stylesheet deserialization`() {
        val stylesheet = Json.decodeFromString<SnyggStylesheet>(BasicStylesheetJson)
        assertEquals("https://schemas.florisboard.org/snygg/v2/stylesheet", stylesheet.schema)
        assertEquals(3, stylesheet.rules.size)

        val defines = stylesheet.rules[SnyggRule.definedVariablesRule()]
        assertNotNull(defines)
        assertEquals(1, defines.properties.size)
        val definesTestValue: SnyggValue? = defines.properties["--test"]
        assertIs<SnyggSolidColorValue>(definesTestValue)
        assertEquals(Color.Transparent, definesTestValue.color)

        val smartbar = stylesheet.rules[SnyggRule("smartbar")]
        assertNotNull(smartbar)
        assertEquals(3, smartbar.properties.size)
        val smartbarBackground = assertIs<SnyggSolidColorValue>(smartbar.background)
        assertEquals(Color.Transparent, smartbarBackground.color)
        val smartbarBorderColor = assertIs<SnyggDefinedVarValue>(smartbar.borderColor)
        assertEquals("--test", smartbarBorderColor.key)
        val smartbarShadowElevation = assertIs<SnyggDpSizeValue>(smartbar.shadowElevation)
        assertEquals(3.dp, smartbarShadowElevation.dp)

        val keyboard = stylesheet.rules[SnyggRule("keyboard")]
        assertNotNull(keyboard)
        assertEquals(3, keyboard.properties.size)
        val keyboardBackground = assertIs<SnyggSolidColorValue>(keyboard.background)
        assertEquals(Color(255, 255, 255), keyboardBackground.color)
        val keyboardWidth = assertIs<SnyggDpSizeValue>(keyboard.width)
        assertEquals(20.dp, keyboardWidth.dp)
        val keyboardHeight = assertIs<SnyggDpSizeValue>(keyboard.height)
        assertEquals(30.dp, keyboardHeight.dp)
    }
}
