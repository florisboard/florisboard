package org.florisboard.lib.snygg

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import org.florisboard.lib.snygg.value.SnyggCircleShapeValue
import org.florisboard.lib.snygg.value.SnyggDefinedVarValue
import org.florisboard.lib.snygg.value.SnyggDpSizeValue
import org.florisboard.lib.snygg.value.SnyggRectangleShapeValue
import org.florisboard.lib.snygg.value.SnyggRoundedCornerPercentShapeValue
import org.florisboard.lib.snygg.value.SnyggSpSizeValue
import org.florisboard.lib.snygg.value.SnyggStaticColorValue
import org.florisboard.lib.snygg.value.SnyggUriValue
import org.florisboard.lib.snygg.value.SnyggValue
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.assertThrows
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull

class SnyggStylesheetTest {
    private val SCHEMA_LINE = """"${'$'}schema": "https://schemas.florisboard.org/snygg/v2/stylesheet""""

    @Nested
    inner class BasicTests {
        val BasicStylesheetJson = """
            {
              $SCHEMA_LINE,
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
        fun `basic deserialization`() {
            val stylesheet = Json.decodeFromString<SnyggStylesheet>(BasicStylesheetJson)
            assertEquals("https://schemas.florisboard.org/snygg/v2/stylesheet", stylesheet.schema)
            assertEquals(3, stylesheet.rules.size)

            val defines = stylesheet.rules[SnyggAnnotationRule.Defines]
            assertNotNull(defines)
            assertEquals(1, defines.properties.size)
            val definesTestValue: SnyggValue? = defines.properties["--test"]
            assertIs<SnyggStaticColorValue>(definesTestValue)
            assertEquals(Color.Transparent, definesTestValue.color)

            val smartbar = stylesheet.rules[SnyggElementRule("smartbar")]
            assertNotNull(smartbar)
            assertEquals(3, smartbar.properties.size)
            val smartbarBackground = assertIs<SnyggStaticColorValue>(smartbar.background)
            assertEquals(Color.Transparent, smartbarBackground.color)
            val smartbarBorderColor = assertIs<SnyggDefinedVarValue>(smartbar.borderColor)
            assertEquals("--test", smartbarBorderColor.key)
            val smartbarShadowElevation = assertIs<SnyggDpSizeValue>(smartbar.shadowElevation)
            assertEquals(3.dp, smartbarShadowElevation.dp)

            val keyboard = stylesheet.rules[SnyggElementRule("keyboard")]
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
        fun `basic deserialization with missing schema`() {
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
        fun `basic deserialization with invalid rule`() {
            val json = """
            {
              $SCHEMA_LINE,
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
        fun `basic serialization`() {
            val stylesheet = SnyggStylesheet(
                schema = "https://schemas.florisboard.org/snygg/v2/stylesheet",
                rules = mapOf(
                    SnyggElementRule("smartbar") to SnyggPropertySet(
                        mapOf(
                            "background" to SnyggStaticColorValue(Color(255, 0, 0)),
                            "shape" to SnyggRectangleShapeValue(),
                        )
                    ),
                    SnyggElementRule("key", selector = SnyggSelector.PRESSED) to SnyggPropertySet(
                        mapOf(
                            "shape" to SnyggCircleShapeValue(),
                        )
                    ),
                ),
            )
            @Language("json")
            val expectedJson =
                """{"${'$'}schema":"https://schemas.florisboard.org/snygg/v2/stylesheet","smartbar":{"background":"rgba(255,0,0,1)","shape":"rectangle()"},"key:pressed":{"shape":"circle()"}}"""
            val actualJson = Json.encodeToString(stylesheet)
            assertEquals(expectedJson, actualJson)
        }

        @Test
        fun `deserialization with src in element`() {
            val json = """
            {
              $SCHEMA_LINE,
              "@defines": {
                "--test": "transparent"
              },
              "smartbar": {
                "background": "#00000000",
                "shadow-elevation": "3dp",
                "src": "uri(`path/to/file`)"
              }
            }
            """.trimIndent()
            assertThrows<IllegalArgumentException> {
                Json.decodeFromString<SnyggStylesheet>(json)
            }
        }
    }

    @Nested
    inner class VariableTests {
        @Test
        fun `deserialization with valid variables`() {
            val json = """
            {
              $SCHEMA_LINE,
              "@defines": {
                "--primary": "#123456",
                "--main-height": "6dp",
                "--shape-12": "rounded-corner(12%, 12%, 12%, 12%)"
              }
            }
            """.trimIndent()
            val stylesheet = Json.decodeFromString<SnyggStylesheet>(json)

            assertEquals(1, stylesheet.rules.size)
            val (rule, definesProperties) = stylesheet.rules.entries.first()
            assertIs<SnyggAnnotationRule.Defines>(rule)
            val variables = definesProperties.properties
            assertEquals(3, variables.size)

            val primaryValue = assertNotNull(variables["--primary"])
            val primaryColor = assertIs<SnyggStaticColorValue>(primaryValue)
            assertEquals(Color(0x12, 0x34, 0x56), primaryColor.color)

            val mainHeightValue = assertNotNull(variables["--main-height"])
            val mainHeightSize = assertIs<SnyggDpSizeValue>(mainHeightValue)
            assertEquals(6.dp, mainHeightSize.dp)

            val shape12Value = assertNotNull(variables["--shape-12"])
            val shape12Shape = assertIs<SnyggRoundedCornerPercentShapeValue>(shape12Value)
            assertEquals(12, shape12Shape.topStart)
        }

        @Test
        fun `deserialization with invalid defines rule should fail`() {
            val json = """
            {
              $SCHEMA_LINE,
              "@defines variables": {
                "--test": "transparent"
              }
            }
            """.trimIndent()
            assertThrows<IllegalArgumentException> {
                Json.decodeFromString<SnyggStylesheet>(json)
            }
        }

        @Test
        fun `deserialization with invalid variable name should fail`() {
            val json = """
            {
              $SCHEMA_LINE,
              "@defines": {
                "test": "transparent"
              }
            }
            """.trimIndent()
            assertThrows<IllegalArgumentException> {
                Json.decodeFromString<SnyggStylesheet>(json)
            }
        }

        @Test
        fun `deserialization with variable in font should fail`() {
            val json = """
            {
              $SCHEMA_LINE,
              "@defines": {
                "--test": "transparent"
              },
              "@font": {
                "--test": "transparent"
              }
            }
            """.trimIndent()
            assertThrows<IllegalArgumentException> {
                Json.decodeFromString<SnyggStylesheet>(json)
            }
        }

        @Test
        fun `deserialization with variable in element should fail`() {
            val json = """
            {
              $SCHEMA_LINE,
              "@defines": {
                "--test": "transparent"
              },
              "smartbar": {
                "background": "#00000000",
                "shadow-elevation": "3dp",
                "--test": "transparent"
              }
            }
            """.trimIndent()
            assertThrows<IllegalArgumentException> {
                Json.decodeFromString<SnyggStylesheet>(json)
            }
        }
    }

    @Nested
    inner class FontAnnotationTests {
        @Test
        fun `deserialization with valid font`() {
            val json = """
        {
          $SCHEMA_LINE,
          "@font `Comic Sans`": {
            "src": "uri(`flex:/path/to/file`)"
          }
        }
        """.trimIndent()
            val stylesheet = Json.decodeFromString<SnyggStylesheet>(json)
            assertEquals(1, stylesheet.rules.size)
            val (rule, fontProperties) = stylesheet.rules.entries.first()
            val fontRule = assertIs<SnyggAnnotationRule.Font>(rule)
            assertEquals("font", fontRule.name)
            assertEquals("Comic Sans", fontRule.fontName)
            val src = assertIs<SnyggUriValue>(fontProperties.src)
            assertEquals("flex", src.uri.scheme)
            assertEquals("/path/to/file", src.uri.path)
        }

        @Test
        fun `deserialization with missing font name should fail`() {
            val json = """
        {
          $SCHEMA_LINE,
          "@font": {
            "src": "uri(`flex:/path/to/file`)"
          }
        }
        """.trimIndent()
            assertThrows<IllegalArgumentException> {
                Json.decodeFromString<SnyggStylesheet>(json)
            }
        }

        @Test
        fun `deserialization with empty font name should fail`() {
            val json = """
        {
          $SCHEMA_LINE,
          "@font ``": {
            "src": "uri(`path/to/file`)"
          }
        }
        """.trimIndent()
            assertThrows<IllegalArgumentException> {
                Json.decodeFromString<SnyggStylesheet>(json)
            }
        }

        @Test
        fun `deserialization with invalid font name should fail`() {
            val json = """
        {
          $SCHEMA_LINE,
          "@font `Test ^$%\\``": {
            "src": "uri(`path/to/file`)"
          }
        }
        """.trimIndent()
            assertThrows<IllegalArgumentException> {
                Json.decodeFromString<SnyggStylesheet>(json)
            }
        }
    }
}
