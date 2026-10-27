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

import dev.patrickgold.florisboard.ime.io.AndroidStorage
import dev.patrickgold.florisboard.lib.devtools.flogError
import kotlinx.io.files.SystemFileSystem
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
        check(storage is AndroidStorage)
        val effStorage = if (extensionRef.isCache) storage.cacheStorage else storage.internalStorage
        // resolve implies within storage bounds & exists
        val canonicalPath = effStorage.resolveCanonicalPath(extensionRef)
        check(SystemFileSystem.metadataOrNull(canonicalPath)?.isRegularFile == true) {
            "Calculated path '$canonicalPath' is not a file"
        }
        canonicalPath.toString()
    }.onFailure { exception ->
        flogError { "FlorisAssetResolver failed to resolve URI '$uri'\n  error: ${exception.message}\n  with:  $theme" }
    }
}
