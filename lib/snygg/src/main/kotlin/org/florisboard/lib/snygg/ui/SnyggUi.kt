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
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.takeOrElse
import kotlinx.coroutines.runBlocking
import org.florisboard.lib.color.ColorMappings
import org.florisboard.lib.snygg.CompiledFontFamilyData
import org.florisboard.lib.snygg.SnyggAttributes
import org.florisboard.lib.snygg.SnyggQueryAttributes
import org.florisboard.lib.snygg.SnyggRule
import org.florisboard.lib.snygg.SnyggSelector
import org.florisboard.lib.snygg.SnyggSinglePropertySet
import org.florisboard.lib.snygg.SnyggSinglePropertySetEditor
import org.florisboard.lib.snygg.SnyggStylesheet
import org.florisboard.lib.snygg.SnyggTheme
import org.florisboard.lib.snygg.value.SnyggAssetResolver
import org.florisboard.lib.snygg.value.SnyggDefaultAssetResolver
import org.florisboard.lib.snygg.value.SnyggDpSizeValue
import org.florisboard.lib.snygg.value.SnyggNoValue
import org.florisboard.lib.snygg.value.SnyggPaddingValue
import org.florisboard.lib.snygg.value.SnyggStaticColorValue
import org.florisboard.lib.snygg.value.SnyggUriValue
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

internal val LocalSnyggFontSizeMultiplier: ProvidableCompositionLocal<Float> =
    compositionLocalOf {
        error("ProvideSnyggTheme not called.")
    }

internal val LocalSnyggAssetResolver: ProvidableCompositionLocal<SnyggAssetResolver> =
    compositionLocalOf {
        error("ProvideSnyggTheme not called.")
    }

internal val LocalSnyggPreloadedCustomFontFamilies: ProvidableCompositionLocal<CompiledFontFamilyData> =
    compositionLocalOf {
        error("ProvideSnyggTheme not called.")
    }

