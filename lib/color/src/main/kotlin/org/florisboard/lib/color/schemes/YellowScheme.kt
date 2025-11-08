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

package org.florisboard.lib.color.schemes

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

private val primaryLight = Color(0xFF685F12)
private val onPrimaryLight = Color(0xFFFFFFFF)
private val primaryContainerLight = Color(0xFFF1E48A)
private val onPrimaryContainerLight = Color(0xFF4F4800)
private val secondaryLight = Color(0xFF645F41)
private val onSecondaryLight = Color(0xFFFFFFFF)
private val secondaryContainerLight = Color(0xFFEBE3BD)
private val onSecondaryContainerLight = Color(0xFF4B472B)
private val tertiaryLight = Color(0xFF406652)
private val onTertiaryLight = Color(0xFFFFFFFF)
private val tertiaryContainerLight = Color(0xFFC2ECD3)
private val onTertiaryContainerLight = Color(0xFF294E3B)
private val errorLight = Color(0xFFBA1A1A)
private val onErrorLight = Color(0xFFFFFFFF)
private val errorContainerLight = Color(0xFFFFDAD6)
private val onErrorContainerLight = Color(0xFF93000A)
private val backgroundLight = Color(0xFFFFF9EB)
private val onBackgroundLight = Color(0xFF1D1C13)
private val surfaceLight = Color(0xFFFFF9EB)
private val onSurfaceLight = Color(0xFF1D1C13)
private val surfaceVariantLight = Color(0xFFE8E2D0)
private val onSurfaceVariantLight = Color(0xFF4A473A)
private val outlineLight = Color(0xFF7B7768)
private val outlineVariantLight = Color(0xFFCBC6B5)
private val scrimLight = Color(0xFF000000)
private val inverseSurfaceLight = Color(0xFF323027)
private val inverseOnSurfaceLight = Color(0xFFF6F0E3)
private val inversePrimaryLight = Color(0xFFD4C871)
private val surfaceDimLight = Color(0xFFDFDACC)
private val surfaceBrightLight = Color(0xFFFFF9EB)
private val surfaceContainerLowestLight = Color(0xFFFFFFFF)
private val surfaceContainerLowLight = Color(0xFFF9F3E5)
private val surfaceContainerLight = Color(0xFFF3EEE0)
private val surfaceContainerHighLight = Color(0xFFEDE8DA)
private val surfaceContainerHighestLight = Color(0xFFE7E2D5)

private val primaryDark = Color(0xFFD4C871)
private val onPrimaryDark = Color(0xFF363100)
private val primaryContainerDark = Color(0xFF4F4800)
private val onPrimaryContainerDark = Color(0xFFF1E48A)
private val secondaryDark = Color(0xFFCEC7A3)
private val onSecondaryDark = Color(0xFF343117)
private val secondaryContainerDark = Color(0xFF4B472B)
private val onSecondaryContainerDark = Color(0xFFEBE3BD)
private val tertiaryDark = Color(0xFFA7D0B7)
private val onTertiaryDark = Color(0xFF103726)
private val tertiaryContainerDark = Color(0xFF294E3B)
private val onTertiaryContainerDark = Color(0xFFC2ECD3)
private val errorDark = Color(0xFFFFB4AB)
private val onErrorDark = Color(0xFF690005)
private val errorContainerDark = Color(0xFF93000A)
private val onErrorContainerDark = Color(0xFFFFDAD6)
private val backgroundDark = Color(0xFF15130C)
private val onBackgroundDark = Color(0xFFE7E2D5)
private val surfaceDark = Color(0xFF15130C)
private val onSurfaceDark = Color(0xFFE7E2D5)
private val surfaceVariantDark = Color(0xFF4A473A)
private val onSurfaceVariantDark = Color(0xFFCBC6B5)
private val outlineDark = Color(0xFF959181)
private val outlineVariantDark = Color(0xFF4A473A)
private val scrimDark = Color(0xFF000000)
private val inverseSurfaceDark = Color(0xFFE7E2D5)
private val inverseOnSurfaceDark = Color(0xFF323027)
private val inversePrimaryDark = Color(0xFF685F12)
private val surfaceDimDark = Color(0xFF15130C)
private val surfaceBrightDark = Color(0xFF3B3930)
private val surfaceContainerLowestDark = Color(0xFF0F0E07)
private val surfaceContainerLowDark = Color(0xFF1D1C13)
private val surfaceContainerDark = Color(0xFF212017)
private val surfaceContainerHighDark = Color(0xFF2C2A21)
private val surfaceContainerHighestDark = Color(0xFF37352C)

