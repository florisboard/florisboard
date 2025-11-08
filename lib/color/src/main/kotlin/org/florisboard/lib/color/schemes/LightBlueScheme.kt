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

private val primaryLight = Color(0xFF27638A)
private val onPrimaryLight = Color(0xFFFFFFFF)
private val primaryContainerLight = Color(0xFFCAE6FF)
private val onPrimaryContainerLight = Color(0xFF004B70)
private val secondaryLight = Color(0xFF50606E)
private val onSecondaryLight = Color(0xFFFFFFFF)
private val secondaryContainerLight = Color(0xFFD3E5F5)
private val onSecondaryContainerLight = Color(0xFF384956)
private val tertiaryLight = Color(0xFF65587B)
private val onTertiaryLight = Color(0xFFFFFFFF)
private val tertiaryContainerLight = Color(0xFFEBDDFF)
private val onTertiaryContainerLight = Color(0xFF4D4162)
private val errorLight = Color(0xFFBA1A1A)
private val onErrorLight = Color(0xFFFFFFFF)
private val errorContainerLight = Color(0xFFFFDAD6)
private val onErrorContainerLight = Color(0xFF93000A)
private val backgroundLight = Color(0xFFF7F9FF)
private val onBackgroundLight = Color(0xFF181C20)
private val surfaceLight = Color(0xFFF7F9FF)
private val onSurfaceLight = Color(0xFF181C20)
private val surfaceVariantLight = Color(0xFFDDE3EA)
private val onSurfaceVariantLight = Color(0xFF41474D)
private val outlineLight = Color(0xFF72787E)
private val outlineVariantLight = Color(0xFFC1C7CE)
private val scrimLight = Color(0xFF000000)
private val inverseSurfaceLight = Color(0xFF2D3135)
private val inverseOnSurfaceLight = Color(0xFFEEF1F6)
private val inversePrimaryLight = Color(0xFF96CDF8)
private val surfaceDimLight = Color(0xFFD7DADF)
private val surfaceBrightLight = Color(0xFFF7F9FF)
private val surfaceContainerLowestLight = Color(0xFFFFFFFF)
private val surfaceContainerLowLight = Color(0xFFF1F4F9)
private val surfaceContainerLight = Color(0xFFEBEEF3)
private val surfaceContainerHighLight = Color(0xFFE5E8ED)
private val surfaceContainerHighestLight = Color(0xFFE0E3E8)

private val primaryDark = Color(0xFF96CDF8)
private val onPrimaryDark = Color(0xFF00344F)
private val primaryContainerDark = Color(0xFF004B70)
private val onPrimaryContainerDark = Color(0xFFCAE6FF)
private val secondaryDark = Color(0xFFB7C9D9)
private val onSecondaryDark = Color(0xFF22323F)
private val secondaryContainerDark = Color(0xFF384956)
private val onSecondaryContainerDark = Color(0xFFD3E5F5)
private val tertiaryDark = Color(0xFFCFC0E8)
private val onTertiaryDark = Color(0xFF362B4B)
private val tertiaryContainerDark = Color(0xFF4D4162)
private val onTertiaryContainerDark = Color(0xFFEBDDFF)
private val errorDark = Color(0xFFFFB4AB)
private val onErrorDark = Color(0xFF690005)
private val errorContainerDark = Color(0xFF93000A)
private val onErrorContainerDark = Color(0xFFFFDAD6)
private val backgroundDark = Color(0xFF101417)
private val onBackgroundDark = Color(0xFFE0E3E8)
private val surfaceDark = Color(0xFF101417)
private val onSurfaceDark = Color(0xFFE0E3E8)
private val surfaceVariantDark = Color(0xFF41474D)
private val onSurfaceVariantDark = Color(0xFFC1C7CE)
private val outlineDark = Color(0xFF8B9198)
private val outlineVariantDark = Color(0xFF41474D)
private val scrimDark = Color(0xFF000000)
private val inverseSurfaceDark = Color(0xFFE0E3E8)
private val inverseOnSurfaceDark = Color(0xFF2D3135)
private val inversePrimaryDark = Color(0xFF27638A)
private val surfaceDimDark = Color(0xFF101417)
private val surfaceBrightDark = Color(0xFF363A3E)
private val surfaceContainerLowestDark = Color(0xFF0B0F12)
private val surfaceContainerLowDark = Color(0xFF181C20)
private val surfaceContainerDark = Color(0xFF1C2024)
private val surfaceContainerHighDark = Color(0xFF262A2E)
private val surfaceContainerHighestDark = Color(0xFF313539)

val lightBlueLightScheme = lightColorScheme(
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

val lightBlueDarkScheme = darkColorScheme(
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
