/*
 * Copyright (C) 2021-2026 The FlorisBoard Contributors
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

package dev.patrickgold.florisboard.ime.io

import android.net.Uri
import dev.patrickgold.jetpref.datastore.model.PreferenceSerializer
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.k3lp.lib.meta.source.SourceFileRef

/**
 * A universal resource reference, capable to point to destinations within
 * FlorisBoard's app user interface screens, APK assets, cache and internal
 * storage, external resources provided to FlorisBoard via content URIs, as
 * well as hyperlinks.
 *
 * [android.net.Uri] is used as the underlying implementation for storing the
 * reference and also handles parsing of raw string URIs.
 *
 * The reference is immutable. If a change is required, consider constructing
 * a new reference with the provided builder methods.
 *
 * @property uri The underlying URI, which can be used for external references
 *  to pass along to the system.
 */
@Serializable(with = FlorisRef.Serializer::class)
@JvmInline
value class FlorisRef private constructor(val uri: Uri) : SourceFileRef {
    companion object {
        internal const val SCHEME_FLORIS = "florisboard"

        internal const val AUTHORITY_APP_UI = "app-ui"

        internal const val AUTHORITY_ASSETS = "assets"

        internal const val AUTHORITY_CACHE = "cache"

        internal const val AUTHORITY_INTERNAL = "internal"

        private const val URL_HTTP_PREFIX = "http://"
        private const val URL_HTTPS_PREFIX = "https://"
        private const val URL_MAILTO_PREFIX = "mailto:"

        /**
         * Constructs a new [FlorisRef] pointing to a UI screen within the app
         * user interface.
         *
         * @param path The relative path of the UI screen this ref should point
         *  too, including optional data arguments.
         *
         * @return The newly constructed reference.
         */
        fun app(path: String) = Uri.Builder().run {
            scheme(SCHEME_FLORIS)
            authority(AUTHORITY_APP_UI)
            encodedPath(path)
            FlorisRef(build())
        }

        /**
         * Constructs a new [FlorisRef] pointing to a resource within the
         * FlorisBoard APK assets.
         *
         * @param path The relative path from the APK assets root the resource
         *  is located.
         *
         * @return The newly constructed reference.
         */
        fun assets(path: String) = Uri.Builder().run {
            scheme(SCHEME_FLORIS)
            authority(AUTHORITY_ASSETS)
            encodedPath(path)
            FlorisRef(build())
        }

        /**
         * Constructs a new [FlorisRef] pointing to a resource within the
         * cache storage of FlorisBoard.
         *
         * @param path The relative path from the cache root directory.
         *
         * @return The newly constructed reference.
         */
        fun cache(path: String) = Uri.Builder().run {
            scheme(SCHEME_FLORIS)
            authority(AUTHORITY_CACHE)
            encodedPath(path)
            FlorisRef(build())
        }

        /**
         * Constructs a new [FlorisRef] pointing to a resource within the
         * internal storage of FlorisBoard.
         *
         * @param path The relative path from the internal storage root directory.
         *
         * @return The newly constructed reference.
         */
        fun internal(path: String) = Uri.Builder().run {
            scheme(SCHEME_FLORIS)
            authority(AUTHORITY_INTERNAL)
            encodedPath(path)
            FlorisRef(build())
        }

        /**
         * Constructs a new reference from given [uri], this can point to any
         * destination, regardless of within FlorisBoard or not.
         *
         * @param uri The destination, denoted by a system URI format.
         *
         * @return The newly constructed reference.
         */
        fun from(uri: Uri) = FlorisRef(uri)

        /**
         * Constructs a new reference from given [str], this can point to any
         * destination, regardless of within FlorisBoard or not.
         *
         * @param str An RFC 2396-compliant, encoded URI string.
         *
         * @return The newly constructed reference.
         */
        fun from(str: String): FlorisRef {
            // First two entries only kept due to backwards-compatibility reasons.
            return when {
                str.startsWith("assets:") -> assets(str.substring(7))
                str.startsWith("internal:") -> internal(str.substring(9))
                else -> FlorisRef(Uri.parse(str))
            }
        }

        /**
         * Constructs a new reference from given [url], which is a URL.
         *
         * @param url An URL pointing to a web page. If the scheme is missing, `https` is assumed.
         *
         * @return The newly constructed reference.
         */
        fun fromUrl(url: String): FlorisRef {
            return FlorisRef(when {
                url.startsWith(URL_HTTP_PREFIX) ||
                    url.startsWith(URL_HTTPS_PREFIX) ||
                    url.startsWith(URL_MAILTO_PREFIX) -> Uri.parse(url)
                else -> Uri.parse("$URL_HTTPS_PREFIX$url").normalizeScheme()
            })
        }

        /**
         * Constructs a new reference from given [scheme] and [path], this can
         * point to any destination, regardless of within FlorisBoard or not.
         *
         * @param scheme The scheme of this reference.
         * @param path The relative path of this reference.
         *
         * @return The newly constructed reference.
         */
        fun from(scheme: String, path: String) = Uri.Builder().run {
            scheme(scheme)
            encodedPath(path)
            FlorisRef(build())
        }
    }

    /**
     * True if the scheme and authority indicates a reference to an app user interface
     * component (screen), false otherwise.
     */
    val isAppUi: Boolean
        get() = uri.scheme == SCHEME_FLORIS && uri.authority == AUTHORITY_APP_UI

    /**
     * True if the scheme and authority indicates a reference to a FlorisBoard APK asset
     * resource, false otherwise.
     */
    val isAssets: Boolean
        get() = uri.scheme == SCHEME_FLORIS && uri.authority == AUTHORITY_ASSETS

    /**
     * True if the scheme indicates a reference to a FlorisBoard cache
     * resource, false otherwise.
     */
    val isCache: Boolean
        get() = uri.scheme == SCHEME_FLORIS && uri.authority == AUTHORITY_CACHE

    /**
     * True if the scheme indicates a reference to a FlorisBoard internal
     * storage resource, false otherwise.
     */
    val isInternal: Boolean
        get() = uri.scheme == SCHEME_FLORIS && uri.authority == AUTHORITY_INTERNAL

    /**
     * True if the scheme references any other external resource (URL, content
     * resolver, etc.), false otherwise.
     */
    val isExternal: Boolean
        get() = uri.scheme != SCHEME_FLORIS

    /**
     * Returns the scheme of this URI, or an empty string if no scheme is
     * specified.
     */
    val scheme: String
        get() = uri.scheme ?: ""

    /**
     * Returns the authority of this URI, or an empty string if no authority
     * is specified.
     */
    val authority: String
        get() = uri.authority ?: ""

    /**
     * Returns the relative path of this URI, without a leading forward slash.
     * Works only for assets, cache or internal references.
     */
    val relativePath: String
        get() = (uri.path ?: "").removePrefix("/")

    /**
     * Returns the name segment of the relative path, that is the last segment in
     * relative path separated by a slash. Returns the full relative path if no
     * slashes are present.
     */
    val pathName: String
        get() {
            val relPath = uri.path ?: return ""
            val slashIndex = relPath.lastIndexOf('/')
            return relPath.substring(slashIndex + 1)
        }

    /**
     * Returns if this URI contains data for all valid parts of a FlorisRef.
     */
    val isValid: Boolean
        get() = scheme.isNotBlank() && authority.isNotBlank()

    /**
     * Returns if this URI contains data for all valid parts of a FlorisRef.
     */
    val isInvalid: Boolean
        get() = !isValid

    /**
     * Returns a new reference pointing to a sub directory(file with given [name].
     *
     * @param name The name of the sub file/directory.
     *
     * @return The newly constructed reference.
     */
    fun subRef(name: String) = from(uri.buildUpon().run {
        appendEncodedPath(name)
        build()
    })

    /**
     * Returns the encoded string representation of this URI.
     */
    override fun toString(): String {
        return uri.toString()
    }

    object Serializer : PreferenceSerializer<FlorisRef>, KSerializer<FlorisRef> {
        override val descriptor = PrimitiveSerialDescriptor("FlorisRef", PrimitiveKind.STRING)

        override fun serialize(value: FlorisRef): String {
            return value.toString()
        }

        override fun serialize(encoder: Encoder, value: FlorisRef) {
            encoder.encodeString(value.toString())
        }

        override fun deserialize(value: String): FlorisRef {
            return from(value)
        }

        override fun deserialize(decoder: Decoder): FlorisRef {
            return from(decoder.decodeString())
        }
    }
}
