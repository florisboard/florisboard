/*
 * Copyright (C) 2026 The FlorisBoard Contributors
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

package dev.patrickgold.florisboard.ime.io

import kotlinx.serialization.KSerializer
import kotlinx.serialization.StringFormat
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

interface Storage {
    fun canAccessUnderlyingMedium(ref: FlorisRef): Boolean

    fun resolveAbsolutePath(ref: FlorisRef): String

    fun resolveCanonicalPath(ref: FlorisRef): String

    fun basename(ref: FlorisRef): String

    fun exists(ref: FlorisRef): Boolean

    fun list(ref: FlorisRef): List<FlorisRef>

    fun readText(ref: FlorisRef): String

    fun writeText(ref: FlorisRef, text: String)

    fun copy(srcRef: FlorisRef, dstRef: FlorisRef)

    fun delete(ref: FlorisRef, mustExist: Boolean = true)
}

fun <T> Storage.readJson(
    ref: FlorisRef,
    serializer: KSerializer<T>,
    config: StringFormat = Json,
): T {
    val jsonStr = readText(ref)
    return config.decodeFromString(serializer, jsonStr)
}

inline fun <reified T> Storage.readJson(
    ref: FlorisRef,
    config: StringFormat = Json,
): T {
    val jsonStr = readText(ref)
    return config.decodeFromString(jsonStr)
}

fun <T> Storage.writeJson(
    ref: FlorisRef,
    value: T,
    serializer: KSerializer<T>,
    config: StringFormat = Json,
) {
    val jsonStr = config.encodeToString(serializer, value)
    writeText(ref, jsonStr)
}

inline fun <reified T> Storage.writeJson(
    ref: FlorisRef,
    value: T,
    config: StringFormat = Json,
) {
    val jsonStr = config.encodeToString(value)
    writeText(ref, jsonStr)
}
