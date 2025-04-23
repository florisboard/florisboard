package org.florisboard.lib.snygg

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertThrows
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SnyggRuleTest {
    private val testRule: SnyggRule = SnyggElementRule(
        name = "smartbar",
        attributes = SnyggElementRule.Attributes(mapOf(
            "code" to listOf(0, 1, 2, 3, 4, 5),
            "group" to listOf(0, 2, 3, 4),
            "shift" to listOf(0, 2, 4),
            "test" to listOf(0, 1),
        )),
        selector = SnyggSelector.PRESSED,
    )

    private val testFontRule: SnyggRule = SnyggAnnotationRule.Font(
        fontName = "My Comic Sans"
    )

    @Test
    fun `test toString`() {
        assertEquals("smartbar[code=0..5][group=0,2..4][shift=0,2,4][test=0,1]:pressed", testRule.toString())
        assertEquals("@defines", SnyggAnnotationRule.Defines.toString())
        assertEquals("@font `My Comic Sans`", testFontRule.toString())
    }

    @Test
    fun `test fromOrNull`() {
        assertEquals(testRule, SnyggRule.fromOrNull("smartbar[code=0..5][group=0,2..4][shift=0,2,4][test=0,1]:pressed"))
    }

    @Test
    fun `test fromOrNull elementName = smartbar`() {
        assertEquals(SnyggElementRule("smartbar"), SnyggRule.fromOrNull("smartbar"))
    }

    @Test
    fun `test fromOrNull attributes duplicate entries`() {
        assertEquals(SnyggElementRule("smartbar", SnyggElementRule.Attributes.of("code" to listOf(-1826))), SnyggRule.fromOrNull("smartbar[code=-1826,-1826,-1826,-1826]"))
    }

    @Test
    fun `test fromOrNull annotation`() {
        assertEquals(SnyggAnnotationRule.Defines, SnyggRule.fromOrNull("@defines"))
    }

    @Test
    fun `test rule ordering`() {
        val ruleList = listOf(
            SnyggAnnotationRule.Defines to
                SnyggElementRule("test"),
            SnyggAnnotationRule.Font(fontName = "Comic Sans") to
                SnyggElementRule("test"),
            SnyggAnnotationRule.Defines to
                SnyggAnnotationRule.Font(fontName = "Comic Sans"),
            SnyggAnnotationRule.Font(fontName = "Arial") to
                SnyggAnnotationRule.Font(fontName = "Comic Sans"),
            SnyggElementRule(name = "a-test") to
                SnyggElementRule(name = "b-test"),
            SnyggElementRule(name = "test") to
                SnyggElementRule(name = "test", selector = SnyggSelector.PRESSED),
            SnyggElementRule(name = "test") to
                SnyggElementRule(name = "test", selector = SnyggSelector.FOCUS),
            SnyggElementRule(name = "test") to
                SnyggElementRule(name = "test", selector = SnyggSelector.HOVER),
            SnyggElementRule(name = "test") to
                SnyggElementRule(name = "test", selector = SnyggSelector.DISABLED),
            SnyggElementRule(name = "test", selector = SnyggSelector.PRESSED) to
                SnyggElementRule(name = "test", selector = SnyggSelector.FOCUS),
            SnyggElementRule(name = "test", selector = SnyggSelector.FOCUS) to
                SnyggElementRule(name = "test", selector = SnyggSelector.HOVER),
            SnyggElementRule(name = "test", selector = SnyggSelector.HOVER) to
                SnyggElementRule(name = "test", selector = SnyggSelector.DISABLED),
            SnyggElementRule(name = "test") to
                SnyggElementRule(name = "test", SnyggElementRule.Attributes.of("code" to listOf(10))),
            SnyggElementRule(name = "test", SnyggElementRule.Attributes.of("code" to listOf(10))) to
                SnyggElementRule(name = "test", SnyggElementRule.Attributes.of("code" to listOf(11))),
            SnyggElementRule(name = "test", SnyggElementRule.Attributes.of("code" to listOf(10))) to
                SnyggElementRule(name = "test", SnyggElementRule.Attributes.of("code" to listOf(10, 11))),
            SnyggElementRule(name = "test", SnyggElementRule.Attributes.of("code" to listOf(10))) to
                SnyggElementRule(name = "test", SnyggElementRule.Attributes.of("group" to listOf(10))),
            SnyggElementRule(name = "test", SnyggElementRule.Attributes.of("groups" to listOf(10))) to
                SnyggElementRule(name = "test", SnyggElementRule.Attributes.of("groups" to listOf(11))),
            SnyggElementRule(name = "test", SnyggElementRule.Attributes.of("shiftStates" to listOf(0))) to
                SnyggElementRule(name = "test", SnyggElementRule.Attributes.of("shiftStates" to listOf(1))),
            SnyggElementRule(name = "test", SnyggElementRule.Attributes.of("code" to listOf(32), "shiftStates" to listOf(0))) to
                SnyggElementRule(name = "test", SnyggElementRule.Attributes.of("code" to listOf(32), "shiftStates" to listOf(1))),
            SnyggElementRule(name = "test", SnyggElementRule.Attributes.of("code" to listOf(10))) to
                SnyggElementRule(name = "test", SnyggElementRule.Attributes.of("code" to listOf(10), "group" to listOf(10)),),
            SnyggElementRule(name = "test", SnyggElementRule.Attributes.of("code" to listOf(10))) to
                SnyggElementRule(name = "test", SnyggElementRule.Attributes.of("code" to listOf(10)), SnyggSelector.PRESSED),
        )
        assertAll(ruleList.map { (lowerRule, upperRule) -> {
            assertTrue("$lowerRule should be less than $upperRule") { lowerRule < upperRule }
            assertFalse("$lowerRule should not be greater than $upperRule") { lowerRule > upperRule }
            assertTrue("$upperRule should be greater than $lowerRule") { upperRule > lowerRule }
            assertFalse("$upperRule should not be less than $lowerRule") { upperRule < lowerRule }
        } })
    }

    @Test
    fun `test rule equality`() {
        val ruleList = listOf(
            SnyggElementRule("smartbar") to
                SnyggElementRule("smartbar"),
            SnyggElementRule("smartbar") to
                SnyggElementRule("smartbar", SnyggElementRule.Attributes(), SnyggSelector.NONE)
        )
        assertAll(ruleList.map { (leftRule, rightRule) -> {
            assertTrue { leftRule == rightRule }
            assertTrue { leftRule.compareTo(rightRule) == 0 }
        } })
    }

    @Test
    fun `test direct invalid input`(){
        assertFails { SnyggElementRule("@") }
        assertFails { SnyggElementRule("button_name") }
    }

    @Test
    fun `test fromOrNull with invalid input`() {
        val invalidInput = listOf(
            "",
            " ",
            "button_name",
            ".button",
            "#button",
            "[code=1]",
            "button#id",
            "button[code=]",
            "button[group]",
            "button[mode=normal]",
            "button::pressed",
            "button::focus",
            "button::disabled",
            "smartbar:this\$is%illegal",
            "smartbar::",
            "smartbar:",
            "smartbar[code=]",
            "smartbar[&=0..5]",
            "smart.bar",
            "@defines:pressed",
            "\$paragraph",
            "@",
            "@defines:pressed",
            "@defines[code=-1826,-1826,-1826,-1826]",
        )
        invalidInput.forEach { input ->
            assertNull(SnyggRule.fromOrNull(input))
        }
    }

    @Test
    fun `test constructor with invalid element name`() {
        assertThrows<IllegalArgumentException> {
            SnyggElementRule("not so valid !!!!!")
        }
    }

    @Nested
    inner class AttributesCopyHelpers {
        @Test
        fun `Attributes including`() = with(SnyggElementRule.Attributes.Companion) {
            val expectedToActual = listOf(
                of() to of().including(),
                of("test" to listOf(1)) to of().including("test" to 1),
                of("test" to listOf(1)) to of("test" to listOf(1)).including("test" to 1),
                of("test" to listOf(1), "code" to listOf(2)) to of("test" to listOf(1)).including("code" to 2),
                of("multiple" to listOf(1,2,3)) to of("multiple" to listOf(1,3)).including("multiple" to 2),
                of("multiple" to listOf(1,2,3)) to of().including("multiple" to 2, "multiple" to 3, "multiple" to 1),
                of("a" to listOf(-1), "b" to listOf(-44)) to of("b" to listOf(-44)).including("a" to -1),
            )
            assertAll(expectedToActual.map { (expected, actual) -> {
                assertEquals(expected, actual)
            }})
        }

        @Test
        fun `Attributes excluding`() = with(SnyggElementRule.Attributes.Companion) {
            val expectedToActual = listOf(
                of() to of().excluding(),
                of("a" to listOf(1)) to of("a" to listOf(1)).excluding("b" to 1),
                of() to of("test" to listOf(1)).excluding("test" to 1),
                of("test" to listOf(1)) to of("test" to listOf(1,44)).excluding("test" to 44),
                of("test" to listOf(1)) to of("test" to listOf(1), "code" to listOf(2)).excluding("code" to 2),
            )
            assertAll(expectedToActual.map { (expected, actual) -> {
                assertEquals(expected, actual)
            }})
        }

        @Test
        fun `Attributes toggling`() = with(SnyggElementRule.Attributes.Companion) {
            val expectedToActual = listOf(
                of() to of().toggling(),
                of() to of("test" to listOf(1)).toggling("test" to 1),
                of("test" to listOf(1)) to of().toggling("test" to 1),
                of("test" to listOf(1)) to of("test" to listOf(1), "code" to listOf(2)).toggling("code" to 2),
            )
            assertAll(expectedToActual.map { (expected, actual) -> {
                assertEquals(expected, actual)
            }})
        }
    }
}
