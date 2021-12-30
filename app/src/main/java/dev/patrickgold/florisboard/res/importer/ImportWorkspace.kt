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

package dev.patrickgold.florisboard.res.importer

import android.content.Context
import dev.patrickgold.florisboard.res.io.FsDir
import dev.patrickgold.florisboard.res.io.FsFile
import dev.patrickgold.florisboard.res.io.subDir
import java.io.Closeable
import java.util.*

class ImportWorkspace(context: Context) : Closeable {
    companion object {
        private const val WORKSPACE_INPUT_DIR_NAME = "input"
        private const val WORKSPACE_OUTPUT_DIR_NAME = "output"
    }

    val wsUuid: String = UUID.randomUUID().toString()
    val wsDir: FsDir = context.cacheDir.subDir(wsUuid)
    val wsInputDir: FsDir = wsDir.subDir(WORKSPACE_INPUT_DIR_NAME)
    val wsOutputDir: FsFile = wsDir.subDir(WORKSPACE_OUTPUT_DIR_NAME)
    val wsInputFiles = mutableListOf<ImportFileInfo>()

    @PublishedApi
    internal fun ensureWorkspaceIntegrity() {
        check(wsDir.exists()) {
            """
            Workspace cache directory '$wsUuid' has been deleted. Either the cache has been cleared
            by the system or resource manager or this importer workspace is not valid anymore.
            """.trimIndent()
        }
    }

    fun mkdirs() {
        wsDir.mkdirs()
        wsInputDir.mkdirs()
        wsOutputDir.mkdirs()
    }

    override fun close() {
        wsDir.deleteRecursively()
    }
}
