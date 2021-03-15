/*
 * Copyright (C) 2020 Patrick Goldinger
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

package dev.patrickgold.florisboard.ime.extension

import java.util.*

/**
 * Sealed class which specifies where an asset comes from. There are 3 different types, all of which
 * require a different approach on how to access the actual asset.
 */
sealed class AssetSource {
    /**
     * The asset comes pre-built with the application, thus all paths must be relative to the asset
     * directory of FlorisBoard.
     */
    object Assets : AssetSource()

    /**
     * The asset is saved in the internal storage of FlorisBoard, all relative paths must therefore
     * be treated as such.
     */
    object Internal : AssetSource()

    /**
     * Asset source is an external extension, which requires the package name and possibly other
     * data. Currently NYI.
     * TODO: Implement external extensions
     */
    data class External(val packageName: String) : AssetSource() {
        override fun toString(): String {
            return super.toString()
        }
    }

    companion object {
        private val externalRegex: Regex = """^external\\(([a-z]+\\.)*[a-z]+\\)\$""".toRegex()

        fun fromString(str: String): Result<AssetSource> {
            return when (val string = str.toLowerCase(Locale.ENGLISH)) {
                "assets" -> Result.success(Assets)
                "internal" -> Result.success(Internal)
                else -> {
                    if (string.matches(externalRegex)) {
                        val packageName = string.substring(9, string.length - 1)
                        Result.success(External(packageName))
                    } else {
                        Result.failure(Exception("'$str' is not a valid AssetSource."))
                    }
                }
            }
        }
    }

    override fun toString(): String {
        return when (this) {
            is Assets -> "assets"
            is Internal -> "internal"
            is External -> "external($packageName)"
        }
    }
}
