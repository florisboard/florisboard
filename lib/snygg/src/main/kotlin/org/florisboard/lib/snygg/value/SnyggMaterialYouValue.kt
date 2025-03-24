/*
 * Copyright (C) 2023-2025 The FlorisBoard Contributors
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

package org.florisboard.lib.snygg.value

import android.content.Context
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.isUnspecified
import org.florisboard.lib.android.AndroidVersion
import org.florisboard.lib.color.ColorMappings
import org.florisboard.lib.color.ColorPalette
import org.florisboard.lib.color.getColor
import org.florisboard.lib.snygg.value.MaterialYouColor.ColorName
import org.florisboard.lib.snygg.value.MaterialYouColor.ColorNameId
import org.florisboard.lib.snygg.value.MaterialYouColor.darkColorName
import org.florisboard.lib.snygg.value.MaterialYouColor.lightColorName

sealed interface SnyggMaterialYouValue : SnyggAppearanceValue {
    val colorName: String
    val dark: Boolean

    fun loadColor(context: Context) = MaterialYouColor.loadColor(context, colorName, dark)
}

sealed interface SnyggMaterialYouValueEncoder<T : SnyggMaterialYouValue> : SnyggValueEncoder {
    val clazz: Class<T>
    val valueName: String

    override val spec: SnyggValueSpec
        get() = SnyggValueSpec {
            function(name = valueName) {
                commaList {
                    +string(id = ColorNameId, regex = ColorName)
                }
            }
        }

    override fun defaultValue() = newInstance(ColorPalette.Primary.id)

    override fun serialize(v: SnyggValue) = runCatching<String> {
        require(v::class.java == clazz)

        val map = snyggIdToValueMapOf(ColorNameId to (v as SnyggMaterialYouValue).colorName)
        return@runCatching spec.pack(map)
    }

    override fun deserialize(v: String) = runCatching<SnyggValue> {
        val map = snyggIdToValueMapOf()
        spec.parse(v, map)

        return@runCatching newInstance(map.getOrThrow<String>(ColorNameId))
    }

    fun newInstance(colorName: String): T
}

data class SnyggMaterialYouLightColorValue(override val colorName: String, override val dark: Boolean = false) :
    SnyggMaterialYouValue {

    companion object : SnyggMaterialYouValueEncoder<SnyggMaterialYouLightColorValue> {
        override val clazz = SnyggMaterialYouLightColorValue::class.java
        override val valueName = lightColorName

        override fun newInstance(colorName: String) = SnyggMaterialYouLightColorValue(colorName)
    }

    override fun encoder() = Companion
}

data class SnyggMaterialYouDarkColorValue(override val colorName: String, override val dark: Boolean = true) :
    SnyggMaterialYouValue {

    companion object : SnyggMaterialYouValueEncoder<SnyggMaterialYouDarkColorValue> {
        override val clazz = SnyggMaterialYouDarkColorValue::class.java
        override val valueName = darkColorName

        override fun newInstance(colorName: String) = SnyggMaterialYouDarkColorValue(colorName)
    }

    override fun encoder() = Companion
}

@Suppress("ConstPropertyName")
object MaterialYouColor {
    const val ColorNameId = "name"
    val ColorName = """^\w*$""".toRegex()

    const val lightColorName = "dynamic-light-color"
    const val darkColorName = "dynamic-dark-color"

    private var lightColorScheme: ColorScheme? = null
    private var darkColorScheme: ColorScheme? = null

    private var accentColor: Color = Color.Unspecified

    fun updateAccentColor(newColor: Color) {
        accentColor = newColor
        resetColorSchemeCache()
    }

    private fun getAndCacheColorScheme(context: Context, dark: Boolean): ColorScheme {
        return if (dark) {
            if (darkColorScheme == null) {
                darkColorScheme = if (AndroidVersion.ATLEAST_API31_S && accentColor.isUnspecified) {
                    dynamicDarkColorScheme(context)
                } else {
                    ColorMappings.getColorSchemeOrDefault(color = accentColor, true)
                }
            }
            darkColorScheme!!
        } else {
            if (lightColorScheme == null) {
                lightColorScheme = if (AndroidVersion.ATLEAST_API31_S && accentColor.isUnspecified) {
                    dynamicLightColorScheme(context)
                } else {
                    ColorMappings.getColorSchemeOrDefault(color = accentColor, false)
                }
            }
            lightColorScheme!!
        }
    }

    fun resetColorSchemeCache() {
        lightColorScheme = null
        darkColorScheme = null
    }

    fun loadColor(context: Context, colorName: String, dark: Boolean): Color {
        return getAndCacheColorScheme(context, dark).getColor(colorName)
    }
}
