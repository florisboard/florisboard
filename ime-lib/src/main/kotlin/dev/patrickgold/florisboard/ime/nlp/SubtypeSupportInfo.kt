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

package dev.patrickgold.florisboard.ime.nlp

import kotlinx.serialization.Serializable

@Serializable
data class SubtypeSupportInfo(
    val state: SubtypeSupportState,
    val reason: String?,
) {
    fun isFullySupported(): Boolean {
        return state == SubtypeSupportState.FullySupported
    }

    fun isPartiallySupported(): Boolean {
        return state == SubtypeSupportState.PartiallySupported
    }

    fun isUnsupported(): Boolean {
        return state == SubtypeSupportState.Unsupported
    }

    companion object {
        fun fullySupported(): SubtypeSupportInfo {
            return SubtypeSupportInfo(SubtypeSupportState.FullySupported, null)
        }

        fun partiallySupported(reason: String? = null): SubtypeSupportInfo {
            return SubtypeSupportInfo(SubtypeSupportState.PartiallySupported, reason)
        }

        fun unsupported(reason: String? = null): SubtypeSupportInfo {
            return SubtypeSupportInfo(SubtypeSupportState.Unsupported, reason)
        }
    }
}

@Serializable
enum class SubtypeSupportState {
    FullySupported,
    PartiallySupported,
    Unsupported;
}
