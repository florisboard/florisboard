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

package dev.patrickgold.florisboard.ime.keyboard

import dev.patrickgold.florisboard.lib.ext.ExtensionComponent
import dev.patrickgold.florisboard.lib.ext.ExtensionComponentName
import kotlinx.serialization.Serializable

typealias LayoutArrangement = List<List<AbstractKeyData>>

@Serializable
data class LayoutArrangementComponent(
    override val id: String,
    override val label: String,
    override val authors: List<String>,
    val direction: String,
    val modifier: ExtensionComponentName? = null,
    val arrangementFile: String? = null,
) : ExtensionComponent {
    fun arrangementFile(type: LayoutType) = arrangementFile ?: "layouts/${type.id}/$id.json"
}
