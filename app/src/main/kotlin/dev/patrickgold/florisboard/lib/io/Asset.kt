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

package dev.patrickgold.florisboard.lib.io

/**
 * Interface for an Asset to use within FlorisBoard. An asset is everything from a dictionary to a
 * keyboard layout to a extended popup mapping, etc. Assets are very important for the splitting
 * FlorisBoard's resources into assets.
 */
interface Asset {
    /**
     * The name of the Asset, must be unique throughout all Assets. Is used to internally identify
     * and sort the Asset. This name is non-translatable and thus is a static string.
     */
    val name: String

    /**
     * The display name of the Asset. This is the label which will be shown to the user in the
     * Settings UI. Currently also a static string.
     * TODO: make this string localize-able
     */
    val label: String

    /**
     * A list of authors who actively worked on the content of this Asset. Any content of string is
     * valid, but the best practice is to use the GitHub username.
     */
    val authors: List<String>
}
