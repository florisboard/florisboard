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

import kotlinx.io.RawSink
import kotlinx.io.RawSource

/**
 * An interface providing basic operations on an underlying storage medium.
 *
 * This API is designed to closely follow [kotlinx.io.files.FileSystem].
 */
interface Storage {
    fun canAccessUnderlyingMedium(ref: FlorisRef): Boolean

    fun delete(ref: FlorisRef, mustExist: Boolean = true)

    fun deleteContents(ref: FlorisRef) {
        list(ref).forEach { deleteRecursively(it) }
    }

    fun deleteRecursively(ref: FlorisRef) {
        list(ref).forEach { deleteRecursively(it) }
        delete(ref)
    }

    fun exists(ref: FlorisRef): Boolean

    fun list(ref: FlorisRef): List<FlorisRef>

    fun mkdirs(ref: FlorisRef)

    fun source(ref: FlorisRef): RawSource

    fun sink(ref: FlorisRef): RawSink

    companion object
}
