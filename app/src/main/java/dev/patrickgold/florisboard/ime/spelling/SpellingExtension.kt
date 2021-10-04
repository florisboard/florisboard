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

import dev.patrickgold.florisboard.common.FlorisLocale
import dev.patrickgold.florisboard.res.ext.Extension
import dev.patrickgold.florisboard.res.ext.ExtensionMeta
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import java.io.File

@SerialName("ime:spelling-extension")
@Serializable
data class SpellingExtension(
    override val meta: ExtensionMeta,
    override val dependencies: List<String>,
    @SerialName("language")
    @Serializable(with = FlorisLocale.Serializer::class)
    val locale: FlorisLocale,
    val originalSourceId: String,
    val affFile: String,
    val dicFile: String,
) : Extension() {

    @Transient var dict: SpellingDict? = null

    override fun onAfterLoad(cacheDir: File) {
        dict = SpellingDict.new(cacheDir.absolutePath, this)
    }

    override fun onBeforeUnload(cacheDir: File) {
        dict?.dispose()
        dict = null
    }
}
