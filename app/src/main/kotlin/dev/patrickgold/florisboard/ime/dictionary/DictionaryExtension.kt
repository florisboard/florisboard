/*
 * Copyright (C) 2023 Patrick Goldinger
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

package dev.patrickgold.florisboard.ime.dictionary

import dev.patrickgold.florisboard.ime.keyboard.LayoutType
import dev.patrickgold.florisboard.lib.FlorisLocale
import dev.patrickgold.florisboard.lib.ext.Extension
import dev.patrickgold.florisboard.lib.ext.ExtensionComponent
import dev.patrickgold.florisboard.lib.ext.ExtensionComponentName
import dev.patrickgold.florisboard.lib.ext.ExtensionEditor
import dev.patrickgold.florisboard.lib.ext.ExtensionMeta
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DictionaryComponent(
    override val id: String,
    override val label: String,
    override val authors: List<String>,
    val locale: FlorisLocale,
    val dictionaryFile: String? = null,
) : ExtensionComponent {
    fun dictionaryFile(type: LayoutType) = dictionaryFile ?: "dictionaries/$id.json"
}

@SerialName(DictionaryExtension.SERIAL_TYPE)
@Serializable
data class DictionaryExtension(
    override val meta: ExtensionMeta,
    override val dependencies: List<String>? = null,
    val dictionaries: List<DictionaryComponent> = listOf(),
) : Extension() {

    companion object {
        const val SERIAL_TYPE = "ime.extension.dictionary"
    }

    override fun serialType() = SERIAL_TYPE

    override fun components(): List<ExtensionComponent> {
        return dictionaries
    }

    override fun edit(): ExtensionEditor {
        TODO("Not yet implemented")
    }
}

@Suppress("NOTHING_TO_INLINE")
inline fun extCoreDictionary(id: String): ExtensionComponentName {
    val language = id.split("-")[0]
    return ExtensionComponentName(
        extensionId = "org.florisboard.dictionaries.${language}",
        componentId = id,
    )
}
