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

import dev.patrickgold.florisboard.BuildConfig

/**
 * Interface for an `extension.json` file, which serves as a configuration of an extension
 * package for FlorisBoard (`.flex` archive files).
 */
interface ExtensionConfig {
    companion object {
        const val DEFAULT_FILE_EXTENSION = "flex"
        const val DEFAULT_NAME = "extension.json"
        const val DEFAULT_ID = "${BuildConfig.APPLICATION_ID}.imported.%s.%s"

        const val CUSTOM_LICENSE_IDENTIFIER = "@custom"

        fun createIdForImport(groupName: String, extensionName: String): String {
            return String.format(DEFAULT_ID, groupName, extensionName)
        }
    }

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
    val description: String

    /**
     * A list of authors who actively worked on the content of this extension. Any content of string is
     * valid, but the best practice is to use the GitHub username.
     */
    val authors: List<String>

    /**
     * A valid license identifier, according to the [SPDX license list](https://spdx.org/licenses/).
     * Use `@custom` if you use a license that is not listed in the above list.
     */
    val license: String

    /**
     * Additional optional license text file. Use a relative path to your license file.
     */
    val licenseFile: String

    /**
     * The long description or readme file of this extension, will be shown in the expanded description.
     * Use a relative path to the readme file.
     *
     * Recommended limit: 4000 characters
     */
    val readmeFile: String
}
