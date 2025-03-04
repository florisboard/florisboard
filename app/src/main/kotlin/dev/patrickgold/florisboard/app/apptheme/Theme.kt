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
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
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
    isMaterialYouAware: Boolean,
    themeColor: Color,
    theme: AppTheme,
): ColorScheme {
    val isDark = isSystemInDarkTheme()

    return when (theme) {

        AppTheme.AUTO -> {
            if (isMaterialYouAware && AndroidVersion.ATLEAST_API31_S) {
                when {
                    isDark -> dynamicDarkColorScheme(context)
                    else -> dynamicLightColorScheme(context)
                }
            } else {
                ColorMappings.getColorSchemeOrDefault(themeColor, isDark, true)
            }
        }

        AppTheme.DARK -> {
            if (isMaterialYouAware && AndroidVersion.ATLEAST_API31_S) {
                dynamicDarkColorScheme(context)
            } else {
                ColorMappings.getColorSchemeOrDefault(themeColor, isDark = true, settings = true)
            }
        }

        AppTheme.LIGHT -> {
            if (isMaterialYouAware && AndroidVersion.ATLEAST_API31_S) {
                dynamicLightColorScheme(context)
            } else {
                ColorMappings.getColorSchemeOrDefault(themeColor, isDark = false, settings = true)
            }
        }

        AppTheme.AMOLED_DARK -> {
            if (isMaterialYouAware && AndroidVersion.ATLEAST_API31_S) {
                dynamicDarkColorScheme(context).amoled()
            } else {
                ColorMappings.getColorSchemeOrDefault(themeColor, isDark = true, settings = true).amoled()
            }
        }

        AppTheme.AUTO_AMOLED -> {
            if (isMaterialYouAware && AndroidVersion.ATLEAST_API31_S) {
                when {
                    isDark -> dynamicDarkColorScheme(context).amoled()
                    else -> dynamicLightColorScheme(context)
                }
            } else {
                with(ColorMappings.getColorSchemeOrDefault(themeColor, isDark, true)) {
                    if (isDark) amoled() else this
                }
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
    isMaterialYouAware: Boolean,
    content: @Composable () -> Unit,
) {
    val prefs by florisPreferenceModel()
    val accent by prefs.other.accentColor.observeAsState()

    val colors = getColorScheme(
        context = LocalContext.current,
        theme = theme,
        isMaterialYouAware = isMaterialYouAware,
        themeColor = accent,
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
