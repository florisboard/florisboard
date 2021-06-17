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

package dev.patrickgold.florisboard.res

import android.net.Uri
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/**
 * A universal resource reference, capable to point to destinations within
 * FlorisBoard's APK assets, cache and internal storage, external resources
 * provided to FlorisBoard via content URIs, as well as hyperlinks.
 * [android.net.Uri] is used as the underlying implementation for storing the
 * reference and also handles parsing of raw string URIs.
 *
 * The reference is immutable. If a change is required, consider constructing
 * a new reference with the provided builder methods.
 *
 * @property uri The underlying URI, which can be used for external references
 *  to pass along to the system.
 */
@JvmInline
value class FlorisRef private constructor(val uri: Uri) {
    companion object {
        private const val SCHEME_FLORIS_ASSETS = "floris-assets"
        private const val SCHEME_FLORIS_CACHE = "floris-cache"
        private const val SCHEME_FLORIS_INTERNAL = "floris-internal"

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
            scheme(SCHEME_FLORIS_ASSETS)
            path(path)
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
            scheme(SCHEME_FLORIS_CACHE)
            path(path)
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
            scheme(SCHEME_FLORIS_INTERNAL)
            path(path)
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
        fun from(str: String) = FlorisRef(Uri.parse(str))
    }

    /**
     * True if the scheme indicates a reference to a FlorisBoard APK asset
     * resource, false otherwise.
     */
    val isAssets: Boolean
        get() = uri.scheme == SCHEME_FLORIS_ASSETS

    /**
     * True if the scheme indicates a reference to a FlorisBoard cache
     * resource, false otherwise.
     */
    val isCache: Boolean
        get() = uri.scheme == SCHEME_FLORIS_CACHE

    /**
     * True if the scheme indicates a reference to a FlorisBoard internal
     * storage resource, false otherwise.
     */
    val isInternal: Boolean
        get() = uri.scheme == SCHEME_FLORIS_INTERNAL

    /**
     * True if the scheme references any other external resource (URL, content
     * resolver, etc.), false otherwise.
     */
    val isExternal: Boolean
        get() = !isAssets && !isCache && !isInternal

    /**
     * Returns the scheme of this URI, or an empty string if no scheme is
     * specified.
     */
    val scheme: String
        get() = uri.scheme ?: ""

    /**
     * Returns the path of this URI, either relative or absolute depending on
     * the use case of this reference, or an empty string if no path is
     * specified.
     */
    val path: String
        get() = uri.path ?: ""

    /**
     * Allows this URI to be used depending on where this reference points to.
     * It is guaranteed that one of the four lambda parameters is executed.
     *
     * @param assets The lambda to run when the reference points to the FlorisBoard
     *  APK assets. Defaults to do nothing.
     * @param cache The lambda to run when the reference points to the FlorisBoard
     *  cache resources. Defaults to do nothing.
     * @param internal The lambda to run when the reference points to the FlorisBoard
     *  internal storage. Defaults to do nothing.
     * @param external The lambda to run when the reference points to an external
     * resource. Defaults to do nothing.
     */
    fun whenSchemeIs(
        assets: (ref: FlorisRef) -> Unit = { /* Do nothing */ },
        cache: (ref: FlorisRef) -> Unit = { /* Do nothing */ },
        internal: (ref: FlorisRef) -> Unit = { /* Do nothing */ },
        external: (ref: FlorisRef) -> Unit = { /* Do nothing */ }
    ) {
        contract {
            callsInPlace(assets, InvocationKind.AT_MOST_ONCE)
            callsInPlace(cache, InvocationKind.AT_MOST_ONCE)
            callsInPlace(internal, InvocationKind.AT_MOST_ONCE)
            callsInPlace(external, InvocationKind.AT_MOST_ONCE)
        }
        when {
            isAssets -> assets(this)
            isCache -> internal(this)
            isInternal -> internal(this)
            else -> external(this)
        }
    }

    /**
     * Returns the encoded string representation of this URI.
     */
    override fun toString(): String {
        return uri.toString()
    }
}
