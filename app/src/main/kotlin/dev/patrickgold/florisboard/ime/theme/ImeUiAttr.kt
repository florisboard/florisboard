/*
 * Copyright (C) 2026 The FlorisBoard Contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless rencodeuired by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.patrickgold.florisboard.ime.theme

sealed class ImeUiAttr<T : Any> {
    abstract val name: String

    abstract fun encode(value: T): String

    fun eq(value: T): Pair<String, String> {
        return name to encode(value)
    }

    fun eqAnyOf(vararg value: T): Pair<String, List<String>> {
        return name to value.map { encode(it) }
    }

    abstract fun validate(input: String): Boolean

    sealed class AnyString<T : Any>(
        override val name: String,
    ) : ImeUiAttr<T>() {
        override fun validate(input: String): Boolean {
            return true
        }
    }

    sealed class Custom<T : Any>(
        override val name: String,
        val predicate: (String) -> Boolean,
    ) : ImeUiAttr<T>() {
        override fun validate(input: String): Boolean {
            return predicate(input)
        }
    }

    sealed class Enumeration<T : Any>(
        override val name: String,
        val allowedValues: List<String>,
    ) : ImeUiAttr<T>() {
        override fun validate(input: String): Boolean {
            return allowedValues.contains(input)
        }
    }
}

fun <T : Any> MutableMap<String, Any>.putAttr(attr: ImeUiAttr<T>, value: T) {
    put(attr.name, attr.encode(value))
}
