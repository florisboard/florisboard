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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.DefaultShadowColor
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.takeOrElse
import org.florisboard.lib.color.ColorMappings
import org.florisboard.lib.snygg.SnyggPropertySet
import org.florisboard.lib.snygg.SnyggQueryAttributes
import org.florisboard.lib.snygg.SnyggRule
import org.florisboard.lib.snygg.SnyggSelector
import org.florisboard.lib.snygg.SnyggStylesheet
import org.florisboard.lib.snygg.SnyggTheme
import org.florisboard.lib.snygg.value.SnyggAssetResolver
import org.florisboard.lib.snygg.value.SnyggCustomFontFamilyValue
import org.florisboard.lib.snygg.value.SnyggDefaultAssetResolver
import org.florisboard.lib.snygg.value.SnyggDpSizeValue
import org.florisboard.lib.snygg.value.SnyggFontStyleValue
import org.florisboard.lib.snygg.value.SnyggFontWeightValue
import org.florisboard.lib.snygg.value.SnyggGenericFontFamilyValue
import org.florisboard.lib.snygg.value.SnyggObjectFitValue
import org.florisboard.lib.snygg.value.SnyggPaddingValue
import org.florisboard.lib.snygg.value.SnyggShapeValue
import org.florisboard.lib.snygg.value.SnyggSpSizeValue
import org.florisboard.lib.snygg.value.SnyggStaticColorValue
import org.florisboard.lib.snygg.value.SnyggTextAlignValue
import org.florisboard.lib.snygg.value.SnyggTextDecorationLineValue
import org.florisboard.lib.snygg.value.SnyggTextOverflowValue
import org.florisboard.lib.snygg.value.SnyggValue

internal val LocalSnyggTheme: ProvidableCompositionLocal<SnyggTheme> =
    compositionLocalOf {
        error("ProvideSnyggTheme not called.")
    }

internal val LocalSnyggDynamicLightColorScheme: ProvidableCompositionLocal<ColorScheme> =
    compositionLocalOf {
        error("ProvideSnyggTheme not called.")
    }

internal val LocalSnyggDynamicDarkColorScheme: ProvidableCompositionLocal<ColorScheme> =
    compositionLocalOf {
        error("ProvideSnyggTheme not called.")
    }

internal val LocalSnyggAssetResolver: ProvidableCompositionLocal<SnyggAssetResolver> =
    compositionLocalOf {
        error("ProvideSnyggTheme not called.")
    }

internal val LocalSnyggParentStyle: ProvidableCompositionLocal<SnyggPropertySet> =
    compositionLocalOf {
        SnyggPropertySet()
    }

/**
 * Creates a [SnyggTheme] that is remembered across compositions.
 *
 * When the stylesheet changes the [SnyggTheme] is recompiled.
 *
 * @param stylesheet [SnyggStylesheet] the [SnyggTheme] is compiled from
 */
@Composable
fun rememberSnyggTheme(
    stylesheet: SnyggStylesheet,
    assetResolver: SnyggAssetResolver = SnyggDefaultAssetResolver,
) = remember(stylesheet, assetResolver) {
    SnyggTheme.compileFrom(stylesheet, assetResolver)
}

/**
 * Provides the snygg theme to use for all snygg ui composable functions.
 *
 * Use [rememberSnyggTheme] to convert a [SnyggStylesheet] to a [SnyggTheme].
 *
 * @param snyggTheme The [SnyggTheme] for the composable functions.
 * @param dynamicAccentColor The [Color] for the dynamic color schemes.
 * [Color.Unspecified] means default/material you color.
 * @param assetResolver The [SnyggAssetResolver] used to resolve [an asset Uri][org.florisboard.lib.snygg.value.SnyggUriValue].
 *
 * @see rememberSnyggTheme
 * @see SnyggAssetResolver
 */
