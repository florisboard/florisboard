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

package dev.patrickgold.florisboard.snygg

import androidx.annotation.FloatRange
import androidx.annotation.IntRange
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import dev.patrickgold.florisboard.snygg.value.RgbaColor
import dev.patrickgold.florisboard.snygg.value.SnyggDpSizeValue
import dev.patrickgold.florisboard.snygg.value.SnyggImageRefValue
import dev.patrickgold.florisboard.snygg.value.SnyggImplicitInheritValue
import dev.patrickgold.florisboard.snygg.value.SnyggPercentageSizeValue
import dev.patrickgold.florisboard.snygg.value.SnyggSolidColorValue
import dev.patrickgold.florisboard.snygg.value.SnyggSpSizeValue
import dev.patrickgold.florisboard.snygg.value.SnyggValue

class SnyggPropertySet(val properties: Map<String, SnyggValue>) {
    val width = properties[Snygg.Width] ?: SnyggImplicitInheritValue
    val height = properties[Snygg.Height] ?: SnyggImplicitInheritValue

    val background = properties[Snygg.Background] ?: SnyggImplicitInheritValue

    val borderTop = properties[Snygg.BorderTop] ?: properties[Snygg.Border] ?: SnyggImplicitInheritValue
    val borderBottom = properties[Snygg.BorderBottom] ?: properties[Snygg.Border] ?: SnyggImplicitInheritValue
    val borderStart = properties[Snygg.BorderStart] ?: properties[Snygg.Border] ?: SnyggImplicitInheritValue
    val borderEnd = properties[Snygg.BorderEnd] ?: properties[Snygg.Border] ?: SnyggImplicitInheritValue

    val fontFamily = properties[Snygg.FontFamily] ?: SnyggImplicitInheritValue
    val fontSize = properties[Snygg.FontSize] ?: SnyggImplicitInheritValue
    val fontStyle = properties[Snygg.FontStyle] ?: SnyggImplicitInheritValue
    val fontVariant = properties[Snygg.FontVariant] ?: SnyggImplicitInheritValue
    val fontWeight = properties[Snygg.FontWeight] ?: SnyggImplicitInheritValue

    val foreground = properties[Snygg.Foreground] ?: SnyggImplicitInheritValue

    val shadow = properties[Snygg.Shadow] ?: SnyggImplicitInheritValue

    fun edit() = SnyggPropertySetEditor(properties)
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

    var borderTop: SnyggValue?
        get() =  getProperty(Snygg.BorderTop)
        set(v) = setProperty(Snygg.BorderTop, v)
    var borderBottom: SnyggValue?
        get() =  getProperty(Snygg.BorderBottom)
        set(v) = setProperty(Snygg.BorderBottom, v)
    var borderStart: SnyggValue?
        get() =  getProperty(Snygg.BorderStart)
        set(v) = setProperty(Snygg.BorderStart, v)
    var borderEnd: SnyggValue?
        get() =  getProperty(Snygg.BorderEnd)
        set(v) = setProperty(Snygg.BorderEnd, v)

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

    var foreground: SnyggValue?
        get() =  getProperty(Snygg.Foreground)
        set(v) = setProperty(Snygg.Foreground, v)

    var shadow: SnyggValue?
        get() =  getProperty(Snygg.Shadow)
        set(v) = setProperty(Snygg.Shadow, v)

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
}
