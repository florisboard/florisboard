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

package dev.patrickgold.florisboard.res.ext

import android.content.Context
import dev.patrickgold.florisboard.common.resultErr
import dev.patrickgold.florisboard.common.resultOk
import dev.patrickgold.florisboard.res.FlorisRef
import dev.patrickgold.florisboard.res.ZipUtils
import kotlinx.serialization.Polymorphic
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import java.io.File

/**
 * An extension container holding a parsed config, a working directory file
 * object as well as a reference to the original flex file.
 *
 * @property meta The parsed config of this extension.
 * @property workingDir The working directory, used as a cache and as a staging
 *  area for modifications to extension files.
 * @property sourceRef Optional, defines where the original flex file is stored.
 */
@Polymorphic
@Serializable
abstract class Extension {
    @Transient var workingDir: File? = null
    @Transient var sourceRef: FlorisRef? = null

    abstract val meta: ExtensionMeta
    abstract val dependencies: List<String>?

    abstract fun serialType(): String

    fun isLoaded() = workingDir != null

    open fun onBeforeLoad(context: Context, cacheDir: File) {
        /* Empty */
    }

    open fun onAfterLoad(context: Context, cacheDir: File) {
        /* Empty */
    }

    fun load(context: Context, force: Boolean = false): Result<Unit> {
        val cacheDir = File(context.cacheDir, meta.id)
        if (cacheDir.exists()) {
            if (force) {
                cacheDir.deleteRecursively()
            } else {
                // TODO: check if extension loaded should be kept as is
                cacheDir.deleteRecursively()
            }
        }
        cacheDir.mkdirs()
        val sourceRef = sourceRef ?: return resultOk()
        onBeforeLoad(context, cacheDir)
        ZipUtils.unzip(context, sourceRef, cacheDir).onFailure { return resultErr(it) }
        workingDir = cacheDir
        onAfterLoad(context, cacheDir)
        return resultOk()
    }

    open fun onBeforeUnload(context: Context, cacheDir: File) {
        /* Empty */
    }

    open fun onAfterUnload(context: Context, cacheDir: File) {
        /* Empty */
    }

    fun unload(context: Context) {
        val cacheDir = workingDir ?: File(context.cacheDir, meta.id)
        if (!cacheDir.exists()) return
        onBeforeUnload(context, cacheDir)
        cacheDir.deleteRecursively()
        workingDir = null
        onAfterUnload(context, cacheDir)
    }

    fun readExtensionFile(context: Context, relPath: String): String? {
        val cacheDir = File(context.cacheDir, meta.id)
        if (cacheDir.exists() && cacheDir.isDirectory) {
            val file = File(cacheDir, relPath)
            if (file.exists() && file.isFile) {
                return try {
                    file.readText()
                } catch (e: Exception) {
                    null
                }
            }
        }
        return null
    }

    abstract fun edit(): ExtensionEditor
}

interface ExtensionEditor {
    val meta: ExtensionMetaEditor
    val workingDir: File?
}
