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
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.LocalContentColor
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
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.takeOrElse
import org.florisboard.lib.color.ColorMappings
import org.florisboard.lib.snygg.SnyggPropertySet
import org.florisboard.lib.snygg.SnyggQueryAttributes
import org.florisboard.lib.snygg.SnyggRule
import org.florisboard.lib.snygg.SnyggSelector
import org.florisboard.lib.snygg.SnyggStylesheet
import org.florisboard.lib.snygg.SnyggTheme
import org.florisboard.lib.snygg.value.SnyggAssetResolver
import org.florisboard.lib.snygg.value.SnyggDefaultAssetResolver
import org.florisboard.lib.snygg.value.SnyggDpSizeValue
import org.florisboard.lib.snygg.value.SnyggPaddingValue
import org.florisboard.lib.snygg.value.SnyggStaticColorValue
import org.florisboard.lib.snygg.value.SnyggValue
import org.florisboard.lib.snygg.value.SnyggYesValue

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

internal val LocalSnyggParentSelector: ProvidableCompositionLocal<SnyggSelector> =
    compositionLocalOf {
        SnyggSelector.NONE
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
internal fun ProvideSnyggStyle(
    elementName: String?,
    attributes: Map<String, Int>,
    selector: SnyggSelector?,
    content: @Composable (style: SnyggPropertySet) -> Unit
) {
    val theme = LocalSnyggTheme.current
    val style = theme.rememberQuery(elementName, attributes, selector)
    val parentSelector = selector ?: LocalSnyggParentSelector.current
    CompositionLocalProvider(
        LocalSnyggParentStyle provides style,
        LocalSnyggParentSelector provides parentSelector,
        LocalContentColor provides style.foreground(),
    ) {
        content(style)
    }
}

val SnyggRule.Companion.Saver: Saver<SnyggRule?, String>
    get() = Saver(
        save = { it?.toString() ?: "" },
        restore = { fromOrNull(it) },
    )

val SnyggRule.Companion.NonNullSaver: Saver<SnyggRule, String>
    get() = Saver(
        save = { it.toString() },
        restore = { fromOrNull(it)!! },
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
    elementName: String?,
    attributes: SnyggQueryAttributes,
    selector: SnyggSelector? = null,
): SnyggPropertySet {
    val mergedSelector = selector ?: LocalSnyggParentSelector.current
    val parentStyle = LocalSnyggParentStyle.current
    val dynamicLightColorScheme = LocalSnyggDynamicLightColorScheme.current
    val dynamicDarkColorScheme = LocalSnyggDynamicDarkColorScheme.current
    return remember(elementName, attributes, mergedSelector, parentStyle, dynamicLightColorScheme, dynamicDarkColorScheme) {
        query(
            elementName = elementName ?: "",
            attributes,
            mergedSelector,
            parentStyle,
            dynamicLightColorScheme,
            dynamicDarkColorScheme,
        )
    }
}

fun Modifier.snyggBackground(
    style: SnyggPropertySet,
    default: Color = Color.Unspecified,
    shape: Shape = style.shape(),
): Modifier {
    val modifier = when (val bg = style.background) {
        is SnyggStaticColorValue -> this.background(
            color = bg.color,
            shape = shape,
        )
        else if (default.isSpecified) -> {
            this.background(
                color = default,
                shape = shape,
            )
        }
        else -> this
    }
    if (style.clip is SnyggYesValue) {
        return modifier.clip(shape)
    }
    return modifier
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

@Composable
internal fun Modifier.snyggIconSize(style: SnyggPropertySet): Modifier {
    return with(LocalDensity.current) {
        val fontSize = style.fontSize(default = 0.sp)
        if (fontSize.isSp && fontSize >= 1.sp) {
            this@snyggIconSize.size(fontSize.toDp())
        } else {
            this@snyggIconSize
        }
    }
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
