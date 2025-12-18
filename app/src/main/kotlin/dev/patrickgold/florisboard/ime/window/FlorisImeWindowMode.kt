/*
 * Copyright (C) 2025 The FlorisBoard Contributors
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

package dev.patrickgold.florisboard.ime.window

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class FlorisImeWindowType {
    FIXED,
    FLOATING;
}

enum class FlorisImeWindowMode(
    val windowType: FlorisImeWindowType,
    val screenWidth: ScreenWidth,
) {
    FULL(
        windowType = FlorisImeWindowType.FIXED,
        screenWidth = ScreenWidth.Unconstrained,
    ),
    ONE_HANDED_LEFT(
        windowType = FlorisImeWindowType.FIXED,
        screenWidth = ScreenWidth.LessThan(600.dp),
    ),
    ONE_HANDED_RIGHT(
        windowType = FlorisImeWindowType.FIXED,
        screenWidth = ScreenWidth.LessThan(600.dp),
    ),
    COMPACT_LEFT(
        windowType = FlorisImeWindowType.FIXED,
        screenWidth = ScreenWidth.GreaterEqual(600.dp),
    ),
    COMPACT_CENTER(
        windowType = FlorisImeWindowType.FIXED,
        screenWidth = ScreenWidth.GreaterEqual(600.dp),
    ),
    COMPACT_RIGHT(
        windowType = FlorisImeWindowType.FIXED,
        screenWidth = ScreenWidth.GreaterEqual(600.dp),
    ),
    THUMBS(
        windowType = FlorisImeWindowType.FIXED,
        screenWidth = ScreenWidth.GreaterEqual(600.dp),
    ),
    FLOATING(
        windowType = FlorisImeWindowType.FLOATING,
        screenWidth = ScreenWidth.Unconstrained,
    );

    sealed interface ScreenWidth {
        data object Unconstrained : ScreenWidth

        data class LessThan(val dp: Dp) : ScreenWidth

        data class GreaterEqual(val dp: Dp) : ScreenWidth
    }
}
