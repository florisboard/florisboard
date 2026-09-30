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

import androidx.annotation.FloatRange
import androidx.annotation.IntRange
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.isSpecified
import org.florisboard.lib.snygg.value.RgbaColor
import org.florisboard.lib.snygg.value.SnyggCircleShapeValue
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
import org.florisboard.lib.snygg.value.SnyggUriValue
import org.florisboard.lib.snygg.value.SnyggValue
import org.florisboard.lib.snygg.value.SnyggYesValue
import org.florisboard.lib.snygg.value.isInherit
import org.florisboard.lib.snygg.value.isUndefined
import java.util.UUID

sealed interface SnyggPropertySetEditor {
    fun build(): SnyggPropertySet
}

class SnyggSinglePropertySetEditor(initProperties: Map<String, SnyggValue>? = null) : SnyggPropertySetEditor {
    val uuid = UUID.randomUUID().toString()
    val properties = mutableMapOf<String, SnyggValue>()

    init {
        if (initProperties != null) {
            properties.putAll(initProperties)
        }
    }

    @Suppress("NOTHING_TO_INLINE")
    private inline fun getProperty(key: String): SnyggValue? {
        return properties[key]
    }

    private fun setProperty(key: String, value: SnyggValue?) {
        if (value == null) {
            properties.remove(key)
        } else {
            properties[key] = value
        }
    }

    internal fun applyAll(
        thisStyle: SnyggSinglePropertySet,
        parentStyle: SnyggSinglePropertySet,
        fontSizeMultiplier: Float,
    ) {
        for ((property, value) in thisStyle.properties) {
            when {
                value.isUndefined() -> setProperty(property, null)
                value.isInherit() -> setProperty(property, parentStyle.properties[property])
                else -> {
                    val transformedValue = when (value) {
                        is SnyggSpSizeValue if value.sp.isSpecified -> {
                            SnyggSpSizeValue(value.sp * fontSizeMultiplier)
                        }
                        else -> {
                            value
                        }
                    }
                    setProperty(property, transformedValue)
                }
            }
        }
        inheritImplicitly(parentStyle)
    }

    internal fun inheritImplicitly(parentStyle: SnyggSinglePropertySet) {
        // TODO: pattern properties??
        for ((property, propertySpec) in SnyggSpec.elementsSpec.properties) {
            if (propertySpec.inheritsImplicitly() && !properties.contains(property)) {
                // inherit implicitly
                setProperty(property, parentStyle.properties[property])
            }
        }
    }

    override fun build() = SnyggSinglePropertySet(properties.toMap())

    infix fun String.to(v: SnyggValue) {
        properties[this] = v
    }

    @Deprecated(
        level = DeprecationLevel.ERROR,
        message = "Only snygg values are allowed",
    )
    infix fun String.to(v: Any): Nothing {
        throw IllegalArgumentException("Only snygg values are allowed (given value: $v)")
    }

    var background: SnyggValue?
        get() =  getProperty(Snygg.Background)
        set(v) = setProperty(Snygg.Background, v)
    var foreground: SnyggValue?
        get() =  getProperty(Snygg.Foreground)
        set(v) = setProperty(Snygg.Foreground, v)

    var backgroundImage: SnyggValue?
        get() =  getProperty(Snygg.BackgroundImage)
        set(v) = setProperty(Snygg.BackgroundImage, v)
    var contentScale: SnyggValue?
        get() =  getProperty(Snygg.ContentScale)
        set(v) = setProperty(Snygg.ContentScale, v)

    var borderColor: SnyggValue?
        get() =  getProperty(Snygg.BorderColor)
        set(v) = setProperty(Snygg.BorderColor, v)
    var borderStyle: SnyggValue?
        get() =  getProperty(Snygg.BorderStyle)
        set(v) = setProperty(Snygg.BorderStyle, v)
    var borderWidth: SnyggValue?
        get() =  getProperty(Snygg.BorderWidth)
        set(v) = setProperty(Snygg.BorderWidth, v)

    var fontFamily: SnyggValue?
        get() =  getProperty(Snygg.FontFamily)
        set(v) = setProperty(Snygg.FontFamily, v)
    var fontSize: SnyggValue?
        get() =  getProperty(Snygg.FontSize)
        set(v) = setProperty(Snygg.FontSize, v)
    var fontStyle: SnyggValue?
        get() =  getProperty(Snygg.FontStyle)
        set(v) = setProperty(Snygg.FontStyle, v)
    var fontWeight: SnyggValue?
        get() =  getProperty(Snygg.FontWeight)
        set(v) = setProperty(Snygg.FontWeight, v)
    var letterSpacing: SnyggValue?
        get() =  getProperty(Snygg.LetterSpacing)
        set(v) = setProperty(Snygg.LetterSpacing, v)

