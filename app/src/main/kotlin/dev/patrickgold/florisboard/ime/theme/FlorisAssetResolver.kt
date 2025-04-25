/*
 * Copyright (C) 2025 The FlorisBoard Contributors
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

package dev.patrickgold.florisboard.ime.theme

import android.content.Context
import dev.patrickgold.florisboard.lib.cache.loadedExtensionDir
import dev.patrickgold.florisboard.lib.devtools.flogError
import org.florisboard.lib.kotlin.io.subFile
import org.florisboard.lib.snygg.value.SnyggAssetResolver
import java.net.URI

class FlorisAssetResolver(val context: Context, val extId: String) : SnyggAssetResolver() {
    override fun resolveAbsolutPath(uri: URI) = runCatching {
        require(uri.scheme == "flex")
        require(uri.authority.isNullOrEmpty())
        val baseDir = context.loadedExtensionDir(extId)
        val absFile = baseDir.subFile(uri.path).canonicalFile
        val absPath = absFile.absolutePath
        check(absPath.startsWith(baseDir.absolutePath))
        absPath
    }.onFailure { exception ->
        flogError { "FlorisAssetResolver failed to resolve URI `$uri` (error: $exception)" }
    }
}
