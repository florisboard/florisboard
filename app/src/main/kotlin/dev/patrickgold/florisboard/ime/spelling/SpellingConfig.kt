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

data class SpellingConfig(
    val basePath: String,
    val importSources: List<ImportSource>
) {
    companion object {
        fun default() = SpellingConfig("ime/spelling", listOf())
    }

    data class ImportSource(
        val id: String,
        val label: String,
        val url: String? = null,
        val format: ImportFormat
    )

    sealed interface ImportFormat {
        data class Archive(
            val file: FileInput,
        ) : ImportFormat

        data class Raw(
            val affFile: FileInput,
            val dicFile: FileInput,
        ) : ImportFormat
    }

    data class FileInput(
        val fileNameRegex: Regex,
        val isRequired: Boolean,
    )
}
