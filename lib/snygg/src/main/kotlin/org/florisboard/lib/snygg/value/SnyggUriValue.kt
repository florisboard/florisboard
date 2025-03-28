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

package org.florisboard.lib.snygg.value

import java.net.URI

data class SnyggUriValue(val uri: URI) : SnyggValue {
    companion object : SnyggValueEncoder {
        private const val EnclosedUriFunction = "uri"
        private const val EnclosedUriId = "enclosedUri"
        private val EnclosedUriPattern = """`[^`]*`""".toRegex()

        override val spec = SnyggValueSpec {
            function(name = EnclosedUriFunction) {
                string(id = EnclosedUriId, regex = EnclosedUriPattern)
            }
        }

        override fun defaultValue() = SnyggUriValue(URI.create(""))

        override fun serialize(v: SnyggValue) = runCatching<String> {
            require(v is SnyggUriValue)
            val map = snyggIdToValueMapOf(EnclosedUriId to "`${v.uri}`")
            return@runCatching spec.pack(map)
        }

        override fun deserialize(v: String) = runCatching<SnyggValue> {
            val map = snyggIdToValueMapOf()
            spec.parse(v, map)
            val enclosedUri = map.getString(EnclosedUriId)
            val uri = enclosedUri.substring(1, enclosedUri.length - 1)
            return@runCatching SnyggUriValue(URI.create(uri))
        }
    }

    override fun encoder() = Companion
}

/**
 * The AssetResolver for [SnyggUriValue].
 *
 * Override this class if you want to use [SnyggUriValue] (image and custom font-family support)
 * and pass the instance to [ProvideSnyggTheme][org.florisboard.lib.snygg.ui.ProvideSnyggTheme]
 *
 * @see [org.florisboard.lib.snygg.ui.ProvideSnyggTheme]
 */
abstract class SnyggAssetResolver {
    /**
     * Resolve a [URI] to an absolute path.
     * If the asset cannot be found i.e. the uri is invalid, return [Result.Failure]
     *
     * @param uri the [URI] of the asset
     *
     * @return Result with the absolute path ([String]) of the asset or a [Result.Failure]
     */
    abstract fun resolveAbsolutPath(uri: URI): Result<String>
}

internal object SnyggDefaultAssetResolver : SnyggAssetResolver() {
    override fun resolveAbsolutPath(uri: URI): Result<String> {
        return Result.failure(NotImplementedError("Default asset resolver does not implement path resolve ability"))
    }
}
