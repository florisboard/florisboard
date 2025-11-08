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


private val primaryLight = Color(0xFF4B662C)
private val onPrimaryLight = Color(0xFFFFFFFF)
private val primaryContainerLight = Color(0xFFCCEDA4)
private val onPrimaryContainerLight = Color(0xFF344E16)
private val secondaryLight = Color(0xFF576249)
private val onSecondaryLight = Color(0xFFFFFFFF)
private val secondaryContainerLight = Color(0xFFDBE7C8)
private val onSecondaryContainerLight = Color(0xFF404A33)
private val tertiaryLight = Color(0xFF386663)
private val onTertiaryLight = Color(0xFFFFFFFF)
private val tertiaryContainerLight = Color(0xFFBBECE8)
private val onTertiaryContainerLight = Color(0xFF1F4E4B)
private val errorLight = Color(0xFFBA1A1A)
private val onErrorLight = Color(0xFFFFFFFF)
private val errorContainerLight = Color(0xFFFFDAD6)
private val onErrorContainerLight = Color(0xFF93000A)
private val backgroundLight = Color(0xFFF9FAEF)
private val onBackgroundLight = Color(0xFF1A1C16)
private val surfaceLight = Color(0xFFF9FAEF)
private val onSurfaceLight = Color(0xFF1A1C16)
private val surfaceVariantLight = Color(0xFFE1E4D5)
private val onSurfaceVariantLight = Color(0xFF44483D)
private val outlineLight = Color(0xFF75796C)
private val outlineVariantLight = Color(0xFFC5C8BA)
private val scrimLight = Color(0xFF000000)
private val inverseSurfaceLight = Color(0xFF2F312A)
private val inverseOnSurfaceLight = Color(0xFFF0F2E6)
private val inversePrimaryLight = Color(0xFFB1D18A)
private val surfaceDimLight = Color(0xFFDADBD0)
private val surfaceBrightLight = Color(0xFFF9FAEF)
private val surfaceContainerLowestLight = Color(0xFFFFFFFF)
private val surfaceContainerLowLight = Color(0xFFF3F4E9)
private val surfaceContainerLight = Color(0xFFEEEFE4)
private val surfaceContainerHighLight = Color(0xFFE8E9DE)
private val surfaceContainerHighestLight = Color(0xFFE2E3D8)

private val primaryDark = Color(0xFFB1D18A)
private val onPrimaryDark = Color(0xFF1F3701)
private val primaryContainerDark = Color(0xFF344E16)
private val onPrimaryContainerDark = Color(0xFFCCEDA4)
private val secondaryDark = Color(0xFFBFCBAD)
private val onSecondaryDark = Color(0xFF2A331E)
private val secondaryContainerDark = Color(0xFF404A33)
private val onSecondaryContainerDark = Color(0xFFDBE7C8)
private val tertiaryDark = Color(0xFFA0D0CC)
private val onTertiaryDark = Color(0xFF003735)
private val tertiaryContainerDark = Color(0xFF1F4E4B)
private val onTertiaryContainerDark = Color(0xFFBBECE8)
private val errorDark = Color(0xFFFFB4AB)
private val onErrorDark = Color(0xFF690005)
private val errorContainerDark = Color(0xFF93000A)
private val onErrorContainerDark = Color(0xFFFFDAD6)
private val backgroundDark = Color(0xFF12140E)
private val onBackgroundDark = Color(0xFFE2E3D8)
private val surfaceDark = Color(0xFF12140E)
private val onSurfaceDark = Color(0xFFE2E3D8)
private val surfaceVariantDark = Color(0xFF44483D)
private val onSurfaceVariantDark = Color(0xFFC5C8BA)
private val outlineDark = Color(0xFF8E9285)
private val outlineVariantDark = Color(0xFF44483D)
private val scrimDark = Color(0xFF000000)
private val inverseSurfaceDark = Color(0xFFE2E3D8)
private val inverseOnSurfaceDark = Color(0xFF2F312A)
private val inversePrimaryDark = Color(0xFF4B662C)
private val surfaceDimDark = Color(0xFF12140E)
private val surfaceBrightDark = Color(0xFF383A32)
private val surfaceContainerLowestDark = Color(0xFF0C0F09)
private val surfaceContainerLowDark = Color(0xFF1A1C16)
private val surfaceContainerDark = Color(0xFF1E201A)
private val surfaceContainerHighDark = Color(0xFF282B24)
private val surfaceContainerHighestDark = Color(0xFF33362E)

val lightGreenLightScheme = lightColorScheme(
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

val lightGreenDarkScheme = darkColorScheme(
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
