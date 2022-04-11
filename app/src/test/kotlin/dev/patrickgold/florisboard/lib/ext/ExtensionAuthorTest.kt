package dev.patrickgold.florisboard.lib.ext

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe

class ExtensionAuthorTest : FreeSpec({
    val validAuthorPairs = listOf(
        "Jane Doe" to ExtensionMaintainer(name = "Jane Doe"),
        "jane123" to ExtensionMaintainer(name = "jane123"),
        "__jane__" to ExtensionMaintainer(name = "__jane__"),
        "jane.doe" to ExtensionMaintainer(name = "jane.doe"),
        "Jane Doe <jane.doe@gmail.com>" to ExtensionMaintainer(name = "Jane Doe", email = "jane.doe@gmail.com"),
        "Jane Doe (jane-doe.com)" to ExtensionMaintainer(name = "Jane Doe", url = "jane-doe.com"),
        "Jane Doe <jane.doe@gmail.com> (jane-doe.com)" to ExtensionMaintainer(name = "Jane Doe", email = "jane.doe@gmail.com", url = "jane-doe.com"),
    )

    "Test ExtensionAuthor.from()" - {
        "With valid, well-formatted input" - {
            validAuthorPairs.forEach { (authorStr, authorObj) ->
                "`$authorStr`" {
                    ExtensionMaintainer.from(authorStr) shouldBe authorObj
                }
            }
        }

        "With valid, ill-formatted input" - {
            listOf(
                "  Jane Doe " to ExtensionMaintainer(name = "Jane Doe"),
                " jane123" to ExtensionMaintainer(name = "jane123"),
                "  Jane Doe    <jane.doe@gmail.com>     " to ExtensionMaintainer(name = "Jane Doe", email = "jane.doe@gmail.com"),
            ).forEach { (authorStr, authorObj) ->
                "`$authorStr`" {
                    ExtensionMaintainer.from(authorStr) shouldBe authorObj
                }
            }
        }

        "With invalid input" - {
            listOf(
                "",
                " ",
                "<jane.doe@gmail.com>",
                " <jane.doe@gmail.com>",
                "<jane.doe@gmail.com> (jane-doe.com)",
                "Jane Doe <<jane.doe@gmail.com>> ((jane-doe.com))",
                "Jane Doe <jane.doe@gmail.com) (jane-doe.com)",
            ).forEach { authorStr ->
                "`$authorStr` should be null" {
                    ExtensionMaintainer.from(authorStr) shouldBe null
                }
            }
        }
    }

    "Test ExtensionAuthor.toString()" - {
        validAuthorPairs.forEach { (authorStr, authorObj) ->
            "`$authorStr`" {
                authorObj.toString() shouldBe authorStr
            }
        }
    }
})
