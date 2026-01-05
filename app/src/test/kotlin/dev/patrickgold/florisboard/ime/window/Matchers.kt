/*
 * Copyright (C) 2026 The FlorisBoard Contributors
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
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import dev.patrickgold.florisboard.shouldBeLessThanOrEqualTo
import io.kotest.assertions.withClue
import io.kotest.matchers.comparables.shouldBeGreaterThanOrEqualTo
import io.kotest.matchers.ranges.shouldBeIn
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

fun ImeWindowSpec.shouldBeFixedNormal() = this
    .shouldBeInstanceOf<ImeWindowSpec.Fixed>()
    .also { fixedMode shouldBe ImeWindowMode.Fixed.NORMAL }

fun ImeWindowSpec.shouldBeFixedCompact() = this
    .shouldBeInstanceOf<ImeWindowSpec.Fixed>()
    .also { fixedMode shouldBe ImeWindowMode.Fixed.COMPACT }

fun ImeWindowSpec.Floating.shouldBeConstrainedTo(rootBounds: DpRect, tolerance: Dp) {
    withClue("keyboard height should be constrained") {
        props.keyboardHeight.shouldBeIn(constraints.minKeyboardHeight..constraints.maxKeyboardHeight)
    }
    withClue("keyboard width should be constrained") {
        props.keyboardWidth.shouldBeIn(constraints.minKeyboardWidth..constraints.maxKeyboardWidth)
    }
    withClue("offset left should not push window out of root bounds") {
        props.offsetLeft.shouldBeGreaterThanOrEqualTo(0.dp)
        props.offsetLeft.shouldBeLessThanOrEqualTo(rootBounds.width - props.keyboardWidth, tolerance)
    }
    withClue("offset bottom should not push window out of root bounds") {
        props.offsetBottom.shouldBeGreaterThanOrEqualTo(0.dp)
        props.offsetBottom.shouldBeLessThanOrEqualTo(rootBounds.height - props.keyboardHeight, tolerance)
    }
}
