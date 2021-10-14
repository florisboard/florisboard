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

package dev.patrickgold.florisboard.ime.snygg

import kotlinx.serialization.Serializable

typealias SnyggStylesheetRules = Map<SnyggRule, SnyggPropertySet>

@Serializable
class SnyggStylesheet( val rules: SnyggStylesheetRules) {
    fun get(
        element: String,
        code: Int = -1,
        group: Int = -1,
        inputMode: Int = -1,
        isHover: Boolean = false,
        isFocus: Boolean = false,
        isPressed: Boolean = false,
    ): SnyggPropertySet {
        TODO()
    }

    fun merge(other: SnyggStylesheet): SnyggStylesheet {
        TODO()
    }
}
