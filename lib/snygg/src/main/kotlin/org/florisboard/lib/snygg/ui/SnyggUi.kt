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

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.DefaultShadowColor
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.takeOrElse
import org.florisboard.lib.color.ColorMappings
import org.florisboard.lib.snygg.SnyggPropertySet
import org.florisboard.lib.snygg.SnyggQueryAttributes
import org.florisboard.lib.snygg.SnyggQuerySelectors
import org.florisboard.lib.snygg.SnyggRule
import org.florisboard.lib.snygg.SnyggStylesheet
import org.florisboard.lib.snygg.SnyggTheme
import org.florisboard.lib.snygg.value.SnyggDpSizeValue
import org.florisboard.lib.snygg.value.SnyggFontStyleValue
import org.florisboard.lib.snygg.value.SnyggFontWeightValue
import org.florisboard.lib.snygg.value.SnyggPaddingValue
import org.florisboard.lib.snygg.value.SnyggShapeValue
import org.florisboard.lib.snygg.value.SnyggSpSizeValue
import org.florisboard.lib.snygg.value.SnyggStaticColorValue
import org.florisboard.lib.snygg.value.SnyggValue

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

internal val LocalSnyggParentStyle: ProvidableCompositionLocal<SnyggPropertySet> =
    staticCompositionLocalOf {
        SnyggPropertySet()
    }

@Composable
fun rememberSnyggTheme(stylesheet: SnyggStylesheet) = remember(stylesheet) {
    SnyggTheme.compileFrom(stylesheet)
}

@Composable
fun ProvideSnyggTheme(
    snyggTheme: SnyggTheme,
    dynamicAccentColor: Color = Color.Unspecified,
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

@Composable
internal fun ProvideSnyggParentStyle(
    parentStyle: SnyggPropertySet,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(
        LocalSnyggParentStyle provides parentStyle,
        content = content,
    )
}

val SnyggRule.Companion.Saver: Saver<SnyggRule?, String>
    get() = Saver(
        save = { it?.toString() ?: "" },
        restore = { fromOrNull(it) },
    )

@Composable
internal fun SnyggTheme.rememberQuery(
    elementName: String,
    attributes: SnyggQueryAttributes,
    selectors: SnyggQuerySelectors,
): SnyggPropertySet {
    val parentStyle = LocalSnyggParentStyle.current
    val dynamicLightColorScheme = LocalSnyggDynamicLightColorScheme.current
    val dynamicDarkColorScheme = LocalSnyggDynamicDarkColorScheme.current
    return remember(elementName, attributes, selectors, parentStyle, dynamicLightColorScheme, dynamicDarkColorScheme) {
        query(
            elementName,
            attributes,
            selectors,
            parentStyle,
            dynamicLightColorScheme,
            dynamicDarkColorScheme,
        )
    }
}

internal fun Modifier.snyggBackground(
    style: SnyggPropertySet,
    fallbackColor: Color = Color.Unspecified,
    shape: Shape = style.shape(),
): Modifier {
    return when (val bg = style.background) {
        is SnyggStaticColorValue -> this.background(
            color = bg.color,
            shape = shape,
        )
        else if (fallbackColor.isSpecified) -> {
            this.background(
                color = fallbackColor,
                shape = shape,
            )
        }
        else -> this
    }
}

internal fun Modifier.snyggBorder(
    style: SnyggPropertySet,
    width: Dp = style.borderWidth.dpSize().takeOrElse { 0.dp }.coerceAtLeast(0.dp),
    color: Color = style.borderColor.colorOrDefault(default = Color.Unspecified),
    shape: Shape = style.shape(),
): Modifier {
    return if (color.isSpecified) {
        this.border(width, color, shape)
    } else {
        this
    }
}

internal fun Modifier.snyggClip(
    style: SnyggPropertySet,
    shape: Shape = style.shape(),
): Modifier {
    return this.clip(shape)
}

internal fun Modifier.snyggMargin(
    style: SnyggPropertySet,
): Modifier {
    return when (style.margin) {
        is SnyggPaddingValue -> this.padding(style.margin.values)
        else -> return this
    }
}

internal fun Modifier.snyggPadding(
    style: SnyggPropertySet,
): Modifier {
    return when (style.padding) {
        is SnyggPaddingValue -> this.padding(style.padding.values)
        else -> return this
    }
}

internal fun Modifier.snyggShadow(
    style: SnyggPropertySet,
    elevation: Dp = style.shadowElevation.dpSize().takeOrElse { 0.dp }.coerceAtLeast(0.dp),
    shape: Shape = style.shape(),
    color: Color = style.shadowColor.colorOrDefault(default = DefaultShadowColor),
): Modifier {
    return this.shadow(elevation, shape, clip = false, ambientColor = color, spotColor = color)
}

/// SnyggValue helpers

internal fun SnyggValue.colorOrDefault(default: Color): Color {
    return when (this) {
        is SnyggStaticColorValue -> this.color
        else -> default
    }
}

internal fun SnyggValue.dpSize(default: Dp = Dp.Unspecified): Dp {
    return when (this) {
        is SnyggDpSizeValue -> this.dp
        else -> default
    }
}

internal fun SnyggPropertySet.background(default: Color = Color.Unspecified): Color {
    return when (background) {
        is SnyggStaticColorValue -> background.color
        else -> default
    }
}

internal fun SnyggPropertySet.foreground(default: Color = Color.Unspecified): Color {
    return when (foreground) {
        is SnyggStaticColorValue -> foreground.color
        else -> default
    }
}

internal fun SnyggPropertySet.fontSize(default: TextUnit = TextUnit.Unspecified): TextUnit {
    return when (fontSize) {
        is SnyggSpSizeValue -> fontSize.sp
        else -> default
    }
}

internal fun SnyggPropertySet.fontStyle(default: FontStyle? = null): FontStyle? {
    return when (fontStyle) {
        is SnyggFontStyleValue -> fontStyle.fontStyle
        else -> default
    }
}

internal fun SnyggPropertySet.fontWeight(default: FontWeight? = null): FontWeight? {
    return when (fontWeight) {
        is SnyggFontWeightValue -> fontWeight.fontWeight
        else -> default
    }
}

internal fun SnyggPropertySet.shape(): Shape {
    return when (shape) {
        is SnyggShapeValue -> shape.shape
        else -> RectangleShape
    }
}
