/*
 * Copyright (C) 2020 Patrick Goldinger
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

package dev.patrickgold.florisboard.ime.theme

import android.content.Context
import android.graphics.Color
import androidx.annotation.ColorInt
import dev.patrickgold.florisboard.R
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * Sealed class which allows for a field to have dynamic types, dependent on the configuration.
 * This class is a key component in providing a way to dynamically change the type of an attribute
 * while sealing this process within one class. Allows for easy addition of new theme types in the
 * future.
 */
@Serializable(with = ThemeValueSerializer::class)
sealed class ThemeValue {
    /**
     * This holds a reference to another [ThemeValue] by specifying a group and attribute name.
     */
    data class Reference(val group: String, val attr: String) : ThemeValue() {
        override fun toString(): String {
            return "@$group/$attr"
        }
    }

    /**
     * This holds a solid color as a color int.
     */
    data class SolidColor(@ColorInt val color: Int) : ThemeValue() {
        companion object {
            val TRANSPARENT = SolidColor(Color.TRANSPARENT)
            val BLACK = SolidColor(Color.BLACK)
            val WHITE = SolidColor(Color.WHITE)
        }

        override fun toString(): String {
            return "#" + String.format("%08X", color)
        }

        fun complimentaryTextColor(isAlt: Boolean = false): SolidColor {
            val ret = if (Color.red(color) * 0.299 + Color.green(color) * 0.587 +
                Color.blue(color) * 0.114 > 186) {
                Color.BLACK
            } else {
                Color.WHITE
            }
            return SolidColor(
                if (isAlt) {
                    Color.argb(
                        0x60,
                        Color.red(ret),
                        Color.green(ret),
                        Color.blue(ret)
                    )
                } else {
                    ret
                }
            )
        }
    }

    /**
     * This holds a linear gradient. Currently NYI.
     */
    data class LinearGradient(val dummy: Int) : ThemeValue() {
        override fun toString(): String {
            return "--undefined--"
        }
    }

    /**
     * This holds a radial gradient. Currently NYI.
     */
    data class RadialGradient(val dummy: Int) : ThemeValue() {
        override fun toString(): String {
            return "--undefined--"
        }
    }

    /**
     * This holds a boolean state variable.
     */
    data class OnOff(val state: Boolean) : ThemeValue() {
        override fun toString(): String {
            return state.toString()
        }
    }

    /**
     * This holds a value as a string. Often used as a fallback when the input text does not match
     * any other theme value type.
     */
    data class Other(val rawValue: String) : ThemeValue() {
        override fun toString(): String {
            return rawValue
        }
    }

    companion object {
        private val REFERENCE_REGEX = """^(@[a-zA-Z]+/[a-zA-Z]+)${'$'}""".toRegex()
        private val SOLID_COLOR_REGEX = """^#([a-fA-F0-9]{6}|[a-fA-F0-9]{8})${'$'}""".toRegex()
        private val ON_OFF_REGEX = """^((true)|(false))${'$'}""".toRegex()

        /**
         * A map of the theme value type names and their corresponding UI strings.
         */
        val UI_STRING_MAP: Map<String, Int> = mapOf(
            Pair(Reference::class.simpleName!!, R.string.settings__theme_editor__value_type_reference),
            Pair(SolidColor::class.simpleName!!, R.string.settings__theme_editor__value_type_solid_color),
            //Pair(LinearGradient::class.simpleName!!, R.string.settings__theme_editor__value_type_lin_grad),
            //Pair(RadialGradient::class.simpleName!!, R.string.settings__theme_editor__value_type_rad_grad),
            Pair(OnOff::class.simpleName!!, R.string.settings__theme_editor__value_type_on_off),
            Pair(Other::class.simpleName!!, R.string.settings__theme_editor__value_type_other)
        )

        /**
         * Generates a [ThemeValue] from a given [str]. Returns [Other] if no matches are found.
         */
        fun fromString(str: String): ThemeValue {
            return when {
                str.matches(REFERENCE_REGEX) -> {
                    val items = str.substring(1).split("/")
                    Reference(items[0], items[1])
                }
                str.matches(SOLID_COLOR_REGEX) -> {
                    SolidColor(Color.parseColor(str))
                }
                str.matches(ON_OFF_REGEX) -> {
                    OnOff(str == "true")
                }
                else -> {
                    Other(str)
                }
            }
        }
    }

    /**
     * Converts this theme value to a [SolidColor], regardless of this value's type. If the
     * conversion fails, a [SolidColor] with full transparency will be returned.
     */
    fun toSolidColor(): SolidColor {
        return when (this) {
            is SolidColor -> {
                this
            }
            else -> {
                SolidColor(0)
            }
        }
    }

    /**
     * Converts this theme value to [OnOff], regardless of this value's type. If the
     * conversion fails, a [OnOff] with state false will be returned.
     */
    fun toOnOff(): OnOff {
        return when (this) {
            is OnOff -> {
                this
            }
            else -> {
                OnOff(false)
            }
        }
    }

    /**
     * Converts this theme value to a string representation which can be shown to the user.
     */
    fun toSummaryString(context: Context): String {
        val themeTypeStr = UI_STRING_MAP[this::class.simpleName!!]?.let {
            context.resources.getString(it)
        }
        return "$themeTypeStr | $this"
    }
}

class ThemeValueSerializer : KSerializer<ThemeValue> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("ThemeValue", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: ThemeValue) {
        encoder.encodeString(value.toString())
    }

    override fun deserialize(decoder: Decoder): ThemeValue {
        return ThemeValue.fromString(decoder.decodeString())
    }
}
