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
import okio.IOException
import okio.Path.Companion.toPath
import okio.buffer
import okio.openZip
import kotlin.uuid.Uuid

fun Storage.readText(
    ref: FlorisRef,
): String {
    return source(ref).buffer().use { it.readString(Charsets.UTF_8) }
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

fun Storage.writeText(
    ref: FlorisRef,
    text: String,
) {
    sink(ref).buffer().use { it.writeString(text, Charsets.UTF_8) }
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

private val WORKSPACES_BASE_REF = FlorisRef.cache("workspaces")

fun Storage.createWorkspace(): FlorisRef {
    val id = Uuid.random().toHexDashString()
    val ref = WORKSPACES_BASE_REF.subRef(id)
    mkdirs(ref)
    return ref
}

fun Storage.deleteAllWorkspaces() {
    deleteContents(WORKSPACES_BASE_REF)
}

fun Storage.unzip(srcRef: FlorisRef, dstRef: FlorisRef) {
    val (srcFs, srcPath) = resolve(srcRef)
    val (dstFs, dstPath) = resolve(dstRef)
    val zipFs = srcFs.openZip(srcPath)
    for (entry in zipFs.listRecursively(".".toPath())) {
        val entryRel = entry.toString().removePrefix("/").toPath()
        val meta = zipFs.metadata(entry)
        if (meta.isDirectory) {
            dstFs.createDirectories(dstPath / entryRel)
        } else if (meta.isRegularFile) {
            zipFs.source(entry).use { source ->
                dstFs.sink(dstPath / entryRel).buffer().use { sink ->
                    sink.writeAll(source)
                }
            }
        } else {
            throw IOException("invalid entry: '$entry'")
        }
    }
}
