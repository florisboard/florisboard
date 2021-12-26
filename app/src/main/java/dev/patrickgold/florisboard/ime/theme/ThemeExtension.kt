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

package dev.patrickgold.florisboard.ime.theme

import dev.patrickgold.florisboard.res.ext.Extension
import dev.patrickgold.florisboard.res.ext.ExtensionComponent
import dev.patrickgold.florisboard.res.ext.ExtensionComponentName
import dev.patrickgold.florisboard.res.ext.ExtensionEditor
import dev.patrickgold.florisboard.res.ext.ExtensionMeta
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

private const val SERIAL_TYPE = "ime.extension.theme"

@SerialName(SERIAL_TYPE)
@Serializable
data class ThemeExtension(
    override val meta: ExtensionMeta,
    override val dependencies: List<String>? = null,
    val themes: List<ThemeExtensionComponent>,
) : Extension() {

    override fun serialType() = SERIAL_TYPE

    override fun edit(): ExtensionEditor {
        TODO("Not yet implemented")
    }
}

@Serializable
data class ThemeExtensionComponent(
    override val id: String,
    override val label: String,
    override val authors: List<String>,
    @SerialName("isNight")
    val isNightTheme: Boolean = true,
    val isBorderless: Boolean = false,
    val isMaterialYouAware: Boolean = false,
    @SerialName("stylesheet")
    val stylesheetPath: String? = null,
) : ExtensionComponent {
    fun stylesheetPath() = "stylesheets/$id.json"
}

@Suppress("NOTHING_TO_INLINE")
inline fun extCoreTheme(id: String): ExtensionComponentName {
    return ExtensionComponentName(
        extensionId = "org.florisboard.themes",
        componentId = id,
    )
}
