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

private val primaryLight = Color(0xFF5E5E5F)
private val onPrimaryLight = Color(0xFFFFFFFF)
private val primaryContainerLight = Color(0xFF9E9E9E)
private val onPrimaryContainerLight = Color(0xFF343636)
private val secondaryLight = Color(0xFF5F5E5E)
private val onSecondaryLight = Color(0xFFFFFFFF)
private val secondaryContainerLight = Color(0xFFE4E2E1)
private val onSecondaryContainerLight = Color(0xFF656464)
private val tertiaryLight = Color(0xFF615D5F)
private val onTertiaryLight = Color(0xFFFFFFFF)
private val tertiaryContainerLight = Color(0xFFA29D9F)
private val onTertiaryContainerLight = Color(0xFF383537)
private val errorLight = Color(0xFFBA1A1A)
private val onErrorLight = Color(0xFFFFFFFF)
private val errorContainerLight = Color(0xFFFFDAD6)
private val onErrorContainerLight = Color(0xFF93000A)
private val backgroundLight = Color(0xFFFCF8F8)
private val onBackgroundLight = Color(0xFF1C1B1B)
private val surfaceLight = Color(0xFFFCF8F8)
private val onSurfaceLight = Color(0xFF1C1B1B)
private val surfaceVariantLight = Color(0xFFE0E3E3)
private val onSurfaceVariantLight = Color(0xFF444748)
private val outlineLight = Color(0xFF747878)
private val outlineVariantLight = Color(0xFFC4C7C7)
private val scrimLight = Color(0xFF000000)
private val inverseSurfaceLight = Color(0xFF313030)
private val inverseOnSurfaceLight = Color(0xFFF4F0EF)
private val inversePrimaryLight = Color(0xFFC7C6C6)
private val surfaceDimLight = Color(0xFFDDD9D9)
private val surfaceBrightLight = Color(0xFFFCF8F8)
private val surfaceContainerLowestLight = Color(0xFFFFFFFF)
private val surfaceContainerLowLight = Color(0xFFF7F3F2)
private val surfaceContainerLight = Color(0xFFF1EDEC)
private val surfaceContainerHighLight = Color(0xFFEBE7E7)
private val surfaceContainerHighestLight = Color(0xFFE5E2E1)
private val primaryDark = Color(0xFFC7C6C6)
private val onPrimaryDark = Color(0xFF2F3131)
private val primaryContainerDark = Color(0xFF9E9E9E)
private val onPrimaryContainerDark = Color(0xFF343636)
private val secondaryDark = Color(0xFFC8C6C6)
private val onSecondaryDark = Color(0xFF303030)
private val secondaryContainerDark = Color(0xFF474747)
private val onSecondaryContainerDark = Color(0xFFB6B5B4)
private val tertiaryDark = Color(0xFFCBC5C7)
private val onTertiaryDark = Color(0xFF323031)
private val tertiaryContainerDark = Color(0xFFA29D9F)
private val onTertiaryContainerDark = Color(0xFF383537)
private val errorDark = Color(0xFFFFB4AB)
private val onErrorDark = Color(0xFF690005)
private val errorContainerDark = Color(0xFF93000A)
private val onErrorContainerDark = Color(0xFFFFDAD6)
private val backgroundDark = Color(0xFF141313)
private val onBackgroundDark = Color(0xFFE5E2E1)
private val surfaceDark = Color(0xFF141313)
private val onSurfaceDark = Color(0xFFE5E2E1)
private val surfaceVariantDark = Color(0xFF444748)
private val onSurfaceVariantDark = Color(0xFFC4C7C7)
private val outlineDark = Color(0xFF8E9192)
private val outlineVariantDark = Color(0xFF444748)
private val scrimDark = Color(0xFF000000)
private val inverseSurfaceDark = Color(0xFFE5E2E1)
private val inverseOnSurfaceDark = Color(0xFF313030)
private val inversePrimaryDark = Color(0xFF5E5E5F)
private val surfaceDimDark = Color(0xFF141313)
private val surfaceBrightDark = Color(0xFF3A3939)
private val surfaceContainerLowestDark = Color(0xFF0E0E0E)
private val surfaceContainerLowDark = Color(0xFF1C1B1B)
private val surfaceContainerDark = Color(0xFF201F1F)
private val surfaceContainerHighDark = Color(0xFF2A2A2A)
private val surfaceContainerHighestDark = Color(0xFF353434)


val grayLightScheme = lightColorScheme(
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

val grayDarkScheme = darkColorScheme(
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
