package org.florisboard.lib.snygg

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.florisboard.lib.snygg.value.SnyggDefinedVarValue
import org.florisboard.lib.snygg.value.SnyggDpSizeValue
import org.florisboard.lib.snygg.value.SnyggRectangleShapeValue
import org.florisboard.lib.snygg.value.SnyggShapeValue
import org.florisboard.lib.snygg.value.SnyggSolidColorValue
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull

class SnyggStylesheetEditorTest {
    @Test
    fun `test simple dynamically-built stylesheet`() {
        val stylesheet = SnyggStylesheet.v2 {
            defines {
                "--small-size" to size(12.dp)
                "--shape-modern" to roundedCornerShape(16.dp)
            }
            "smartbar" {
                "background" to rgbaColor(255, 0, 0)
                "shape" to `var`("--shape-modern")
            }
        }

        assertEquals(SnyggStylesheet.SCHEMA_V2, stylesheet.schema)
        assertEquals(2, stylesheet.rules.size)

        val defines = stylesheet.rules[SnyggRule.definedVariablesRule()]
        assertNotNull(defines)
        val definesSmallSize = assertIs<SnyggDpSizeValue>(defines.properties["--small-size"])
        assertNotNull(definesSmallSize)
        assertEquals(12.dp, definesSmallSize.dp)
        val definesShapeModern = assertIs<SnyggShapeValue>(defines.properties["--shape-modern"])
        assertNotNull(definesShapeModern)
        assertEquals(RoundedCornerShape(16.dp), definesShapeModern.shape)

        val smartbar = stylesheet.rules[SnyggRule("smartbar")]
        assertNotNull(smartbar)
        val smartbarBackground = assertIs<SnyggSolidColorValue>(smartbar.background)
        assertNotNull(smartbar.background)
        assertEquals(Color(255, 0, 0), smartbarBackground.color)
        val smartbarShape = assertIs<SnyggDefinedVarValue>(smartbar.shape)
        assertNotNull(smartbarShape)
        assertEquals("--shape-modern", smartbarShape.key)
    }

    @Test
    fun `test simple editor noop`() {
        val stylesheet = SnyggStylesheet(
            schema = "/schema/path",
            rules = mapOf(
                SnyggRule("smartbar") to SnyggPropertySet(mapOf(
                    "background" to SnyggSolidColorValue(Color(255, 0, 0)),
                    "shape" to SnyggRectangleShapeValue(),
                )),
            ),
        )
        val stylesheetAfterEdit = stylesheet.edit().build()
        assertEquals(stylesheet.schema, stylesheetAfterEdit.schema)
        assertEquals(stylesheet.rules, stylesheetAfterEdit.rules)
    }
}
