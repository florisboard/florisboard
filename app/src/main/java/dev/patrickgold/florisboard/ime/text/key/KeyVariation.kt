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

package dev.patrickgold.florisboard.ime.text.key

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class KeyVariation(val value: Int) {
    @SerialName("all")
    ALL(0),
    @SerialName("email")
    EMAIL_ADDRESS(1),
    @SerialName("normal")
    NORMAL(2),
    @SerialName("password")
    PASSWORD(3),
    @SerialName("uri")
    URI(4);

    companion object {
        fun fromInt(int: Int) = values().firstOrNull { it.value == int } ?: ALL
    }

    fun toInt() = value
}
