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
import dev.patrickgold.florisboard.res.FlorisRef
import kotlinx.serialization.Transient
import java.io.File

/**
 * An extension container holding a parsed config, a working directory file
 * object as well as a reference to the original flex file.
 *
 * @property meta The parsed config of this extension.
 * @property workingDir The working directory, used as a cache and as a staging
 *  area for modifications to extension files.
 * @property flexFile Optional, defines where the original flex file is stored.
 */
abstract class Extension {
    @Transient private var workingDir: FlorisRef? = null
    @Transient private var sourceRef: FlorisRef? = null

    abstract val meta: ExtensionMeta
    abstract val dependencies: List<String>

    open fun onBeforeLoad() {
        /* Empty */
    }

    open fun onAfterLoad() {
        /* Empty */
    }

    fun load(context: Context, force: Boolean = false) {
        val cacheDir = File(context.cacheDir, meta.id)
        if (cacheDir.exists()) {
            if (force) {
                cacheDir.deleteRecursively()
            } else {
                // TODO: check if extension loaded should be kept as is
                cacheDir.deleteRecursively()
            }
        }
        onBeforeLoad()
        cacheDir.mkdirs()
        // TODO: Load here
        onAfterLoad()
    }

    open fun onBeforeUnload() {
        /* Empty */
    }

    open fun onAfterUnload() {
        /* Empty */
    }

    fun unload(context: Context) {
        val cacheDir = File(context.cacheDir, meta.id)
        if (!cacheDir.exists()) return
        onBeforeUnload()
        cacheDir.deleteRecursively()
        onAfterUnload()
    }
}
