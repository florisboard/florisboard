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

package dev.patrickgold.florisboard.lib.compose

import androidx.compose.runtime.saveable.Saver
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isUnspecified
import androidx.compose.ui.unit.sp

infix fun TextUnit.safeTimes(other: Float): TextUnit {
    return if (this.isUnspecified) 0.sp else this.times(other)
}

infix fun TextUnit.safeTimes(other: Double): TextUnit {
    return if (this.isUnspecified) this else this.times(other)
}

infix fun TextUnit.safeTimes(other: Int): TextUnit {
    return if (this.isUnspecified) this else this.times(other)
}

val DpSizeSaver = Saver<Dp, Float>(
    save = { it.value },
    restore = { it.dp },
)
