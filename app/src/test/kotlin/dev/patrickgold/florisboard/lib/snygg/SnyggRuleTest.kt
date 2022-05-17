package dev.patrickgold.florisboard.lib.snygg

import io.kotest.core.spec.style.FunSpec
import io.kotest.data.forAll
import io.kotest.data.headers
import io.kotest.data.row
import io.kotest.data.table
import io.kotest.inspectors.forAll
import io.kotest.matchers.comparables.shouldBeGreaterThan
import io.kotest.matchers.comparables.shouldBeLessThan
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import nl.jqno.equalsverifier.EqualsVerifier

class SnyggRuleTest : FunSpec({
    val validTestPairs = listOf(
        "@annotation" to SnyggRule(isAnnotation = true, element = "annotation"),
        "button" to SnyggRule(element = "button"),
        "button-name" to SnyggRule(element = "button-name"),
        "button042" to SnyggRule(element = "button042"),
        "button4-test1" to SnyggRule(element = "button4-test1"),
        "button[code=113]" to SnyggRule(element = "button", codes = listOf(113)),
        "button[code=-113]" to SnyggRule(element = "button", codes = listOf(-113)),
        "button[code=113|212]" to SnyggRule(element = "button", codes = listOf(113,212)),
        "button[code=113][group=1]" to SnyggRule(element = "button", codes = listOf(113), groups = listOf(1)),
        "button:pressed" to SnyggRule(element = "button", pressedSelector = true),
        "button:focus" to SnyggRule(element = "button", focusSelector = true),
        "button:disabled" to SnyggRule(element = "button", disabledSelector = true),
        "button:focus:disabled" to SnyggRule(element = "button", focusSelector = true, disabledSelector = true),
        "button:pressed:disabled" to SnyggRule(element = "button", pressedSelector = true, disabledSelector = true),
        "button:pressed:focus" to SnyggRule(element = "button", pressedSelector = true, focusSelector = true),
        "button:pressed:focus:disabled" to SnyggRule(element = "button", pressedSelector = true, focusSelector = true, disabledSelector = true),
        "button[code=113][group=1]:pressed" to SnyggRule(element = "button", codes = listOf(113), groups = listOf(1), pressedSelector = true),
    )

    context("from()") {
        test("Valid input") {
            validTestPairs.forAll { (ruleStr, ruleObj) ->
                SnyggRule.from(ruleStr) shouldBe ruleObj
            }
        }
        test("Invalid input") {
            listOf(
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
            ).forAll { ruleStr ->
                SnyggRule.from(ruleStr).shouldBeNull()
            }
        }
    }

    test("toString()") {
        validTestPairs.forAll { (ruleStr, ruleObj) ->
            ruleObj.toString() shouldBe ruleStr
        }
    }

    context("compareTo()") {
        test("Equal rules") {
            validTestPairs.forAll { (_, ruleObj) ->
                val rule1 = ruleObj.copy()
                val rule2 = ruleObj.copy()
                rule1.compareTo(rule2) shouldBe 0 // equal
            }
        }

        test("Unequal rules") {
            forAll(
                row(
                    SnyggRule(element = "a-test"),
                    SnyggRule(element = "b-test"),
                ),
                row(
                    SnyggRule(element = "test", codes = listOf()),
                    SnyggRule(element = "test", codes = listOf(10)),
                ),
                row(
                    SnyggRule(element = "test", codes = listOf(10)),
                    SnyggRule(element = "test", codes = listOf(11)),
                ),
                row(
                    SnyggRule(element = "test", codes = listOf(10)),
                    SnyggRule(element = "test", codes = listOf(10,11)),
                ),
                row(
                    SnyggRule(element = "test", groups = listOf(10)),
                    SnyggRule(element = "test", groups = listOf(11)),
                ),
                row(
                    SnyggRule(element = "test", shiftStates = listOf(0)),
                    SnyggRule(element = "test", shiftStates = listOf(1)),
                ),
                row(
                    SnyggRule(element = "test", codes = listOf(32), shiftStates = listOf(0)),
                    SnyggRule(element = "test", codes = listOf(32), shiftStates = listOf(1)),
                ),
                row(
                    SnyggRule(element = "test", pressedSelector = false),
                    SnyggRule(element = "test", pressedSelector = true),
                ),
                row(
                    SnyggRule(element = "test", focusSelector = false),
                    SnyggRule(element = "test", focusSelector = true),
                ),
                row(
                    SnyggRule(element = "test", disabledSelector = false),
                    SnyggRule(element = "test", disabledSelector = true),
                ),
                row(
                    SnyggRule(element = "test", pressedSelector = true),
                    SnyggRule(element = "test", focusSelector = true),
                ),
                row(
                    SnyggRule(element = "test", focusSelector = true),
                    SnyggRule(element = "test", disabledSelector = true),
                ),
            ) { lower, higher ->
                lower.compareTo(higher) shouldBeLessThan 0
                higher.compareTo(lower) shouldBeGreaterThan 0
            }
        }

        test("Special element ordering") {
            forAll(
                row(
                    SnyggRule(element = "keyboard"),
                    SnyggRule(element = "a"),
                ),
                row(
                    SnyggRule(element = "key"),
                    SnyggRule(element = "a"),
                ),
                row(
                    SnyggRule(element = "key-hint"),
                    SnyggRule(element = "a"),
                ),
                row(
                    SnyggRule(element = "key-popup"),
                    SnyggRule(element = "a"),
                ),
                row(
                    SnyggRule(element = "keyboard"),
                    SnyggRule(element = "key"),
                ),
                row(
                    SnyggRule(element = "key"),
                    SnyggRule(element = "key-hint"),
                ),
                row(
                    SnyggRule(element = "key-hint"),
                    SnyggRule(element = "key-popup"),
                ),
            ) { lower, higher ->
                lower.compareTo(higher) shouldBeLessThan 0
                higher.compareTo(lower) shouldBeGreaterThan 0
            }
        }
    }

    context("equals() / hashCode()") {
        test("Generic") {
            EqualsVerifier.forClass(SnyggRule::class.java).verify()
        }

        val edgeCases = table(
            headers("rule1", "rule2", "isEqual"),
            row(
                SnyggRule(element = "test", codes = listOf(10)),
                SnyggRule(element = "test", codes = listOf(10)),
                true,
            ),
            row(
                SnyggRule(element = "test", codes = listOf(10, 11)),
                SnyggRule(element = "test", codes = listOf(10, 11)),
                true,
            ),
            row(
                SnyggRule(element = "test", codes = listOf(10, 11)),
                SnyggRule(element = "test", codes = listOf(11, 10)),
                true,
            ),
            row(
                SnyggRule(element = "test", codes = listOf(10, 11, 12)),
                SnyggRule(element = "test", codes = listOf(-10, -11, -12)),
                false,
            ),
            row(
                SnyggRule(element = "test", shiftStates = listOf(0, 1, 2)),
                SnyggRule(element = "test", shiftStates = listOf(0, 1, 2)),
                true,
            ),
            row(
                SnyggRule(element = "test", shiftStates = listOf()),
                SnyggRule(element = "test", shiftStates = listOf(0)),
                false,
            ),

            row(
                SnyggRule(element = "test", shiftStates = listOf(0, 1, 2)),
                SnyggRule(element = "test", shiftStates = listOf(0, 1, 2)),
                true,
            ),
            row(
                SnyggRule(element = "test", shiftStates = listOf()),
                SnyggRule(element = "test", shiftStates = listOf(2)),
                false,
            ),
            row(
                SnyggRule(element = "test", shiftStates = listOf(1)),
                SnyggRule(element = "test", shiftStates = listOf(2)),
                false,
            ),
            row(
                SnyggRule(element = "system-nav-bar", shiftStates = listOf(2)),
                SnyggRule(element = "system-nav-bar", shiftStates = listOf(1, 2)),
                false,
            ),
            row(
                SnyggRule(element = "key", groups = listOf(10)),
                SnyggRule(element = "key", groups = listOf(10, 113, 114)),
                false,
            ),

            row(
                SnyggRule(element = "test", codes = listOf(10)),
                SnyggRule(element = "test", shiftStates = listOf(2)),
                false,
            ),
        )

        test("Edge cases in equals()") {
            edgeCases.forAll { rule1, rule2, isEqual ->
                (rule1 == rule2) shouldBe isEqual
            }
        }
        test("Edge cases in hashCode()") {
            edgeCases.forAll { rule1, rule2, isEqual ->
                (rule1.hashCode() == rule2.hashCode()) shouldBe isEqual
            }
        }
    }
})
