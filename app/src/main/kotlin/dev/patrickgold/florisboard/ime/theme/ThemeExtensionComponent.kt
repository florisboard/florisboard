/*
 * Copyright (C) 2022 Patrick Goldinger
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

package dev.patrickgold.florisboard.ime.theme

import dev.patrickgold.florisboard.lib.ext.ExtensionComponent
import dev.patrickgold.florisboard.lib.ext.ExtensionComponentName
import dev.patrickgold.florisboard.lib.snygg.SnyggStylesheetEditor
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Suppress("NOTHING_TO_INLINE")
inline fun extCoreTheme(id: String) = ExtensionComponentName(
    extensionId = "org.florisboard.themes",
    componentId = id,
)

interface ThemeExtensionComponent : ExtensionComponent {
    companion object {
        fun defaultStylesheetPath(id: String): String {
            return "stylesheets/$id.json"
        }
    }

    override val id: String
    override val label: String
    override val authors: List<String>
    val isNightTheme: Boolean
    val isBorderless: Boolean
    val isMaterialYouAware: Boolean
    val stylesheetPath: String?

    fun stylesheetPath(): String = stylesheetPath.takeUnless { it.isNullOrBlank() } ?: defaultStylesheetPath(id)
}

@Serializable
data class ThemeExtensionComponentImpl(
    override val id: String,
    override val label: String,
    override val authors: List<String>,
    @SerialName("isNight")
    override val isNightTheme: Boolean = true,
    override val isBorderless: Boolean = false,
    override val isMaterialYouAware: Boolean = false,
    @SerialName("stylesheet")
    override val stylesheetPath: String? = null,
) : ThemeExtensionComponent {

    fun edit() = ThemeExtensionComponentEditor(
        id, label, authors, isNightTheme, isBorderless, isMaterialYouAware, stylesheetPath ?: "",
    )
}

class ThemeExtensionComponentEditor(
    override var id: String = "",
    override var label: String = "",
    override var authors: List<String> = emptyList(),
    override var isNightTheme: Boolean = true,
    override var isBorderless: Boolean = false,
    override var isMaterialYouAware: Boolean = false,
    override var stylesheetPath: String = "",
) : ThemeExtensionComponent {

    var stylesheetPathOnLoad: String? = null
    var stylesheetEditor: SnyggStylesheetEditor? = null

    fun build(): ThemeExtensionComponentImpl {
        val component = ThemeExtensionComponentImpl(
            id = id.trim(),
            label = label.trim(),
            authors = authors.filterNot { it.isBlank() },
            isNightTheme = isNightTheme,
            isBorderless = isBorderless,
            isMaterialYouAware = isMaterialYouAware,
            stylesheetPath = stylesheetPath.takeUnless { it.isBlank() },
        )
        check(id.isNotBlank()) { "Theme component ID cannot be blank" }
        check(label.isNotBlank()) { "Theme component label cannot be blank" }
        check(authors.isNotEmpty()) { "Theme component authors must contain at least one non-blank author field" }
        return component
    }
}
