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

package dev.patrickgold.florisboard.lib.io

import kotlinx.serialization.StringFormat
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Public typealias for [java.io.File]. As a file object can either be a file
 * or a directory, this typealias allows you to be more verbose on what you
 * expect, in this case a file system directory.
 */
typealias FsDir = java.io.File

/**
 * Public typealias for [java.io.File]. As a file object can either be a file
 * or a directory, this typealias allows you to be more verbose on what you
 * expect, in this case a file system file.
 */
typealias FsFile = java.io.File

@Suppress("NOTHING_TO_INLINE")
inline fun FsDir.subDir(relPath: String) = FsDir(this, relPath)

@Suppress("NOTHING_TO_INLINE")
inline fun FsDir.subFile(relPath: String) = FsFile(this, relPath)

fun FsDir.deleteContentsRecursively() {
    this.listFiles()?.forEach { it.deleteRecursively() }
}

inline fun <reified T> FsFile.readJson(config: StringFormat = Json): T {
    val text = this.readText()
    return config.decodeFromString(text)
}

inline fun <reified T> FsFile.writeJson(value: T, config: StringFormat = Json) {
    val text = config.encodeToString(value)
    return this.writeText(text)
}

inline val FsFile/* and FsDir */.parentDir: FsDir? get() = this.parentFile
