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
import okio.FileHandle
import okio.FileMetadata
import okio.FileNotFoundException
import okio.FileSystem
import okio.IOException
import okio.Path
import okio.Path.Companion.toPath
import okio.Sink
import okio.Source
import okio.source

class AssetFileSystem(
    val context: Context,
) : FileSystem() {
    override fun canonicalize(path: Path): Path {
        return ROOT.resolve(path, normalize = true)
    }

    override fun metadataOrNull(path: Path): FileMetadata? {
        try {
            context.assets.open(path.toString()).close()
            return FileMetadata(isRegularFile = true)
        } catch (_: FileNotFoundException) {
            return try {
                val list = context.assets.list(path.toString())
                if (list.isNullOrEmpty()) {
                    null
                } else {
                    FileMetadata(isDirectory = true)
                }
            } catch (_: IOException) {
                null
            }
        } catch (_: IOException) {
            return null
        }
    }

    override fun list(dir: Path): List<Path> {
        val list = context.assets.list(dir.toString()) ?:
            throw IOException("cannot list dir for '$dir'")
        return list.map { name -> dir / name }
    }

    override fun listOrNull(dir: Path): List<Path>? {
        return try {
            list(dir)
        } catch (_: IOException) {
            null
        }
    }

    override fun openReadOnly(file: Path): FileHandle {
        TODO("Not yet implemented")
    }

    override fun openReadWrite(
        file: Path,
        mustCreate: Boolean,
        mustExist: Boolean
    ): FileHandle {
        throw IOException("$this is read-only")
    }

    override fun source(file: Path): Source {
        return context.assets.open(file.toString()).source()
    }

    override fun sink(file: Path, mustCreate: Boolean): Sink {
        throw IOException("$this is read-only")
    }

    override fun appendingSink(file: Path, mustExist: Boolean): Sink {
        throw IOException("$this is read-only")
    }

    override fun createDirectory(dir: Path, mustCreate: Boolean) {
        throw IOException("$this is read-only")
    }

    override fun atomicMove(source: Path, target: Path) {
        throw IOException("$this is read-only")
    }

    override fun delete(path: Path, mustExist: Boolean) {
        throw IOException("$this is read-only")
    }

    override fun createSymlink(source: Path, target: Path) {
        throw IOException("$this is read-only")
    }

    companion object {
        private val ROOT = "/".toPath()
    }
}
