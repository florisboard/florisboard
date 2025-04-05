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

package org.florisboard.lib.snygg

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement
import org.florisboard.lib.snygg.value.SnyggCustomFontFamilyValue
import org.florisboard.lib.snygg.value.SnyggFontStyleValue
import org.florisboard.lib.snygg.value.SnyggFontWeightValue
import org.florisboard.lib.snygg.value.SnyggGenericFontFamilyValue
import org.florisboard.lib.snygg.value.SnyggLineClampValue
import org.florisboard.lib.snygg.value.SnyggObjectFitValue
import org.florisboard.lib.snygg.value.SnyggShapeValue
import org.florisboard.lib.snygg.value.SnyggSpSizeValue
import org.florisboard.lib.snygg.value.SnyggStaticColorValue
import org.florisboard.lib.snygg.value.SnyggTextAlignValue
import org.florisboard.lib.snygg.value.SnyggTextDecorationLineValue
import org.florisboard.lib.snygg.value.SnyggTextOverflowValue
import org.florisboard.lib.snygg.value.SnyggUndefinedValue
import org.florisboard.lib.snygg.value.SnyggValue

data class SnyggPropertySet internal constructor(
    val properties: Map<String, SnyggValue> = emptyMap(),
) {
    val background = properties[Snygg.Background] ?: SnyggUndefinedValue
    val foreground = properties[Snygg.Foreground] ?: SnyggUndefinedValue

    val backgroundImage = properties[Snygg.BackgroundImage] ?: SnyggUndefinedValue
    val objectFit = properties[Snygg.ObjectFit] ?: SnyggUndefinedValue

    val borderColor = properties[Snygg.BorderColor] ?: SnyggUndefinedValue
    val borderStyle = properties[Snygg.BorderStyle] ?: SnyggUndefinedValue
    val borderWidth = properties[Snygg.BorderWidth] ?: SnyggUndefinedValue

    val fontFamily = properties[Snygg.FontFamily] ?: SnyggUndefinedValue
    val fontSize = properties[Snygg.FontSize] ?: SnyggUndefinedValue
    val fontStyle = properties[Snygg.FontStyle] ?: SnyggUndefinedValue
    val fontWeight = properties[Snygg.FontWeight] ?: SnyggUndefinedValue
    val letterSpacing = properties[Snygg.LetterSpacing] ?: SnyggUndefinedValue

    val lineClamp = properties[Snygg.LineClamp] ?: SnyggUndefinedValue
    val lineHeight = properties[Snygg.LineHeight] ?: SnyggUndefinedValue

    val textAlign = properties[Snygg.TextAlign] ?: SnyggUndefinedValue
    val textDecorationLine = properties[Snygg.TextDecorationLine] ?: SnyggUndefinedValue
    val textOverflow = properties[Snygg.TextOverflow] ?: SnyggUndefinedValue

    val margin = properties[Snygg.Margin] ?: SnyggUndefinedValue
    val padding = properties[Snygg.Padding] ?: SnyggUndefinedValue

    val shadowColor = properties[Snygg.ShadowColor] ?: SnyggUndefinedValue
    val shadowElevation = properties[Snygg.ShadowElevation] ?: SnyggUndefinedValue

    val shape = properties[Snygg.Shape] ?: SnyggUndefinedValue

    val src = properties[Snygg.Src] ?: SnyggUndefinedValue

    fun edit() = SnyggPropertySetEditor(properties)

    override fun toString(): String {
        return properties.toString()
    }

    companion object {
        fun from(rule: SnyggRule, jsonObject: JsonObject): SnyggPropertySet {
            val editor = SnyggPropertySetEditor()
            propLoop@ for ((property, valueElem) in jsonObject) {
                val encoders = SnyggSpec.encodersOf(rule, property)
                requireNotNull(encoders) { "Unknown or disallowed property: $property" }
                val valueStr = Json.decodeFromJsonElement<String>(valueElem)
                for (encoder in encoders) {
                    val value = encoder.deserialize(valueStr).getOrNull()
                    if (value != null) {
                        editor.properties[property] = value
                        continue@propLoop
                    }
                }
                error("No value encoder found for property: $property")
            }
            return editor.build()
        }
    }

    fun toJsonObject(): JsonObject {
        val rawProperties = properties.mapValues { (_, value) ->
            val valueStr = value.encoder().serialize(value).getOrThrow()
            Json.encodeToJsonElement(valueStr)
        }
        return JsonObject(rawProperties)
    }

    fun background(default: Color = Color.Unspecified): Color {
        return when (background) {
            is SnyggStaticColorValue -> background.color
            else -> default
        }
    }

    fun foreground(default: Color = Color.Unspecified): Color {
        return when (foreground) {
            is SnyggStaticColorValue -> foreground.color
            else -> default
        }
    }

    fun objectFit(default: ContentScale = ContentScale.Fit): ContentScale {
        return when (objectFit) {
            is SnyggObjectFitValue -> objectFit.contentScale
            else -> default
        }
    }

    fun fontFamily(theme: SnyggTheme, default: FontFamily? = null): FontFamily? {
        return when (val family = fontFamily) {
            is SnyggGenericFontFamilyValue -> family.fontFamily
            is SnyggCustomFontFamilyValue -> theme.getFontFamily(family.fontName)
            else -> default
        }
    }

    fun fontSize(default: TextUnit = TextUnit.Unspecified): TextUnit {
        return when (fontSize) {
            is SnyggSpSizeValue -> fontSize.sp
            else -> default
        }
    }

    fun fontStyle(default: FontStyle? = null): FontStyle? {
        return when (fontStyle) {
            is SnyggFontStyleValue -> fontStyle.fontStyle
            else -> default
        }
    }

    fun fontWeight(default: FontWeight? = null): FontWeight? {
        return when (fontWeight) {
            is SnyggFontWeightValue -> fontWeight.fontWeight
            else -> default
        }
    }

    fun letterSpacing(default: TextUnit = TextUnit.Unspecified): TextUnit {
        return when (letterSpacing) {
            is SnyggSpSizeValue -> letterSpacing.sp
            else -> default
        }
    }

    fun lineClamp(default: Int = Int.MAX_VALUE): Int {
        return when (lineClamp) {
            is SnyggLineClampValue -> lineClamp.maxLines
            else -> default
        }
    }

    fun lineHeight(default: TextUnit = TextUnit.Unspecified): TextUnit {
        return when (lineHeight) {
            is SnyggSpSizeValue -> lineHeight.sp
            else -> default
        }
    }

    fun textAlign(default: TextAlign? = null): TextAlign? {
        return when (textAlign) {
            is SnyggTextAlignValue -> textAlign.textAlign
            else -> default
        }
    }

    fun textDecorationLine(default: TextDecoration? = null): TextDecoration? {
        return when (textDecorationLine) {
            is SnyggTextDecorationLineValue -> textDecorationLine.textDecoration
            else -> default
        }
    }

    fun textOverflow(default: TextOverflow = TextOverflow.Clip): TextOverflow {
        return when (textOverflow) {
            is SnyggTextOverflowValue -> textOverflow.textOverflow
            else -> default
        }
    }

    fun shape(): Shape {
        return when (shape) {
            is SnyggShapeValue -> shape.shape
            else -> RectangleShape
        }
    }
}
