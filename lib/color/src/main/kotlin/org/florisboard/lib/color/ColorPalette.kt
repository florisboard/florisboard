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

package org.florisboard.lib.color

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color

enum class ColorPalette(val id: String) {
    Primary("primary"),
    OnPrimary("onPrimary"),
    PrimaryContainer("primaryContainer"),
    OnPrimaryContainer("onPrimaryContainer"),
    InversePrimary("inversePrimary"),
    Secondary("secondary"),
    OnSecondary("onSecondary"),
    SecondaryContainer("secondaryContainer"),
    OnSecondaryContainer("onSecondaryContainer"),
    Tertiary("tertiary"),
    OnTertiary("onTertiary"),
    TertiaryContainer("tertiaryContainer"),
    OnTertiaryContainer("onTertiaryContainer"),
    Background("background"),
    OnBackground("onBackground"),
    // Surface("surface"), // removed and replaced by the specific Surface* variants
    OnSurface("onSurface"),
    SurfaceVariant("surfaceVariant"),
    OnSurfaceVariant("onSurfaceVariant"),
    SurfaceTint("surfaceTint"),
    InverseSurface("inverseSurface"),
    InverseOnSurface("inverseOnSurface"),
    Error("error"),
    OnError("onError"),
    ErrorContainer("errorContainer"),
    OnErrorContainer("onErrorContainer"),
    Outline("outline"),
    OutlineVariant("outlineVariant"),
    Scrim("scrim"),
    SurfaceBright("surfaceBright"),
    SurfaceDim("surfaceDim"),
    SurfaceContainer("surfaceContainer"),
    SurfaceContainerHigh("surfaceContainerHigh"),
    SurfaceContainerHighest("surfaceContainerHighest"),
    SurfaceContainerLow("surfaceContainerLow"),
    SurfaceContainerLowest("surfaceContainerLowest");

    companion object {
        val colorNames = entries.map { it.id }
    }
}

fun ColorScheme.getColor(id: String): Color {
    return when (id) {
        ColorPalette.Primary.id -> this.primary
        ColorPalette.OnPrimary.id -> this.onPrimary
        ColorPalette.PrimaryContainer.id -> this.primaryContainer
        ColorPalette.OnPrimaryContainer.id -> this.onPrimaryContainer
        ColorPalette.InversePrimary.id -> this.inversePrimary
        ColorPalette.Secondary.id -> this.secondary
        ColorPalette.OnSecondary.id -> this.onSecondary
        ColorPalette.SecondaryContainer.id -> this.secondaryContainer
        ColorPalette.OnSecondaryContainer.id -> this.onSecondaryContainer
        ColorPalette.Tertiary.id -> this.tertiary
        ColorPalette.OnTertiary.id -> this.onTertiary
        ColorPalette.TertiaryContainer.id -> this.tertiaryContainer
        ColorPalette.OnTertiaryContainer.id -> this.onTertiaryContainer
        ColorPalette.Background.id -> this.background
        ColorPalette.OnBackground.id -> this.onBackground
        // ColorPalette.Surface.id -> colorScheme.surface
        ColorPalette.OnSurface.id -> this.onSurface
        ColorPalette.SurfaceVariant.id -> this.surfaceVariant
        ColorPalette.OnSurfaceVariant.id -> this.onSurfaceVariant
        ColorPalette.SurfaceTint.id -> this.surfaceTint
        ColorPalette.InverseSurface.id -> this.inverseSurface
        ColorPalette.InverseOnSurface.id -> this.inverseOnSurface
        ColorPalette.Error.id -> this.error
        ColorPalette.OnError.id -> this.onError
        ColorPalette.ErrorContainer.id -> this.errorContainer
        ColorPalette.OnErrorContainer.id -> this.onErrorContainer
        ColorPalette.Outline.id -> this.outline
        ColorPalette.OutlineVariant.id -> this.outlineVariant
        ColorPalette.Scrim.id -> this.scrim
        ColorPalette.SurfaceBright.id -> this.surfaceBright
        ColorPalette.SurfaceDim.id -> this.surfaceDim
        ColorPalette.SurfaceContainer.id -> this.surfaceContainer
        ColorPalette.SurfaceContainerHigh.id -> this.surfaceContainerHigh
        ColorPalette.SurfaceContainerHighest.id -> this.surfaceContainerHighest
        ColorPalette.SurfaceContainerLow.id -> this.surfaceContainerLow
        ColorPalette.SurfaceContainerLowest.id -> this.surfaceContainerLowest
        else -> this.primary
    }
}
