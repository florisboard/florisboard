package org.florisboard.lib.snygg

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.florisboard.lib.snygg.value.SnyggDefinedVarValue
import org.florisboard.lib.snygg.value.SnyggDpSizeValue
import org.florisboard.lib.snygg.value.SnyggRectangleShapeValue
import org.florisboard.lib.snygg.value.SnyggSpSizeValue
import org.florisboard.lib.snygg.value.SnyggStaticColorValue
import org.florisboard.lib.snygg.value.SnyggValue
import org.junit.jupiter.api.assertThrows
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull

class SnyggStylesheetTest {
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
            "border-width": "20dp",
            "font-size": "30sp"
          }
        }
        """.trimIndent()

    @Test
    fun `test basic stylesheet deserialization`() {
        val stylesheet = Json.decodeFromString<SnyggStylesheet>(BasicStylesheetJson)
        assertEquals("https://schemas.florisboard.org/snygg/v2/stylesheet", stylesheet.schema)
        assertEquals(3, stylesheet.rules.size)

        val defines = stylesheet.rules[SnyggRule.definedVariablesRule()]
        assertNotNull(defines)
        assertEquals(1, defines.properties.size)
        val definesTestValue: SnyggValue? = defines.properties["--test"]
        assertIs<SnyggStaticColorValue>(definesTestValue)
        assertEquals(Color.Transparent, definesTestValue.color)

        val smartbar = stylesheet.rules[SnyggRule("smartbar")]
        assertNotNull(smartbar)
        assertEquals(3, smartbar.properties.size)
        val smartbarBackground = assertIs<SnyggStaticColorValue>(smartbar.background)
        assertEquals(Color.Transparent, smartbarBackground.color)
        val smartbarBorderColor = assertIs<SnyggDefinedVarValue>(smartbar.borderColor)
        assertEquals("--test", smartbarBorderColor.key)
        val smartbarShadowElevation = assertIs<SnyggDpSizeValue>(smartbar.shadowElevation)
        assertEquals(3.dp, smartbarShadowElevation.dp)

        val keyboard = stylesheet.rules[SnyggRule("keyboard")]
        assertNotNull(keyboard)
        assertEquals(3, keyboard.properties.size)
        val keyboardBackground = assertIs<SnyggStaticColorValue>(keyboard.background)
        assertEquals(Color(255, 255, 255), keyboardBackground.color)
        val keyboardBorderWidth = assertIs<SnyggDpSizeValue>(keyboard.borderWidth)
        assertEquals(20.dp, keyboardBorderWidth.dp)
        val keyboardFontSize = assertIs<SnyggSpSizeValue>(keyboard.fontSize)
        assertEquals(30.sp, keyboardFontSize.sp)
    }

    @Test
    fun `test basic stylesheet deserialization with missing schema`() {
        val json = """
        {
          "@defines": {
            "--test": "transparent"
          },
          "smartbar": {
            "background": "#00000000",
            "border-color": "var(--test)",
            "shadow-elevation": "3dp"
          }
        }
        """.trimIndent()
        assertThrows<SerializationException> {
            Json.decodeFromString<SnyggStylesheet>(json)
        }
    }

    @Test
    fun `test basic stylesheet deserialization with invalid rule`() {
        val json = """
        {
          "${'$'}schema": "https://schemas.florisboard.org/snygg/v2/stylesheet",
          "@defines": {
            "--test": "transparent"
          },
          "not valid !!!!!": {
            "background": "#00000000",
            "border-color": "var(--test)",
            "shadow-elevation": "3dp"
          }
        }
        """.trimIndent()
        assertThrows<SerializationException> {
            Json.decodeFromString<SnyggStylesheet>(json)
        }
    }

    @Test
    fun `test basic stylesheet serialization`() {
        val stylesheet = SnyggStylesheet(
            schema = "https://schemas.florisboard.org/snygg/v2/stylesheet",
            rules = mapOf(
                SnyggRule("smartbar") to SnyggPropertySet(mapOf(
                    "background" to SnyggStaticColorValue(Color(255, 0, 0)),
                    "shape" to SnyggRectangleShapeValue(),
                )),
            ),
        )
        val expectedJson = """{"${'$'}schema":"https://schemas.florisboard.org/snygg/v2/stylesheet","smartbar":{"background":"rgba(255,0,0,1)","shape":"rectangle()"}}"""
        val actualJson = Json.encodeToString(stylesheet)
        assertEquals(expectedJson, actualJson)
    }
}
