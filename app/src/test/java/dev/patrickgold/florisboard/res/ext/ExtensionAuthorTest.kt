package dev.patrickgold.florisboard.res.ext

import android.app.Application
import dev.patrickgold.florisboard.res.FlorisRef
import io.kotest.core.spec.style.FreeSpec
import io.kotest.extensions.robolectric.RobolectricTest
import io.kotest.matchers.shouldBe

@RobolectricTest(application = Application::class)
class ExtensionAuthorTest : FreeSpec({
    val validAuthorPairs = listOf(
        "Jane Doe" to ExtensionAuthor(name = "Jane Doe"),
        "jane123" to ExtensionAuthor(name = "jane123"),
        "__jane__" to ExtensionAuthor(name = "__jane__"),
        "jane.doe" to ExtensionAuthor(name = "jane.doe"),
        "Jane Doe <jane.doe@gmail.com>" to ExtensionAuthor(name = "Jane Doe", email = "jane.doe@gmail.com"),
        "Jane Doe (jane-doe.com)" to ExtensionAuthor(name = "Jane Doe", url = FlorisRef.from("jane-doe.com")),
        "Jane Doe <jane.doe@gmail.com> (jane-doe.com)" to ExtensionAuthor(name = "Jane Doe", email = "jane.doe@gmail.com", url = FlorisRef.from("jane-doe.com")),
    )

    "Test ExtensionAuthor.from()" - {
        "With valid, well-formatted input" - {
            validAuthorPairs.forEach { (authorStr, authorObj) ->
                "`$authorStr`" {
                    ExtensionAuthor.from(authorStr) shouldBe authorObj
                }
            }
        }

        "With valid, ill-formatted input" - {
            listOf(
                "  Jane Doe " to ExtensionAuthor(name = "Jane Doe"),
                " jane123" to ExtensionAuthor(name = "jane123"),
                "  Jane Doe    <jane.doe@gmail.com>     " to ExtensionAuthor(name = "Jane Doe", email = "jane.doe@gmail.com"),
            ).forEach { (authorStr, authorObj) ->
                "`$authorStr`" {
                    ExtensionAuthor.from(authorStr) shouldBe authorObj
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
                    ExtensionAuthor.from(authorStr) shouldBe null
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
