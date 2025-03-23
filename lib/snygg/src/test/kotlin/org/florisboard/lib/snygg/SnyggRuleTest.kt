package org.florisboard.lib.snygg

import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertThrows
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SnyggRuleTest {
    private val testRule: SnyggRule = SnyggRule(
        elementName = "smartbar",
        attributes = SnyggRule.Attributes(mapOf(
            "code" to listOf(0, 1, 2, 3, 4, 5),
            "group" to listOf(0, 2, 3, 4),
            "shift" to listOf(0, 2, 4),
            "test" to listOf(0, 1),
        )),
        selectors = SnyggRule.Selectors(pressed = true, focus = true, disabled = true),
    )

    @Test
    fun `test toString`() {
        assertEquals("smartbar[code=0..5][group=0,2..4][shift=0,2,4][test=0,1]:pressed:focus:disabled", testRule.toString())
    }

    @Test
    fun `test fromOrNull`() {
        assertEquals(testRule, SnyggRule.fromOrNull("smartbar[code=0..5][group=0,2..4][shift=0,2,4][test=0,1]:pressed:focus:disabled"))
    }

    @Test
    fun `test fromOrNull elementName = smartbar`() {
        assertEquals(SnyggRule("smartbar"), SnyggRule.fromOrNull("smartbar"))
    }

    @Test
    fun `test fromOrNull attributes duplicate entries`() {
        assertEquals(SnyggRule("smartbar", SnyggRule.Attributes.of("code" to listOf(-1826))), SnyggRule.fromOrNull("smartbar[code=-1826,-1826,-1826,-1826]"))
    }

    @Test
    fun `test fromOrNull annotation`() {
        assertEquals(SnyggRule("@defines"), SnyggRule.fromOrNull("@defines"))
    }

    @Test
    fun `test rule ordering`() {
        val ruleList = listOf(
            SnyggRule("@defines") to
                SnyggRule("test"),
            SnyggRule(elementName = "a-test") to
                SnyggRule(elementName = "b-test"),
            SnyggRule(elementName = "test", selectors = SnyggRule.Selectors()) to
                SnyggRule(elementName = "test", selectors = SnyggRule.Selectors(pressed = true)),
            SnyggRule(elementName = "test", selectors = SnyggRule.Selectors()) to
                SnyggRule(elementName = "test", selectors = SnyggRule.Selectors(focus = true)),
            SnyggRule(elementName = "test", selectors = SnyggRule.Selectors()) to
                SnyggRule(elementName = "test", selectors = SnyggRule.Selectors(disabled = true)),
            SnyggRule(elementName = "test", selectors = SnyggRule.Selectors(pressed = true)) to
                SnyggRule(elementName = "test", selectors = SnyggRule.Selectors(focus = true)),
            SnyggRule(elementName = "test", selectors = SnyggRule.Selectors(focus = true)) to
                SnyggRule(elementName = "test", selectors = SnyggRule.Selectors(disabled = true)),
            SnyggRule(elementName = "test", selectors = SnyggRule.Selectors(pressed = true)) to
                SnyggRule(elementName = "test", selectors = SnyggRule.Selectors(focus = true, disabled = true)),
            SnyggRule(elementName = "test", selectors = SnyggRule.Selectors(pressed = true, focus = true)) to
                SnyggRule(elementName = "test", selectors = SnyggRule.Selectors(disabled = true)),
            SnyggRule(elementName = "test") to
                SnyggRule(elementName = "test", SnyggRule.Attributes.of("code" to listOf(10))),
            SnyggRule(elementName = "test", SnyggRule.Attributes.of("code" to listOf(10))) to
                SnyggRule(elementName = "test", SnyggRule.Attributes.of("code" to listOf(11))),
            SnyggRule(elementName = "test", SnyggRule.Attributes.of("code" to listOf(10))) to
                SnyggRule(elementName = "test", SnyggRule.Attributes.of("code" to listOf(10, 11))),
            SnyggRule(elementName = "test", SnyggRule.Attributes.of("code" to listOf(10))) to
                SnyggRule(elementName = "test", SnyggRule.Attributes.of("group" to listOf(10))),
            SnyggRule(elementName = "test", SnyggRule.Attributes.of("groups" to listOf(10))) to
                SnyggRule(elementName = "test", SnyggRule.Attributes.of("groups" to listOf(11))),
            SnyggRule(elementName = "test", SnyggRule.Attributes.of("shiftStates" to listOf(0))) to
                SnyggRule(elementName = "test", SnyggRule.Attributes.of("shiftStates" to listOf(1))),
            SnyggRule(elementName = "test", SnyggRule.Attributes.of("code" to listOf(32), "shiftStates" to listOf(0))) to
                SnyggRule(elementName = "test", SnyggRule.Attributes.of("code" to listOf(32), "shiftStates" to listOf(1))),
            SnyggRule(elementName = "test", SnyggRule.Attributes.of("code" to listOf(10))) to
                SnyggRule(elementName = "test", SnyggRule.Attributes.of("code" to listOf(10), "group" to listOf(10)),),
            SnyggRule(elementName = "test", SnyggRule.Attributes.of("code" to listOf(10))) to
                SnyggRule(elementName = "test", SnyggRule.Attributes.of("code" to listOf(10)), SnyggRule.Selectors(pressed = true)),
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
            SnyggRule("smartbar") to
                SnyggRule("smartbar"),
            SnyggRule("smartbar") to
                SnyggRule("smartbar", SnyggRule.Attributes(), SnyggRule.Selectors())
        )
        assertAll(ruleList.map { (leftRule, rightRule) -> {
            assertTrue { leftRule == rightRule }
            assertTrue { leftRule.compareTo(rightRule) == 0 }
        } })
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
            SnyggRule("not so valid !!!!!")
        }
    }
}
