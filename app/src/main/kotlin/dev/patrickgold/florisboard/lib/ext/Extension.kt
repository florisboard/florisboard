/*
 * Copyright (C) 2021-2025 The FlorisBoard Contributors
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

package dev.patrickgold.florisboard.lib.ext

import android.content.Context
import android.net.Uri
import dev.patrickgold.florisboard.BuildConfig
import dev.patrickgold.florisboard.lib.io.FlorisRef
import dev.patrickgold.florisboard.lib.io.ZipUtils
import kotlinx.serialization.Polymorphic
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.florisboard.lib.kotlin.io.FsDir
import org.florisboard.lib.kotlin.io.FsFile
import org.florisboard.lib.kotlin.resultErr
import org.florisboard.lib.kotlin.resultOk

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
    @Transient var workingDir: FsDir? = null
    @Transient var sourceRef: FlorisRef? = null

    abstract val meta: ExtensionMeta
    abstract val dependencies: List<String>?

    abstract fun serialType(): String

    abstract fun components(): List<ExtensionComponent>

    fun isLoaded() = workingDir != null

    open fun onBeforeLoad(context: Context, cacheDir: FsDir) {
        /* Empty */
    }

    open fun onAfterLoad(context: Context, cacheDir: FsDir) {
        /* Empty */
    }

    fun load(context: Context, force: Boolean = false): Result<Unit> {
        val cacheDir = FsDir(context.cacheDir, meta.id)
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

    open fun onBeforeUnload(context: Context, cacheDir: FsDir) {
        /* Empty */
    }

    open fun onAfterUnload(context: Context, cacheDir: FsDir) {
        /* Empty */
    }

    fun unload(context: Context) {
        val cacheDir = workingDir ?: FsDir(context.cacheDir, meta.id)
        if (!cacheDir.exists()) return
        onBeforeUnload(context, cacheDir)
        cacheDir.deleteRecursively()
        workingDir = null
        onAfterUnload(context, cacheDir)
    }

    fun readExtensionFile(context: Context, relPath: String): String? {
        val cacheDir = FsDir(context.cacheDir, meta.id)
        if (cacheDir.exists() && cacheDir.isDirectory) {
            val file = FsFile(cacheDir, relPath)
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

/**
 * Generates an update url for [Extension] lists.
 *
 * @param version the version of the api path
 * @param host the host for the addons store
 * @return the Url
 */
internal fun List<Extension>.generateUpdateUrl(
    version: String = BuildConfig.FLADDONS_API_VERSION,
    host: String = BuildConfig.FLADDONS_STORE_URL,
): String {
    return Uri.Builder().run {
        scheme("https")
        authority(host)
        appendPath("check-updates")
        // TODO: Uncomment when version is supported by the addons store api
        //appendPath(version)
        encodedFragment(
            buildString {
                append("data={")
                for (extension in this@generateUpdateUrl) {
                    append(extension.meta.getUpdateJsonPair())
                    if (extension != this@generateUpdateUrl.last()) {
                        append(",")
                    }
                }
                append("}")
            }
        )
    }.build().toString()
}

interface ExtensionEditor {
    var meta: ExtensionMeta
    val dependencies: MutableList<String>

    fun build(): Extension
}
