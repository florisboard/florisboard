package org.florisboard.lib.snygg.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

data class SnyggUiDefaults(
    val fallbackContentColor: Color,
    val fallbackSurfaceColor: Color,
)

internal val LocalSnyggUiDefaults = staticCompositionLocalOf {
    SnyggUiDefaults(
        fallbackContentColor = Color.Black,
        fallbackSurfaceColor = Color.White,
    )
}

@Composable fun ProvideSnyggUiDefaults(
    defaults: SnyggUiDefaults,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalSnyggUiDefaults provides defaults) {
        content()
    }
}
