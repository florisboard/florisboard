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

package dev.patrickgold.florisboard.lib.ext

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames

/**
 * Class for an `extension.json` file, which serves as a configuration of an extension
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
@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class ExtensionMeta(
    /**
     * The unique identifier of this extension, adhering to
     * [Java™ package name standards](https://docs.oracle.com/javase/tutorial/java/package/namingpkgs.html)
     * and this regex: `^[a-z][a-z0-9_]*(\.[a-z][a-z0-9_]*)*$`
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
    @JsonNames("authors")
    val maintainers: List<ExtensionMaintainer>,

    /**
     * A valid license identifier, according to the [SPDX license list](https://spdx.org/licenses/).
     * Use an SPDX license expression if this extension has multiple licenses.
     */
    val license: String,
)
