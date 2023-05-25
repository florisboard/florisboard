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

package dev.patrickgold.florisboard.ime.core

import kotlinx.serialization.Serializable

/**
 * Data class which represents a computed user-specified set of language and layout.
 *
 * @property id The ID of this subtype.
 * @property primaryLocale The primary locale tag of this subtype.
 * @property secondaryLocales The secondary locale tags of this subtype. May be an empty list.
 */
@Serializable
data class ComputedSubtype(
    val id: Long,
    val primaryLocale: String,
    val secondaryLocales: List<String>,
) {
    fun isFallback(): Boolean {
        return id < 0
    }
}
