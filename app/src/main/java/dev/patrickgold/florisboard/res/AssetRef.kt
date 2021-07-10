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

package dev.patrickgold.florisboard.res

/**
 * Data class which is a reference to an asset file. It indicates in which storage medium the asset
 * is as well as the relative path to it.
 *
 * @property source The source in which the asset is (APK assets, internal storage, external)
 * @property path The relative path to the asset within [source]. Must not begin and end with a
 *  forward slash.
 */
@Deprecated("This class will slowly be replaced by '.res.FlorisRef', as it unifies the Android URI type with the internal referencing system. Consider using the new type and phase out this class.")
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

        @Deprecated("This class will slowly be replaced by '.res.FlorisRef', as it unifies the Android URI type with the internal referencing system. Consider using FlorisUri.assets(path)",
            ReplaceWith(
                "FlorisRef.assets(path)",
                "dev.patrickgold.florisboard.res.FlorisRef"
            )
        )
        fun assets(path: String) = AssetRef(AssetSource.Assets, path)

        @Deprecated("This class will slowly be replaced by '.res.FlorisRef', as it unifies the Android URI type with the internal referencing system. Consider using FlorisUri.internal(path)",
            ReplaceWith(
                "FlorisRef.internal(path)",
                "dev.patrickgold.florisboard.res.FlorisRef"
            )
        )
        fun internal(path: String) = AssetRef(AssetSource.Internal, path)
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
