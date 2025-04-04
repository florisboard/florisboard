package org.florisboard.lib.snygg

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
        elementName = "smartbar",
        attributes = SnyggElementRule.Attributes(mapOf(
            "code" to listOf(0, 1, 2, 3, 4, 5),
            "group" to listOf(0, 2, 3, 4),
            "shift" to listOf(0, 2, 4),
            "test" to listOf(0, 1),
        )),
        selector = SnyggSelector.PRESSED,
    )

    @Test
    fun `test toString`() {
        assertEquals("smartbar[code=0..5][group=0,2..4][shift=0,2,4][test=0,1]:pressed", testRule.toString())
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
            SnyggElementRule(elementName = "a-test") to
                SnyggElementRule(elementName = "b-test"),
            SnyggElementRule(elementName = "test") to
                SnyggElementRule(elementName = "test", selector = SnyggSelector.PRESSED),
            SnyggElementRule(elementName = "test") to
                SnyggElementRule(elementName = "test", selector = SnyggSelector.FOCUS),
            SnyggElementRule(elementName = "test") to
                SnyggElementRule(elementName = "test", selector = SnyggSelector.HOVER),
            SnyggElementRule(elementName = "test") to
                SnyggElementRule(elementName = "test", selector = SnyggSelector.DISABLED),
            SnyggElementRule(elementName = "test", selector = SnyggSelector.PRESSED) to
                SnyggElementRule(elementName = "test", selector = SnyggSelector.FOCUS),
            SnyggElementRule(elementName = "test", selector = SnyggSelector.FOCUS) to
                SnyggElementRule(elementName = "test", selector = SnyggSelector.HOVER),
            SnyggElementRule(elementName = "test", selector = SnyggSelector.HOVER) to
                SnyggElementRule(elementName = "test", selector = SnyggSelector.DISABLED),
            SnyggElementRule(elementName = "test") to
                SnyggElementRule(elementName = "test", SnyggElementRule.Attributes.of("code" to listOf(10))),
            SnyggElementRule(elementName = "test", SnyggElementRule.Attributes.of("code" to listOf(10))) to
                SnyggElementRule(elementName = "test", SnyggElementRule.Attributes.of("code" to listOf(11))),
            SnyggElementRule(elementName = "test", SnyggElementRule.Attributes.of("code" to listOf(10))) to
                SnyggElementRule(elementName = "test", SnyggElementRule.Attributes.of("code" to listOf(10, 11))),
            SnyggElementRule(elementName = "test", SnyggElementRule.Attributes.of("code" to listOf(10))) to
                SnyggElementRule(elementName = "test", SnyggElementRule.Attributes.of("group" to listOf(10))),
            SnyggElementRule(elementName = "test", SnyggElementRule.Attributes.of("groups" to listOf(10))) to
                SnyggElementRule(elementName = "test", SnyggElementRule.Attributes.of("groups" to listOf(11))),
            SnyggElementRule(elementName = "test", SnyggElementRule.Attributes.of("shiftStates" to listOf(0))) to
                SnyggElementRule(elementName = "test", SnyggElementRule.Attributes.of("shiftStates" to listOf(1))),
            SnyggElementRule(elementName = "test", SnyggElementRule.Attributes.of("code" to listOf(32), "shiftStates" to listOf(0))) to
                SnyggElementRule(elementName = "test", SnyggElementRule.Attributes.of("code" to listOf(32), "shiftStates" to listOf(1))),
            SnyggElementRule(elementName = "test", SnyggElementRule.Attributes.of("code" to listOf(10))) to
                SnyggElementRule(elementName = "test", SnyggElementRule.Attributes.of("code" to listOf(10), "group" to listOf(10)),),
            SnyggElementRule(elementName = "test", SnyggElementRule.Attributes.of("code" to listOf(10))) to
                SnyggElementRule(elementName = "test", SnyggElementRule.Attributes.of("code" to listOf(10)), SnyggSelector.PRESSED),
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
}
