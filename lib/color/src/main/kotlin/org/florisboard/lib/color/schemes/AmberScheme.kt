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

private val primaryLight = Color(0xFF775A0B)
private val onPrimaryLight = Color(0xFFFFFFFF)
private val primaryContainerLight = Color(0xFFFFDF9E)
private val onPrimaryContainerLight = Color(0xFF5B4300)
private val secondaryLight = Color(0xFF6B5D3F)
private val onSecondaryLight = Color(0xFFFFFFFF)
private val secondaryContainerLight = Color(0xFFF5E0BB)
private val onSecondaryContainerLight = Color(0xFF52452A)
private val tertiaryLight = Color(0xFF4A6547)
private val onTertiaryLight = Color(0xFFFFFFFF)
private val tertiaryContainerLight = Color(0xFFCCEBC4)
private val onTertiaryContainerLight = Color(0xFF334D31)
private val errorLight = Color(0xFFBA1A1A)
private val onErrorLight = Color(0xFFFFFFFF)
private val errorContainerLight = Color(0xFFFFDAD6)
private val onErrorContainerLight = Color(0xFF93000A)
private val backgroundLight = Color(0xFFFFF8F2)
private val onBackgroundLight = Color(0xFF1F1B13)
private val surfaceLight = Color(0xFFFFF8F2)
private val onSurfaceLight = Color(0xFF1F1B13)
private val surfaceVariantLight = Color(0xFFEDE1CF)
private val onSurfaceVariantLight = Color(0xFF4D4639)
private val outlineLight = Color(0xFF7F7667)
private val outlineVariantLight = Color(0xFFD0C5B4)
private val scrimLight = Color(0xFF000000)
private val inverseSurfaceLight = Color(0xFF353027)
private val inverseOnSurfaceLight = Color(0xFFF9EFE2)
private val inversePrimaryLight = Color(0xFFE9C16C)
private val surfaceDimLight = Color(0xFFE2D9CC)
private val surfaceBrightLight = Color(0xFFFFF8F2)
private val surfaceContainerLowestLight = Color(0xFFFFFFFF)
private val surfaceContainerLowLight = Color(0xFFFCF2E5)
private val surfaceContainerLight = Color(0xFFF6ECDF)
private val surfaceContainerHighLight = Color(0xFFF1E7D9)
private val surfaceContainerHighestLight = Color(0xFFEBE1D4)
private val primaryDark = Color(0xFFE9C16C)
private val onPrimaryDark = Color(0xFF3F2E00)
private val primaryContainerDark = Color(0xFF5B4300)
private val onPrimaryContainerDark = Color(0xFFFFDF9E)
private val secondaryDark = Color(0xFFD8C4A0)
private val onSecondaryDark = Color(0xFF3A2F15)
private val secondaryContainerDark = Color(0xFF52452A)
private val onSecondaryContainerDark = Color(0xFFF5E0BB)
private val tertiaryDark = Color(0xFFB0CFAA)
private val onTertiaryDark = Color(0xFF1D361C)
private val tertiaryContainerDark = Color(0xFF334D31)
private val onTertiaryContainerDark = Color(0xFFCCEBC4)
private val errorDark = Color(0xFFFFB4AB)
private val onErrorDark = Color(0xFF690005)
private val errorContainerDark = Color(0xFF93000A)
private val onErrorContainerDark = Color(0xFFFFDAD6)
private val backgroundDark = Color(0xFF17130B)
private val onBackgroundDark = Color(0xFFEBE1D4)
private val surfaceDark = Color(0xFF17130B)
private val onSurfaceDark = Color(0xFFEBE1D4)
private val surfaceVariantDark = Color(0xFF4D4639)
private val onSurfaceVariantDark = Color(0xFFD0C5B4)
private val outlineDark = Color(0xFF998F80)
private val outlineVariantDark = Color(0xFF4D4639)
private val scrimDark = Color(0xFF000000)
private val inverseSurfaceDark = Color(0xFFEBE1D4)
private val inverseOnSurfaceDark = Color(0xFF353027)
private val inversePrimaryDark = Color(0xFF775A0B)
private val surfaceDimDark = Color(0xFF17130B)
private val surfaceBrightDark = Color(0xFF3E382F)
private val surfaceContainerLowestDark = Color(0xFF110E07)
private val surfaceContainerLowDark = Color(0xFF1F1B13)
private val surfaceContainerDark = Color(0xFF231F17)
private val surfaceContainerHighDark = Color(0xFF2E2921)
private val surfaceContainerHighestDark = Color(0xFF39342B)

val amberLightScheme = lightColorScheme(
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

val amberDarkScheme = darkColorScheme(
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
