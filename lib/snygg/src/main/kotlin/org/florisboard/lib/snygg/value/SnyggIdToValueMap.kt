/*
 * Copyright (C) 2021-2025 The FlorisBoard Contributors
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

package org.florisboard.lib.snygg.value

typealias SnyggIdToValueMap = MutableMap<String, Any>
fun snyggIdToValueMapOf(vararg pairs: Pair<String, Any>): SnyggIdToValueMap = mutableMapOf(*pairs)

@Suppress("UNCHECKED_CAST")
@Throws(ClassCastException::class, NoSuchElementException::class)
fun <V: Any> SnyggIdToValueMap.getOrThrow(key: String): V = getValue(key) as V

@Suppress("UNCHECKED_CAST")
fun <V: Any> SnyggIdToValueMap.getOrNull(key: String): V? = getOrDefault(key, null) as? V

fun SnyggIdToValueMap.add(vararg pairs: Pair<String, Any>) = putAll(pairs)
