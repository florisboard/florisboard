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
import kotlinx.io.files.FileNotFoundException
import kotlinx.io.files.FileSystem
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem

abstract class FileStorage(
    val fs: FileSystem = SystemFileSystem,
) : Storage {
    abstract fun getBasePath(): Path

    fun resolveCanonicalPath(ref: FlorisRef): Path {
        val basePath = fs.resolve(getBasePath())
        val path = fs.resolve(Path(basePath, ref.relativePath))
        // TODO startsWith check
        return path
    }

    override fun delete(ref: FlorisRef, mustExist: Boolean) {
        try {
            val path = resolveCanonicalPath(ref)
            fs.delete(path, mustExist)
        } catch (e: FileNotFoundException) {
            if (mustExist) {
                throw e
            } else {
                return
            }
        }
    }

    final override fun exists(ref: FlorisRef): Boolean {
        val path = resolveCanonicalPath(ref)
        return fs.exists(path)
    }

    override fun list(ref: FlorisRef): List<FlorisRef> {
        val path = resolveCanonicalPath(ref)
        return fs.list(path).map { ref.subRef(it.name) }
    }

    override fun mkdirs(ref: FlorisRef) {
        // TODO resolveCanonicalPath will fail
        val path = resolveCanonicalPath(ref)
        fs.createDirectories(path)
    }

    override fun source(ref: FlorisRef): RawSource {
        val path = resolveCanonicalPath(ref)
        return fs.source(path)
    }

    override fun sink(ref: FlorisRef): RawSink {
        val path = resolveCanonicalPath(ref)
        return fs.sink(path)
    }
}
