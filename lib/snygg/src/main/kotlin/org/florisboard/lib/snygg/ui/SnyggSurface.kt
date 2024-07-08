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

package org.florisboard.lib.snygg.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalAbsoluteTonalElevation
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.takeOrElse
import org.florisboard.lib.snygg.SnyggPropertySet

val NoContentPadding = PaddingValues(all = 0.dp)

@Composable
fun SnyggSurface(
    modifier: Modifier = Modifier,
    style: SnyggPropertySet,
    clip: Boolean = false,
    contentPadding: PaddingValues = NoContentPadding,
    clickAndSemanticsModifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val context = LocalContext.current
    val uiDefaults = LocalSnyggUiDefaults.current
    val elevationDp = style.shadowElevation.dpSize().takeOrElse { 0.dp }.coerceAtLeast(0.dp)
    val contentColor = style.foreground.solidColor(context, default = uiDefaults.fallbackContentColor)
    val absoluteElevation = LocalAbsoluteTonalElevation.current + elevationDp
    CompositionLocalProvider(
        LocalContentColor provides contentColor,
        LocalAbsoluteTonalElevation provides absoluteElevation,
    ) {
        Box(
            modifier = modifier
                .snyggShadow(style)
                .snyggBorder(context, style)
                .then(if (clip) Modifier.snyggClip(style) else Modifier)
                .snyggBackground(context, style)
                .then(clickAndSemanticsModifier)
                .padding(contentPadding),
            propagateMinConstraints = false,
            content = content,
        )
    }
}
