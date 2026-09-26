/*
 * Copyright (C) 2021-2026 The FlorisBoard Contributors
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

import dev.patrickgold.florisboard.ime.extension.Extension
import dev.patrickgold.florisboard.ime.extension.ExtensionComponent
import dev.patrickgold.florisboard.ime.extension.ExtensionComponentName
import dev.patrickgold.florisboard.ime.extension.ExtensionDependencyDecl
import dev.patrickgold.florisboard.ime.extension.ExtensionManifest
import dev.patrickgold.florisboard.ime.extension.ExtensionMeta
import dev.patrickgold.florisboard.ime.io.FlorisRef
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames
import org.florisboard.lib.snygg.color.MaterialYouFlags

class ThemeExtension(
    override val manifest: Manifest,
    override val sourceRef: FlorisRef,
) : Extension<ThemeExtension.Manifest> {
    companion object {
        const val SERIAL_TYPE = "ime.extension.theme"
    }

    @SerialName(SERIAL_TYPE)
    @Serializable
    data class Manifest(
        override val meta: ExtensionMeta,
        override val dependencies: ExtensionDependencyDecl = emptyMap(),
        val themes: List<ThemeComponent>,
    ) : ExtensionManifest

    @OptIn(ExperimentalSerializationApi::class)
    @Serializable
    data class ThemeComponent(
        override val id: String,
        @JsonNames("label")
        override val name: String,
        override val authors: List<String>,
        @SerialName("isNight")
        val isNightTheme: Boolean = true,
        @EncodeDefault
        val materialYouFlags: MaterialYouFlags = MaterialYouFlags(),
        @SerialName("stylesheet")
        val stylesheetPath: String? = null,
    ) : ExtensionComponent {
        fun stylesheetPath(): String = stylesheetPath.takeUnless { it.isNullOrBlank() } ?: defaultStylesheetPath(id)

        fun defaultStylesheetPath(id: String): String {
            return "stylesheets/$id.json"
        }
    }
}

fun extCoreTheme(id: String) = ExtensionComponentName(
    extensionId = "org.florisboard.themes",
    componentId = id,
)

fun extPreviewTheme(id: String) = ExtensionComponentName(
    extensionId = "local.themes.preview",
    componentId = id,
)
