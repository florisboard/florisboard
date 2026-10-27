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

abstract class StorageMultiplexer : Storage {
    abstract fun selectStorageBy(ref: FlorisRef): Storage

    override fun canAccessUnderlyingMedium(ref: FlorisRef): Boolean {
        return selectStorageBy(ref).canAccessUnderlyingMedium(ref)
    }

    override fun delete(ref: FlorisRef, mustExist: Boolean) {
        return selectStorageBy(ref).delete(ref, mustExist)
    }

    override fun deleteContents(ref: FlorisRef) {
        return selectStorageBy(ref).deleteContents(ref)
    }

    override fun deleteRecursively(ref: FlorisRef) {
        return selectStorageBy(ref).deleteRecursively(ref)
    }

    override fun exists(ref: FlorisRef): Boolean {
        return selectStorageBy(ref).exists(ref)
    }

    override fun list(ref: FlorisRef): List<FlorisRef> {
        return selectStorageBy(ref).list(ref)
    }

    override fun mkdirs(ref: FlorisRef) {
        return selectStorageBy(ref).mkdirs(ref)
    }

    override fun source(ref: FlorisRef): RawSource {
        return selectStorageBy(ref).source(ref)
    }

    override fun sink(ref: FlorisRef): RawSink {
        return selectStorageBy(ref).sink(ref)
    }
}
