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

import okio.FileSystem
import okio.IOException
import okio.Path
import okio.Sink
import okio.Source
import okio.buffer

/**
 * An interface providing basic operations on an underlying storage medium.
 *
 * This API is designed to closely follow [okio.FileSystem].
 */
abstract class Storage {
    abstract fun canAccessStorage(ref: FlorisRef): Boolean

    abstract fun resolve(ref: FlorisRef): Pair<FileSystem, Path>

    fun delete(ref: FlorisRef, mustExist: Boolean = false) {
        val (fs, path) = resolve(ref)
        fs.delete(path, mustExist)
    }

    fun deleteContents(ref: FlorisRef) {
        val (fs, path) = resolve(ref)
        fs.listRecursively(path, followSymlinks = false).forEach { subPath ->
            if (subPath != path) {
                fs.deleteRecursively(subPath)
            }
        }
    }

    fun deleteRecursively(ref: FlorisRef, mustExist: Boolean = false) {
        val (fs, path) = resolve(ref)
        fs.deleteRecursively(path, mustExist)
    }

    fun exists(ref: FlorisRef): Boolean {
        val (fs, path) = resolve(ref)
        return fs.exists(path)
    }

    fun list(ref: FlorisRef): List<FlorisRef> {
        val (fs, path) = resolve(ref)
        return fs.list(path).map { ref.subRef(it.name) }
    }

    fun mkdirs(ref: FlorisRef) {
        val (fs, path) = resolve(ref)
        fs.createDirectories(path)
    }

    fun source(ref: FlorisRef): Source {
        val (fs, path) = resolve(ref)
        return fs.source(path)
    }

    fun sink(ref: FlorisRef): Sink {
        val (fs, path) = resolve(ref)
        return fs.sink(path)
    }

    fun copy(srcRef: FlorisRef, dstRef: FlorisRef) {
        val (srcFs, srcPath) = resolve(srcRef)
        val (dstFs, dstPath) = resolve(dstRef)
        if (srcFs === dstFs) {
            srcFs.copy(srcPath, dstPath)
        } else {
            srcFs.source(srcPath).use { source ->
                dstFs.sink(dstPath).buffer().use { sink ->
                    sink.writeAll(source)
                }
            }
        }
    }

    fun atomicMove(srcRef: FlorisRef, dstRef: FlorisRef) {
        val (srcFs, srcPath) = resolve(srcRef)
        val (dstFs, dstPath) = resolve(dstRef)
        if (srcFs === dstFs) {
            srcFs.atomicMove(srcPath, dstPath)
        } else {
            throw IOException("atomic move requires same fs, but was different: srcFs=$srcFs, dstFs=$dstFs")
        }
    }
}