val yellowLightScheme = lightColorScheme(
    primary = primaryLight,
    onPrimary = onPrimaryLight,
    primaryContainer = primaryContainerLight,
    onPrimaryContainer = onPrimaryContainerLight,
    secondary = secondaryLight,
    onSecondary = onSecondaryLight,
    secondaryContainer = secondaryContainerLight,
    onSecondaryContainer = onSecondaryContainerLight,
    tertiary = tertiaryLight,
    onTertiary = onTertiaryLight,
    tertiaryContainer = tertiaryContainerLight,
    onTertiaryContainer = onTertiaryContainerLight,
    error = errorLight,
    onError = onErrorLight,
    errorContainer = errorContainerLight,
    onErrorContainer = onErrorContainerLight,
    background = backgroundLight,
    onBackground = onBackgroundLight,
    surface = surfaceLight,
    onSurface = onSurfaceLight,
    surfaceVariant = surfaceVariantLight,
    onSurfaceVariant = onSurfaceVariantLight,
    outline = outlineLight,
    outlineVariant = outlineVariantLight,
    scrim = scrimLight,
    inverseSurface = inverseSurfaceLight,
    inverseOnSurface = inverseOnSurfaceLight,
    inversePrimary = inversePrimaryLight,
    surfaceDim = surfaceDimLight,
    surfaceBright = surfaceBrightLight,
    surfaceContainerLowest = surfaceContainerLowestLight,
    surfaceContainerLow = surfaceContainerLowLight,
    surfaceContainer = surfaceContainerLight,
    surfaceContainerHigh = surfaceContainerHighLight,
    surfaceContainerHighest = surfaceContainerHighestLight,
)

val yellowDarkScheme = darkColorScheme(
    primary = primaryDark,
    onPrimary = onPrimaryDark,
    primaryContainer = primaryContainerDark,
    onPrimaryContainer = onPrimaryContainerDark,
    secondary = secondaryDark,
    onSecondary = onSecondaryDark,
    secondaryContainer = secondaryContainerDark,
    onSecondaryContainer = onSecondaryContainerDark,
    tertiary = tertiaryDark,
    onTertiary = onTertiaryDark,
    tertiaryContainer = tertiaryContainerDark,
    onTertiaryContainer = onTertiaryContainerDark,
    error = errorDark,
    onError = onErrorDark,
    errorContainer = errorContainerDark,
    onErrorContainer = onErrorContainerDark,
    background = backgroundDark,
    onBackground = onBackgroundDark,
    surface = surfaceDark,
    onSurface = onSurfaceDark,
    surfaceVariant = surfaceVariantDark,
    onSurfaceVariant = onSurfaceVariantDark,
    outline = outlineDark,
    outlineVariant = outlineVariantDark,
    scrim = scrimDark,
    inverseSurface = inverseSurfaceDark,
    inverseOnSurface = inverseOnSurfaceDark,
    inversePrimary = inversePrimaryDark,
    surfaceDim = surfaceDimDark,
    surfaceBright = surfaceBrightDark,
    surfaceContainerLowest = surfaceContainerLowestDark,
    surfaceContainerLow = surfaceContainerLowDark,
    surfaceContainer = surfaceContainerDark,
    surfaceContainerHigh = surfaceContainerHighDark,
    surfaceContainerHighest = surfaceContainerHighestDark,
)
