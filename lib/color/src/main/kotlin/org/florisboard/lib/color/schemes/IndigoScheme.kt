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

private val primaryLight = Color(0xFF515B92)
private val onPrimaryLight = Color(0xFFFFFFFF)
private val primaryContainerLight = Color(0xFFDEE0FF)
private val onPrimaryContainerLight = Color(0xFF394379)
private val secondaryLight = Color(0xFF5B5D72)
private val onSecondaryLight = Color(0xFFFFFFFF)
private val secondaryContainerLight = Color(0xFFE0E1F9)
private val onSecondaryContainerLight = Color(0xFF434659)
private val tertiaryLight = Color(0xFF77536D)
private val onTertiaryLight = Color(0xFFFFFFFF)
private val tertiaryContainerLight = Color(0xFFFFD7F1)
private val onTertiaryContainerLight = Color(0xFF5D3C55)
private val errorLight = Color(0xFFBA1A1A)
private val onErrorLight = Color(0xFFFFFFFF)
private val errorContainerLight = Color(0xFFFFDAD6)
private val onErrorContainerLight = Color(0xFF93000A)
private val backgroundLight = Color(0xFFFBF8FF)
private val onBackgroundLight = Color(0xFF1B1B21)
private val surfaceLight = Color(0xFFFBF8FF)
private val onSurfaceLight = Color(0xFF1B1B21)
private val surfaceVariantLight = Color(0xFFE3E1EC)
private val onSurfaceVariantLight = Color(0xFF46464F)
private val outlineLight = Color(0xFF767680)
private val outlineVariantLight = Color(0xFFC7C5D0)
private val scrimLight = Color(0xFF000000)
private val inverseSurfaceLight = Color(0xFF303036)
private val inverseOnSurfaceLight = Color(0xFFF2EFF7)
private val inversePrimaryLight = Color(0xFFBAC3FF)
private val surfaceDimLight = Color(0xFFDBD9E0)
private val surfaceBrightLight = Color(0xFFFBF8FF)
private val surfaceContainerLowestLight = Color(0xFFFFFFFF)
private val surfaceContainerLowLight = Color(0xFFF5F2FA)
private val surfaceContainerLight = Color(0xFFEFEDF4)
private val surfaceContainerHighLight = Color(0xFFE9E7EF)
private val surfaceContainerHighestLight = Color(0xFFE4E1E9)

private val primaryDark = Color(0xFFBAC3FF)
private val onPrimaryDark = Color(0xFF222C61)
private val primaryContainerDark = Color(0xFF394379)
private val onPrimaryContainerDark = Color(0xFFDEE0FF)
private val secondaryDark = Color(0xFFC3C5DD)
private val onSecondaryDark = Color(0xFF2D2F42)
private val secondaryContainerDark = Color(0xFF434659)
private val onSecondaryContainerDark = Color(0xFFE0E1F9)
private val tertiaryDark = Color(0xFFE6BAD7)
private val onTertiaryDark = Color(0xFF44263D)
private val tertiaryContainerDark = Color(0xFF5D3C55)
private val onTertiaryContainerDark = Color(0xFFFFD7F1)
private val errorDark = Color(0xFFFFB4AB)
private val onErrorDark = Color(0xFF690005)
private val errorContainerDark = Color(0xFF93000A)
private val onErrorContainerDark = Color(0xFFFFDAD6)
private val backgroundDark = Color(0xFF121318)
private val onBackgroundDark = Color(0xFFE4E1E9)
private val surfaceDark = Color(0xFF121318)
private val onSurfaceDark = Color(0xFFE4E1E9)
private val surfaceVariantDark = Color(0xFF46464F)
private val onSurfaceVariantDark = Color(0xFFC7C5D0)
private val outlineDark = Color(0xFF90909A)
private val outlineVariantDark = Color(0xFF46464F)
private val scrimDark = Color(0xFF000000)
private val inverseSurfaceDark = Color(0xFFE4E1E9)
private val inverseOnSurfaceDark = Color(0xFF303036)
private val inversePrimaryDark = Color(0xFF515B92)
private val surfaceDimDark = Color(0xFF121318)
private val surfaceBrightDark = Color(0xFF39393F)
private val surfaceContainerLowestDark = Color(0xFF0D0E13)
private val surfaceContainerLowDark = Color(0xFF1B1B21)
private val surfaceContainerDark = Color(0xFF1F1F25)
private val surfaceContainerHighDark = Color(0xFF29292F)
private val surfaceContainerHighestDark = Color(0xFF34343A)

val indigoLightScheme = lightColorScheme(
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

val indigoDarkScheme = darkColorScheme(
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
