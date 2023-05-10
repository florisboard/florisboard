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

package dev.patrickgold.florisboard.lib.snygg.value

import android.content.Context
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.ui.graphics.Color
import dev.patrickgold.florisboard.lib.snygg.value.MaterialYouColor.ColorName
import dev.patrickgold.florisboard.lib.snygg.value.MaterialYouColor.ColorNameId
import dev.patrickgold.florisboard.lib.snygg.value.MaterialYouColor.primary

sealed interface SnyggMaterialYouValue : SnyggValue {
    val colorName: String
    val dark: Boolean

    fun loadColor(context: Context) = MaterialYouColor.loadColor(context, colorName, dark)
}

sealed interface SnyggMaterialYouValueEncoder<T : SnyggMaterialYouValue> : SnyggValueEncoder {
    val clazz: Class<T>

    override val spec: SnyggValueSpec
        get() = SnyggValueSpec {
            function(name = "color-name") {
                commaList {
                    +string(id = ColorNameId, regex = ColorName)
                }
            }
        }

    override fun defaultValue() = newInstance(primary)

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

    private lateinit var lightColorScheme: ColorScheme
    private lateinit var darkColorScheme: ColorScheme

    private fun getAndCacheColorScheme(context: Context, dark: Boolean): ColorScheme {
        return if (dark) {
            if (!this::darkColorScheme.isInitialized) {
                darkColorScheme = dynamicDarkColorScheme(context)
            }

            darkColorScheme
        } else {
            if (!this::lightColorScheme.isInitialized) {
                lightColorScheme = dynamicLightColorScheme(context)
            }

            lightColorScheme
        }
    }

    const val primary = "primary"
    private const val onPrimary = "onPrimary"
    private const val primaryContainer = "primaryContainer"
    private const val onPrimaryContainer = "onPrimaryContainer"
    private const val inversePrimary = "inversePrimary"
    private const val secondary = "secondary"
    private const val onSecondary = "onSecondary"
    private const val secondaryContainer = "secondaryContainer"
    private const val onSecondaryContainer = "onSecondaryContainer"
    private const val tertiary = "tertiary"
    private const val onTertiary = "onTertiary"
    private const val tertiaryContainer = "tertiaryContainer"
    private const val onTertiaryContainer = "onTertiaryContainer"
    private const val background = "background"
    private const val onBackground = "onBackground"
    private const val surface = "surface"
    private const val onSurface = "onSurface"
    private const val surfaceVariant = "surfaceVariant"
    private const val onSurfaceVariant = "onSurfaceVariant"
    private const val surfaceTint = "surfaceTint"
    private const val inverseSurface = "inverseSurface"
    private const val inverseOnSurface = "inverseOnSurface"
    private const val error = "error"
    private const val onError = "onError"
    private const val errorContainer = "errorContainer"
    private const val onErrorContainer = "onErrorContainer"
    private const val outline = "outline"
    private const val outlineVariant = "outlineVariant"
    private const val scrim = "scrim"

    val colorNames = listOf(
        primary,
        onPrimary,
        primaryContainer,
        onPrimaryContainer,
        inversePrimary,
        secondary,
        onSecondary,
        secondaryContainer,
        onSecondaryContainer,
        tertiary,
        onTertiary,
        tertiaryContainer,
        onTertiaryContainer,
        background,
        onBackground,
        surface,
        onSurface,
        surfaceVariant,
        onSurfaceVariant,
        surfaceTint,
        inverseSurface,
        inverseOnSurface,
        error,
        onError,
        errorContainer,
        onErrorContainer,
        outline,
        outlineVariant,
        scrim
    )

    fun loadColor(context: Context, colorName: String, dark: Boolean): Color {
        val colorScheme = getAndCacheColorScheme(context, dark)

        return when (colorName) {
            primary -> colorScheme.primary
            onPrimary -> colorScheme.onPrimary
            primaryContainer -> colorScheme.primaryContainer
            onPrimaryContainer -> colorScheme.onPrimaryContainer
            inversePrimary -> colorScheme.inversePrimary
            secondary -> colorScheme.secondary
            onSecondary -> colorScheme.onSecondary
            secondaryContainer -> colorScheme.secondaryContainer
            onSecondaryContainer -> colorScheme.onSecondaryContainer
            tertiary -> colorScheme.tertiary
            onTertiary -> colorScheme.onTertiary
            tertiaryContainer -> colorScheme.tertiaryContainer
            onTertiaryContainer -> colorScheme.onTertiaryContainer
            background -> colorScheme.background
            onBackground -> colorScheme.onBackground
            surface -> colorScheme.surface
            onSurface -> colorScheme.onSurface
            surfaceVariant -> colorScheme.surfaceVariant
            onSurfaceVariant -> colorScheme.onSurfaceVariant
            surfaceTint -> colorScheme.surfaceTint
            inverseSurface -> colorScheme.inverseSurface
            inverseOnSurface -> colorScheme.inverseOnSurface
            error -> colorScheme.error
            onError -> colorScheme.onError
            errorContainer -> colorScheme.errorContainer
            onErrorContainer -> colorScheme.onErrorContainer
            outline -> colorScheme.outline
            outlineVariant -> colorScheme.outlineVariant
            scrim -> colorScheme.scrim
            else -> colorScheme.primary
        }
    }
}

data class SnyggMaterialYouLightColorValue(override val colorName: String, override val dark: Boolean = false) : SnyggMaterialYouValue {

    companion object : SnyggMaterialYouValueEncoder<SnyggMaterialYouLightColorValue> {
        override val clazz = SnyggMaterialYouLightColorValue::class.java

        override fun newInstance(colorName: String) = SnyggMaterialYouLightColorValue(colorName)
    }

    override fun encoder() = Companion
}

data class SnyggMaterialYouDarkColorValue(override val colorName: String, override val dark: Boolean = true) : SnyggMaterialYouValue {

    companion object : SnyggMaterialYouValueEncoder<SnyggMaterialYouDarkColorValue> {
        override val clazz = SnyggMaterialYouDarkColorValue::class.java

        override fun newInstance(colorName: String) = SnyggMaterialYouDarkColorValue(colorName)
    }

    override fun encoder() = Companion
}
