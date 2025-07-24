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

package dev.patrickgold.florisboard.ime.theme

import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import dev.patrickgold.florisboard.app.florisPreferenceModel
import dev.patrickgold.florisboard.keyboardManager
import dev.patrickgold.florisboard.lib.observeAsNonNullState
import dev.patrickgold.florisboard.themeManager
import dev.patrickgold.jetpref.datastore.model.observeAsState
import org.florisboard.lib.snygg.SnyggAttributes
import org.florisboard.lib.snygg.SnyggQueryAttributes
import org.florisboard.lib.snygg.ui.ProvideSnyggTheme
import org.florisboard.lib.snygg.ui.rememberSnyggTheme

private val LocalConfig = staticCompositionLocalOf<ThemeExtensionComponent> { error("not init") }

object FlorisImeTheme {
    val config: ThemeExtensionComponent
        @Composable
        @ReadOnlyComposable
        get() = LocalConfig.current
}

@Composable
fun FlorisImeTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val keyboardManager by context.keyboardManager()
    val themeManager by context.themeManager()

    val prefs by florisPreferenceModel()
    val accentColor by prefs.theme.accentColor.observeAsState()

    val activeThemeInfo by themeManager.activeThemeInfo.observeAsNonNullState()
    val activeConfig = remember(activeThemeInfo) { activeThemeInfo.config }
    val activeStyle = remember(activeThemeInfo) { activeThemeInfo.stylesheet }

    val assetResolver = remember(activeThemeInfo) {
        FlorisAssetResolver(context, activeThemeInfo)
    }
    val snyggTheme = rememberSnyggTheme(activeStyle, assetResolver)
    val fontSizeMultiplier = prefs.keyboard.fontSizeMultiplier()

    val state by keyboardManager.activeState.collectAsState()
    val attributes = mapOf(
        FlorisImeUi.Attr.Mode to state.keyboardMode.toString(),
        FlorisImeUi.Attr.ShiftState to state.inputShiftState.toString(),
    )

    MaterialTheme {
        CompositionLocalProvider(
            LocalConfig provides activeConfig,
            LocalTextStyle provides TextStyle.Default,
        ) {
            ProvideSnyggTheme(
                snyggTheme = snyggTheme,
                dynamicAccentColor = accentColor,
                fontSizeMultiplier = fontSizeMultiplier,
                assetResolver = assetResolver,
                rootAttributes = attributes,
                content = content,
            )
        }
    }
}
