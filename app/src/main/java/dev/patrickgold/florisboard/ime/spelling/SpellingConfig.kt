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

package dev.patrickgold.florisboard.ime.spelling

import dev.patrickgold.florisboard.common.RegexSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.io.File

@Serializable
data class SpellingConfig(
    @SerialName("basePath")
    val basePath: String,
    @SerialName("importSources")
    val importSources: List<ImportSource>
) {
    companion object {
        fun default() = SpellingConfig("ime/spelling", listOf())
    }

    @Serializable
    data class ImportSource(
        @SerialName("id")
        val id: String,
        @SerialName("label")
        val label: String,
        @SerialName("url")
        val url: String? = null,
        @SerialName("format")
        val format: ImportFormat
    )

    sealed interface ImportFormat {
        @Serializable
        @SerialName("archive")
        data class Archive(
            @SerialName("file")
            val file: FileInput,
        ) : ImportFormat

        @Serializable
        @SerialName("raw")
        data class Raw(
            @SerialName("affFile")
            val affFile: FileInput,
            @SerialName("dicFile")
            val dicFile: FileInput
        ) : ImportFormat
    }

    @Serializable
    data class FileInput(
        @SerialName("name")
        @Serializable(with = RegexSerializer::class)
        val fileNameRegex: Regex,
        @SerialName("isRequired")
        val isRequired: Boolean
    )
}
