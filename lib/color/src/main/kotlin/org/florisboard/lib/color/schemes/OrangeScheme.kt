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

private val primaryLight = Color(0xFF855318)
private val onPrimaryLight = Color(0xFFFFFFFF)
private val primaryContainerLight = Color(0xFFFFDCBE)
private val onPrimaryContainerLight = Color(0xFF693C00)
private val secondaryLight = Color(0xFF725A42)
private val onSecondaryLight = Color(0xFFFFFFFF)
private val secondaryContainerLight = Color(0xFFFFDCBE)
private val onSecondaryContainerLight = Color(0xFF59422C)
private val tertiaryLight = Color(0xFF58633A)
private val onTertiaryLight = Color(0xFFFFFFFF)
private val tertiaryContainerLight = Color(0xFFDCE8B4)
private val onTertiaryContainerLight = Color(0xFF414B24)
private val errorLight = Color(0xFFBA1A1A)
private val onErrorLight = Color(0xFFFFFFFF)
private val errorContainerLight = Color(0xFFFFDAD6)
private val onErrorContainerLight = Color(0xFF93000A)
private val backgroundLight = Color(0xFFFFF8F5)
private val onBackgroundLight = Color(0xFF211A14)
private val surfaceLight = Color(0xFFFFF8F5)
private val onSurfaceLight = Color(0xFF211A14)
private val surfaceVariantLight = Color(0xFFF2DFD1)
private val onSurfaceVariantLight = Color(0xFF51453A)
private val outlineLight = Color(0xFF837468)
private val outlineVariantLight = Color(0xFFD5C3B5)
private val scrimLight = Color(0xFF000000)
private val inverseSurfaceLight = Color(0xFF372F28)
private val inverseOnSurfaceLight = Color(0xFFFDEEE3)
private val inversePrimaryLight = Color(0xFFFDB975)
private val surfaceDimLight = Color(0xFFE6D7CD)
private val surfaceBrightLight = Color(0xFFFFF8F5)
private val surfaceContainerLowestLight = Color(0xFFFFFFFF)
private val surfaceContainerLowLight = Color(0xFFFFF1E7)
private val surfaceContainerLight = Color(0xFFFAEBE0)
private val surfaceContainerHighLight = Color(0xFFF4E5DB)
private val surfaceContainerHighestLight = Color(0xFFEFE0D5)
private val primaryDark = Color(0xFFFDB975)
private val onPrimaryDark = Color(0xFF4A2800)
private val primaryContainerDark = Color(0xFF693C00)
private val onPrimaryContainerDark = Color(0xFFFFDCBE)
private val secondaryDark = Color(0xFFE1C1A4)
private val onSecondaryDark = Color(0xFF402C18)
private val secondaryContainerDark = Color(0xFF59422C)
private val onSecondaryContainerDark = Color(0xFFFFDCBE)
private val tertiaryDark = Color(0xFFC0CC9A)
private val onTertiaryDark = Color(0xFF2B3410)
private val tertiaryContainerDark = Color(0xFF414B24)
private val onTertiaryContainerDark = Color(0xFFDCE8B4)
private val errorDark = Color(0xFFFFB4AB)
private val onErrorDark = Color(0xFF690005)
private val errorContainerDark = Color(0xFF93000A)
private val onErrorContainerDark = Color(0xFFFFDAD6)
private val backgroundDark = Color(0xFF19120C)
private val onBackgroundDark = Color(0xFFEFE0D5)
private val surfaceDark = Color(0xFF19120C)
private val onSurfaceDark = Color(0xFFEFE0D5)
private val surfaceVariantDark = Color(0xFF51453A)
private val onSurfaceVariantDark = Color(0xFFD5C3B5)
private val outlineDark = Color(0xFF9D8E81)
private val outlineVariantDark = Color(0xFF51453A)
private val scrimDark = Color(0xFF000000)
private val inverseSurfaceDark = Color(0xFFEFE0D5)
private val inverseOnSurfaceDark = Color(0xFF372F28)
private val inversePrimaryDark = Color(0xFF855318)
private val surfaceDimDark = Color(0xFF19120C)
private val surfaceBrightDark = Color(0xFF403830)
private val surfaceContainerLowestDark = Color(0xFF130D07)
private val surfaceContainerLowDark = Color(0xFF211A14)
private val surfaceContainerDark = Color(0xFF261E18)
private val surfaceContainerHighDark = Color(0xFF302822)
private val surfaceContainerHighestDark = Color(0xFF3C332C)

val orangeLightScheme = lightColorScheme(
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

val orangeDarkScheme = darkColorScheme(
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
