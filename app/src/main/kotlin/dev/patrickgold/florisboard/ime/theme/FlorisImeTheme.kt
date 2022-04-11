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

package dev.patrickgold.florisboard.ime.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import dev.patrickgold.florisboard.lib.observeAsNonNullState
import dev.patrickgold.florisboard.lib.snygg.SnyggStylesheet
import dev.patrickgold.florisboard.themeManager

private val LocalConfig = staticCompositionLocalOf<ThemeExtensionComponent> { error("not init") }
private val LocalStyle = staticCompositionLocalOf<SnyggStylesheet> { error("not init") }

object FlorisImeTheme {
    val config: ThemeExtensionComponent
        @Composable
        @ReadOnlyComposable
        get() = LocalConfig.current

    val style: SnyggStylesheet
        @Composable
        @ReadOnlyComposable
        get() = LocalStyle.current

    @Composable
    fun fallbackSurfaceColor(): Color {
        return if (config.isNightTheme) Color.Black else Color.White
    }

    @Composable
    fun fallbackContentColor(): Color {
        return if (config.isNightTheme) Color.White else Color.Black
    }
}

@Composable
fun FlorisImeTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val themeManager by context.themeManager()

    val activeThemeInfo by themeManager.activeThemeInfo.observeAsNonNullState()
    val activeConfig = remember(activeThemeInfo) { activeThemeInfo.config }
    val activeStyle = remember(activeThemeInfo) { activeThemeInfo.stylesheet }
    CompositionLocalProvider(LocalConfig provides activeConfig, LocalStyle provides activeStyle) {
        content()
    }
}
