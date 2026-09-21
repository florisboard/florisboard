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

import android.content.Context
import kotlinx.io.RawSink
import kotlinx.io.RawSource
import kotlinx.io.asSource
import kotlinx.io.files.Path

class AndroidStorage(
    val id: Int,
    val context: Context,
    val isUserUnlocked: Boolean,
) : StorageMultiplexer() {
    private val assetsStorage = object : Storage {
        override fun canAccessUnderlyingMedium(ref: FlorisRef): Boolean {
            return ref.isAssets
        }

        override fun delete(ref: FlorisRef, mustExist: Boolean) {
            throw UnsupportedOperationException("Cannot delete within assets ($ref)")
        }

        override fun deleteContents(ref: FlorisRef) {
            throw UnsupportedOperationException("Cannot delete within assets ($ref)")
        }

        override fun deleteRecursively(ref: FlorisRef) {
            throw UnsupportedOperationException("Cannot delete within assets ($ref)")
        }

        override fun exists(ref: FlorisRef): Boolean {
            TODO("Not yet implemented")
        }

        override fun list(ref: FlorisRef): List<FlorisRef> {
            return context.assets.list(ref.relativePath)?.map { name ->
                ref.subRef(name)
            } ?: emptyList()
        }

        override fun mkdirs(ref: FlorisRef) {
            throw UnsupportedOperationException("Cannot mkdirs within assets ($ref)")
        }

        override fun nameOf(ref: FlorisRef): String {
            val slashIndex = ref.relativePath.lastIndexOf('/')
            return ref.relativePath.substring(slashIndex + 1)
        }

        override fun source(ref: FlorisRef): RawSource {
            return context.assets.open(ref.relativePath).asSource()
        }

        override fun sink(ref: FlorisRef): RawSink {
            throw UnsupportedOperationException("Cannot write to assets ($ref)")
        }
    }

    private val cacheStorage = object : FileStorage() {
        override fun getBasePath(): Path {
            return Path(context.noBackupFilesDir.path, "cache")
        }

        override fun canAccessUnderlyingMedium(ref: FlorisRef): Boolean {
            return isUserUnlocked && ref.isCache
        }
    }

    private val internalStorage = object : FileStorage() {
        override fun getBasePath(): Path {
            return Path(context.filesDir.path)
        }

        override fun canAccessUnderlyingMedium(ref: FlorisRef): Boolean {
            return isUserUnlocked && ref.isInternal
        }
    }

    override fun selectStorageBy(ref: FlorisRef): Storage {
        return when {
            ref.isAssets -> assetsStorage
            ref.isCache -> cacheStorage
            ref.isInternal -> internalStorage
            else -> throw UnsupportedOperationException("No storage available for ref $ref")
        }
    }
}
