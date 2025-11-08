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

private val primaryLight = Color(0xFF006A60)
private val onPrimaryLight = Color(0xFFFFFFFF)
private val primaryContainerLight = Color(0xFF9EF2E4)
private val onPrimaryContainerLight = Color(0xFF005048)
private val secondaryLight = Color(0xFF4A635F)
private val onSecondaryLight = Color(0xFFFFFFFF)
private val secondaryContainerLight = Color(0xFFCCE8E2)
private val onSecondaryContainerLight = Color(0xFF334B47)
private val tertiaryLight = Color(0xFF456179)
private val onTertiaryLight = Color(0xFFFFFFFF)
private val tertiaryContainerLight = Color(0xFFCCE5FF)
private val onTertiaryContainerLight = Color(0xFF2D4961)
private val errorLight = Color(0xFFBA1A1A)
private val onErrorLight = Color(0xFFFFFFFF)
private val errorContainerLight = Color(0xFFFFDAD6)
private val onErrorContainerLight = Color(0xFF93000A)
private val backgroundLight = Color(0xFFF4FBF8)
private val onBackgroundLight = Color(0xFF161D1C)
private val surfaceLight = Color(0xFFF4FBF8)
private val onSurfaceLight = Color(0xFF161D1C)
private val surfaceVariantLight = Color(0xFFDAE5E1)
private val onSurfaceVariantLight = Color(0xFF3F4947)
private val outlineLight = Color(0xFF6F7977)
private val outlineVariantLight = Color(0xFFBEC9C6)
private val scrimLight = Color(0xFF000000)
private val inverseSurfaceLight = Color(0xFF2B3230)
private val inverseOnSurfaceLight = Color(0xFFECF2EF)
private val inversePrimaryLight = Color(0xFF82D5C8)
private val surfaceDimLight = Color(0xFFD5DBD9)
private val surfaceBrightLight = Color(0xFFF4FBF8)
private val surfaceContainerLowestLight = Color(0xFFFFFFFF)
private val surfaceContainerLowLight = Color(0xFFEFF5F2)
private val surfaceContainerLight = Color(0xFFE9EFED)
private val surfaceContainerHighLight = Color(0xFFE3EAE7)
private val surfaceContainerHighestLight = Color(0xFFDDE4E1)

private val primaryDark = Color(0xFF82D5C8)
private val onPrimaryDark = Color(0xFF003731)
private val primaryContainerDark = Color(0xFF005048)
private val onPrimaryContainerDark = Color(0xFF9EF2E4)
private val secondaryDark = Color(0xFFB1CCC6)
private val onSecondaryDark = Color(0xFF1C3531)
private val secondaryContainerDark = Color(0xFF334B47)
private val onSecondaryContainerDark = Color(0xFFCCE8E2)
private val tertiaryDark = Color(0xFFADCAE6)
private val onTertiaryDark = Color(0xFF153349)
private val tertiaryContainerDark = Color(0xFF2D4961)
private val onTertiaryContainerDark = Color(0xFFCCE5FF)
private val errorDark = Color(0xFFFFB4AB)
private val onErrorDark = Color(0xFF690005)
private val errorContainerDark = Color(0xFF93000A)
private val onErrorContainerDark = Color(0xFFFFDAD6)
private val backgroundDark = Color(0xFF0E1513)
private val onBackgroundDark = Color(0xFFDDE4E1)
private val surfaceDark = Color(0xFF0E1513)
private val onSurfaceDark = Color(0xFFDDE4E1)
private val surfaceVariantDark = Color(0xFF3F4947)
private val onSurfaceVariantDark = Color(0xFFBEC9C6)
private val outlineDark = Color(0xFF899390)
private val outlineVariantDark = Color(0xFF3F4947)
private val scrimDark = Color(0xFF000000)
private val inverseSurfaceDark = Color(0xFFDDE4E1)
private val inverseOnSurfaceDark = Color(0xFF2B3230)
private val inversePrimaryDark = Color(0xFF006A60)
private val surfaceDimDark = Color(0xFF0E1513)
private val surfaceBrightDark = Color(0xFF343B39)
private val surfaceContainerLowestDark = Color(0xFF090F0E)
private val surfaceContainerLowDark = Color(0xFF161D1C)
private val surfaceContainerDark = Color(0xFF1A2120)
private val surfaceContainerHighDark = Color(0xFF252B2A)
private val surfaceContainerHighestDark = Color(0xFF303635)


val tealLightScheme = lightColorScheme(
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

val tealDarkScheme = darkColorScheme(
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
