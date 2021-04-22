/*
 * Copyright (C) 2020 Patrick Goldinger
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

package dev.patrickgold.florisboard.ime.popup

import dev.patrickgold.florisboard.ime.extension.Asset
import dev.patrickgold.florisboard.ime.text.key.KeyVariation
import dev.patrickgold.florisboard.ime.text.key.TextKeyData
import kotlinx.serialization.Serializable

/**
 * An object which maps each base key to its extended popups. This can be done for each
 * key variation. [KeyVariation.ALL] is always the fallback for each key.
 */
typealias PopupMapping = Map<KeyVariation, Map<String, PopupSet<TextKeyData>>>

/**
 * Class which contains an extended popup mapping to use for adding popups subtype based on the
 * keyboard layout.
 *
 * @property mapping The mapping of the base keys to their popups. See [PopupMapping] for more info.
 */
@Serializable
class PopupExtension(
    override val name: String,
    override var label: String = DUMMY,
    override val authors: List<String>,
    val mapping: PopupMapping
) : Asset {
    init {
        /*
        Must use dummy string because of issue with using other parameters as a default value for string in Kotlin
        serialization. See https://github.com/Kotlin/kotlinx.serialization/issues/133
         */
        if (label == DUMMY) {
            label = name
        }
    }

    companion object {
        private const val DUMMY: String = "___dummy_string_for_ktx_serialization___"

        fun empty() = PopupExtension("", "", listOf(), mapOf())
    }
}
