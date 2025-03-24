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

package org.florisboard.lib.snygg.ui

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import org.florisboard.lib.color.ColorMappings
import org.florisboard.lib.snygg.SnyggPropertySet
import org.florisboard.lib.snygg.SnyggQueryAttributes
import org.florisboard.lib.snygg.SnyggQuerySelectors
import org.florisboard.lib.snygg.SnyggRule
import org.florisboard.lib.snygg.SnyggStylesheet
import org.florisboard.lib.snygg.SnyggTheme

internal val LocalSnyggTheme: ProvidableCompositionLocal<SnyggTheme> =
    staticCompositionLocalOf {
        error("ProvideSnyggTheme not called.")
    }

internal val LocalSnyggDynamicLightColorScheme: ProvidableCompositionLocal<ColorScheme> =
    staticCompositionLocalOf {
        error("ProvideSnyggTheme not called.")
    }

internal val LocalSnyggDynamicDarkColorScheme: ProvidableCompositionLocal<ColorScheme> =
    staticCompositionLocalOf {
        error("ProvideSnyggTheme not called.")
    }

@Composable
fun rememberSnyggTheme(stylesheet: SnyggStylesheet) = remember(stylesheet) {
    SnyggTheme.compileFrom(stylesheet)
}

@Composable
fun ProvideSnyggTheme(
    snyggTheme: SnyggTheme,
    dynamicAccentColor: Color,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val lightScheme = ColorMappings.dynamicLightColorScheme(context, dynamicAccentColor)
    val darkScheme = ColorMappings.dynamicDarkColorScheme(context, dynamicAccentColor)
    CompositionLocalProvider(
        LocalSnyggTheme provides snyggTheme,
        LocalSnyggDynamicLightColorScheme provides lightScheme,
        LocalSnyggDynamicDarkColorScheme provides darkScheme,
        content = content,
    )
}

val SnyggRule.Companion.Saver: Saver<SnyggRule?, String>
    get() = Saver(
        save = { it?.toString() ?: "" },
        restore = { fromOrNull(it) },
    )

@Composable
internal fun SnyggTheme.query(
    elementName: String,
    attributes: SnyggQueryAttributes,
    selectors: SnyggQuerySelectors,
): SnyggPropertySet {
    val dynamicLightColorScheme = LocalSnyggDynamicLightColorScheme.current
    val dynamicDarkColorScheme = LocalSnyggDynamicDarkColorScheme.current
    return remember(elementName, attributes, selectors, dynamicLightColorScheme, dynamicDarkColorScheme) {
        queryStatic(
            elementName,
            attributes,
            selectors,
            dynamicLightColorScheme,
            dynamicDarkColorScheme,
        )
    }
}
