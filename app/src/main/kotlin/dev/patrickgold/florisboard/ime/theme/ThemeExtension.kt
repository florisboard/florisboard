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

import dev.patrickgold.florisboard.lib.ext.Extension
import dev.patrickgold.florisboard.lib.ext.ExtensionEditor
import dev.patrickgold.florisboard.lib.ext.ExtensionMeta
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@SerialName(ThemeExtension.SERIAL_TYPE)
@Serializable
class ThemeExtension(
    override val meta: ExtensionMeta,
    override val dependencies: List<String>? = null,
    val themes: List<ThemeExtensionComponentImpl>,
) : Extension() {

    companion object {
        const val SERIAL_TYPE = "ime.extension.theme"
    }

    override fun serialType() = SERIAL_TYPE

    override fun components() = themes

    override fun edit() = ThemeExtensionEditor(
        meta = meta,
        dependencies = dependencies?.toMutableList() ?: mutableListOf(),
        themes = themes.map { it.edit() }.toMutableList(),
    )
}

class ThemeExtensionEditor(
    override var meta: ExtensionMeta,
    override val dependencies: MutableList<String>,
    val themes: MutableList<ThemeExtensionComponentEditor>,
) : ExtensionEditor {

    override fun build() = ThemeExtension(
        meta = meta,
        dependencies = dependencies.takeUnless { it.isEmpty() }?.toList(),
        themes = themes.map { it.build() },
    )
}
