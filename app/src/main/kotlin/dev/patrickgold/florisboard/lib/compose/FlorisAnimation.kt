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

package dev.patrickgold.florisboard.lib.compose

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkVertically
import androidx.compose.ui.Alignment

fun EnterTransition.Companion.verticalTween(
    duration: Int,
    expandFrom: Alignment.Vertical = Alignment.Bottom,
): EnterTransition {
    return fadeIn(tween(duration)) + expandVertically(tween(duration), expandFrom)
}

fun ExitTransition.Companion.verticalTween(
    duration: Int,
    shrinkTowards: Alignment.Vertical = Alignment.Bottom,
): ExitTransition {
    return fadeOut(tween(duration)) + shrinkVertically(tween(duration), shrinkTowards)
}

fun EnterTransition.Companion.horizontalTween(
    duration: Int,
    expandFrom: Alignment.Horizontal = Alignment.End,
): EnterTransition {
    return fadeIn(tween(duration)) + expandHorizontally(tween(duration), expandFrom)
}

fun ExitTransition.Companion.horizontalTween(
    duration: Int,
    shrinkTowards: Alignment.Horizontal = Alignment.End,
): ExitTransition {
    return fadeOut(tween(duration)) + shrinkHorizontally(tween(duration), shrinkTowards)
}
