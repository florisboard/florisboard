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

import android.content.Context
import com.github.michaelbull.result.Result

/**
 * Interface for an Asset to use within FlorisBoard. An asset is everything from a dictionary to a
 * keyboard layout to a extended popup mapping, etc. Assets are very important for the splitting
 * FlorisBoard's resources into assets.
 *
 * NOTE: At the current state, this is only a simple implementation idea and only PopupMappingAsset
 * partly uses it. This package and it's classes are expected to grow and gain more importance over
 * time.
 */
interface Asset {
    /**
     * The type of the Asset. Currently a string, but will probably be a sealed class in the future.
     */
    val type: String

    /**
     * The name of the Asset.
     */
    val name: String

    /**
     * A list of authors who actively worked on the content of this Asset.
     */
    val authors: List<String>

    /**
     * "Static" functions which every Asset should provide.
     */
    interface Companion<T> {
        /**
         * Creates an empty Asset.
         */
        fun empty(): T

        /**
         * Loads an Asset from the specified path.
         */
        fun fromFile(context: Context, path: String): Result<T, Throwable>
    }
}
