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

private val primaryLight = Color(0xFF8F4C33)
private val onPrimaryLight = Color(0xFFFFFFFF)
private val primaryContainerLight = Color(0xFFFFDBCF)
private val onPrimaryContainerLight = Color(0xFF72361E)
private val secondaryLight = Color(0xFF77574C)
private val onSecondaryLight = Color(0xFFFFFFFF)
private val secondaryContainerLight = Color(0xFFFFDBCF)
private val onSecondaryContainerLight = Color(0xFF5D4035)
private val tertiaryLight = Color(0xFF695E2F)
private val onTertiaryLight = Color(0xFFFFFFFF)
private val tertiaryContainerLight = Color(0xFFF2E2A8)
private val onTertiaryContainerLight = Color(0xFF50471A)
private val errorLight = Color(0xFFBA1A1A)
private val onErrorLight = Color(0xFFFFFFFF)
private val errorContainerLight = Color(0xFFFFDAD6)
private val onErrorContainerLight = Color(0xFF93000A)
private val backgroundLight = Color(0xFFFFF8F6)
private val onBackgroundLight = Color(0xFF231A16)
private val surfaceLight = Color(0xFFFFF8F6)
private val onSurfaceLight = Color(0xFF231A16)
private val surfaceVariantLight = Color(0xFFF5DED6)
private val onSurfaceVariantLight = Color(0xFF53433E)
private val outlineLight = Color(0xFF85736D)
private val outlineVariantLight = Color(0xFFD8C2BB)
private val scrimLight = Color(0xFF000000)
private val inverseSurfaceLight = Color(0xFF392E2B)
private val inverseOnSurfaceLight = Color(0xFFFFEDE7)
private val inversePrimaryLight = Color(0xFFFFB59A)
private val surfaceDimLight = Color(0xFFE8D6D1)
private val surfaceBrightLight = Color(0xFFFFF8F6)
private val surfaceContainerLowestLight = Color(0xFFFFFFFF)
private val surfaceContainerLowLight = Color(0xFFFFF1EC)
private val surfaceContainerLight = Color(0xFFFCEAE4)
private val surfaceContainerHighLight = Color(0xFFF7E4DF)
private val surfaceContainerHighestLight = Color(0xFFF1DFD9)
private val primaryDark = Color(0xFFFFB59A)
private val onPrimaryDark = Color(0xFF55200A)
private val primaryContainerDark = Color(0xFF72361E)
private val onPrimaryContainerDark = Color(0xFFFFDBCF)
private val secondaryDark = Color(0xFFE7BEAF)
private val onSecondaryDark = Color(0xFF442A20)
private val secondaryContainerDark = Color(0xFF5D4035)
private val onSecondaryContainerDark = Color(0xFFFFDBCF)
private val tertiaryDark = Color(0xFFD5C68E)
private val onTertiaryDark = Color(0xFF393005)
private val tertiaryContainerDark = Color(0xFF50471A)
private val onTertiaryContainerDark = Color(0xFFF2E2A8)
private val errorDark = Color(0xFFFFB4AB)
private val onErrorDark = Color(0xFF690005)
private val errorContainerDark = Color(0xFF93000A)
private val onErrorContainerDark = Color(0xFFFFDAD6)
private val backgroundDark = Color(0xFF1A110E)
private val onBackgroundDark = Color(0xFFF1DFD9)
private val surfaceDark = Color(0xFF1A110E)
private val onSurfaceDark = Color(0xFFF1DFD9)
private val surfaceVariantDark = Color(0xFF53433E)
private val onSurfaceVariantDark = Color(0xFFD8C2BB)
private val outlineDark = Color(0xFFA08D86)
private val outlineVariantDark = Color(0xFF53433E)
private val scrimDark = Color(0xFF000000)
private val inverseSurfaceDark = Color(0xFFF1DFD9)
private val inverseOnSurfaceDark = Color(0xFF392E2B)
private val inversePrimaryDark = Color(0xFF8F4C33)
private val surfaceDimDark = Color(0xFF1A110E)
private val surfaceBrightDark = Color(0xFF423733)
private val surfaceContainerLowestDark = Color(0xFF140C09)
private val surfaceContainerLowDark = Color(0xFF231A16)
private val surfaceContainerDark = Color(0xFF271E1A)
private val surfaceContainerHighDark = Color(0xFF322824)
private val surfaceContainerHighestDark = Color(0xFF3D322F)

val brownLightScheme = lightColorScheme(
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

val brownDarkScheme = darkColorScheme(
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
