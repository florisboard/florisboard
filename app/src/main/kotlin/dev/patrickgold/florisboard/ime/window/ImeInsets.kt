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
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import org.florisboard.lib.compose.toDpRect

data class ImeInsets(
    val boundsDp: DpRect,
    val boundsPx: IntRect,
    val formFactor: ImeFormFactor,
) {
    companion object {
        val Zero = ImeInsets(
            boundsDp = DpRect(0.dp, 0.dp, 0.dp, 0.dp),
            boundsPx = IntRect.Zero,
            formFactor = ImeFormFactor.Zero,
        )

        context(density: Density)
        fun of(boundsPx: IntRect): ImeInsets {
            val boundsDp = boundsPx.toDpRect()
            return ImeInsets(
                boundsDp = boundsDp,
                boundsPx = boundsPx,
                formFactor = ImeFormFactor.of(boundsDp),
            )
        }
    }
}

val ImeInsets?.inferredOrientation: ImeOrientation
    get() = this?.boundsDp?.inferredOrientation ?: ImeOrientation.PORTRAIT

val DpRect.inferredOrientation: ImeOrientation
    get() = when {
        width <= height -> ImeOrientation.PORTRAIT
        else -> ImeOrientation.LANDSCAPE
    }
