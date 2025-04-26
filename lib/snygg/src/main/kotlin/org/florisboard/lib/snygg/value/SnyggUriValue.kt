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

import androidx.compose.ui.layout.ContentScale
import java.net.URI

data class SnyggUriValue(val uri: String) : SnyggValue {
    companion object : SnyggValueEncoder {
        private const val EnclosedUriFunction = "uri"
        private const val EnclosedUriId = "enclosedUri"
        // TODO: evaluate the pattern for the URI
        private val EnclosedUriPattern = """`flex:/[^`]+`""".toRegex()

        override val spec = SnyggValueSpec {
            function(name = EnclosedUriFunction) {
                string(id = EnclosedUriId, regex = EnclosedUriPattern)
            }
        }

        override fun defaultValue() = SnyggUriValue("")

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
            return@runCatching SnyggUriValue(URI.create(uri).toString())
        }
    }

    override fun encoder() = Companion
}

data class SnyggContentScaleValue(val contentScale: ContentScale) : SnyggValue {
    companion object : SnyggEnumLikeValueEncoder<ContentScale>(
        serializationId = "textAlign",
        serializationMapping = mapOf(
            "crop" to ContentScale.Crop,
            "fill-bounds" to ContentScale.FillBounds,
            "fill-height" to ContentScale.FillHeight,
            "fill-width" to ContentScale.FillWidth,
            "fit" to ContentScale.Fit,
            "inside" to ContentScale.Inside,
            "none" to ContentScale.None,
        ),
        default = ContentScale.FillBounds,
        construct = { SnyggContentScaleValue(it) },
        destruct = { (it as SnyggContentScaleValue).contentScale },
    )
    override fun encoder() = Companion
}

/**
 * The assert resolver for [SnyggUriValue].
 *
 * Implement this interface if you want to use [SnyggUriValue] (image and custom font-family support)
 * and pass the instance to [org.florisboard.lib.snygg.ui.ProvideSnyggTheme]
 *
 * @since 0.5.0-alpha01
 * @see [org.florisboard.lib.snygg.ui.ProvideSnyggTheme]
 */
interface SnyggAssetResolver {
    /**
     * Resolve a given flex URI to an absolute path.
     * If the asset cannot be found i.e. the uri is invalid, return [Result.Failure].
     *
     * @param uri The URI of the asset.
     * @return Result with the absolute path of the asset or a [Result.Failure].
     * @since 0.5.0-alpha01
     */
    fun resolveAbsolutePath(uri: String): Result<String>
}

internal object SnyggDefaultAssetResolver : SnyggAssetResolver {
    override fun resolveAbsolutePath(uri: String): Result<String> {
        return Result.failure(NotImplementedError("Default asset resolver does not implement path resolve ability"))
    }
}
