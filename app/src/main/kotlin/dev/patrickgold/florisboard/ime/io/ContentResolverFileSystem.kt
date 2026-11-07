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
import okio.FileSystem
import okio.IOException
import okio.Path
import okio.Path.Companion.toPath
import okio.Sink
import okio.Source
import okio.sink
import okio.source
import kotlin.io.encoding.Base64

class ContentResolverFileSystem(
    val context: Context,
) : FileSystem() {
    private fun throwUnsupported(): Nothing {
        throw IOException("$this only supports source and sink operations")
    }

    override fun canonicalize(path: Path): Path {
        throwUnsupported()
    }

    override fun metadataOrNull(path: Path): FileMetadata? {
        throwUnsupported()
    }

    override fun list(dir: Path): List<Path> {
        throwUnsupported()
    }

    override fun listOrNull(dir: Path): List<Path>? {
        throwUnsupported()
    }

    override fun openReadOnly(file: Path): FileHandle {
        throwUnsupported()
    }

    override fun openReadWrite(
        file: Path,
        mustCreate: Boolean,
        mustExist: Boolean
    ): FileHandle {
        throwUnsupported()
    }

    override fun source(file: Path): Source {
        val uri = file.decodeBase64Path().uri
        val contentResolver = context.contentResolver
            ?: throw IOException("No content resolver available")
        val inputStream = contentResolver.openInputStream(uri)
            ?: throw IOException("Content provider recently crashed??")
        return inputStream.source()
    }

    override fun sink(file: Path, mustCreate: Boolean): Sink {
        val uri = file.decodeBase64Path().uri
        val contentResolver = context.contentResolver
            ?: throw IOException("No content resolver available")
        val outputStream = contentResolver.openOutputStream(uri)
            ?: throw IOException("Content provider recently crashed??")
        return outputStream.sink()
    }

    override fun appendingSink(file: Path, mustExist: Boolean): Sink {
        throwUnsupported()
    }

    override fun createDirectory(dir: Path, mustCreate: Boolean) {
        throwUnsupported()
    }

    override fun atomicMove(source: Path, target: Path) {
        throwUnsupported()
    }

    override fun delete(path: Path, mustExist: Boolean) {
        throwUnsupported()
    }

    override fun createSymlink(source: Path, target: Path) {
        throwUnsupported()
    }
}

fun FlorisRef.encodeBase64Path(): Path {
    return Base64.encode(uri.toString().encodeToByteArray()).toPath()
}

fun Path.decodeBase64Path(): FlorisRef {
    return FlorisRef.from(Base64.decode(toString().encodeToByteArray()).decodeToString())
}
