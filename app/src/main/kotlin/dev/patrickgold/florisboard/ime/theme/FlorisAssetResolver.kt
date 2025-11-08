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
import dev.patrickgold.florisboard.lib.devtools.flogError
import org.florisboard.lib.kotlin.io.subFile
import org.florisboard.lib.snygg.value.SnyggAssetResolver
import java.net.URI

class FlorisAssetResolver(val context: Context, val themeInfo: ThemeManager.ThemeInfo) : SnyggAssetResolver {
    override fun resolveAbsolutePath(uri: String) = runCatching {
        val uri = URI.create(uri)
        require(uri.scheme == "flex")
        require(uri.authority.isNullOrEmpty())
        val baseDir = checkNotNull(themeInfo.loadedDir) { "Loaded directory was null" }
        val basePath = baseDir.canonicalPath
        val canonicalFile = baseDir.subFile(uri.path).canonicalFile
        val canonicalPath = canonicalFile.path
        check(canonicalPath.startsWith(basePath)) {
            "Calculated path '$canonicalPath' does not start with base path '$basePath'"
        }
        check(canonicalFile.exists()) {
            "Calculated path '$canonicalPath' does not exist"
        }
        check(canonicalFile.isFile()) {
            "Calculated path '$canonicalPath' is not a file"
        }
        canonicalPath
    }.onFailure { exception ->
        flogError { "FlorisAssetResolver failed to resolve URI '$uri'\n  error: ${exception.message}\n  with:  $themeInfo" }
    }
}
