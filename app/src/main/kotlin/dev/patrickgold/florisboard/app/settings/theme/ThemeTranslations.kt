/*
 * Copyright (C) 2022-2025 The FlorisBoard Contributors
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

package dev.patrickgold.florisboard.app.settings.theme

import android.content.Context
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.ime.theme.FlorisImeUi
import dev.patrickgold.florisboard.lib.UnicodeCtrlChar
import dev.patrickgold.jetpref.material.ui.ColorRepresentation
import org.florisboard.lib.kotlin.simpleNameOrEnclosing
import org.florisboard.lib.snygg.Snygg
import org.florisboard.lib.snygg.SnyggElementRule
import org.florisboard.lib.snygg.value.SnyggCircleShapeValue
import org.florisboard.lib.snygg.value.SnyggCustomFontFamilyValue
import org.florisboard.lib.snygg.value.SnyggCutCornerDpShapeValue
import org.florisboard.lib.snygg.value.SnyggCutCornerPercentShapeValue
import org.florisboard.lib.snygg.value.SnyggDefinedVarValue
import org.florisboard.lib.snygg.value.SnyggDpSizeValue
import org.florisboard.lib.snygg.value.SnyggDynamicDarkColorValue
import org.florisboard.lib.snygg.value.SnyggDynamicLightColorValue
import org.florisboard.lib.snygg.value.SnyggFontStyleValue
import org.florisboard.lib.snygg.value.SnyggFontWeightValue
import org.florisboard.lib.snygg.value.SnyggGenericFontFamilyValue
import org.florisboard.lib.snygg.value.SnyggInheritValue
import org.florisboard.lib.snygg.value.SnyggNoValue
import org.florisboard.lib.snygg.value.SnyggContentScaleValue
import org.florisboard.lib.snygg.value.SnyggPaddingValue
import org.florisboard.lib.snygg.value.SnyggPercentageSizeValue
import org.florisboard.lib.snygg.value.SnyggRectangleShapeValue
import org.florisboard.lib.snygg.value.SnyggRoundedCornerDpShapeValue
import org.florisboard.lib.snygg.value.SnyggRoundedCornerPercentShapeValue
import org.florisboard.lib.snygg.value.SnyggSpSizeValue
import org.florisboard.lib.snygg.value.SnyggStaticColorValue
import org.florisboard.lib.snygg.value.SnyggTextAlignValue
import org.florisboard.lib.snygg.value.SnyggTextDecorationLineValue
import org.florisboard.lib.snygg.value.SnyggTextMaxLinesValue
import org.florisboard.lib.snygg.value.SnyggTextOverflowValue
import org.florisboard.lib.snygg.value.SnyggUndefinedValue
import org.florisboard.lib.snygg.value.SnyggUriValue
import org.florisboard.lib.snygg.value.SnyggValue
import org.florisboard.lib.snygg.value.SnyggValueEncoder
import org.florisboard.lib.snygg.value.SnyggYesValue

internal fun Context.translateElementName(rule: SnyggElementRule, level: SnyggLevel): String {
    return translateElementName(rule.elementName, level) ?: rule.elementName
}

internal fun Context.translateElementName(element: String, level: SnyggLevel): String? {
    return when (level) {
        SnyggLevel.DEVELOPER -> null
        else -> FlorisImeUi.elementNamesToTranslation[element]?.let { getString(it) }
    }
}

private val PropertyNameMap = mapOf(
    Snygg.Background to R.string.snygg__property_name__background,
    Snygg.Foreground to R.string.snygg__property_name__foreground,
    Snygg.BackgroundImage to R.string.snygg__property_name__background_image,
    Snygg.ContentScale to R.string.snygg__property_name__content_scale,
    Snygg.BorderColor to R.string.snygg__property_name__border_color,
    Snygg.BorderStyle to R.string.snygg__property_name__border_style,
    Snygg.BorderWidth to R.string.snygg__property_name__border_width,
    Snygg.FontFamily to R.string.snygg__property_name__font_family,
    Snygg.FontSize to R.string.snygg__property_name__font_size,
    Snygg.FontStyle to R.string.snygg__property_name__font_style,
    Snygg.FontWeight to R.string.snygg__property_name__font_weight,
    Snygg.LetterSpacing to R.string.snygg__property_name__letter_spacing,
    Snygg.LineHeight to R.string.snygg__property_name__line_height,
    Snygg.Margin to R.string.snygg__property_name__margin,
    Snygg.Padding to R.string.snygg__property_name__padding,
    Snygg.ShadowColor to R.string.snygg__property_name__shadow_color,
    Snygg.ShadowElevation to R.string.snygg__property_name__shadow_elevation,
    Snygg.Shape to R.string.snygg__property_name__shape,
    Snygg.Clip to R.string.snygg__property_name__clip,
    Snygg.Src to R.string.snygg__property_name__src,
    Snygg.TextAlign to R.string.snygg__property_name__text_align,
    Snygg.TextDecorationLine to R.string.snygg__property_name__text_decoration_line,
    Snygg.TextMaxLines to R.string.snygg__property_name__text_max_lines,
    Snygg.TextOverflow to R.string.snygg__property_name__text_overflow,
    "--primary" to R.string.snygg__property_name__var_primary,
    "--primary-variant" to R.string.snygg__property_name__var_primary_variant,
    "--secondary" to R.string.snygg__property_name__var_secondary,
    "--secondary-variant" to R.string.snygg__property_name__var_secondary_variant,
    "--background" to R.string.snygg__property_name__var_background,
    "--surface" to R.string.snygg__property_name__var_surface,
    "--surface-variant" to R.string.snygg__property_name__var_surface_variant,
    "--on-primary" to R.string.snygg__property_name__var_on_primary,
    "--on-secondary" to R.string.snygg__property_name__var_on_secondary,
    "--on-background" to R.string.snygg__property_name__var_on_background,
    "--on-surface" to R.string.snygg__property_name__var_on_surface,
    "--on-surface-variant" to R.string.snygg__property_name__var_on_surface_variant,
    "--shape" to R.string.snygg__property_name__var_shape,
    "--shape-variant" to R.string.snygg__property_name__var_shape_variant
)

internal fun Context.translatePropertyName(propertyName: String, level: SnyggLevel): String {
    return when (level) {
        SnyggLevel.DEVELOPER -> null
        else -> PropertyNameMap[propertyName]
    }.let { resId ->
        when {
            resId != null -> {
                getString(resId)
            }
            propertyName.isBlank() -> {
                getString(R.string.general__select_dropdown_value_placeholder)
            }
            else -> {
                propertyName
            }
        }
    }
}

internal fun Context.translatePropertyValue(
    propertyValue: SnyggValue,
    level: SnyggLevel,
    colorRepresentation: ColorRepresentation,
): String {
    return when (propertyValue) {
        is SnyggStaticColorValue -> {
            colorRepresentation.formatColor(propertyValue.color, withAlpha = true)
        }
        else -> when (level) {
            SnyggLevel.DEVELOPER -> null
            else -> when (propertyValue) {
                is SnyggDefinedVarValue -> translatePropertyName(propertyValue.key, level)
                else -> null
            }
        } ?: buildString {
            append(UnicodeCtrlChar.LeftToRightIsolate)
            append(propertyValue.encoder().serialize(propertyValue).getOrElse { propertyValue.toString() })
            append(UnicodeCtrlChar.PopDirectionalIsolate)
        }
    }
}

private val PropertyValueEncoderNameMap = mapOf(
    SnyggUndefinedValue to R.string.general__select_dropdown_value_placeholder,
    SnyggInheritValue to R.string.snygg__property_value__explicit_inherit,
    SnyggDefinedVarValue to R.string.snygg__property_value__defined_var,
    SnyggYesValue to R.string.snygg__property_value__yes,
    SnyggNoValue to R.string.snygg__property_value__no,
    SnyggStaticColorValue to R.string.snygg__property_value__solid_color,
    SnyggDynamicLightColorValue to R.string.snygg__property_value__material_you_light_color,
    SnyggDynamicDarkColorValue to R.string.snygg__property_value__material_you_dark_color,
    SnyggGenericFontFamilyValue to R.string.snygg__property_value__font_family_generic,
    SnyggCustomFontFamilyValue to R.string.snygg__property_value__font_family_custom,
    SnyggFontStyleValue to R.string.snygg__property_value__font_style,
    SnyggFontWeightValue to R.string.snygg__property_value__font_weight,
    SnyggPaddingValue to R.string.snygg__property_value__padding,
    SnyggRectangleShapeValue to R.string.snygg__property_value__rectangle_shape,
    SnyggCircleShapeValue to R.string.snygg__property_value__circle_shape,
    SnyggCutCornerDpShapeValue to R.string.snygg__property_value__cut_corner_shape_dp,
    SnyggCutCornerPercentShapeValue to R.string.snygg__property_value__cut_corner_shape_percent,
    SnyggRoundedCornerDpShapeValue to R.string.snygg__property_value__rounded_corner_shape_dp,
    SnyggRoundedCornerPercentShapeValue to R.string.snygg__property_value__rounded_corner_shape_percent,
    SnyggDpSizeValue to R.string.snygg__property_value__dp_size,
    SnyggSpSizeValue to R.string.snygg__property_value__sp_size,
    SnyggPercentageSizeValue to R.string.snygg__property_value__percentage_size,
    SnyggContentScaleValue to R.string.snygg__property_value__content_scale,
    SnyggTextAlignValue to R.string.snygg__property_value__text_align,
    SnyggTextDecorationLineValue to R.string.snygg__property_value__text_decoration_line,
    SnyggTextMaxLinesValue to R.string.snygg__property_value__text_max_lines,
    SnyggTextOverflowValue to R.string.snygg__property_value__text_overflow,
    SnyggUriValue to R.string.snygg__property_value__uri,
)

internal fun Context.translatePropertyValueEncoderName(encoder: SnyggValueEncoder): String {
    return PropertyValueEncoderNameMap[encoder]?.let { getString(it) }
        ?: encoder::class.simpleNameOrEnclosing().orEmpty()
}
