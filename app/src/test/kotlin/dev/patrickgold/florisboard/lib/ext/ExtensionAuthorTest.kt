/*
 * Copyright (C) 2025 The FlorisBoard Contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.patrickgold.florisboard.lib.ext

import io.kotest.core.spec.style.FunSpec
import io.kotest.datatest.withData
import io.kotest.matchers.shouldBe

class ExtensionAuthorTest : FunSpec({
    val validAuthorPairs = listOf(
        "Jane Doe" to ExtensionMaintainer(name = "Jane Doe"),
        "jane123" to ExtensionMaintainer(name = "jane123"),
        "__jane__" to ExtensionMaintainer(name = "__jane__"),
        "jane.doe" to ExtensionMaintainer(name = "jane.doe"),
        "Jane Doe <jane.doe@gmail.com>" to ExtensionMaintainer(name = "Jane Doe", email = "jane.doe@gmail.com"),
        "Jane Doe (jane-doe.com)" to ExtensionMaintainer(name = "Jane Doe", url = "jane-doe.com"),
        "Jane Doe <jane.doe@gmail.com> (jane-doe.com)" to ExtensionMaintainer(
            name = "Jane Doe",
            email = "jane.doe@gmail.com",
            url = "jane-doe.com"
        ),
    )

    context("ExtensionAuthor.from()") {
        context("with valid, well-formatted input") {
            withData(validAuthorPairs) { (authorStr, authorObj) ->
                ExtensionMaintainer.from(authorStr) shouldBe authorObj
            }
        }

        context("with valid, ill-formatted input") {
            withData(
                "  Jane Doe " to ExtensionMaintainer(name = "Jane Doe"),
                " jane123" to ExtensionMaintainer(name = "jane123"),
                "  Jane Doe    <jane.doe@gmail.com>     " to ExtensionMaintainer(
                    name = "Jane Doe",
                    email = "jane.doe@gmail.com"
                ),
            ) { (authorStr, authorObj) ->
                ExtensionMaintainer.from(authorStr) shouldBe authorObj
            }
        }

        context("With invalid input") {
            withData(
                nameFn = { "`$it`" },
                "",
                " ",
                "<jane.doe@gmail.com>",
                " <jane.doe@gmail.com>",
                "<jane.doe@gmail.com> (jane-doe.com)",
                "Jane Doe <<jane.doe@gmail.com>> ((jane-doe.com))",
                "Jane Doe <jane.doe@gmail.com) (jane-doe.com)",
            ) { authorStr ->
                ExtensionMaintainer.from(authorStr) shouldBe null
            }
        }
    }

    context("Test ExtensionAuthor.toString()") {
        withData(validAuthorPairs) { (authorStr, authorObj) ->
            authorObj.toString() shouldBe authorStr
        }
    }
})
