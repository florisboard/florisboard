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

package org.florisboard.lib.color

import android.content.Context
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.isUnspecified
import org.florisboard.lib.android.AndroidVersion
import org.florisboard.lib.color.schemes.amberDarkScheme
import org.florisboard.lib.color.schemes.amberLightScheme
import org.florisboard.lib.color.schemes.blueDarkScheme
import org.florisboard.lib.color.schemes.blueGrayDarkScheme
import org.florisboard.lib.color.schemes.blueGrayLightScheme
import org.florisboard.lib.color.schemes.blueLightScheme
import org.florisboard.lib.color.schemes.brownDarkScheme
import org.florisboard.lib.color.schemes.brownLightScheme
import org.florisboard.lib.color.schemes.cyanDarkScheme
import org.florisboard.lib.color.schemes.cyanLightScheme
import org.florisboard.lib.color.schemes.deepPurpleDarkScheme
import org.florisboard.lib.color.schemes.deepPurpleLightScheme
import org.florisboard.lib.color.schemes.florisDefaultKeyboardDarkScheme
import org.florisboard.lib.color.schemes.florisDefaultKeyboardLightScheme
import org.florisboard.lib.color.schemes.florisDefaultSettingsDarkScheme
import org.florisboard.lib.color.schemes.florisDefaultSettingsLightScheme
import org.florisboard.lib.color.schemes.grayDarkScheme
import org.florisboard.lib.color.schemes.grayLightScheme
import org.florisboard.lib.color.schemes.indigoDarkScheme
import org.florisboard.lib.color.schemes.indigoLightScheme
import org.florisboard.lib.color.schemes.lightBlueDarkScheme
import org.florisboard.lib.color.schemes.lightBlueLightScheme
import org.florisboard.lib.color.schemes.lightGreenDarkScheme
import org.florisboard.lib.color.schemes.lightGreenLightScheme
import org.florisboard.lib.color.schemes.lightPinkDarkScheme
import org.florisboard.lib.color.schemes.lightPinkLightScheme
import org.florisboard.lib.color.schemes.limeDarkScheme
import org.florisboard.lib.color.schemes.limeLightScheme
import org.florisboard.lib.color.schemes.orangeDarkScheme
import org.florisboard.lib.color.schemes.orangeLightScheme
import org.florisboard.lib.color.schemes.pinkDarkScheme
import org.florisboard.lib.color.schemes.pinkLightScheme
import org.florisboard.lib.color.schemes.purpleDarkScheme
import org.florisboard.lib.color.schemes.purpleLightScheme
import org.florisboard.lib.color.schemes.redDarkScheme
import org.florisboard.lib.color.schemes.redLightScheme
import org.florisboard.lib.color.schemes.tealDarkScheme
import org.florisboard.lib.color.schemes.tealLightScheme
import org.florisboard.lib.color.schemes.yellowDarkScheme
import org.florisboard.lib.color.schemes.yellowLightScheme

val DEFAULT_GREEN = Color(0xFF4CAF50)

@Immutable
data class ColorMappings(val light: ColorScheme, val dark: ColorScheme) {
    companion object {
        val default = ColorMappings(florisDefaultKeyboardLightScheme, florisDefaultKeyboardDarkScheme)
        private val red = ColorMappings(redLightScheme, redDarkScheme)
        private val pink = ColorMappings(pinkLightScheme, pinkDarkScheme)
        private val lightPink = ColorMappings(lightPinkLightScheme, lightPinkDarkScheme)
        private val purple = ColorMappings(purpleLightScheme, purpleDarkScheme)
        private val deepPurple = ColorMappings(deepPurpleLightScheme, deepPurpleDarkScheme)
        private val indigo = ColorMappings(indigoLightScheme, indigoDarkScheme)
        private val blue = ColorMappings(blueLightScheme, blueDarkScheme)
        private val lightBlue = ColorMappings(lightBlueLightScheme, lightBlueDarkScheme)
        private val cyan = ColorMappings(cyanLightScheme, cyanDarkScheme)
        private val teal = ColorMappings(tealLightScheme, tealDarkScheme)
        private val lightGreen = ColorMappings(lightGreenLightScheme, lightGreenDarkScheme)
        private val lime = ColorMappings(limeLightScheme, limeDarkScheme)
        private val yellow = ColorMappings(yellowLightScheme, yellowDarkScheme)
        private val amber = ColorMappings(amberLightScheme, amberDarkScheme)
        private val orange = ColorMappings(orangeLightScheme, orangeDarkScheme)
        private val brown = ColorMappings(brownLightScheme, brownDarkScheme)
        private val blueGray = ColorMappings(blueGrayLightScheme, blueGrayDarkScheme)
        private val gray = ColorMappings(grayLightScheme, grayDarkScheme)

        val schemes = mapOf(
            DEFAULT_GREEN to default, // GREEN 500
            Color(0xFFF44336) to red, // RED 500
            Color(0xFFE91E63) to pink, // PINK 500
            Color(0xFFFF2C93) to lightPink, // LIGHT PINK 500
            Color(0xFF9C27B0) to purple, // PURPLE 500
            Color(0xFF673AB7) to deepPurple, // DEEP PURPLE 500
            Color(0xFF3F51B5) to indigo, // INDIGO 500
            Color(0xFF2196F3) to blue, // BLUE 500
            Color(0xFF03A9F4) to lightBlue, // LIGHT BLUE 500
            Color(0xFF00BCD4) to cyan, // CYAN 500
            Color(0xFF009688) to teal, // TEAL 500
            Color(0xFF8BC34A) to lightGreen, // LIGHT GREEN 500
            Color(0xFFCDDC39) to lime, // LIME 500
            Color(0xFFFFEB3B) to yellow, // YELLOW 500
            Color(0xFFFFC107) to amber, // AMBER 500
            Color(0xFFFF9800) to orange, // ORANGE 500
            Color(0xFF795548) to brown, // BROWN 500
            Color(0xFF607D8B) to blueGray, // BLUE GREY 500
            Color(0xFF9E9E9E) to gray, // GRAY 500
        )

        val colors = schemes.keys.toTypedArray()

        private fun getColorSchemeOrDefault(
            color: Color,
            isDark: Boolean,
            settings: Boolean = false,
        ): ColorScheme {
            if (settings && color == DEFAULT_GREEN) {
                return if (isDark) {
                    florisDefaultSettingsDarkScheme
                } else {
                    florisDefaultSettingsLightScheme
                }
            }
            return schemes
                .getOrDefault(
                    color,
                    default,
                ).let {
                    when {
                        isDark -> it.dark
                        else -> it.light
                    }
                }
        }

        fun dynamicLightColorScheme(context: Context, accentColor: Color): ColorScheme {
            return if (AndroidVersion.ATLEAST_API31_S && accentColor.isUnspecified) {
                androidx.compose.material3.dynamicLightColorScheme(context)
            } else {
                getColorSchemeOrDefault(color = accentColor, false)
            }
        }

        fun dynamicDarkColorScheme(context: Context, accentColor: Color): ColorScheme {
            return if (AndroidVersion.ATLEAST_API31_S && accentColor.isUnspecified) {
                androidx.compose.material3.dynamicDarkColorScheme(context)
            } else {
                ColorMappings.getColorSchemeOrDefault(color = accentColor, true)
            }
        }
    }
}

