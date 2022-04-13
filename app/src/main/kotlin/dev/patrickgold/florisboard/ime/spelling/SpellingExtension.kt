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

import android.content.Context
import dev.patrickgold.florisboard.lib.FlorisLocale
import dev.patrickgold.florisboard.lib.ext.Extension
import dev.patrickgold.florisboard.lib.ext.ExtensionComponent
import dev.patrickgold.florisboard.lib.ext.ExtensionEditor
import dev.patrickgold.florisboard.lib.ext.ExtensionMeta
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import java.io.File

@SerialName(SpellingExtension.SERIAL_TYPE)
@Serializable
data class SpellingExtension(
    override val meta: ExtensionMeta,
    override val dependencies: List<String>? = null,
    val spelling: SpellingExtensionConfig,
) : Extension() {

    companion object {
        const val SERIAL_TYPE = "ime.extension.spelling"
    }

    @Transient var dict: SpellingDict? = null

    override fun serialType() = SERIAL_TYPE

    override fun components(): List<ExtensionComponent> {
        return emptyList()
    }

    override fun onAfterLoad(context: Context, cacheDir: File) {
        dict = SpellingDict.new(cacheDir.absolutePath, this)
    }

    override fun onBeforeUnload(context: Context, cacheDir: File) {
        dict?.dispose()
        dict = null
    }

    override fun edit() = SpellingExtensionEditor(
        meta = meta,
        dependencies?.toMutableList() ?: mutableListOf(),
        workingDir,
        spelling.edit(),
    )
}

data class SpellingExtensionEditor(
    override var meta: ExtensionMeta,
    override val dependencies: MutableList<String>,
    var workingDir: File?,
    val spelling: SpellingExtensionConfigEditor,
) : ExtensionEditor {
    override fun build(): SpellingExtension {
        return SpellingExtension(
            meta = meta,
            spelling = spelling.build(),
        ).also {
            it.workingDir = workingDir
        }
    }
}

@Serializable
data class SpellingExtensionConfig(
    @SerialName("language")
    @Serializable(with = FlorisLocale.Serializer::class)
    val locale: FlorisLocale,
    val originalSourceId: String? = null,
    val affFile: String,
    val dicFile: String,
) {
    fun edit() = SpellingExtensionConfigEditor(
        locale.languageTag(), originalSourceId ?: "", affFile, dicFile
    )
}

data class SpellingExtensionConfigEditor(
    var locale: String = "",
    var originalSourceId: String = "",
    var affFile: String = "",
    var dicFile: String = "",
) {
    fun build(): SpellingExtensionConfig {
        val config = SpellingExtensionConfig(
            locale.trim().let { FlorisLocale.from(it) },
            originalSourceId.trim().ifBlank { null },
            affFile.trim(),
            dicFile.trim(),
        )
        check(config.affFile.isNotBlank()) { "Spelling extension aff file path cannot be blank" }
        check(config.dicFile.isNotBlank()) { "Spelling extension dic file path cannot be blank" }
        return config
    }
}