    var lineHeight: SnyggValue?
        get() =  getProperty(Snygg.LineHeight)
        set(v) = setProperty(Snygg.LineHeight, v)

    var margin: SnyggValue?
        get() =  getProperty(Snygg.Margin)
        set(v) = setProperty(Snygg.Margin, v)
    var padding: SnyggValue?
        get() =  getProperty(Snygg.Padding)
        set(v) = setProperty(Snygg.Padding, v)

    var shadowColor: SnyggValue?
        get() =  getProperty(Snygg.ShadowColor)
        set(v) = setProperty(Snygg.ShadowColor, v)
    var shadowElevation: SnyggValue?
        get() =  getProperty(Snygg.ShadowElevation)
        set(v) = setProperty(Snygg.ShadowElevation, v)

    var shape: SnyggValue?
        get() =  getProperty(Snygg.Shape)
        set(v) = setProperty(Snygg.Shape, v)
    var clip: SnyggValue?
        get() =  getProperty(Snygg.Clip)
        set(v) = setProperty(Snygg.Clip, v)

    var src: SnyggValue?
        get() =  getProperty(Snygg.Src)
        set(v) = setProperty(Snygg.Src, v)

    var textAlign: SnyggValue?
        get() =  getProperty(Snygg.TextAlign)
        set(v) = setProperty(Snygg.TextAlign, v)
    var textDecorationLine: SnyggValue?
        get() =  getProperty(Snygg.TextDecorationLine)
        set(v) = setProperty(Snygg.TextDecorationLine, v)
    var textMaxLines: SnyggValue?
        get() =  getProperty(Snygg.TextMaxLines)
        set(v) = setProperty(Snygg.TextMaxLines, v)
    var textOverflow: SnyggValue?
        get() =  getProperty(Snygg.TextOverflow)
        set(v) = setProperty(Snygg.TextOverflow, v)

    fun rgbaColor(
        @IntRange(from = RgbaColor.ColorRangeMin.toLong(), to = RgbaColor.ColorRangeMax.toLong())
        r: Int,
        @IntRange(from = RgbaColor.ColorRangeMin.toLong(), to = RgbaColor.ColorRangeMax.toLong())
        g: Int,
        @IntRange(from = RgbaColor.ColorRangeMin.toLong(), to = RgbaColor.ColorRangeMax.toLong())
        b: Int,
        @FloatRange(from = RgbaColor.AlphaRangeMin.toDouble(), to = RgbaColor.AlphaRangeMax.toDouble())
        a: Float = RgbaColor.AlphaRangeMax,
    ): SnyggStaticColorValue {
        require(r in RgbaColor.ColorRange)
        require(g in RgbaColor.ColorRange)
        require(b in RgbaColor.ColorRange)
        require(a in RgbaColor.AlphaRange)
        val red = r.toFloat() / RgbaColor.ColorRangeMax
        val green = g.toFloat() / RgbaColor.ColorRangeMax
        val blue = b.toFloat() / RgbaColor.ColorRangeMax
        return SnyggStaticColorValue(Color(red, green, blue, a))
    }

    fun dynamicLightColor(name: String): SnyggDynamicLightColorValue {
        return SnyggDynamicLightColorValue(name)
    }

    fun dynamicDarkColor(name: String): SnyggDynamicDarkColorValue {
        return SnyggDynamicDarkColorValue(name)
    }

    fun genericFontFamily(fontFamily: FontFamily): SnyggGenericFontFamilyValue {
        return SnyggGenericFontFamilyValue(fontFamily)
    }

    fun fontStyle(fontStyle: FontStyle): SnyggFontStyleValue {
        return SnyggFontStyleValue(fontStyle)
    }

    fun fontWeight(fontWeight: FontWeight): SnyggFontWeightValue {
        return SnyggFontWeightValue(fontWeight)
    }

    fun textAlign(textAlign: TextAlign): SnyggTextAlignValue {
        return SnyggTextAlignValue(textAlign)
    }

    fun textDecorationLine(textDecoration: TextDecoration): SnyggTextDecorationLineValue {
        return SnyggTextDecorationLineValue(textDecoration)
    }

    fun textMaxLinesNone(): SnyggTextMaxLinesValue {
        return SnyggTextMaxLinesValue.defaultValue()
    }

    fun textMaxLines(maxLines: Int): SnyggTextMaxLinesValue {
        require(maxLines >= 1)
        return SnyggTextMaxLinesValue(maxLines)
    }

