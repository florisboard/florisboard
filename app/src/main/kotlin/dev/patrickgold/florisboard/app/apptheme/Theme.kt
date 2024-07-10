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

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import dev.patrickgold.florisboard.app.AppTheme
import org.florisboard.lib.android.AndroidVersion
import org.florisboard.lib.theme.amoledDark
import org.florisboard.lib.theme.amoledFlorisColorScheme
import org.florisboard.lib.theme.darkFlorisColorScheme
import org.florisboard.lib.theme.lightFlorisColorScheme

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
fun FlorisAppTheme(
    theme: AppTheme,
    isMaterialYouAware: Boolean,
    content: @Composable () -> Unit
) {

    val colors = if (AndroidVersion.ATLEAST_API31_S) {
        when (theme) {
            AppTheme.AUTO -> when {
                isMaterialYouAware -> when {
                    isSystemInDarkTheme() -> dynamicDarkColorScheme(LocalContext.current)
                    else -> dynamicLightColorScheme(LocalContext.current)
                }

                else -> {
                    when {
                        isSystemInDarkTheme() -> darkFlorisColorScheme
                        else -> lightFlorisColorScheme
                    }
                }
            }

            AppTheme.AUTO_AMOLED -> when {
                isMaterialYouAware -> when {
                    isSystemInDarkTheme() -> dynamicDarkColorScheme(LocalContext.current).copy(
                        background = amoledDark,
                        surface = amoledDark,
                    )

                    else -> dynamicLightColorScheme(LocalContext.current)
                }

                else -> {
                    when {
                        isSystemInDarkTheme() -> amoledFlorisColorScheme
                        else -> lightFlorisColorScheme
                    }
                }
            }

            AppTheme.LIGHT -> when {
                isMaterialYouAware -> dynamicLightColorScheme(LocalContext.current)
                else -> lightFlorisColorScheme
            }

            AppTheme.DARK -> when {
                isMaterialYouAware -> dynamicDarkColorScheme(LocalContext.current)
                else -> darkFlorisColorScheme
            }

            AppTheme.AMOLED_DARK -> when {
                isMaterialYouAware -> dynamicDarkColorScheme(LocalContext.current).copy(
                    background = amoledDark,
                    surface = amoledDark,
                )

                else -> amoledFlorisColorScheme
            }
        }
    } else {
        when (theme) {
            AppTheme.AUTO -> when {
                isSystemInDarkTheme() -> darkFlorisColorScheme
                else -> lightFlorisColorScheme
            }

            AppTheme.AUTO_AMOLED -> when {
                isSystemInDarkTheme() -> darkFlorisColorScheme
                else -> lightFlorisColorScheme
            }

            AppTheme.LIGHT -> lightFlorisColorScheme
            AppTheme.DARK -> darkFlorisColorScheme
            AppTheme.AMOLED_DARK -> amoledFlorisColorScheme
        }
    }

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
