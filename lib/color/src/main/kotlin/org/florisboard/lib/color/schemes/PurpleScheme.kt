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

private val primaryLight = Color(0xFF7B4E7F)
private val onPrimaryLight = Color(0xFFFFFFFF)
private val primaryContainerLight = Color(0xFFFFD6FE)
private val onPrimaryContainerLight = Color(0xFF613766)
private val secondaryLight = Color(0xFF6B586B)
private val onSecondaryLight = Color(0xFFFFFFFF)
private val secondaryContainerLight = Color(0xFFF4DBF1)
private val onSecondaryContainerLight = Color(0xFF534153)
private val tertiaryLight = Color(0xFF82524A)
private val onTertiaryLight = Color(0xFFFFFFFF)
private val tertiaryContainerLight = Color(0xFFFFDAD4)
private val onTertiaryContainerLight = Color(0xFF673B34)
private val errorLight = Color(0xFFBA1A1A)
private val onErrorLight = Color(0xFFFFFFFF)
private val errorContainerLight = Color(0xFFFFDAD6)
private val onErrorContainerLight = Color(0xFF93000A)
private val backgroundLight = Color(0xFFFFF7FA)
private val onBackgroundLight = Color(0xFF1F1A1F)
private val surfaceLight = Color(0xFFFFF7FA)
private val onSurfaceLight = Color(0xFF1F1A1F)
private val surfaceVariantLight = Color(0xFFECDFE8)
private val onSurfaceVariantLight = Color(0xFF4D444C)
private val outlineLight = Color(0xFF7F747D)
private val outlineVariantLight = Color(0xFFD0C3CC)
private val scrimLight = Color(0xFF000000)
private val inverseSurfaceLight = Color(0xFF352F34)
private val inverseOnSurfaceLight = Color(0xFFF9EEF5)
private val inversePrimaryLight = Color(0xFFEBB5ED)
private val surfaceDimLight = Color(0xFFE2D7DE)
private val surfaceBrightLight = Color(0xFFFFF7FA)
private val surfaceContainerLowestLight = Color(0xFFFFFFFF)
private val surfaceContainerLowLight = Color(0xFFFCF0F7)
private val surfaceContainerLight = Color(0xFFF6EBF2)
private val surfaceContainerHighLight = Color(0xFFF0E5EC)
private val surfaceContainerHighestLight = Color(0xFFEBDFE6)

private val primaryDark = Color(0xFFEBB5ED)
private val onPrimaryDark = Color(0xFF49204E)
private val primaryContainerDark = Color(0xFF613766)
private val onPrimaryContainerDark = Color(0xFFFFD6FE)
private val secondaryDark = Color(0xFFD7BFD5)
private val onSecondaryDark = Color(0xFF3B2B3C)
private val secondaryContainerDark = Color(0xFF534153)
private val onSecondaryContainerDark = Color(0xFFF4DBF1)
private val tertiaryDark = Color(0xFFF6B8AD)
private val onTertiaryDark = Color(0xFF4C251F)
private val tertiaryContainerDark = Color(0xFF673B34)
private val onTertiaryContainerDark = Color(0xFFFFDAD4)
private val errorDark = Color(0xFFFFB4AB)
private val onErrorDark = Color(0xFF690005)
private val errorContainerDark = Color(0xFF93000A)
private val onErrorContainerDark = Color(0xFFFFDAD6)
private val backgroundDark = Color(0xFF171216)
private val onBackgroundDark = Color(0xFFEBDFE6)
private val surfaceDark = Color(0xFF171216)
private val onSurfaceDark = Color(0xFFEBDFE6)
private val surfaceVariantDark = Color(0xFF4D444C)
private val onSurfaceVariantDark = Color(0xFFD0C3CC)
private val outlineDark = Color(0xFF998D96)
private val outlineVariantDark = Color(0xFF4D444C)
private val scrimDark = Color(0xFF000000)
private val inverseSurfaceDark = Color(0xFFEBDFE6)
private val inverseOnSurfaceDark = Color(0xFF352F34)
private val inversePrimaryDark = Color(0xFF7B4E7F)
private val surfaceDimDark = Color(0xFF171216)
private val surfaceBrightDark = Color(0xFF3E373D)
private val surfaceContainerLowestDark = Color(0xFF110D11)
private val surfaceContainerLowDark = Color(0xFF1F1A1F)
private val surfaceContainerDark = Color(0xFF231E23)
private val surfaceContainerHighDark = Color(0xFF2E282D)
private val surfaceContainerHighestDark = Color(0xFF393338)

val purpleLightScheme = lightColorScheme(
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

val purpleDarkScheme = darkColorScheme(
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
