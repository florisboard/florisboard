/*
 * Copyright (C) 2021 Patrick Goldinger
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

package dev.patrickgold.florisboard.lib.snygg.value

@Suppress("UNCHECKED_CAST")
@JvmInline
value class SnyggIdToValueMap private constructor(private val list: MutableList<Pair<String, Any>>) {
    companion object {
        fun new() = SnyggIdToValueMap(mutableListOf())

        fun new(vararg pairs: Pair<String, Any>) = SnyggIdToValueMap(pairs.toMutableList())

        fun new(list: List<Pair<String, Any>>) = SnyggIdToValueMap(list.toMutableList())
    }

    fun <T : Any> getOrNull(id: String): T? {
        return try {
            list.find { it.first == id }?.second as? T
        } catch (e: Exception) {
            null
        }
    }

    @Throws(ClassCastException::class, NullPointerException::class)
    fun <T : Any> getOrThrow(id: String): T {
        return list.find { it.first == id }!!.second as T
    }

    fun <T : Any> add(pair: Pair<String, T>) {
        list.add(pair)
    }
}
