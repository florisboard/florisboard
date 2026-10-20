/*
 * Copyright (C) 2022-2025 The FlorisBoard Contributors
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

package org.florisboard.lib.compose

import androidx.compose.runtime.saveable.Saver
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.dp
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

val DpSizeSaver = Saver<Dp, Float>(
    save = { it.value },
    restore = { it.dp },
)

context(density: Density)
fun IntRect.toDpRect(): DpRect {
    return with(density) {
        DpRect(left.toDp(), top.toDp(), right.toDp(), bottom.toDp())
    }
}

context(density: Density)
fun Offset.toDp(): DpOffset {
    return with(density) {
        DpOffset(x.toDp(), y.toDp())
    }
}

fun Offset.toIntOffset(): IntOffset {
    return IntOffset(x.toInt(), y.toInt())
}

private const val MM_PER_INCH = 25.4f
private const val BASELINE_DPI = 160f

context(density: Density)
fun Dp.toMm(): Float {
    return (this.value * MM_PER_INCH) / (density.density * BASELINE_DPI)
}

object DpSizeSerializer : KSerializer<Dp> {
    override val descriptor = PrimitiveSerialDescriptor("DpSize", PrimitiveKind.FLOAT)

    override fun serialize(encoder: Encoder, value: Dp) {
        encoder.encodeFloat(value.value)
    }

    override fun deserialize(decoder: Decoder): Dp {
        return decoder.decodeFloat().dp
    }
}
