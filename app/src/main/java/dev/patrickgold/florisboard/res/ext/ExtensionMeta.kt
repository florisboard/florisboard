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

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames

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
interface ExtensionMeta {
    /**
     * The unique identifier of this extension, adhering to
     * [Java™ package name standards](https://docs.oracle.com/javase/tutorial/java/package/namingpkgs.html).
     */
    val id: String

    /**
     * The version of this extension.
     */
    val version: String

    /**
     * The title label of the extension. This title will be shown to the user in the Settings UI.
     *
     * Recommended limit: 50 characters
     */
    val title: String

    /**
     * The short description of this extension, will be shown as a summary text in the package list, as
     * well as the first paragraph of the expanded description.
     *
     * Recommended limit: 80 characters
     */
    val description: String?

    /**
     * The keywords for this extension. Useful for searching an extension in the extension store.
     *
     * Recommended limit: 30 characters / keyword
     */
    val keywords: List<String>?

    /**
     * A link to the homepage of this extension or author.
     */
    val homepage: String?

    /**
     * A link to this extension's issue tracker.
     */
    val issueTracker: String?

    /**
     * A list of maintainers who (actively) worked on putting the content together for this extension. Note that
     * the actual author of each file within the extension (theme, layout, e.g.) may be different and is specified
     * in the file meta itself.
     *
     * Format: `Your Name <email@address.com> (www.maintainer.com)`
     *  - Name is required
     *  - Email is optional, if included must be within the `<` and `>` symbols
     *  - URL is optional, if included must be within the `(` and `)` symbols
     *
     * Order of the above fields is important for parsing.
     */
    val maintainers: List<ExtensionMaintainer>

    /**
     * A valid license identifier, according to the [SPDX license list](https://spdx.org/licenses/).
     * Use an SPDX license expression if this extension has multiple licenses.
     */
    val license: String
}

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class ExtensionMetaImpl(
    override val id: String,
    override val version: String,
    override val title: String,
    override val description: String? = null,
    override val keywords: List<String>? = null,
    override val homepage: String? = null,
    override val issueTracker: String? = null,
    @JsonNames("authors")
    override val maintainers: List<ExtensionMaintainerImpl>,
    override val license: String,
) : ExtensionMeta {

    fun edit() = ExtensionMetaEditor(
        id, version, title, description ?: "", keywords?.toMutableList() ?: mutableListOf(),
        homepage ?: "", issueTracker ?: "", maintainers.map { it.edit() }.toMutableList(), license
    )
}

class ExtensionMetaEditor(
    override var id: String = "",
    override var version: String = "",
    override var title: String = "",
    override var description: String = "",
    override var keywords: MutableList<String> = mutableListOf(),
    override var homepage: String = "",
    override var issueTracker: String = "",
    override var maintainers: MutableList<ExtensionMaintainerEditor> = mutableListOf(),
    override var license: String = "",
) : ExtensionMeta {

    fun build(): ExtensionMetaImpl {
        val meta = ExtensionMetaImpl(
            id.trim(),
            version.trim(),
            title.trim(),
            description.trim().ifBlank { null },
            keywords.mapNotNull { it.trim().ifBlank { null } }.ifEmpty { null },
            homepage.trim().ifBlank { null },
            issueTracker.trim().ifBlank { null },
            maintainers.map { it.build() },
            license.trim(),
        )
        check(meta.id.isNotBlank()) { "Extension ID cannot be blank" }
        check(meta.version.isNotBlank()) { "Extension version string cannot be blank" }
        check(meta.title.isNotBlank()) { "Extension title cannot be blank" }
        check(meta.maintainers.isNotEmpty()) { "At least one extension maintainer must be defined" }
        check(meta.license.isNotBlank()) { "Extension license identifier cannot be blank" }
        return meta
    }
}
