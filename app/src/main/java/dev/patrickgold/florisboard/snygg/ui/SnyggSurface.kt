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

package dev.patrickgold.florisboard.snygg.ui

import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.patrickgold.florisboard.snygg.value.SnyggValue

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun SnyggSurface(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    background: SnyggValue,
    border: SnyggValue? = null,
    elevation: Dp = 0.dp,
    enabled: Boolean = true,
    shape: SnyggValue? = null,
    content: @Composable () -> Unit
) {
    if (onClick != null) {
        Surface(
            modifier = modifier,
            onClick = onClick,
            color = background.solidColor(), // TODO: support gradients and images
            elevation = elevation,
            enabled = enabled,
            content = content,
        )
    } else {
        Surface(
            modifier = modifier,
            color = background.solidColor(), // TODO: support gradients and images
            elevation = elevation,
            content = content,
        )
    }
}
