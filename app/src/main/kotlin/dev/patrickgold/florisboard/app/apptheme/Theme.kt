/*
 * Copyright (C) 2021 Patrick Goldinger
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

package dev.patrickgold.florisboard.app.apptheme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Colors
import androidx.compose.material.MaterialTheme
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import dev.patrickgold.florisboard.app.AppTheme

private val AmoledDarkColorPalette = darkColors(
    primary = Green500,
    primaryVariant = Green700,
    secondary = Orange700,
    secondaryVariant = Orange900,

    background = Color(0xFF000000),
    surface = Color(0xFF212121),
)

private val DarkColorPalette = darkColors(
    primary = Green500,
    primaryVariant = Green700,
    secondary = Orange700,
    secondaryVariant = Orange900,

    background = Color(0xFF1F1F1F),
    surface = Color(0xFF212121),
)

private val LightColorPalette = lightColors(
    primary = Green500,
    primaryVariant = Green700,
    secondary = Orange700,
    secondaryVariant = Orange900,

    background = Color(0xFFFFFFFF),
    surface = Color(0xFFE7E7E7),

    /* Other default colors to override
    background = Color.White,
    surface = Color.White,
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onBackground = Color.Black,
    onSurface = Color.Black,
    */
)

@Composable
fun FlorisAppTheme(
    theme: AppTheme,
    content: @Composable () -> Unit
) {
    val colors = when (theme) {
        AppTheme.AUTO -> when {
            isSystemInDarkTheme() -> DarkColorPalette
            else -> LightColorPalette
        }
        AppTheme.AUTO_AMOLED -> when {
            isSystemInDarkTheme() -> AmoledDarkColorPalette
            else -> LightColorPalette
        }
        AppTheme.LIGHT -> LightColorPalette
        AppTheme.DARK -> DarkColorPalette
        AppTheme.AMOLED_DARK -> AmoledDarkColorPalette
    }

    MaterialTheme(
        colors = colors,
        typography = Typography,
        shapes = Shapes,
        content = content,
    )
}

val Colors.outline: Color
    @Composable
    get() = this.onSurface.copy(alpha = ButtonDefaults.OutlinedBorderOpacity)
