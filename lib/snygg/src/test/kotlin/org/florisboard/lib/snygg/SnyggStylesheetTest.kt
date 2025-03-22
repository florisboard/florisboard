package org.florisboard.lib.snygg

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

val StylesheetJsonStr = """
{
  "${'$'}schema": "./schemas/stylesheet.schema.json",
  "@defines": {
    "--test": "transparent"
  },
  "smartbar": {
    "border-color": "var(--test)",
    "background": "#00000000",
    "shadow-elevation": "3dp"
  },
  "keyboard": {
    "background": "rgb(255,255,255)",
    "width": "20",
    "height": "30"
  }
}
""".trimIndent()

// TODO: WIP
class SnyggStylesheetTest {

    @Test
    fun test() {
        assertEquals<SnyggStylesheet?>(null, SnyggStylesheet.fromJsonString(StylesheetJsonStr))
    }
}
