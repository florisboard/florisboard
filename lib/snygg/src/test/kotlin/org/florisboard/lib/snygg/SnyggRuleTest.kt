package org.florisboard.lib.snygg

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertThrows
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SnyggRuleTest {
    @Nested
    inner class Serialization {
        val validRules = listOf(
            "@defines" to SnyggAnnotationRule.Defines,
            "@font `Test`" to SnyggAnnotationRule.Font("Test"),
            "@font `Comic Sans`" to SnyggAnnotationRule.Font("Comic Sans"),
            "elem" to SnyggElementRule(
                elementName = "elem",
            ),
            "elem-with-hyphens" to SnyggElementRule(
                elementName = "elem-with-hyphens",
            ),
            "elem[code=1]" to SnyggElementRule(
                elementName = "elem",
                attributes = SnyggAttributes.of("code" to listOf(1)),
            ),
            "elem[code=1,3..6,`str`]" to SnyggElementRule(
                elementName = "elem",
                attributes = SnyggAttributes.of("code" to listOf(1,3,4,5,6,"str")),
            ),
            "elem[code=-200,10,`str`]" to SnyggElementRule(
                elementName = "elem",
                attributes = SnyggAttributes.of("code" to listOf(-200,10,"str")),
            ),
            "elem[code=`str with spaces`]" to SnyggElementRule(
                elementName = "elem",
                attributes = SnyggAttributes.of("code" to listOf("str with spaces")),
            ),
            "elem[code=1]:pressed" to SnyggElementRule(
                elementName = "elem",
                attributes = SnyggAttributes.of("code" to listOf(1)),
                selector = SnyggSelector.PRESSED,
            ),
            "elem:pressed" to SnyggElementRule(
                elementName = "elem",
                selector = SnyggSelector.PRESSED,
            ),
            "elem:focus" to SnyggElementRule(
                elementName = "elem",
                selector = SnyggSelector.FOCUS,
            ),
            "elem:hover" to SnyggElementRule(
                elementName = "elem",
                selector = SnyggSelector.HOVER,
            ),
            "elem:disabled" to SnyggElementRule(
                elementName = "elem",
                selector = SnyggSelector.DISABLED,
            ),
            "smartbar[code=0..5][group=0,2..4][shift=0,2,4][test=0,1]:pressed" to SnyggElementRule(
                elementName = "smartbar",
                attributes = SnyggAttributes.of(
                    "code" to listOf(0, 1, 2, 3, 4, 5),
                    "group" to listOf(0, 2, 3, 4),
                    "shift" to listOf(0, 2, 4),
                    "test" to listOf(0, 1),
                ),
                selector = SnyggSelector.PRESSED,
            ),
        )

        val validNonOrderedAttributes = listOf(
            // non-ordered to ordered output
            "elem[code=2,1]" to "elem[code=1,2]",
            "elem[code=`str`,1]" to "elem[code=1,`str`]",
            "elem[code=2,`str`,1]" to "elem[code=1,2,`str`]",
            "elem[code=1,2,3]" to "elem[code=1..3]",
            "elem[code=1..1]" to "elem[code=1]",
            "elem[code=1..0]" to "elem",
            "elem[code=6..2]" to "elem",
            "elem[code=`6`]" to "elem[code=6]",
            "elem[b=1][a=1]" to "elem[a=1][b=1]",
            "elem[a=1][a=2]" to "elem[a=1,2]",
            "elem[a=1,5][a=3,5]" to "elem[a=1,3,5]",
            "elem[a=`str`,1][a=2,1]" to "elem[a=1,2,`str`]",
        )

        val invalidRules = listOf(
            "",
            " ",
            "button_name",
            ".button",
            "#button",
            "[code=1]",
            "button#id",
            "button[]",
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
            "smartbar[code=,]",
            "smartbar[code='string']",
            "smartbar[code=\"string\"]",
            "smartbar[&=0..5]",
            "smart.bar",
            ":pressed",
            "@defines:pressed",
            "\$paragraph",
            "@",
            "@defines:pressed",
            "@defines[code=-1826,-1826,-1826,-1826]",
            "@font",
            "@font ",
            "@font ``",
            "@font ''",
            "@font \"\"",
            "@font \"test\"",
            "@font(`Test`)",
        )

        @Test
        fun `serialize valid rules succeeds`() {
            assertAll(validRules.map { (expectedRaw, rule) -> {
                assertEquals(expectedRaw, rule.toString())
            } })
        }

        @Test
        fun `deserialize valid rules succeeds`() {
            assertAll(validRules.map { (raw, expectedRule) -> {
                val actualRule = SnyggRule.fromOrNull(raw)
                assertNotNull(actualRule)
                assertEquals(expectedRule::class, actualRule::class)
                assertEquals(expectedRule, actualRule)
            } })
        }

        @Test
        fun `deserialize non-ordered attributes serializes to ordered`() {
            assertAll(validNonOrderedAttributes.map { (nonOrderedRaw, orderedRaw) -> {
                assertEquals(orderedRaw, SnyggRule.fromOrNull(nonOrderedRaw)?.toString())
            } })
        }

        @Test
        fun `deserialize invalid rules fails`() {
            assertAll(invalidRules.map { raw -> {
                assertNull(SnyggRule.fromOrNull(raw))
            } })
        }
    }

    @Nested
    inner class Semantics {
        @Test
        fun `rule ordering`() {
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
                SnyggElementRule(elementName = "test", SnyggAttributes.of("code" to listOf(10))) to
                    SnyggElementRule(elementName = "test", selector = SnyggSelector.PRESSED),
                SnyggElementRule(elementName = "test", SnyggAttributes.of("code" to listOf(10))) to
                    SnyggElementRule(elementName = "test", selector = SnyggSelector.FOCUS),
                SnyggElementRule(elementName = "test", SnyggAttributes.of("code" to listOf(10))) to
                    SnyggElementRule(elementName = "test", selector = SnyggSelector.HOVER),
                SnyggElementRule(elementName = "test", SnyggAttributes.of("code" to listOf(10))) to
                    SnyggElementRule(elementName = "test", selector = SnyggSelector.DISABLED),
                SnyggElementRule(elementName = "test") to
                    SnyggElementRule(elementName = "test", SnyggAttributes.of("code" to listOf(10))),
                SnyggElementRule(elementName = "test", SnyggAttributes.of("code" to listOf(10))) to
                    SnyggElementRule(elementName = "test", SnyggAttributes.of("code" to listOf(11))),
                SnyggElementRule(elementName = "test", SnyggAttributes.of("code" to listOf(10))) to
                    SnyggElementRule(elementName = "test", SnyggAttributes.of("code" to listOf(10, 11))),
                SnyggElementRule(elementName = "test", SnyggAttributes.of("code" to listOf(10))) to
                    SnyggElementRule(elementName = "test", SnyggAttributes.of("group" to listOf(10))),
                SnyggElementRule(elementName = "test", SnyggAttributes.of("groups" to listOf(10))) to
                    SnyggElementRule(elementName = "test", SnyggAttributes.of("groups" to listOf(11))),
                SnyggElementRule(elementName = "test", SnyggAttributes.of("shiftStates" to listOf(0))) to
                    SnyggElementRule(elementName = "test", SnyggAttributes.of("shiftStates" to listOf(1))),
                SnyggElementRule(elementName = "test", SnyggAttributes.of("code" to listOf(32), "shiftStates" to listOf(0))) to
                    SnyggElementRule(
                        elementName = "test",
                        SnyggAttributes.of("code" to listOf(32), "shiftStates" to listOf(1))
                    ),
                SnyggElementRule(elementName = "test", SnyggAttributes.of("code" to listOf(10))) to
                    SnyggElementRule(elementName = "test", SnyggAttributes.of("code" to listOf(10), "group" to listOf(10)),),
                SnyggElementRule(elementName = "test", SnyggAttributes.of("code" to listOf(10))) to
                    SnyggElementRule(elementName = "test", SnyggAttributes.of("code" to listOf(10)), SnyggSelector.PRESSED),
            )
            assertAll(ruleList.map { (lowerRule, upperRule) ->
                {
                    assertTrue("$lowerRule should be less than $upperRule") { lowerRule < upperRule }
                    assertFalse("$lowerRule should not be greater than $upperRule") { lowerRule > upperRule }
                    assertTrue("$upperRule should be greater than $lowerRule") { upperRule > lowerRule }
                    assertFalse("$upperRule should not be less than $lowerRule") { upperRule < lowerRule }
                }
            })
        }

        @Test
        fun `rule equality`() {
            val ruleList = listOf(
                SnyggElementRule("smartbar") to
                    SnyggElementRule("smartbar"),
                SnyggElementRule("smartbar") to
                    SnyggElementRule("smartbar", SnyggAttributes(), SnyggSelector.NONE)
            )
            assertAll(ruleList.map { (leftRule, rightRule) -> {
                assertTrue { leftRule == rightRule }
                assertTrue { leftRule.compareTo(rightRule) == 0 }
            } })
        }

        @Test
        fun `element rule constructor with invalid name fails`() {
            assertThrows<IllegalArgumentException> {
                SnyggElementRule("not so valid !!!!!")
            }
        }
    }

    @Nested
    inner class AttributesCopyHelpers {
        @Test
        fun `copy including`() = with(SnyggAttributes.Companion) {
            val expectedToActual = listOf(
                of() to of().including(),
                of("test" to listOf(1)) to of().including("test" to 1),
                of("test" to listOf(1)) to of("test" to listOf(1)).including("test" to 1),
                of("test" to listOf(1), "code" to listOf(2)) to of("test" to listOf(1)).including("code" to 2),
                of("multiple" to listOf(1,2,3)) to of("multiple" to listOf(1,3)).including("multiple" to 2),
                of("multiple" to listOf(1,2,3)) to of().including("multiple" to 2, "multiple" to 3, "multiple" to 1),
                of("a" to listOf(-1), "b" to listOf(-44)) to of("b" to listOf(-44)).including("a" to -1),
                of("a" to listOf(1, "test"), "b" to listOf(4)) to of("b" to listOf(4)).including("a" to 1, "a" to "test"),
            )
            assertAll(expectedToActual.map { (expected, actual) -> {
                assertEquals(expected, actual)
            }})
        }

        @Test
        fun `copy excluding`() = with(SnyggAttributes.Companion) {
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
        fun `copy toggling`() = with(SnyggAttributes.Companion) {
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
