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

import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import dev.patrickgold.florisboard.dp
import dev.patrickgold.florisboard.floatMaybeConstant
import io.kotest.property.Arb
import io.kotest.property.arbitrary.arbitrary
import io.kotest.property.arbitrary.enum
import io.kotest.property.arbitrary.float
import io.kotest.property.arbitrary.int

fun Arb.Companion.density() = arbitrary {
    val density = Arb.float(0.75f, 4.0f, includeNaNs = false).bind()
    val fontScale = Arb.float(0.85f, 2.0f, includeNaNs = false).bind()
    Density(density, fontScale)
}

fun Arb.Companion.rootBoundsPx() = arbitrary {
    val widthPx = Arb.int(0, 4000).bind()
    val heightPx = Arb.int(0, 4000).bind()
    IntRect(0, 0, widthPx, heightPx)
}

fun Arb.Companion.rootInsets() = arbitrary {
    val density = Arb.density().bind()
    val boundsPx = Arb.rootBoundsPx().bind()
    with(density) { ImeInsets.Root.of(boundsPx) }
}

fun Arb.Companion.rootInsetsWithHorizontalOffset() = arbitrary {
    val rootInsets = Arb.rootInsets().bind()
    val x = Arb.floatMaybeConstant(-rootInsets.boundsDp.width.value, rootInsets.boundsDp.width.value).bind()
    rootInsets to DpOffset(x.dp, 0.dp)
}

fun Arb.Companion.rootInsetsWithVerticalOffset() = arbitrary {
    val rootInsets = Arb.rootInsets().bind()
    val y = Arb.floatMaybeConstant(-rootInsets.boundsDp.height.value, rootInsets.boundsDp.height.value).bind()
    rootInsets to DpOffset(0.dp, y.dp)
}

fun Arb.Companion.rootInsetsWithAnyOffset() = arbitrary {
    val rootInsets = Arb.rootInsets().bind()
    val x = Arb.floatMaybeConstant(-rootInsets.boundsDp.width.value, rootInsets.boundsDp.width.value).bind()
    val y = Arb.floatMaybeConstant(-rootInsets.boundsDp.height.value, rootInsets.boundsDp.height.value).bind()
    rootInsets to DpOffset(x.dp, y.dp)
}

fun Arb.Companion.rootInsetsWithUpwardOffset() = arbitrary {
    val rootInsets = Arb.rootInsets().bind()
    val y = Arb.floatMaybeConstant(-rootInsets.boundsDp.height.value, 0f).bind()
    rootInsets to DpOffset(0.dp, y.dp)
}

fun Arb.Companion.rootInsetsWithDownwardOffset() = arbitrary {
    val rootInsets = Arb.rootInsets().bind()
    val y = Arb.floatMaybeConstant(0f, rootInsets.boundsDp.height.value).bind()
    rootInsets to DpOffset(0.dp, y.dp)
}

fun Arb.Companion.rootInsetsWithLeftwardOffset() = arbitrary {
    val rootInsets = Arb.rootInsets().bind()
    val x = Arb.floatMaybeConstant(-rootInsets.boundsDp.width.value, 0f).bind()
    rootInsets to DpOffset(x.dp, 0.dp)
}

fun Arb.Companion.rootInsetsWithRightwardOffset() = arbitrary {
    val rootInsets = Arb.rootInsets().bind()
    val x = Arb.floatMaybeConstant(0f, rootInsets.boundsDp.width.value).bind()
    rootInsets to DpOffset(x.dp, 0.dp)
}

fun Arb.Companion.windowConfigFixed() = arbitrary {
    val keyboardHeight = Arb.dp().bind()
    val paddingLeft = Arb.dp().bind()
    val paddingRight = Arb.dp().bind()
    val paddingBottom = Arb.dp().bind()
    val fixedMode = Arb.enum<ImeWindowMode.Fixed>().bind()

    val props = ImeWindowProps.Fixed(
        keyboardHeight = keyboardHeight,
        paddingLeft = paddingLeft,
        paddingRight = paddingRight,
        paddingBottom = paddingBottom,
    )

    ImeWindowConfig(
        mode = ImeWindowMode.FIXED,
        fixedMode = fixedMode,
        fixedProps = mapOf(fixedMode to props),
    )
}

fun Arb.Companion.windowConfigFloating() = arbitrary {
    val keyboardHeight = Arb.dp().bind()
    val keyboardWidth = Arb.dp().bind()
    val offsetLeft = Arb.dp().bind()
    val offsetBottom = Arb.dp().bind()
    val floatingMode = Arb.enum<ImeWindowMode.Floating>().bind()

    val props = ImeWindowProps.Floating(
        keyboardHeight = keyboardHeight,
        keyboardWidth = keyboardWidth,
        offsetLeft = offsetLeft,
        offsetBottom = offsetBottom,
    )

    ImeWindowConfig(
        mode = ImeWindowMode.FLOATING,
        floatingMode = floatingMode,
        floatingProps = mapOf(floatingMode to props),
    )
}
