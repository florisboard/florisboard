/*
 * Copyright (C) 2021-2025 The FlorisBoard Contributors
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

import android.app.Activity
import android.content.Context
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import dev.patrickgold.florisboard.app.AppTheme
import dev.patrickgold.florisboard.app.florisPreferenceModel
import dev.patrickgold.jetpref.datastore.model.observeAsState
import org.florisboard.lib.android.AndroidVersion
import org.florisboard.lib.color.ColorMappings

/*private val AmoledDarkColorPalette = darkColorScheme(
    primary = Green500,
    secondary = Green700,
    tertiary = Orange700,
    // = Orange900,

    background = Color(0xFF000000),
    surface = Color(0xFF212121),
)

private val DarkColorPalette = darkColorScheme(
    primary = Green500,
    secondary = Green700,
    tertiary = Orange700,
    //secondaryVariant = Orange900,

    background = Color(0xFF1F1F1F),
    surface = Color(0xFF212121),
)

private val LightColorPalette = lightColorScheme(
    primary = Green500,
    secondary = Green700,
    tertiary = Orange700,
    //secondaryVariant = Orange900,

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
)*/


@Composable
fun getColorScheme(
    context: Context,
    theme: AppTheme,
): ColorScheme {
    val prefs by florisPreferenceModel()
    val accentColor by prefs.other.accentColor.observeAsState()
    val isDark = isSystemInDarkTheme()

    return when (theme) {
        AppTheme.AUTO -> {
            if (isDark) {
                ColorMappings.dynamicDarkColorScheme(context, accentColor)
            } else {
                ColorMappings.dynamicLightColorScheme(context, accentColor)
            }
        }

        AppTheme.DARK -> {
            ColorMappings.dynamicDarkColorScheme(context, accentColor)
        }

        AppTheme.LIGHT -> {
            ColorMappings.dynamicLightColorScheme(context, accentColor)
        }

        AppTheme.AMOLED_DARK -> {
            ColorMappings.dynamicDarkColorScheme(context, accentColor).amoled()
        }

        AppTheme.AUTO_AMOLED -> {
            if (isDark) {
                ColorMappings.dynamicDarkColorScheme(context, accentColor).amoled()
            } else {
                ColorMappings.dynamicLightColorScheme(context, accentColor)
            }
        }
    }
}

fun ColorScheme.amoled(): ColorScheme {
    return this.copy(background = Color.Black, surface = Color.Black)
}

@Composable
fun FlorisAppTheme(
    theme: AppTheme,
    content: @Composable () -> Unit,
) {
    val colors = getColorScheme(
        context = LocalContext.current,
        theme = theme,
    )

    val darkTheme =
        theme == AppTheme.DARK
            || theme == AppTheme.AMOLED_DARK
            || (theme == AppTheme.AUTO && isSystemInDarkTheme())
            || (theme == AppTheme.AUTO_AMOLED && isSystemInDarkTheme())

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colors,
        typography = Typography,
        content = content,
    )
}
