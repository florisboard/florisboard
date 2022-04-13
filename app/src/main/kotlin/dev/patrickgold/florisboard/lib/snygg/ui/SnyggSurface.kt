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

package dev.patrickgold.florisboard.lib.snygg.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.LocalAbsoluteElevation
import androidx.compose.material.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.takeOrElse
import dev.patrickgold.florisboard.ime.theme.FlorisImeTheme
import dev.patrickgold.florisboard.lib.snygg.SnyggPropertySet

val NoContentPadding = PaddingValues(all = 0.dp)

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun SnyggSurface(
    modifier: Modifier = Modifier,
    style: SnyggPropertySet,
    clip: Boolean = false,
    contentPadding: PaddingValues = NoContentPadding,
    clickAndSemanticsModifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val elevationDp = style.shadowElevation.dpSize().takeOrElse { 0.dp }.coerceAtLeast(0.dp)
    val contentColor = style.foreground.solidColor(default = FlorisImeTheme.fallbackContentColor())
    val absoluteElevation = LocalAbsoluteElevation.current + elevationDp
    CompositionLocalProvider(
        LocalContentColor provides contentColor,
        LocalAbsoluteElevation provides absoluteElevation,
    ) {
        Box(
            modifier = modifier
                .snyggShadow(style)
                .snyggBorder(style)
                .then(if (clip) Modifier.snyggClip(style) else Modifier)
                .snyggBackground(style)
                .then(clickAndSemanticsModifier)
                .padding(contentPadding),
            propagateMinConstraints = false,
            content = content,
        )
    }
}
