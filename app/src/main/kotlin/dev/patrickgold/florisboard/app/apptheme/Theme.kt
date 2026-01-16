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
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import dev.patrickgold.florisboard.app.AppTheme
import dev.patrickgold.florisboard.app.FlorisPreferenceStore
import dev.patrickgold.jetpref.datastore.model.observeAsState
import org.florisboard.lib.color.neutralDynamicColorScheme
import org.florisboard.lib.color.systemAccentOrDefault


@Composable
fun getColorScheme(
    theme: AppTheme,
): ColorScheme {
    val prefs by FlorisPreferenceStore
    val accentColor by prefs.other.accentColor.observeAsState()

    val seedColor = systemAccentOrDefault(accentColor)

    return when (theme) {
        AppTheme.AUTO, AppTheme.AUTO_AMOLED -> {
            neutralDynamicColorScheme(
                primary = seedColor,
                isDark = isSystemInDarkTheme(),
                isAmoled = theme == AppTheme.AUTO_AMOLED,
            )
        }

        AppTheme.DARK, AppTheme.LIGHT -> {
            neutralDynamicColorScheme(primary = seedColor, isDark = theme == AppTheme.DARK)
        }

        AppTheme.AMOLED_DARK -> {
            neutralDynamicColorScheme(primary = seedColor, isDark = true, isAmoled = true)
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
    val colors = getColorScheme(theme = theme)

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
