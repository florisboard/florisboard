/*
 * Copyright (C) 2023 Patrick Goldinger
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
import org.florisboard.lib.snygg.value.MaterialYouColor.ColorName
import org.florisboard.lib.snygg.value.MaterialYouColor.ColorNameId
import org.florisboard.lib.snygg.value.MaterialYouColor.darkColorName
import org.florisboard.lib.snygg.value.MaterialYouColor.lightColorName

sealed interface SnyggMaterialYouValue : SnyggValue {
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

    override fun defaultValue() = newInstance(MaterialYouColor.ColorPalette.Primary.id)

    override fun serialize(v: SnyggValue) = runCatching<String> {
        require(v::class.java == clazz)

        val map = SnyggIdToValueMap.new(ColorNameId to (v as SnyggMaterialYouValue).colorName)
        return@runCatching spec.pack(map)
    }

    override fun deserialize(v: String) = runCatching<SnyggValue> {
        val map = SnyggIdToValueMap.new()
        spec.parse(v, map)

        return@runCatching newInstance(map.getOrThrow<String>(ColorNameId))
    }

    fun newInstance(colorName: String): T
}

object MaterialYouColor {
    const val ColorNameId = "name"
    val ColorName = """^\w*$""".toRegex()

    const val lightColorName = "dynamic-light-color"
    const val darkColorName = "dynamic-dark-color"

    private var lightColorScheme: ColorScheme? = null
    private var darkColorScheme: ColorScheme? = null

    private fun getAndCacheColorScheme(context: Context, dark: Boolean): ColorScheme {
        return if (dark) {
            if (darkColorScheme == null) {
                darkColorScheme = dynamicDarkColorScheme(context)
            }
            darkColorScheme!!
        } else {
            if (lightColorScheme == null) {
                lightColorScheme = dynamicLightColorScheme(context)
            }
            lightColorScheme!!
        }
    }

    fun resetColorSchemeCache() {
        lightColorScheme = null
        darkColorScheme = null
    }

    enum class ColorPalette(val id: String) {
        Primary("primary"),
        OnPrimary("onPrimary"),
        PrimaryContainer("primaryContainer"),
        OnPrimaryContainer("onPrimaryContainer"),
        InversePrimary("inversePrimary"),
        Secondary("secondary"),
        OnSecondary("onSecondary"),
        SecondaryContainer("secondaryContainer"),
        OnSecondaryContainer("onSecondaryContainer"),
        Tertiary("tertiary"),
        OnTertiary("onTertiary"),
        TertiaryContainer("tertiaryContainer"),
        OnTertiaryContainer("onTertiaryContainer"),
        Background("background"),
        OnBackground("onBackground"),
        // Surface("surface"), // removed and replaced by the specific Surface* variants
        OnSurface("onSurface"),
        SurfaceVariant("surfaceVariant"),
        OnSurfaceVariant("onSurfaceVariant"),
        SurfaceTint("surfaceTint"),
        InverseSurface("inverseSurface"),
        InverseOnSurface("inverseOnSurface"),
        Error("error"),
        OnError("onError"),
        ErrorContainer("errorContainer"),
        OnErrorContainer("onErrorContainer"),
        Outline("outline"),
        OutlineVariant("outlineVariant"),
        Scrim("scrim"),
        SurfaceBright("surfaceBright"),
        SurfaceDim("surfaceDim"),
        SurfaceContainer("surfaceContainer"),
        SurfaceContainerHigh("surfaceContainerHigh"),
        SurfaceContainerHighest("surfaceContainerHighest"),
        SurfaceContainerLow("surfaceContainerLow"),
        SurfaceContainerLowest("surfaceContainerLowest")
    }

    val colorNames = ColorPalette.entries.map { it.id }

    fun loadColor(context: Context, colorName: String, dark: Boolean): Color {
        val colorScheme = getAndCacheColorScheme(context, dark)

        return when (colorName) {
            ColorPalette.Primary.id -> colorScheme.primary
            ColorPalette.OnPrimary.id -> colorScheme.onPrimary
            ColorPalette.PrimaryContainer.id -> colorScheme.primaryContainer
            ColorPalette.OnPrimaryContainer.id -> colorScheme.onPrimaryContainer
            ColorPalette.InversePrimary.id -> colorScheme.inversePrimary
            ColorPalette.Secondary.id -> colorScheme.secondary
            ColorPalette.OnSecondary.id -> colorScheme.onSecondary
            ColorPalette.SecondaryContainer.id -> colorScheme.secondaryContainer
            ColorPalette.OnSecondaryContainer.id -> colorScheme.onSecondaryContainer
            ColorPalette.Tertiary.id -> colorScheme.tertiary
            ColorPalette.OnTertiary.id -> colorScheme.onTertiary
            ColorPalette.TertiaryContainer.id -> colorScheme.tertiaryContainer
            ColorPalette.OnTertiaryContainer.id -> colorScheme.onTertiaryContainer
            ColorPalette.Background.id -> colorScheme.background
            ColorPalette.OnBackground.id -> colorScheme.onBackground
            // ColorPalette.Surface.id -> colorScheme.surface
            ColorPalette.OnSurface.id -> colorScheme.onSurface
            ColorPalette.SurfaceVariant.id -> colorScheme.surfaceVariant
            ColorPalette.OnSurfaceVariant.id -> colorScheme.onSurfaceVariant
            ColorPalette.SurfaceTint.id -> colorScheme.surfaceTint
            ColorPalette.InverseSurface.id -> colorScheme.inverseSurface
            ColorPalette.InverseOnSurface.id -> colorScheme.inverseOnSurface
            ColorPalette.Error.id -> colorScheme.error
            ColorPalette.OnError.id -> colorScheme.onError
            ColorPalette.ErrorContainer.id -> colorScheme.errorContainer
            ColorPalette.OnErrorContainer.id -> colorScheme.onErrorContainer
            ColorPalette.Outline.id -> colorScheme.outline
            ColorPalette.OutlineVariant.id -> colorScheme.outlineVariant
            ColorPalette.Scrim.id -> colorScheme.scrim
            ColorPalette.SurfaceBright.id -> colorScheme.surfaceBright
            ColorPalette.SurfaceDim.id -> colorScheme.surfaceDim
            ColorPalette.SurfaceContainer.id -> colorScheme.surfaceContainer
            ColorPalette.SurfaceContainerHigh.id -> colorScheme.surfaceContainerHigh
            ColorPalette.SurfaceContainerHighest.id -> colorScheme.surfaceContainerHighest
            ColorPalette.SurfaceContainerLow.id -> colorScheme.surfaceContainerLow
            ColorPalette.SurfaceContainerLowest.id -> colorScheme.surfaceContainerLowest
            else -> colorScheme.primary
        }
    }
}

data class SnyggMaterialYouLightColorValue(override val colorName: String, override val dark: Boolean = false) : SnyggMaterialYouValue {

    companion object : SnyggMaterialYouValueEncoder<SnyggMaterialYouLightColorValue> {
        override val clazz = SnyggMaterialYouLightColorValue::class.java
        override val valueName = lightColorName

        override fun newInstance(colorName: String) = SnyggMaterialYouLightColorValue(colorName)
    }

    override fun encoder() = Companion
}

data class SnyggMaterialYouDarkColorValue(override val colorName: String, override val dark: Boolean = true) : SnyggMaterialYouValue {

    companion object : SnyggMaterialYouValueEncoder<SnyggMaterialYouDarkColorValue> {
        override val clazz = SnyggMaterialYouDarkColorValue::class.java
        override val valueName = darkColorName

        override fun newInstance(colorName: String) = SnyggMaterialYouDarkColorValue(colorName)
    }

    override fun encoder() = Companion
}
