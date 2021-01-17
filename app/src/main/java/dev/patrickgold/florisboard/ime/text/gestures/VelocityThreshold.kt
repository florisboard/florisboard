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

package dev.patrickgold.florisboard.ime.text.gestures

import java.util.*

/**
 * Enum for declaring the velocity thresholds for swipe gestures.
 */
enum class VelocityThreshold {
    VERY_SLOW,
    SLOW,
    NORMAL,
    FAST,
    VERY_FAST;

    companion object {
        fun fromString(string: String): VelocityThreshold {
            return valueOf(string.toUpperCase(Locale.ENGLISH))
        }
    }

    override fun toString(): String {
        return super.toString().toLowerCase(Locale.ENGLISH)
    }
}
