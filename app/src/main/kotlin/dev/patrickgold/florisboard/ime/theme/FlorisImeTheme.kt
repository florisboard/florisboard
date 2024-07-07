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

import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import dev.patrickgold.florisboard.ime.input.InputShiftState
import dev.patrickgold.florisboard.ime.text.key.KeyCode
import dev.patrickgold.florisboard.lib.observeAsNonNullState
import dev.patrickgold.florisboard.themeManager
import org.florisboard.lib.android.AndroidVersion
import org.florisboard.lib.snygg.Snygg
import org.florisboard.lib.snygg.SnyggStylesheet
import org.florisboard.lib.snygg.ui.ProvideSnyggUiDefaults
import org.florisboard.lib.snygg.ui.SnyggUiDefaults

private val LocalConfig = staticCompositionLocalOf<ThemeExtensionComponent> { error("not init") }
private val LocalStyle = staticCompositionLocalOf<SnyggStylesheet> { error("not init") }

private val MaterialDarkFallbackPalette = darkColorScheme()
private val MaterialLightFallbackPalette = lightColorScheme()

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

    fun init() {
        Snygg.init(
            stylesheetSpec = FlorisImeUiSpec,
            rulePreferredElementSorting = listOf(
                FlorisImeUi.Keyboard,
                FlorisImeUi.Key,
                FlorisImeUi.KeyHint,
                FlorisImeUi.KeyPopup,
                FlorisImeUi.Smartbar,
                FlorisImeUi.SmartbarSharedActionsRow,
                FlorisImeUi.SmartbarSharedActionsToggle,
                FlorisImeUi.SmartbarExtendedActionsRow,
                FlorisImeUi.SmartbarExtendedActionsToggle,
                FlorisImeUi.SmartbarActionKey,
                FlorisImeUi.SmartbarActionTile,
                FlorisImeUi.SmartbarActionsOverflow,
                FlorisImeUi.SmartbarActionsOverflowCustomizeButton,
                FlorisImeUi.SmartbarActionsEditor,
                FlorisImeUi.SmartbarActionsEditorHeader,
                FlorisImeUi.SmartbarActionsEditorSubheader,
                FlorisImeUi.SmartbarCandidatesRow,
                FlorisImeUi.SmartbarCandidateWord,
                FlorisImeUi.SmartbarCandidateClip,
                FlorisImeUi.SmartbarCandidateSpacer,
            ),
            rulePlaceholders = mapOf(
                "c:delete" to KeyCode.DELETE,
                "c:enter" to KeyCode.ENTER,
                "c:shift" to KeyCode.SHIFT,
                "c:space" to KeyCode.SPACE,
                "sh:unshifted" to InputShiftState.UNSHIFTED.value,
                "sh:shifted_manual" to InputShiftState.SHIFTED_MANUAL.value,
                "sh:shifted_automatic" to InputShiftState.SHIFTED_AUTOMATIC.value,
                "sh:caps_lock" to InputShiftState.CAPS_LOCK.value,
            ),
        )
    }
}

@Composable
fun FlorisImeTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val themeManager by context.themeManager()

    val activeThemeInfo by themeManager.activeThemeInfo.observeAsNonNullState()
    val activeConfig = remember(activeThemeInfo) { activeThemeInfo.config }
    val activeStyle = remember(activeThemeInfo) { activeThemeInfo.stylesheet }
    val materialColors = if (activeConfig.isNightTheme) {
        if (AndroidVersion.ATLEAST_API31_S) {
            dynamicDarkColorScheme(context)
        } else {
            MaterialDarkFallbackPalette
        }
    } else {
        if (AndroidVersion.ATLEAST_API31_S) {
            dynamicLightColorScheme(context)
        } else {
            MaterialLightFallbackPalette
        }
    }
    MaterialTheme(materialColors) {
        CompositionLocalProvider(
            LocalConfig provides activeConfig,
            LocalStyle provides activeStyle,
            LocalTextStyle provides TextStyle.Default,
        ) {
            val fallbackContentColor = FlorisImeTheme.fallbackContentColor()
            val fallbackSurfaceColor = FlorisImeTheme.fallbackSurfaceColor()
            val snyggUiDefaults = remember(fallbackContentColor, fallbackSurfaceColor) {
                SnyggUiDefaults(fallbackContentColor, fallbackSurfaceColor)
            }
            ProvideSnyggUiDefaults(snyggUiDefaults, content)
        }
    }
}
