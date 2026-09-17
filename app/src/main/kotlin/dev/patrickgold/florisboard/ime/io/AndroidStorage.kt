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
import org.florisboard.lib.android.reader
import java.io.File

class AndroidStorage(
    val id: Int,
    val context: Context,
    val isUserUnlocked: Boolean,
) : Storage {
    override fun canAccessUnderlyingMedium(ref: FlorisRef): Boolean {
        return when {
            ref.isAssets -> true
            ref.isCache || ref.isInternal -> isUserUnlocked
            else -> false
        }
    }

    override fun resolveAbsolutePath(ref: FlorisRef): String {
        return when {
            ref.isAppUi || ref.isAssets -> ref.relativePath
            ref.isCache -> "${context.cacheDir.absolutePath}/${ref.relativePath}"
            ref.isInternal -> "${context.filesDir.absolutePath}/${ref.relativePath}"
            else -> ref.uri.path ?: ""
        }
    }

    override fun resolveCanonicalPath(ref: FlorisRef): String {
        val absPath = resolveAbsolutePath(ref)
        return when {
            ref.isCache || ref.isInternal -> {
                File(absPath).canonicalPath
            }
            else -> absPath
        }
    }

    override fun basename(ref: FlorisRef): String {
        return when {
            ref.isCache || ref.isInternal -> {
                val absPath = resolveAbsolutePath(ref)
                File(absPath).name
            }
            else -> error("cannot get basename for scheme '${ref.scheme}'")
        }
    }

    override fun exists(ref: FlorisRef): Boolean {
        return when {
            ref.isCache || ref.isInternal -> {
                val absPath = resolveAbsolutePath(ref)
                val absFile = File(absPath)
                absFile.exists()
            }
            else -> error("cannot check existence for scheme '${ref.scheme}'")
        }
    }

    override fun list(ref: FlorisRef): List<FlorisRef> {
        return when {
            ref.isAssets -> {
                context.assets.list(ref.relativePath)?.map { name ->
                    ref.subRef(name)
                } ?: emptyList()
            }
            ref.isCache || ref.isInternal -> {
                val absPath = resolveAbsolutePath(ref)
                val absFile = File(absPath)
                if (absFile.isDirectory) {
                    absFile.listFiles()?.map { file ->
                        ref.subRef(file.name)
                    } ?: emptyList()
                } else {
                    emptyList()
                }
            }
            else -> error("cannot list for scheme '${ref.scheme}'")
        }
    }

    override fun readText(ref: FlorisRef): String {
        return when {
            ref.isAssets -> {
                context.assets.reader(ref.relativePath).use { it.readText() }
            }
            ref.isCache || ref.isInternal -> {
                val absPath = resolveAbsolutePath(ref)
                File(absPath).readText()
            }
            else -> error("cannot read text for scheme '${ref.scheme}'")
        }
    }

    override fun writeText(ref: FlorisRef, text: String) {
        when {
            ref.isCache || ref.isInternal -> {
                val absPath = resolveAbsolutePath(ref)
                File(absPath).writeText(text)
            }
        }
    }

    override fun copy(srcRef: FlorisRef, dstRef: FlorisRef) {
        TODO()
        // src or dst may be a content resolver ref
    }

    override fun delete(ref: FlorisRef, mustExist: Boolean) {
        when {
            ref.isCache || ref.isInternal -> {
                val absPath = resolveAbsolutePath(ref)
                val absFile = File(absPath)
                if (absFile.exists()) {
                    absFile.delete()
                } else if (mustExist) {
                    error("file must exist but didn't")
                }
            }
            else -> error("cannot delete for scheme '${ref.scheme}'")
        }
    }
}
