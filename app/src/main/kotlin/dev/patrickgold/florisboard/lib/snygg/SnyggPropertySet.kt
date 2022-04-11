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

package dev.patrickgold.florisboard.lib.snygg

import androidx.annotation.FloatRange
import androidx.annotation.IntRange
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import dev.patrickgold.florisboard.lib.snygg.value.RgbaColor
import dev.patrickgold.florisboard.lib.snygg.value.SnyggCircleShapeValue
import dev.patrickgold.florisboard.lib.snygg.value.SnyggCutCornerDpShapeValue
import dev.patrickgold.florisboard.lib.snygg.value.SnyggCutCornerPercentShapeValue
import dev.patrickgold.florisboard.lib.snygg.value.SnyggDefinedVarValue
import dev.patrickgold.florisboard.lib.snygg.value.SnyggDpSizeValue
import dev.patrickgold.florisboard.lib.snygg.value.SnyggImageRefValue
import dev.patrickgold.florisboard.lib.snygg.value.SnyggImplicitInheritValue
import dev.patrickgold.florisboard.lib.snygg.value.SnyggPercentageSizeValue
import dev.patrickgold.florisboard.lib.snygg.value.SnyggRectangleShapeValue
import dev.patrickgold.florisboard.lib.snygg.value.SnyggRoundedCornerDpShapeValue
import dev.patrickgold.florisboard.lib.snygg.value.SnyggRoundedCornerPercentShapeValue
import dev.patrickgold.florisboard.lib.snygg.value.SnyggSolidColorValue
import dev.patrickgold.florisboard.lib.snygg.value.SnyggSpSizeValue
import dev.patrickgold.florisboard.lib.snygg.value.SnyggValue

class SnyggPropertySet(val properties: Map<String, SnyggValue>) {
    val width = properties[Snygg.Width] ?: SnyggImplicitInheritValue
    val height = properties[Snygg.Height] ?: SnyggImplicitInheritValue

    val background = properties[Snygg.Background] ?: SnyggImplicitInheritValue
    val foreground = properties[Snygg.Foreground] ?: SnyggImplicitInheritValue

    val borderColor = properties[Snygg.BorderColor] ?: SnyggImplicitInheritValue
    val borderStyle = properties[Snygg.BorderStyle] ?: SnyggImplicitInheritValue
    val borderWidth = properties[Snygg.BorderWidth] ?: SnyggImplicitInheritValue

    val fontFamily = properties[Snygg.FontFamily] ?: SnyggImplicitInheritValue
    val fontSize = properties[Snygg.FontSize] ?: SnyggImplicitInheritValue
    val fontStyle = properties[Snygg.FontStyle] ?: SnyggImplicitInheritValue
    val fontVariant = properties[Snygg.FontVariant] ?: SnyggImplicitInheritValue
    val fontWeight = properties[Snygg.FontWeight] ?: SnyggImplicitInheritValue

    val shadowElevation = properties[Snygg.ShadowElevation] ?: SnyggImplicitInheritValue

    val shape = properties[Snygg.Shape] ?: SnyggImplicitInheritValue

    fun edit() = SnyggPropertySetEditor(properties)

    override fun toString(): String {
        return properties.toString()
    }
}

class SnyggPropertySetEditor(initProperties: Map<String, SnyggValue>? = null) {
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

    fun build() = SnyggPropertySet(properties)

    infix fun String.to(v: SnyggValue) {
        properties[this] = v
    }

    var width: SnyggValue?
        get() =  getProperty(Snygg.Width)
        set(v) = setProperty(Snygg.Width, v)
    var height: SnyggValue?
        get() =  getProperty(Snygg.Height)
        set(v) = setProperty(Snygg.Height, v)

    var background: SnyggValue?
        get() =  getProperty(Snygg.Background)
        set(v) = setProperty(Snygg.Background, v)
    var foreground: SnyggValue?
        get() =  getProperty(Snygg.Foreground)
        set(v) = setProperty(Snygg.Foreground, v)

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
    var fontVariant: SnyggValue?
        get() =  getProperty(Snygg.FontVariant)
        set(v) = setProperty(Snygg.FontVariant, v)
    var fontWeight: SnyggValue?
        get() =  getProperty(Snygg.FontWeight)
        set(v) = setProperty(Snygg.FontWeight, v)

    var shadowElevation: SnyggValue?
        get() =  getProperty(Snygg.ShadowElevation)
        set(v) = setProperty(Snygg.ShadowElevation, v)

    var shape: SnyggValue?
        get() =  getProperty(Snygg.Shape)
        set(v) = setProperty(Snygg.Shape, v)

    fun rgbaColor(
        @IntRange(from = RgbaColor.RedMin.toLong(), to = RgbaColor.RedMax.toLong())
        r: Int,
        @IntRange(from = RgbaColor.GreenMin.toLong(), to = RgbaColor.GreenMax.toLong())
        g: Int,
        @IntRange(from = RgbaColor.BlueMin.toLong(), to = RgbaColor.BlueMax.toLong())
        b: Int,
        @FloatRange(from = RgbaColor.AlphaMin.toDouble(), to = RgbaColor.AlphaMax.toDouble())
        a: Float = RgbaColor.AlphaMax,
    ): SnyggSolidColorValue {
        require(r in RgbaColor.Red)
        require(g in RgbaColor.Green)
        require(b in RgbaColor.Blue)
        require(a in RgbaColor.Alpha)
        val red = r.toFloat() / RgbaColor.RedMax
        val green = g.toFloat() / RgbaColor.GreenMax
        val blue = b.toFloat() / RgbaColor.BlueMax
        return SnyggSolidColorValue(Color(red, green, blue, a))
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

    fun size(dp: Dp): SnyggDpSizeValue {
        return SnyggDpSizeValue(dp)
    }

    fun size(sp: TextUnit): SnyggSpSizeValue {
        return SnyggSpSizeValue(sp)
    }

    fun size(percentage: Float): SnyggPercentageSizeValue {
        return SnyggPercentageSizeValue(percentage)
    }

    fun image(relPath: String): SnyggImageRefValue {
        return SnyggImageRefValue(relPath)
    }

    fun `var`(key: String): SnyggDefinedVarValue {
        return SnyggDefinedVarValue((key))
    }
}
