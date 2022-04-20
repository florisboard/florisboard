/*
 * Copyright (C) 2022 Patrick Goldinger
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

package dev.patrickgold.florisboard.ime.editor

import kotlin.math.max
import kotlin.math.min

/**
 * Data class which specifies the bounds for a range between [start] and [end].
 *
 * @property start The start marker of this region bounds.
 * @property end The end marker of this region bounds.
 */
data class EditorRegion(val start: Int, val end: Int) {
    /** True if the region's start and end markers are valid, false otherwise */
    val isValid: Boolean
        get() = start >= 0 && end >= 0 && length >= 0

    /** True if the region's start and end markers are invalid, false otherwise */
    val isNotValid: Boolean
        get() = !isValid

    /** The length of the region */
    val length: Int
        get() = end - start

    val isCursorMode: Boolean
        get() = length == 0 && isValid

    val isSelectionMode: Boolean
        get() = length != 0 && isValid

    override fun toString(): String = "EditorRegion(start=$start,end=$end)"

    companion object {
        /** Unspecified range */
        val Unspecified = EditorRegion(-1, -1)

        fun normalized(start: Int, end: Int) = EditorRegion(start = min(start, end), end = max(start, end))
    }
}