    fun textOverflow(textOverflow: TextOverflow): SnyggTextOverflowValue {
        return SnyggTextOverflowValue(textOverflow)
    }

    fun rectangleShape(): SnyggRectangleShapeValue {
        return SnyggRectangleShapeValue()
    }

    fun circleShape(): SnyggCircleShapeValue {
        return SnyggCircleShapeValue()
    }

    fun cutCornerShape(cornerSize: Dp): SnyggCutCornerDpShapeValue {
        return SnyggCutCornerDpShapeValue(cornerSize, cornerSize, cornerSize, cornerSize)
    }

    fun cutCornerShape(
        topStart: Dp,
        topEnd: Dp,
        bottomEnd: Dp,
        bottomStart: Dp,
    ): SnyggCutCornerDpShapeValue {
        return SnyggCutCornerDpShapeValue(topStart, topEnd, bottomEnd, bottomStart)
    }

    fun cutCornerShape(cornerSize: Int): SnyggCutCornerPercentShapeValue {
        return SnyggCutCornerPercentShapeValue(cornerSize, cornerSize, cornerSize, cornerSize)
    }

    fun cutCornerShape(
        topStart: Int,
        topEnd: Int,
        bottomEnd: Int,
        bottomStart: Int,
    ): SnyggCutCornerPercentShapeValue {
        return SnyggCutCornerPercentShapeValue(topStart, topEnd, bottomEnd, bottomStart)
    }

    fun roundedCornerShape(cornerSize: Dp): SnyggRoundedCornerDpShapeValue {
        return SnyggRoundedCornerDpShapeValue(cornerSize, cornerSize, cornerSize, cornerSize)
    }

    fun roundedCornerShape(
        topStart: Dp,
        topEnd: Dp,
        bottomEnd: Dp,
        bottomStart: Dp,
    ): SnyggRoundedCornerDpShapeValue {
        return SnyggRoundedCornerDpShapeValue(topStart, topEnd, bottomEnd, bottomStart)
    }

    fun roundedCornerShape(cornerSize: Int): SnyggRoundedCornerPercentShapeValue {
        return SnyggRoundedCornerPercentShapeValue(cornerSize, cornerSize, cornerSize, cornerSize)
    }

    fun roundedCornerShape(
        topStart: Int,
        topEnd: Int,
        bottomEnd: Int,
        bottomStart: Int,
    ): SnyggRoundedCornerPercentShapeValue {
        return SnyggRoundedCornerPercentShapeValue(topStart, topEnd, bottomEnd, bottomStart)
    }

    fun padding(
        start: Dp,
        top: Dp,
        end: Dp,
        bottom: Dp,
    ): SnyggPaddingValue {
        return SnyggPaddingValue(PaddingValues(start, top, end, bottom))
    }

    fun padding(
        horizontal: Dp,
        vertical: Dp,
    ): SnyggPaddingValue {
        return SnyggPaddingValue(PaddingValues(horizontal, vertical))
    }

    fun padding(all: Dp): SnyggPaddingValue {
        return SnyggPaddingValue(PaddingValues(all))
    }

    fun size(dp: Dp): SnyggDpSizeValue {
        return SnyggDpSizeValue(dp)
    }

    fun fontSize(sp: TextUnit): SnyggSpSizeValue {
        return SnyggSpSizeValue(sp)
    }

    fun size(percentage: Float): SnyggPercentageSizeValue {
        return SnyggPercentageSizeValue(percentage)
    }

    fun uri(uri: String): SnyggUriValue {
        return SnyggUriValue(uri)
    }

    fun `var`(key: String): SnyggDefinedVarValue {
        return SnyggDefinedVarValue(key)
    }

    fun inherit(): SnyggInheritValue {
        return SnyggInheritValue
    }

    fun yes(): SnyggYesValue {
        return SnyggYesValue
    }

    fun no(): SnyggNoValue {
        return SnyggNoValue
    }
}

class SnyggMultiplePropertySetsEditor(initSets: List<SnyggSinglePropertySet>? = null) : SnyggPropertySetEditor {
    val sets = mutableListOf<SnyggSinglePropertySetEditor>()

    init {
        initSets?.forEach { sets.add(it.edit()) }
    }

    fun add(configure: SnyggSinglePropertySetEditor.() -> Unit) {
        val editor = SnyggSinglePropertySetEditor()
        editor.configure()
        sets.add(editor)
    }

    override fun build() = SnyggMultiplePropertySets(sets.map { it.build() })
}