internal val LocalSnyggParentStyle: ProvidableCompositionLocal<SnyggSinglePropertySet> =
    compositionLocalOf {
        error("ProvideSnyggTheme not called.")
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
 * @param assetResolver The [SnyggAssetResolver] used to resolve [an asset Uri][org.florisboard.lib.snygg.value.SnyggUriValue]
 *
 * @since 0.5.0-alpha01
 *
 * @see SnyggAssetResolver
 * @see SnyggDefaultAssetResolver
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
 * This function **must** be called for snygg ui composables to work.
 *
 * Use [rememberSnyggTheme] to convert a [SnyggStylesheet] to a [SnyggTheme].
 *
 * @param snyggTheme The [SnyggTheme] for the composable functions.
 * @param dynamicAccentColor The [Color] for the dynamic color schemes.
 * [Color.Unspecified] means default/material you color.
 * @param assetResolver The [SnyggAssetResolver] used to resolve [an asset Uri][org.florisboard.lib.snygg.value.SnyggUriValue].
 *
 * @since 0.5.0-alpha01
 *
 * @see rememberSnyggTheme
 * @see SnyggAssetResolver
 * @see SnyggDefaultAssetResolver
 */
@Composable
fun ProvideSnyggTheme(
    snyggTheme: SnyggTheme,
    dynamicAccentColor: Color = Color.Unspecified,
    fontSizeMultiplier: Float = 1.0f,
    assetResolver: SnyggAssetResolver = SnyggDefaultAssetResolver,
    rootAttributes: SnyggQueryAttributes = emptyMap(),
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val lightScheme = ColorMappings.dynamicLightColorScheme(context, dynamicAccentColor)
    val darkScheme = ColorMappings.dynamicDarkColorScheme(context, dynamicAccentColor)

    val resolver = LocalFontFamilyResolver.current
    val customFontFamilies = remember(snyggTheme) {
        runBlocking {
            snyggTheme.fontFamilies.mapValues { (_, fontFamily) ->
                runCatching { resolver.preload(fontFamily) }.fold(
                    onSuccess = { fontFamily },
                    onFailure = { FontFamily.Default },
                )
            }
        }
    }

    val initFontSize = MaterialTheme.typography.bodyMedium.fontSize
    val initParentStyle = remember(initFontSize) {
        SnyggSinglePropertySetEditor().run {
            fontSize = fontSize(initFontSize)
            build()
        }
    }

    CompositionLocalProvider(
        LocalSnyggTheme provides snyggTheme,
        LocalSnyggDynamicLightColorScheme provides lightScheme,
        LocalSnyggDynamicDarkColorScheme provides darkScheme,
        LocalSnyggFontSizeMultiplier provides fontSizeMultiplier,
        LocalSnyggAssetResolver provides assetResolver,
        LocalSnyggPreloadedCustomFontFamilies provides customFontFamilies,
        LocalSnyggParentStyle provides initParentStyle,
    ) {
        ProvideSnyggStyle("root", rootAttributes, SnyggSelector.NONE) {
            content()
        }
    }
}

@Composable
internal fun ProvideSnyggStyle(
    elementName: String?,
    attributes: SnyggQueryAttributes,
    selector: SnyggSelector?,
    content: @Composable (style: SnyggSinglePropertySet) -> Unit
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

/**
 * Remembers the [PropertySet][SnyggSinglePropertySet] for the params given.
 *
 * @param elementName The name of this element. If `null` the style will be inherited from the parent element.
 * @param attributes The attributes of the element used to refine the query.
 * @param selector A specific SnyggSelector to query the style for.
 *
 * @return the [queried property set][SnyggSinglePropertySet] for usage in custom elements
 */
@Composable
fun rememberSnyggThemeQuery(
    elementName: String,
    attributes: SnyggQueryAttributes = emptyMap(),
    selector: SnyggSelector? = null,
): SnyggSinglePropertySet {
    val theme = LocalSnyggTheme.current
    return theme.rememberQuery(elementName, attributes, selector)
}

@Composable
internal fun SnyggTheme.rememberQuery(
    elementName: String?,
    attributes: SnyggQueryAttributes,
    selector: SnyggSelector? = null,
): SnyggSinglePropertySet {
    val mergedSelector = selector ?: LocalSnyggParentSelector.current
    val parentStyle = LocalSnyggParentStyle.current
    val dynamicLightColorScheme = LocalSnyggDynamicLightColorScheme.current
    val dynamicDarkColorScheme = LocalSnyggDynamicDarkColorScheme.current
    val fontSizeMultiplier = LocalSnyggFontSizeMultiplier.current
    return remember(this, elementName, attributes, mergedSelector, parentStyle, dynamicLightColorScheme, dynamicDarkColorScheme, fontSizeMultiplier) {
        query(
            elementName = elementName ?: "",
            attributes,
            mergedSelector,
            parentStyle,
            dynamicLightColorScheme,
            dynamicDarkColorScheme,
            fontSizeMultiplier,
        )
    }
}

/// Modifier helpers

internal fun Modifier.snyggBackground(
    style: SnyggSinglePropertySet,
    default: Color = Color.Unspecified,
    shape: Shape = style.shape(),
    allowClip: Boolean = true,
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
    if (allowClip && style.clip !is SnyggNoValue) {
        return modifier.clip(shape)
    }
    return modifier
}

internal fun Modifier.snyggBorder(
    style: SnyggSinglePropertySet,
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

internal fun Modifier.snyggMargin(
    style: SnyggSinglePropertySet,
): Modifier {
    return when (style.margin) {
        is SnyggPaddingValue -> this.padding(style.margin.values)
        else -> return this
    }
}

internal fun Modifier.snyggPadding(
    style: SnyggSinglePropertySet,
    default: PaddingValues? = null,
): Modifier {
    return when (style.padding) {
        is SnyggPaddingValue -> this.padding(style.padding.values)
        else if (default != null) -> this.padding(default)
        else -> return this
    }
}

internal fun Modifier.snyggShadow(
    style: SnyggSinglePropertySet,
    elevation: Dp = style.shadowElevation.dpSize().takeOrElse { 0.dp }.coerceAtLeast(0.dp),
    shape: Shape = style.shape(),
    color: Color = style.shadowColor.colorOrDefault(default = DefaultShadowColor),
): Modifier {
    return this.shadow(elevation, shape, clip = false, ambientColor = color, spotColor = color)
}

@Composable
internal fun Modifier.snyggIconSize(
    style: SnyggSinglePropertySet,
): Modifier {
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

internal fun SnyggValue.uriOrNull(): String? {
    return when (this) {
        is SnyggUriValue -> this.uri
        else -> null
    }
}
