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

import android.content.Context
import dev.patrickgold.florisboard.assetManager
import dev.patrickgold.florisboard.ime.snygg.SnyggStylesheet
import dev.patrickgold.florisboard.res.ext.Extension
import dev.patrickgold.florisboard.res.ext.ExtensionEditor
import dev.patrickgold.florisboard.res.ext.ExtensionMeta
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import java.io.File

@SerialName("ime.extension.theme")
@Serializable
data class ThemeExtension(
    override val meta: ExtensionMeta,
    override val dependencies: List<String>? = null,
    val theme: ThemeExtensionConfig,
) : Extension() {

    @Transient private var stylesheet: SnyggStylesheet? = null

    override fun onAfterLoad(context: Context, cacheDir: File) {
        val assetManager by context.assetManager()
        val jsonStylesheet = readExtensionFile(context, theme.stylesheet)
        if (jsonStylesheet != null) {
            assetManager.loadJsonAsset<SnyggStylesheet>(jsonStylesheet).onSuccess {
                stylesheet = it
            }
        }
    }

    override fun onBeforeUnload(context: Context, cacheDir: File) {
        stylesheet = null
    }

    override fun edit(): ExtensionEditor {
        TODO("Not yet implemented")
    }
}

@Serializable
data class ThemeExtensionConfig(
    val isNightTheme: Boolean = true,
    val isMaterialYouAware: Boolean = false,
    val stylesheet: String,
)
