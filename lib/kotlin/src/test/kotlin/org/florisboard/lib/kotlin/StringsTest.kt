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

package org.florisboard.lib.kotlin

import io.kotest.core.spec.style.FunSpec
import io.kotest.datatest.withData
import io.kotest.matchers.shouldBe

class StringsTest : FunSpec({
    context("Test String.curlyFormat (arg mapping)") {
        context("With no template variables") {
            withData(
                Triple("Hello World!", arrayOf(), "Hello World!"),
                Triple("Hello name!", arrayOf("name" to "Alex"), "Hello name!"),
                Triple("Test123", arrayOf(), "Test123"),
                Triple("", arrayOf(), ""),
                Triple(" ", arrayOf(), " "),
            ) { (inputStr, args, formattedStr) ->
                inputStr.curlyFormat(*args) shouldBe formattedStr
            }
        }

        context("With only curly braces in template") {
            withData(
                Triple("{", arrayOf(), "{"),
                Triple("}", arrayOf(), "}"),
                Triple("{}", arrayOf(), "{}"),
                Triple("}{", arrayOf(), "}{"),
                Triple("{{", arrayOf(), "{{"),
                Triple("}}", arrayOf(), "}}"),

                Triple("{", arrayOf("" to "Alex"), "{"),
                Triple("}", arrayOf("" to "Alex"), "}"),
                Triple("{}", arrayOf("" to "Alex"), "{}"),
                Triple("}{", arrayOf("" to "Alex"), "}{"),
                Triple("{{", arrayOf("" to "Alex"), "{{"),
                Triple("}}", arrayOf("" to "Alex"), "}}"),

                Triple("{", arrayOf("name" to "Alex"), "{"),
                Triple("}", arrayOf("name" to "Alex"), "}"),
                Triple("{}", arrayOf("name" to "Alex"), "{}"),
                Triple("}{", arrayOf("name" to "Alex"), "}{"),
                Triple("{{", arrayOf("name" to "Alex"), "{{"),
                Triple("}}", arrayOf("name" to "Alex"), "}}"),
            ) { (inputStr, args, formattedStr) ->
                inputStr.curlyFormat(*args) shouldBe formattedStr
            }
        }

        context("With curly braces and named variables in template") {
            withData(
                Triple("{name", arrayOf(), "{name"),
                Triple("}name", arrayOf(), "}name"),
                Triple("name{", arrayOf(), "name{"),
                Triple("name}", arrayOf(), "name}"),
                Triple("{name}", arrayOf(), "{name}"),
                Triple("}name{", arrayOf(), "}name{"),
                Triple("{name{", arrayOf(), "{name{"),
                Triple("}name}", arrayOf(), "}name}"),

                Triple("{name", arrayOf("name" to "Alex"), "{name"),
                Triple("}name", arrayOf("name" to "Alex"), "}name"),
                Triple("name{", arrayOf("name" to "Alex"), "name{"),
                Triple("name}", arrayOf("name" to "Alex"), "name}"),
                Triple("{name}", arrayOf("name" to "Alex"), "Alex"),
                Triple("}name{", arrayOf("name" to "Alex"), "}name{"),
                Triple("{name{", arrayOf("name" to "Alex"), "{name{"),
                Triple("}name}", arrayOf("name" to "Alex"), "}name}"),

                Triple("{name_with_underscore", arrayOf("name_with_underscore" to "Alex"), "{name_with_underscore"),
                Triple("}name_with_underscore", arrayOf("name_with_underscore" to "Alex"), "}name_with_underscore"),
                Triple("name_with_underscore{", arrayOf("name_with_underscore" to "Alex"), "name_with_underscore{"),
                Triple("name_with_underscore}", arrayOf("name_with_underscore" to "Alex"), "name_with_underscore}"),
                Triple("{name_with_underscore}", arrayOf("name_with_underscore" to "Alex"), "Alex"),
                Triple("}name_with_underscore{", arrayOf("name_with_underscore" to "Alex"), "}name_with_underscore{"),
                Triple("{name_with_underscore{", arrayOf("name_with_underscore" to "Alex"), "{name_with_underscore{"),
                Triple("}name_with_underscore}", arrayOf("name_with_underscore" to "Alex"), "}name_with_underscore}"),
            ) { (inputStr, args, formattedStr) ->
                inputStr.curlyFormat(*args) shouldBe formattedStr
            }
        }

        context("With positional variables in template") {
            withData(
                Triple("Howdy {0}!", arrayOf(), "Howdy {0}!"),
                Triple("Howdy {1}!", arrayOf(), "Howdy {1}!"),
                Triple("Howdy {-1}!", arrayOf(), "Howdy {-1}!"),
                Triple("Howdy {11}!", arrayOf(), "Howdy {11}!"),
                Triple("Howdy {1,1}!", arrayOf(), "Howdy {1,1}!"),
                Triple("Howdy {1.1}!", arrayOf(), "Howdy {1.1}!"),
                Triple("Howdy {00}!", arrayOf(), "Howdy {00}!"),
                Triple("Howdy {01}!", arrayOf(), "Howdy {01}!"),
                Triple("Howdy {0} and {0}!", arrayOf(), "Howdy {0} and {0}!"),
                Triple("Howdy {0} and {1}!", arrayOf(), "Howdy {0} and {1}!"),
                Triple("Howdy {1} and {0}!", arrayOf(), "Howdy {1} and {0}!"),
                Triple("Howdy {1} and {1}!", arrayOf(), "Howdy {1} and {1}!"),

                Triple("Howdy {0}!", arrayOf("" to "Alex", "" to "Emily"), "Howdy Alex!"),
                Triple("Howdy {1}!", arrayOf("" to "Alex", "" to "Emily"), "Howdy Emily!"),
                Triple("Howdy {-1}!", arrayOf("" to "Alex", "" to "Emily"), "Howdy {-1}!"),
                Triple("Howdy {11}!", arrayOf("" to "Alex", "" to "Emily"), "Howdy {11}!"),
                Triple("Howdy {11}!", arrayOf(
                    "" to "Alex", "" to "Emily", "" to "Tom", "" to "Angela", "" to "Bob",
                    "" to "Elon", "" to "Mark", "" to "Samantha", "" to "Alice", "" to "Michael",
                    "" to "Andy", "" to "Tamara",
                ), "Howdy Tamara!"),
                Triple("Howdy {1,1}!", arrayOf("" to "Alex", "" to "Emily"), "Howdy {1,1}!"),
                Triple("Howdy {1.1}!", arrayOf("" to "Alex", "" to "Emily"), "Howdy {1.1}!"),
                Triple("Howdy {00}!", arrayOf("" to "Alex", "" to "Emily"), "Howdy {00}!"),
                Triple("Howdy {01}!", arrayOf("" to "Alex", "" to "Emily"), "Howdy {01}!"),
                Triple("Howdy {0} and {0}!", arrayOf("" to "Alex", "" to "Emily"), "Howdy Alex and Alex!"),
                Triple("Howdy {0} and {1}!", arrayOf("" to "Alex", "" to "Emily"), "Howdy Alex and Emily!"),
                Triple("Howdy {1} and {0}!", arrayOf("" to "Alex", "" to "Emily"), "Howdy Emily and Alex!"),
                Triple("Howdy {1} and {1}!", arrayOf("" to "Alex", "" to "Emily"), "Howdy Emily and Emily!"),
            ) { (inputStr, args, formattedStr) ->
                inputStr.curlyFormat(*args) shouldBe formattedStr
            }
        }
    }

    context("Test String.curlyFormat (arg factory with dictionary)") {
        val dict = listOf(
            "app_name" to "UnitTestApp",
            "test_label" to "Test Label",
        )
        withData(
            Pair("Welcome to {app_name}", "Welcome to UnitTestApp"),
            Pair("Welcome to {app_name} and {file_name}", "Welcome to UnitTestApp and {file_name}"),
            Pair("{ Curly {test_label} }", "{ Curly Test Label }"),
        ) { (inputStr, formattedStr) ->
            inputStr.curlyFormat { key ->
                dict.find { it.first == key }?.second
            } shouldBe formattedStr
        }
    }
})
