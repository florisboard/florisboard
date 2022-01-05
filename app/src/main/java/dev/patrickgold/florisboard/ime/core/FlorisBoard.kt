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

package dev.patrickgold.florisboard.ime.core

import dev.patrickgold.florisboard.ime.text.composing.Appender
import dev.patrickgold.florisboard.ime.text.composing.Composer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Deprecated("Will be removed once composers are re-implemented properly")
class FlorisBoard {
    @Deprecated("Will be removed once composers are re-implemented properly")
    @Serializable
    data class ImeConfig(
        @SerialName("package")
        val packageName: String,
        @SerialName("composers")
        val composers: List<Composer> = listOf(),
    ) {
        @Transient var composerNames: List<String> = listOf()
        @Transient var composerLabels: List<String> = listOf()
        @Transient val composerFromName: Map<String, Composer> = composers.map { it.name to it }.toMap()

        init {
            val tmpComposerList = composers.map { Pair(it.name, it.label) }.toMutableList()
            // Sort composer list alphabetically by the label of a composer
            tmpComposerList.sortBy { it.second }
            // Move selected composers to the top of the list
            for (composerName in listOf(Appender.name)) {
                val index: Int = tmpComposerList.indexOfFirst { it.first == composerName.toString() }
                if (index > 0) {
                    tmpComposerList.add(0, tmpComposerList.removeAt(index))
                }
            }
            composerNames = tmpComposerList.map { it.first }.toList()
            composerLabels = tmpComposerList.map { it.second }.toList()
        }
    }
}
