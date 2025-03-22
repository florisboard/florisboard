package org.florisboard.lib.snygg

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.serialization.json.Json
import org.florisboard.lib.snygg.value.SnyggDefinedVarValue
import org.florisboard.lib.snygg.value.SnyggDpSizeValue
import org.florisboard.lib.snygg.value.SnyggSolidColorValue
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
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
        val definesTestValue = defines.properties["--test"]
        assertNotNull(definesTestValue)
        assertEquals(SnyggSolidColorValue::class, definesTestValue::class)
        assertEquals(Color.Transparent, (definesTestValue as SnyggSolidColorValue).color)

        val smartbar = stylesheet.rules[SnyggRule("smartbar")]
        assertNotNull(smartbar)
        assertEquals(3, smartbar.properties.size)
        assertEquals(SnyggSolidColorValue::class, smartbar.background::class)
        assertEquals(Color.Transparent, (smartbar.background as SnyggSolidColorValue).color)
        assertEquals(SnyggDefinedVarValue::class, smartbar.borderColor::class)
        assertEquals("--test", (smartbar.borderColor as SnyggDefinedVarValue).key)
        assertEquals(SnyggDpSizeValue::class, smartbar.shadowElevation::class)
        assertEquals(3.dp, (smartbar.shadowElevation as SnyggDpSizeValue).dp)

        val keyboard = stylesheet.rules[SnyggRule("keyboard")]
        assertNotNull(keyboard)
        assertEquals(3, keyboard.properties.size)
        assertEquals(SnyggSolidColorValue::class, keyboard.background::class)
        assertEquals(Color(255, 255, 255), (keyboard.background as SnyggSolidColorValue).color)
        assertEquals(SnyggDpSizeValue::class, keyboard.width::class)
        assertEquals(20.dp, (keyboard.width as SnyggDpSizeValue).dp)
        assertEquals(SnyggDpSizeValue::class, keyboard.height::class)
        assertEquals(30.dp, (keyboard.height as SnyggDpSizeValue).dp)
    }
}
