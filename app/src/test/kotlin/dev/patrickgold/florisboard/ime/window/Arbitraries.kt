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

import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.dp
import io.kotest.property.Arb
import io.kotest.property.arbitrary.arbitrary
import io.kotest.property.arbitrary.float
import io.kotest.property.arbitrary.int

val epsilon = 1e-3f.dp

val densitiesArb = arbitrary {
    val density = Arb.float(0.75f, 4.0f).bind()
    val fontScale = Arb.float(0.85f, 2.0f).bind()
    Density(density, fontScale)
}

val rootBoundsPxArb = arbitrary {
    val widthPx = Arb.int(0, 4000).bind()
    val heightPx = Arb.int(0, 4000).bind()
    IntRect(0, 0, widthPx, heightPx)
}

val rootInsetsArb = arbitrary {
    val density = densitiesArb.bind()
    val boundsPx = rootBoundsPxArb.bind()
    with(density) { ImeInsets.Root.of(boundsPx) }
}
