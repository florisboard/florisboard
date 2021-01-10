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

import android.graphics.Color

/**
 * Sealed class.
 */
sealed class ThemeValue {
    data class Reference(val group: String, val attr: String) : ThemeValue() {
        override fun toString(): String {
            return super.toString()
        }
    }
    data class SolidColor(val color: Int) : ThemeValue() {
        override fun toString(): String {
            return super.toString()
        }
    }
    data class LinearGradient(val dummy: Int) : ThemeValue() {
        override fun toString(): String {
            return super.toString()
        }
    }
    data class RadialGradient(val dummy: Int) : ThemeValue() {
        override fun toString(): String {
            return super.toString()
        }
    }
    data class OnOff(val state: Boolean) : ThemeValue() {
        override fun toString(): String {
            return super.toString()
        }
    }
    data class Other(val rawValue: String) : ThemeValue() {
        override fun toString(): String {
            return super.toString()
        }
    }

    companion object {
        private val REFERENCE_REGEX = """^(@[a-zA-Z]+\/[a-zA-Z]+)${'$'}""".toRegex()
        private val SOLID_COLOR_REGEX = """^#([a-fA-F0-9]{6}|[a-fA-F0-9]{8})${'$'}""".toRegex()
        private val ON_OFF_REGEX = """^((true)|(false))${'$'}""".toRegex()

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

    override fun toString(): String {
        return when (this) {
            is Reference -> {
                "@$group/$attr"
            }
            is SolidColor -> {
                "#" + String.format("%08X", color)
            }
            is LinearGradient -> {
                "--undefined--"
            }
            is RadialGradient -> {
                "--undefined--"
            }
            is OnOff -> {
                state.toString()
            }
            is Other -> {
                rawValue
            }
        }
    }
}
