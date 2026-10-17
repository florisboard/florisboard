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
import okio.FileSystem
import okio.IOException
import okio.Path
import okio.Path.Companion.toOkioPath
import okio.Path.Companion.toPath

class AndroidStorage(
    val id: Int,
    val context: Context,
    val isUserUnlocked: Boolean,
) : Storage() {
    private val assetFs = AssetFileSystem(context)
    private val contentResolverFs = ContentResolverFileSystem(context)
    private val systemFs = FileSystem.SYSTEM

    override fun canAccessStorage(ref: FlorisRef): Boolean {
        return when {
            ref.isAssets -> true
            ref.isCache || ref.isInternal -> isUserUnlocked
            else -> false
        }
    }

    override fun resolve(ref: FlorisRef): Pair<FileSystem, Path> {
        return when {
            ref.isAssets -> {
                assetFs to ref.relativePath.toPath(normalize = true)
            }
            ref.isCache -> {
                val base = context.noBackupFilesDir.toOkioPath() / "cache"
                val path = (base / ref.relativePath).normalized()
                // TODO startsWith check
                systemFs to path
            }
            ref.isInternal -> {
                val base = context.filesDir.toOkioPath()
                val path = (base / ref.relativePath).normalized()
                // TODO startsWith check
                systemFs to path
            }
            ref.scheme == "content" -> {
                // is this a hack? yes
                // does it work? also yes :)
                val path = ref.encodeBase64Path()
                contentResolverFs to path
            }
            else -> throw IOException("unknown ref scheme ${ref.scheme}")
        }
    }
}