@Composable
fun ProvideSnyggTheme(
    snyggTheme: SnyggTheme,
    dynamicAccentColor: Color = Color.Unspecified,
    assetResolver: SnyggAssetResolver = SnyggDefaultAssetResolver,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val lightScheme = ColorMappings.dynamicLightColorScheme(context, dynamicAccentColor)
    val darkScheme = ColorMappings.dynamicDarkColorScheme(context, dynamicAccentColor)
    CompositionLocalProvider(
        LocalSnyggTheme provides snyggTheme,
        LocalSnyggDynamicLightColorScheme provides lightScheme,
        LocalSnyggDynamicDarkColorScheme provides darkScheme,
        LocalSnyggAssetResolver provides assetResolver,
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
fun rememberSnyggThemeQuery(
    elementName: String,
    attributes: SnyggQueryAttributes = emptyMap(),
    selector: SnyggSelector? = null,
): SnyggPropertySet {
    val theme = LocalSnyggTheme.current
    return theme.rememberQuery(elementName, attributes, selector)
}

@Composable
internal fun SnyggTheme.rememberQuery(
    elementName: String,
    attributes: SnyggQueryAttributes,
    selector: SnyggSelector? = null,
): SnyggPropertySet {
    val parentStyle = LocalSnyggParentStyle.current
    val dynamicLightColorScheme = LocalSnyggDynamicLightColorScheme.current
    val dynamicDarkColorScheme = LocalSnyggDynamicDarkColorScheme.current
    return remember(elementName, attributes, selector, parentStyle, dynamicLightColorScheme, dynamicDarkColorScheme) {
        query(
            elementName,
            attributes,
            selector,
            parentStyle,
            dynamicLightColorScheme,
            dynamicDarkColorScheme,
        )
    }
}

fun Modifier.snyggBackground(
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

fun Modifier.snyggBorder(
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

fun Modifier.snyggClip(
    style: SnyggPropertySet,
    shape: Shape = style.shape(),
): Modifier {
    return this.clip(shape)
}

fun Modifier.snyggMargin(
    style: SnyggPropertySet,
): Modifier {
    return when (style.margin) {
        is SnyggPaddingValue -> this.padding(style.margin.values)
        else -> return this
    }
}

fun Modifier.snyggPadding(
    style: SnyggPropertySet,
    default: PaddingValues? = null,
): Modifier {
    return when (style.padding) {
        is SnyggPaddingValue -> this.padding(style.padding.values)
        else if (default != null) -> this.padding(default)
        else -> return this
    }
}

fun Modifier.snyggShadow(
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

fun SnyggPropertySet.background(default: Color = Color.Unspecified): Color {
    return when (background) {
        is SnyggStaticColorValue -> background.color
        else -> default
    }
}

fun SnyggPropertySet.foreground(default: Color = Color.Unspecified): Color {
    return when (foreground) {
        is SnyggStaticColorValue -> foreground.color
        else -> default
    }
}

fun SnyggPropertySet.fontSize(default: TextUnit = TextUnit.Unspecified): TextUnit {
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

internal fun SnyggPropertySet.fontFamily(theme: SnyggTheme, default: FontFamily? = null): FontFamily? {
    return when (val family = fontFamily) {
        is SnyggGenericFontFamilyValue -> family.fontFamily
        is SnyggCustomFontFamilyValue -> theme.getFontFamily(family.fontName)
        else -> default
    }
}

internal fun SnyggPropertySet.letterSpacing(default: TextUnit = TextUnit.Unspecified): TextUnit {
    return when (letterSpacing) {
        is SnyggSpSizeValue -> letterSpacing.sp
        else -> default
    }
}

internal fun SnyggPropertySet.lineHeight(default: TextUnit = TextUnit.Unspecified): TextUnit {
    return when (lineHeight) {
        is SnyggSpSizeValue -> lineHeight.sp
        else -> default
    }
}

internal fun SnyggPropertySet.textAlign(default: TextAlign? = null): TextAlign? {
    return when (textAlign) {
        is SnyggTextAlignValue -> textAlign.textAlign
        else -> default
    }
}

internal fun SnyggPropertySet.textDecorationLine(default: TextDecoration? = null): TextDecoration? {
    return when (textDecorationLine) {
        is SnyggTextDecorationLineValue -> textDecorationLine.textDecoration
        else -> default
    }
}

internal fun SnyggPropertySet.textOverflow(default: TextOverflow = TextOverflow.Clip): TextOverflow {
    return when (textOverflow) {
        is SnyggTextOverflowValue -> textOverflow.textOverflow
        else -> default
    }
}

fun SnyggPropertySet.shape(): Shape {
    return when (shape) {
        is SnyggShapeValue -> shape.shape
        else -> RectangleShape
    }
}

internal fun SnyggPropertySet.objectFit(default: ContentScale = ContentScale.Fit): ContentScale {
    return when (objectFit) {
        is SnyggObjectFitValue -> objectFit.contentScale
        else -> default
    }
}
