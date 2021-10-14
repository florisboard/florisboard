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

package dev.patrickgold.florisboard.res.ext

import kotlinx.serialization.Serializable

/**
 * Interface for an `extension.json` file, which serves as a configuration of an extension
 * package for FlorisBoard (`.flex` archive files).
 *
 * Files which are always read (case sensitive):
 *  - extension.json (this file)
 *
 * Files which are always read (case-insensitive, can have any extension)
 *  - README
 *  - CHANGES / CHANGELOG / HISTORY
 *  - LICENSE / LICENSES
 *
 * Should multiple files exist which match the regex, always the first match will be used.
 */
@Serializable
data class ExtensionMeta(
    /**
     * The unique identifier of this extension, adhering to
     * [Java™ package name standards](https://docs.oracle.com/javase/tutorial/java/package/namingpkgs.html).
     */
    val id: String,

    /**
     * The version of this extension.
     */
    val version: String,

    /**
     * The title label of the extension. This title will be shown to the user in the Settings UI.
     *
     * Recommended limit: 50 characters
     */
    val title: String,

    /**
     * The short description of this extension, will be shown as a summary text in the package list, as
     * well as the first paragraph of the expanded description.
     *
     * Recommended limit: 80 characters
     */
    val description: String? = null,

    /**
     * The keywords for this extension. Useful for searching an extension in the extension store.
     *
     * Recommended limit: 30 characters / keyword
     */
    val keywords: List<String>? = null,

    /**
     * A link to the homepage of this extension or author.
     */
    val homepage: String? = null,

    /**
     * A link to this extension's issue tracker.
     */
    val issueTracker: String? = null,

    /**
     * A list of authors who actively worked on the content of this extension.
     *
     * Format: `Your Name <email@address.com> (www.author.com)`
     *  - Name is required
     *  - Email is optional, if included must be within the `<` and `>` symbols
     *  - URL is optional, if included must be within the `(` and `)` symbols
     *
     * Order of the above fields is important for parsing.
     */
    val authors: List<ExtensionAuthor>,

    /**
     * A valid license identifier, according to the [SPDX license list](https://spdx.org/licenses/).
     * Use an SPDX license expression if this extension has multiple licenses.
     */
    val license: String,
) {
    fun edit() = ExtensionMetaEditor(
        id, version, title, description ?: "", keywords?.toMutableList() ?: mutableListOf(),
        homepage ?: "", issueTracker ?: "", authors.map { it.edit() }.toMutableList(), license
    )
}

data class ExtensionMetaEditor(
    var id: String = "",
    var version: String = "",
    var title: String = "",
    var description: String = "",
    var keywords: MutableList<String> = mutableListOf(),
    var homepage: String = "",
    var issueTracker: String = "",
    var authors: MutableList<ExtensionAuthorEditor> = mutableListOf(),
    var license: String = "",
) {
    fun build() = runCatching {
        val meta = ExtensionMeta(
            id.trim(),
            version.trim(),
            title.trim(),
            description.trim().ifBlank { null },
            keywords.mapNotNull { it.trim().ifBlank { null } }.ifEmpty { null },
            homepage.trim().ifBlank { null },
            issueTracker.trim().ifBlank { null },
            authors.map { it.build().getOrThrow() },
            license.trim(),
        )
        check(meta.id.isNotBlank()) { "Extension ID cannot be blank" }
        check(meta.version.isNotBlank()) { "Extension version string cannot be blank" }
        check(meta.title.isNotBlank()) { "Extension title cannot be blank" }
        check(meta.authors.isNotEmpty()) { "At least one extension author must be defined" }
        check(meta.license.isNotBlank()) { "Extension license identifier cannot be blank" }
        return@runCatching meta
    }
}
