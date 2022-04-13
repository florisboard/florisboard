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

package dev.patrickgold.florisboard.lib.io

import dev.patrickgold.florisboard.lib.cache.CacheManager

object FileRegistry {
    val BackupArchive = Entry(
        type = Type.BINARY,
        fileExt = "zip",
        mediaType = "application/zip",
        alternativeMediaTypes = listOf(
            "application/octet-stream",
        ),
    )

    val FlexExtension = Entry(
        type = Type.BINARY,
        fileExt = "flex",
        mediaType = "application/vnd.florisboard.extension+zip",
        alternativeMediaTypes = listOf(
            "application/zip",
            "application/octet-stream",
        ),
    )

    fun guessMediaType(file: FsFile, givenMediaType: String?): String? {
        return when (file.extension) {
            FlexExtension.fileExt -> {
                if (FlexExtension.alternativeMediaTypes.contains(givenMediaType)) {
                    FlexExtension.mediaType
                } else {
                    givenMediaType
                }
            }
            else -> givenMediaType
        }
    }

    fun matchesFileFilter(fileInfo: CacheManager.FileInfo, filter: List<Entry>): Boolean {
        val fileExt = fileInfo.file.extension
        filter.forEach {
            if (it.fileExt == fileExt ||
                it.mediaType == fileInfo.mediaType ||
                it.alternativeMediaTypes.contains(fileInfo.mediaType)
            ) {
                return true
            }
        }
        return false
    }

    data class Entry(
        val type: Type,
        val fileExt: String,
        val mediaType: String,
        val alternativeMediaTypes: List<String> = emptyList(),
    )

    enum class Type(val id: String) {
        BINARY("bin"),
        TEXT("txt");
    }
}
