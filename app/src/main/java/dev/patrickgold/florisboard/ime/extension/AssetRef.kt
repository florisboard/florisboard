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

/**
 * Data class which is a reference to an asset file. It indicates in which storage medium the asset
 * is as well as the relative path to it.
 *
 * @property source The source in which the asset is (APK assets, internal storage, external)
 * @property path The relative path to the asset within [source]. Must not begin and end with a
 *  forward slash.
 */
data class AssetRef(
    val source: AssetSource,
    val path: String
) {
    companion object {
        private const val DELIMITER: String = ":"

        fun fromString(str: String): Result<AssetRef> {
            val items = str.split(DELIMITER)
            if (items.size != 2) {
                return Result.failure(Exception("Unexpected length of given asset ref. Make sure that the asset ref string contains exactly 2 items separated by '$DELIMITER'!"))
            }
            val retSource = AssetSource.fromString(items[0]).getOrElse {
                return Result.failure(Exception(it))
            }
            return Result.success(AssetRef(retSource, items[1]))
        }
    }

    override fun toString(): String {
        val retString: StringBuilder = StringBuilder().apply {
            append(source.toString())
            append(DELIMITER)
            append(path)
        }
        return retString.toString()
    }
}
