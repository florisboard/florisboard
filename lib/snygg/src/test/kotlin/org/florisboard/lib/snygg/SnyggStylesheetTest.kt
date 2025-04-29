package org.florisboard.lib.snygg

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.serialization.SerializationException
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
            val stylesheet = SnyggStylesheet.fromJson(BasicStylesheetJson).getOrThrow()
            assertEquals("https://schemas.florisboard.org/snygg/v2/stylesheet", stylesheet.schema)
            assertEquals(3, stylesheet.rules.size)

            val defines = stylesheet.rules[SnyggAnnotationRule.Defines]
            assertNotNull(defines)
            assertIs<SnyggSinglePropertySet>(defines)
            assertEquals(1, defines.properties.size)
            val definesTestValue: SnyggValue? = defines.properties["--test"]
            assertIs<SnyggStaticColorValue>(definesTestValue)
            assertEquals(Color.Transparent, definesTestValue.color)

            val smartbar = stylesheet.rules[SnyggElementRule("smartbar")]
            assertNotNull(smartbar)
            assertIs<SnyggSinglePropertySet>(smartbar)
            assertEquals(3, smartbar.properties.size)
            val smartbarBackground = assertIs<SnyggStaticColorValue>(smartbar.background)
            assertEquals(Color.Transparent, smartbarBackground.color)
            val smartbarBorderColor = assertIs<SnyggDefinedVarValue>(smartbar.borderColor)
            assertEquals("--test", smartbarBorderColor.key)
            val smartbarShadowElevation = assertIs<SnyggDpSizeValue>(smartbar.shadowElevation)
            assertEquals(3.dp, smartbarShadowElevation.dp)

            val keyboard = stylesheet.rules[SnyggElementRule("keyboard")]
            assertNotNull(keyboard)
            assertIs<SnyggSinglePropertySet>(keyboard)
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
            assertThrows<SnyggMissingSchemaException> {
                SnyggStylesheet.fromJson(json).getOrThrow()
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
            assertThrows<SnyggInvalidRuleException> {
                SnyggStylesheet.fromJson(json).getOrThrow()
            }
        }

        @Test
        fun `basic deserialization with invalid property`() {
            val json = """
            {
              $SCHEMA_LINE,
              "@defines": {
                "--test": "transparent"
              },
              "element": {
                "background": "#00000000",
                "border-color": "var(--test)",
                "schatten-erhebung": "3dp"
              }
            }
            """.trimIndent()
            assertThrows<SnyggInvalidPropertyException> {
                SnyggStylesheet.fromJson(json).getOrThrow()
            }
        }

        @Test
        fun `basic deserialization with invalid value`() {
            val json = """
            {
              $SCHEMA_LINE,
              "@defines": {
                "--test": "transparent"
              },
              "element": {
                "background": "#00000000",
                "border-color": "var(--test)",
                "shadow-elevation": "some invalid text that shouldn't be here"
              }
            }
            """.trimIndent()
            assertThrows<SnyggInvalidValueException> {
                SnyggStylesheet.fromJson(json).getOrThrow()
            }
        }

        @Test
        fun `basic serialization`() {
            val stylesheet = SnyggStylesheet(
                schema = "https://schemas.florisboard.org/snygg/v2/stylesheet",
                rules = mapOf(
                    SnyggElementRule("smartbar") to SnyggSinglePropertySet(
                        mapOf(
                            "background" to SnyggStaticColorValue(Color(255, 0, 0)),
                            "shape" to SnyggRectangleShapeValue(),
                        )
                    ),
                    SnyggElementRule("key", selector = SnyggSelector.PRESSED) to SnyggSinglePropertySet(
                        mapOf(
                            "shape" to SnyggCircleShapeValue(),
                        )
                    ),
                ),
            )
            @Language("json")
            val expectedJson =
                """{"${'$'}schema":"https://schemas.florisboard.org/snygg/v2/stylesheet","smartbar":{"background":"rgba(255,0,0,1)","shape":"rectangle()"},"key:pressed":{"shape":"circle()"}}"""
            val actualJson = stylesheet.toJson().getOrThrow()
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
                SnyggStylesheet.fromJson(json).getOrThrow()
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
            val stylesheet = SnyggStylesheet.fromJson(json).getOrThrow()

            assertEquals(1, stylesheet.rules.size)
            val (rule, definesProperties) = stylesheet.rules.entries.first()
            assertIs<SnyggAnnotationRule.Defines>(rule)
            assertIs<SnyggSinglePropertySet>(definesProperties)
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
            assertThrows<SerializationException> {
                SnyggStylesheet.fromJson(json).getOrThrow()
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
                SnyggStylesheet.fromJson(json).getOrThrow()
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
              "@font `My Font`": [
                {
                  "--test": "transparent"
                }
              ]
            }
            """.trimIndent()
            assertThrows<IllegalArgumentException> {
                SnyggStylesheet.fromJson(json).getOrThrow()
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
                SnyggStylesheet.fromJson(json).getOrThrow()
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
              "@font `Comic Sans`": [
                {
                  "src": "uri(`flex:/path/to/file`)"
                }
              ]
            }
            """.trimIndent()
            val stylesheet = SnyggStylesheet.fromJson(json).getOrThrow()
            assertEquals(1, stylesheet.rules.size)
            val (rule, properties) = stylesheet.rules.entries.first()
            val fontRule = assertIs<SnyggAnnotationRule.Font>(rule)
            assertEquals("font", fontRule.decl().name)
            assertEquals("Comic Sans", fontRule.fontName)
            val fontSets = assertIs<SnyggMultiplePropertySets>(properties).sets
            assertEquals(1, fontSets.size)
            val font = fontSets.first()
            val src = assertIs<SnyggUriValue>(font.src)
            assertEquals("flex:/path/to/file", src.uri)
        }

        @Test
        fun `deserialization as single set should fail`() {
            val json = """
            {
              $SCHEMA_LINE,
              "@font `Comic Sans`": {
                "src": "uri(`flex:/path/to/file`)"
              }
            }
            """.trimIndent()
            assertThrows<IllegalArgumentException> {
                SnyggStylesheet.fromJson(json).getOrThrow()
            }
        }

        @Test
        fun `deserialization of font without required src should fail`() {
            val json = """
            {
              $SCHEMA_LINE,
              "@font `Comic Sans`": [
                {
                }
              ]
            }
            """.trimIndent()
            assertThrows<SerializationException> {
                SnyggStylesheet.fromJson(json).getOrThrow()
            }
        }

        @Test
        fun `deserialization with missing font name should fail`() {
            val json = """
            {
              $SCHEMA_LINE,
              "@font": [
                {
                  "src": "uri(`flex:/path/to/file`)"
                }
              ]
            }
            """.trimIndent()
            assertThrows<IllegalArgumentException> {
                SnyggStylesheet.fromJson(json).getOrThrow()
            }
        }

        @Test
        fun `deserialization with empty font name should fail`() {
            val json = """
            {
              $SCHEMA_LINE,
              "@font ``": [
                {
                  "src": "uri(`flex:/path/to/file`)"
                }
              ]
            }
            """.trimIndent()
            assertThrows<IllegalArgumentException> {
                SnyggStylesheet.fromJson(json).getOrThrow()
            }
        }

        @Test
        fun `deserialization with invalid font name should fail`() {
            val json = """
            {
              $SCHEMA_LINE,
              "@font `Test ^$%\\``": [
                {
                  "src": "uri(`flex:/path/to/file`)"
                }
              ]
            }
            """.trimIndent()
            assertThrows<IllegalArgumentException> {
                SnyggStylesheet.fromJson(json).getOrThrow()
            }
        }

        @Test
        fun `serialization of valid font (with one source)`() {
            val stylesheet = SnyggStylesheet.v2 {
                font("Comic Sans") {
                    add {
                        src = uri("flex:/path/to/font.ttf")
                    }
                }
            }
            @Language("json")
            val expectedJson =
                """{"${'$'}schema":"https://schemas.florisboard.org/snygg/v2/stylesheet","@font `Comic Sans`":[{"src":"uri(`flex:/path/to/font.ttf`)"}]}"""
            val actualJson = stylesheet.toJson().getOrThrow()
            assertEquals(expectedJson, actualJson)
        }

        @Test
        fun `serialization of valid font (with two sources)`() {
            val stylesheet = SnyggStylesheet.v2 {
                font("Comic Sans") {
                    add {
                        src = uri("flex:/path/to/font.ttf")
                    }
                    add {
                        src = uri("flex:/path/to/font2.ttf")
                    }
                }
            }
            @Language("json")
            val expectedJson =
                """{"${'$'}schema":"https://schemas.florisboard.org/snygg/v2/stylesheet","@font `Comic Sans`":[{"src":"uri(`flex:/path/to/font.ttf`)"},{"src":"uri(`flex:/path/to/font2.ttf`)"}]}"""
            val actualJson = stylesheet.toJson().getOrThrow()
            assertEquals(expectedJson, actualJson)
        }

        @Test
        fun `serialization of font without required src should fail`() {
            val stylesheet = SnyggStylesheet.v2 {
                font("Comic Sans") {
                    add {
                    }
                }
            }
            assertThrows<SerializationException> {
                stylesheet.toJson().getOrThrow()
            }
        }
    }

    @Nested
    inner class LenientDeserialization {
        @Test
        fun `deserialization with missing schema but ignoreMissingSchema=true should succeed`() {
            val json = """
            {
              "@defines": {
                "--test": "transparent"
              },
              "element": {
                "background": "#ff0000"
              },
              "element:pressed": {
                "foreground": "#ff0000"
              }
            }
            """.trimIndent()
            val config = SnyggJsonConfiguration.of(ignoreMissingSchema = true)
            val stylesheet = SnyggStylesheet.fromJson(json, config).getOrThrow()

            assertEquals(3, stylesheet.rules.size)
        }

        @Test
        fun `deserialization with invalid schema but ignoreInvalidSchema=true should succeed`() {
            val json = """
            {
              "${'$'}schema": "local/path/to/stylesheet.schema.json",
              "@defines": {
                "--test": "transparent"
              },
              "element": {
                "background": "#ff0000"
              },
              "element:pressed": {
                "foreground": "#ff0000"
              }
            }
            """.trimIndent()
            val config = SnyggJsonConfiguration.of(ignoreInvalidSchema = true)
            val stylesheet = SnyggStylesheet.fromJson(json, config).getOrThrow()

            assertEquals(3, stylesheet.rules.size)
        }

        @Test
        fun `deserialization with unsupported schema but ignoreUnsupportedSchema=true should succeed`() {
            val json = """
            {
              "${'$'}schema": "https://schemas.florisboard.org/snygg/v1/stylesheet",
              "@defines": {
                "--test": "transparent"
              },
              "element": {
                "background": "#ff0000"
              },
              "element:pressed": {
                "foreground": "#ff0000"
              }
            }
            """.trimIndent()
            val config = SnyggJsonConfiguration.of(ignoreUnsupportedSchema = true)
            val stylesheet = SnyggStylesheet.fromJson(json, config).getOrThrow()

            assertEquals(3, stylesheet.rules.size)
        }

        @Test
        fun `deserialization with invalid rule but ignoreInvalidRules=true should succeed`() {
            val json = """
            {
              $SCHEMA_LINE,
              "@defines": {
                "--test": "transparent"
              },
              "element": {
                "background": "#ff0000"
              },
              "elem-invalid{prop=name}": {
                "foreground": "#ff0000"
              }
            }
            """.trimIndent()
            val config = SnyggJsonConfiguration.of(ignoreInvalidRules = true)
            val stylesheet = SnyggStylesheet.fromJson(json, config).getOrThrow()

            assertEquals(2, stylesheet.rules.size)
        }

        @Test
        fun `deserialization with invalid property but ignoreInvalidProperties=true should succeed`() {
            val json = """
            {
              $SCHEMA_LINE,
              "@defines": {
                "--test": "transparent"
              },
              "element": {
                "background": "#ff0000",
                "sunshine": "#ffff00"
              },
              "element:pressed": {
                "foreground": "#ff0000"
              }
            }
            """.trimIndent()
            val config = SnyggJsonConfiguration.of(ignoreInvalidProperties = true)
            val stylesheet = SnyggStylesheet.fromJson(json, config).getOrThrow()

            assertEquals(3, stylesheet.rules.size)
        }

        @Test
        fun `deserialization with invalid value but ignoreInvalidValues=true should succeed`() {
            val json = """
            {
              $SCHEMA_LINE,
              "@defines": {
                "--test": "transparent"
              },
              "element": {
                "background": "#ff0000",
                "foreground": "sunshine-heaven"
              },
              "element:pressed": {
                "foreground": "#ff0000"
              }
            }
            """.trimIndent()
            val config = SnyggJsonConfiguration.of(ignoreInvalidValues = true)
            val stylesheet = SnyggStylesheet.fromJson(json, config).getOrThrow()

            assertEquals(3, stylesheet.rules.size)
        }
    }

    @Nested
    inner class PrettyPrintSerialization {
        @Test
        fun `serialization pretty print format should match`() {
            val expectedJson = """
            {
              $SCHEMA_LINE,
              "@defines": {
                "--test": "rgba(0,0,0,1)"
              },
              "element": {
                "background": "rgba(255,0,0,1)"
              },
              "element:pressed": {
                "foreground": "rgba(255,0,0,1)"
              }
            }
            """.trimIndent()
            val config = SnyggJsonConfiguration.of(prettyPrint = true, prettyPrintIndent = "  ")
            val stylesheet = SnyggStylesheet.fromJson(expectedJson, config).getOrThrow()
            val actualJson = stylesheet.toJson(config).getOrThrow()

            assertEquals(expectedJson, actualJson)
        }
    }
}
