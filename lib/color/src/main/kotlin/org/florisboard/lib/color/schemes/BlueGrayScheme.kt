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

private val primaryLight = Color(0xFF116682)
private val onPrimaryLight = Color(0xFFFFFFFF)
private val primaryContainerLight = Color(0xFFBDE9FF)
private val onPrimaryContainerLight = Color(0xFF004D64)
private val secondaryLight = Color(0xFF4D616C)
private val onSecondaryLight = Color(0xFFFFFFFF)
private val secondaryContainerLight = Color(0xFFD0E6F2)
private val onSecondaryContainerLight = Color(0xFF354A53)
private val tertiaryLight = Color(0xFF5D5B7D)
private val onTertiaryLight = Color(0xFFFFFFFF)
private val tertiaryContainerLight = Color(0xFFE3DFFF)
private val onTertiaryContainerLight = Color(0xFF454364)
private val errorLight = Color(0xFFBA1A1A)
private val onErrorLight = Color(0xFFFFFFFF)
private val errorContainerLight = Color(0xFFFFDAD6)
private val onErrorContainerLight = Color(0xFF93000A)
private val backgroundLight = Color(0xFFF6FAFD)
private val onBackgroundLight = Color(0xFF171C1F)
private val surfaceLight = Color(0xFFF6FAFD)
private val onSurfaceLight = Color(0xFF171C1F)
private val surfaceVariantLight = Color(0xFFDCE4E9)
private val onSurfaceVariantLight = Color(0xFF40484C)
private val outlineLight = Color(0xFF70787D)
private val outlineVariantLight = Color(0xFFC0C8CD)
private val scrimLight = Color(0xFF000000)
private val inverseSurfaceLight = Color(0xFF2C3134)
private val inverseOnSurfaceLight = Color(0xFFEDF1F5)
private val inversePrimaryLight = Color(0xFF8BD0EF)
private val surfaceDimLight = Color(0xFFD6DBDE)
private val surfaceBrightLight = Color(0xFFF6FAFD)
private val surfaceContainerLowestLight = Color(0xFFFFFFFF)
private val surfaceContainerLowLight = Color(0xFFF0F4F8)
private val surfaceContainerLight = Color(0xFFEAEEF2)
private val surfaceContainerHighLight = Color(0xFFE4E9EC)
private val surfaceContainerHighestLight = Color(0xFFDFE3E7)
private val primaryDark = Color(0xFF8BD0EF)
private val onPrimaryDark = Color(0xFF003546)
private val primaryContainerDark = Color(0xFF004D64)
private val onPrimaryContainerDark = Color(0xFFBDE9FF)
private val secondaryDark = Color(0xFFB4CAD6)
private val onSecondaryDark = Color(0xFF1F333C)
private val secondaryContainerDark = Color(0xFF354A53)
private val onSecondaryContainerDark = Color(0xFFD0E6F2)
private val tertiaryDark = Color(0xFFC6C2EA)
private val onTertiaryDark = Color(0xFF2E2D4D)
private val tertiaryContainerDark = Color(0xFF454364)
private val onTertiaryContainerDark = Color(0xFFE3DFFF)
private val errorDark = Color(0xFFFFB4AB)
private val onErrorDark = Color(0xFF690005)
private val errorContainerDark = Color(0xFF93000A)
private val onErrorContainerDark = Color(0xFFFFDAD6)
private val backgroundDark = Color(0xFF0F1417)
private val onBackgroundDark = Color(0xFFDFE3E7)
private val surfaceDark = Color(0xFF0F1417)
private val onSurfaceDark = Color(0xFFDFE3E7)
private val surfaceVariantDark = Color(0xFF40484C)
private val onSurfaceVariantDark = Color(0xFFC0C8CD)
private val outlineDark = Color(0xFF8A9297)
private val outlineVariantDark = Color(0xFF40484C)
private val scrimDark = Color(0xFF000000)
private val inverseSurfaceDark = Color(0xFFDFE3E7)
private val inverseOnSurfaceDark = Color(0xFF2C3134)
private val inversePrimaryDark = Color(0xFF116682)
private val surfaceDimDark = Color(0xFF0F1417)
private val surfaceBrightDark = Color(0xFF353A3D)
private val surfaceContainerLowestDark = Color(0xFF0A0F11)
private val surfaceContainerLowDark = Color(0xFF171C1F)
private val surfaceContainerDark = Color(0xFF1B2023)
private val surfaceContainerHighDark = Color(0xFF262B2D)
private val surfaceContainerHighestDark = Color(0xFF303538)


val blueGrayLightScheme = lightColorScheme(
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

val blueGrayDarkScheme = darkColorScheme(
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
