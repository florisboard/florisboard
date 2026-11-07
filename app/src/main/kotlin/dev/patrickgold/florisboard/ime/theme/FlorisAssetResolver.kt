/*
 * Copyright (C) 2025-2026 The FlorisBoard Contributors
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

import dev.patrickgold.florisboard.ime.io.canonicalizeOrNull
import dev.patrickgold.florisboard.lib.devtools.flogError
import org.florisboard.lib.snygg.value.SnyggAssetResolver
import java.net.URI

// TODO this class needs a proper rewrite
class FlorisAssetResolver(val theme: ThemeController.Theme) : SnyggAssetResolver {
    override fun resolveAbsolutePath(uri: String) = runCatching {
        val uri = URI.create(uri)
        require(uri.scheme == "flex")
        require(uri.authority.isNullOrEmpty())
        val extensionRef = checkNotNull(theme.extensionRef) { "Loaded directory was null" }
        check(extensionRef.isCache || extensionRef.isInternal)
        val storage = checkNotNull(theme.storage) { "Storage was null" }
        // resolve implies within storage bounds & exists
        val (fs, extPath) = storage.resolve(extensionRef)
        val assetPath = fs.canonicalizeOrNull(
            path = extPath / (uri.path ?: "").removePrefix("/"),
            base = extPath,
        )
        checkNotNull(assetPath) {
            "Calculated path '$assetPath' is not within extension directory"
        }
        check(fs.metadataOrNull(assetPath)?.isRegularFile == true) {
            "Calculated path '$assetPath' is not a file"
        }
        assetPath.toString()
    }.onFailure { exception ->
        flogError { "FlorisAssetResolver failed to resolve URI '$uri'\n  error: ${exception.message}\n  with:  $theme" }
    }
}
