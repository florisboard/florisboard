package dev.patrickgold.florisboard.ime.snygg

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe

class SnyggRuleTest : FreeSpec({
    val validTestPairs = listOf(
        "button" to SnyggRule(element = "button"),
        "button-name" to SnyggRule(element = "button-name"),
        "button042" to SnyggRule(element = "button042"),
        "button4-test1" to SnyggRule(element = "button4-test1"),
        "button[code=113]" to SnyggRule(element = "button", codes = listOf(113)),
        "button[code=-113]" to SnyggRule(element = "button", codes = listOf(-113)),
        "button[code=113|212]" to SnyggRule(element = "button", codes = listOf(113,212)),
        "button[code=113][group=1]" to SnyggRule(element = "button", codes = listOf(113), groups = listOf(1)),
        "button:hover" to SnyggRule(element = "button", hoverSelector = true),
        "button:focus" to SnyggRule(element = "button", focusSelector = true),
        "button:pressed" to SnyggRule(element = "button", pressedSelector = true),
        "button:hover:focus" to SnyggRule(element = "button", hoverSelector = true, focusSelector = true),
        "button:hover:pressed" to SnyggRule(element = "button", hoverSelector = true, pressedSelector = true),
        "button:focus:pressed" to SnyggRule(element = "button", focusSelector = true, pressedSelector = true),
        "button:hover:focus:pressed" to SnyggRule(element = "button", hoverSelector = true, focusSelector = true, pressedSelector = true),
        "button[code=113][group=1]:pressed" to SnyggRule(element = "button", codes = listOf(113), groups = listOf(1), pressedSelector = true),
    )

    "Test SnyggRule.from()" - {
        validTestPairs.forEach { (ruleStr, ruleObj) ->
            "`$ruleStr`" {
                SnyggRule.from(ruleStr) shouldBe ruleObj
            }
        }
    }

    "Test SnyggRule.from() with invalid input" - {
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
            "button::hover",
            "button::focus",
            "button::pressed",
        ).forEach { ruleStr ->
            "`$ruleStr` should be null" {
                SnyggRule.from(ruleStr) shouldBe null
            }
        }
    }

    "Test SnyggRule.toString()" - {
        validTestPairs.forEach { (ruleStr, ruleObj) ->
            "`$ruleStr`" {
                ruleObj.toString() shouldBe ruleStr
            }
        }
    }
})
