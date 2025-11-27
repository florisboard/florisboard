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

package org.florisboard.lib.color

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.isUnspecified
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import com.materialkolor.Contrast
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamicColorScheme
import com.materialkolor.dynamiccolor.ColorSpec
import com.materialkolor.scheme.DynamicScheme
import org.florisboard.lib.android.AndroidVersion

val DEFAULT_GREEN = Color(0xFF4CAF50)

object ColorMappings {

    val colors = listOf(
        DEFAULT_GREEN, // GREEN 500
        Color(0xFFF44336), // RED 500
        Color(0xFFE91E63), // PINK 500
        Color(0xFFFF2C93), // LIGHT PINK 500
        Color(0xFF9C27B0), // PURPLE 500
        Color(0xFF673AB7), // DEEP PURPLE 500
        Color(0xFF3F51B5), // INDIGO 500
        Color(0xFF2196F3), // BLUE 500
        Color(0xFF03A9F4), // LIGHT BLUE 500
        Color(0xFF00BCD4), // CYAN 500
        Color(0xFF009688), // TEAL 500
        Color(0xFF8BC34A), // LIGHT GREEN 500
        Color(0xFFCDDC39), // LIME 500
        Color(0xFFFFEB3B), // YELLOW 500
        Color(0xFFFFC107), // AMBER 500
        Color(0xFFFF9800), // ORANGE 500
        Color(0xFF795548), // BROWN 500
        Color(0xFF607D8B), // BLUE GREY 500
        Color(0xFF9E9E9E), // GRAY 500
    ).toTypedArray()
}

@Composable
@RequiresApi(Build.VERSION_CODES.S)
fun getSystemAccent(): Color {
    val context = LocalContext.current
    val resources = LocalResources.current

    return Color(resources.getColor(android.R.color.system_accent1_500, context.theme))
}

@Composable
fun systemAccentOrDefault(default: Color): Color {
    return if (default.isUnspecified && AndroidVersion.ATLEAST_API31_S) {
        getSystemAccent()
    } else {
        default
    }
}


/**
 * This is a helper method that uses [com.materialkolor.PaletteStyle.Neutral] as a default PalleteStyle.
 */
fun neutralDynamicColorScheme(
    primary: Color,
    isDark: Boolean,
    isAmoled: Boolean = false,
    secondary: Color? = null,
    tertiary: Color? = null,
    neutral: Color? = null,
    neutralVariant: Color? = null,
    error: Color? = null,
    style: PaletteStyle = PaletteStyle.Neutral,
    contrastLevel: Double = Contrast.Default.value,
    specVersion: ColorSpec.SpecVersion = ColorSpec.SpecVersion.Default,
    platform: DynamicScheme.Platform = DynamicScheme.Platform.Default,
    modifyColorScheme: ((ColorScheme) -> ColorScheme)? = null,
): ColorScheme =
    dynamicColorScheme(
        seedColor = primary,
        isDark = isDark,
        isAmoled = isAmoled,
        primary = primary,
        secondary = secondary,
        tertiary = tertiary,
        neutral = neutral,
        neutralVariant = neutralVariant,
        error = error,
        style = style,
        contrastLevel = contrastLevel,
        specVersion = specVersion,
        platform = platform,
        modifyColorScheme = modifyColorScheme,
    )
