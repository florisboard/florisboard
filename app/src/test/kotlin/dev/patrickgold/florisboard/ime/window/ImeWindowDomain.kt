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
import net.jqwik.api.Provide
import net.jqwik.api.domains.DomainContextBase
import net.jqwik.kotlin.api.any
import net.jqwik.kotlin.api.combine

// TODO: this class should be provided via @Domain but that doesn't work (hours_wasted = 1)
//       inheriting works, so using this as a workaround for now
open class ImeWindowDomain : DomainContextBase() {
    @Provide
    fun densities() = combine(
        Float.any(0.75f..4.0f),
        Float.any(0.85f..2.0f),
    ) { density, fontScale ->
        Density(density, fontScale)
    }

    @Provide
    private fun rootBoundsPx() = combine(
        Int.any(0..4000),
        Int.any(0..4000),
    ) { widthPx, heightPx ->
        IntRect(0, 0, widthPx, heightPx)
    }

    @Provide
    fun rootInsets() = combine(
        densities(),
        rootBoundsPx(),
    ) { density, boundsPx ->
        with(density) { ImeInsets.Root.of(boundsPx) }
    }
}
