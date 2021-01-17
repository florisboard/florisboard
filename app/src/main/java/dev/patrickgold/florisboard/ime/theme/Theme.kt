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
import dev.patrickgold.florisboard.ime.extension.Asset

/**
 * Data class which holds a parsed theme json file. Used for loading a theme
 * preset in Settings.
 * Note: this implementation is generic and allows for any group/attr names.
 *       FlorisBoard itself expects certain groups and attrs to be able to
 *       color the controls accordingly. See 'ime/theme/floris_day.json'
 *       for a good example of which attributes FlorisBoard needs!
 *
 * @property isNightTheme If this theme is meant for display at day (false)
 *  or night (true). This property is only used to auto-assign this theme to
 *  either the day or night theme list in Settings, which is used when the
 *  user wants to auto-set his theme based on the current time.
 * @property attributes Map which holds the raw attributes of this theme. Note
 *  that the name of this property is 'attributes' within the json file!
 *  Attributes are always grouped together. This ensures a better structure
 *  and easier storage. The group- as well as the attr-name has the same
 *  limitations as the theme [name].
 *  Attribute values can be of different format:
 *  1. A color
 *     Either #RRGGBB or #AARRGGBB (case-insensitive) -> e.g. #A034FF23
 *  2. A static word
 *     - transparent (=0x00000000)
 *     - true (=0x1)
 *     - false (=0x0)
 *  3. A reference to another attribute within the SAME theme, as follows:
 *     @group/attrName -> e.g. @window/textColor
 *     Note that referencing attributes has its limitations:
 *     a. Recursive references will cause an exception.
 *  4. If the value is of any other format, an exception will be thrown.
 */
open class Theme(
    override val name: String,
    override val label: String = name,
    override val authors: List<String>,
    val isNightTheme: Boolean = false,
    val attributes: Map<String, Map<String, ThemeValue>>
) : Asset {
    companion object : Asset.Companion<Theme> {
        private val VALIDATION_REGEX_THEME_LABEL = """^.+${'$'}""".toRegex()
        private val VALIDATION_REGEX_GROUP_NAME = """^[a-zA-Z]+${'$'}""".toRegex()
        private val VALIDATION_REGEX_ATTR_NAME = """^[a-zA-Z]+${'$'}""".toRegex()

        val BASE_THEME: Theme = baseTheme(
            name = "__base__",
            label = "Base Theme",
            authors = listOf("patrickgold"),
            isNightTheme = true
        )

        fun baseTheme(name: String, label: String, authors: List<String>, isNightTheme: Boolean): Theme {
            val bgColor: ThemeValue.SolidColor
            val fgColor: ThemeValue.SolidColor
            if (isNightTheme) {
                bgColor = ThemeValue.SolidColor(Color.BLACK)
                fgColor = ThemeValue.SolidColor(Color.WHITE)
            } else {
                bgColor = ThemeValue.SolidColor(Color.WHITE)
                fgColor = ThemeValue.SolidColor(Color.BLACK)
            }
            return Theme(
                name = name,
                label = label,
                authors = authors,
                isNightTheme = isNightTheme,
                attributes = mapOf(
                    Pair("window", mapOf(
                        Pair("colorPrimary",            ThemeValue.fromString("#4CAF50")),
                        Pair("colorPrimaryDark",        ThemeValue.fromString("#388E3C")),
                        Pair("colorAccent",             ThemeValue.fromString("#FF9800")),
                        Pair("navigationBarColor",      ThemeValue.fromString("@keyboard/background")),
                        Pair("navigationBarLight",      ThemeValue.OnOff(!isNightTheme)),
                        Pair("semiTransparentColor",    ThemeValue.fromString("#20FFFFFF")),
                        Pair("textColor",               fgColor),
                    )),
                    Pair("keyboard", mapOf(
                        Pair("background",              bgColor),
                    )),
                    Pair("key", mapOf(
                        Pair("background",              bgColor),
                        Pair("backgroundPressed",       ThemeValue.fromString("@window/semiTransparentColor")),
                        Pair("foreground",              ThemeValue.fromString("@window/textColor")),
                        Pair("foregroundPressed",       ThemeValue.fromString("@window/textColor")),
                        Pair("showBorder",              ThemeValue.OnOff(false)),
                    )),
                    Pair("media", mapOf(
                        Pair("background",              bgColor),
                        Pair("foreground",              ThemeValue.fromString("@window/textColor")),
                        Pair("foregroundAlt",           fgColor),
                    )),
                    Pair("oneHanded", mapOf(
                        Pair("background",              ThemeValue.fromString("#1B5E20")),
                        Pair("foreground",              ThemeValue.fromString("#EEEEEE")),
                    )),
                    Pair("popup", mapOf(
                        Pair("background",              ThemeValue.fromString("#757575")),
                        Pair("backgroundActive",        ThemeValue.fromString("#BDBDBD")),
                        Pair("foreground",              ThemeValue.fromString("@window/textColor")),
                        Pair("showBorder",              ThemeValue.OnOff(true)),
                    )),
                    Pair("privateMode", mapOf(
                        Pair("background",              ThemeValue.fromString("#A000FF")),
                        Pair("foreground",              ThemeValue.fromString("#FFFFFF")),
                    )),
                    Pair("smartbar", mapOf(
                        Pair("background",              bgColor),
                        Pair("foreground",              ThemeValue.fromString("@window/textColor")),
                        Pair("foregroundAlt",           ThemeValue.fromString("#73FFFFFF")),
                    )),
                    Pair("smartbarButton", mapOf(
                        Pair("background",              ThemeValue.fromString("@key/background")),
                        Pair("foreground",              ThemeValue.fromString("@key/foreground")),
                    ))
                )
            )
        }

        override fun empty(): Theme {
            return Theme("", "", listOf(),
                isNightTheme = true,
                attributes = mapOf()
            )
        }

        fun validateField(field: ValidationField, value: String): Boolean {
            return when (field) {
                ValidationField.THEME_LABEL -> value.matches(VALIDATION_REGEX_THEME_LABEL)
                ValidationField.GROUP_NAME -> value.matches(VALIDATION_REGEX_GROUP_NAME)
                ValidationField.ATTR_NAME -> value.matches(VALIDATION_REGEX_ATTR_NAME)
            }
        }
    }

    fun copy(
        name: String = this.name,
        label: String = this.label,
        authors: List<String> = this.authors.toList(),
        isNightTheme: Boolean = this.isNightTheme,
        attributes: Map<String, Map<String, ThemeValue>> = this.attributes.toMap()
    ): Theme = Theme(name, label, authors, isNightTheme, attributes)

    open fun getAttr(ref: ThemeValue.Reference, s1: String? = null, s2: String? = null): ThemeValue {
        var loopRef = ref
        var firstLoop = true
        var value: ThemeValue
        while (true) {
            value = when {
                firstLoop -> getAttrInternal(loopRef, s1, s2)
                else -> getAttrInternal(loopRef)
            }
            if (value !is ThemeValue.Reference) {
                break
            } else {
                loopRef = value
                firstLoop = false
            }
        }
        return value
    }

    private fun getAttrInternal(ref: ThemeValue.Reference, s1: String? = null, s2: String? = null): ThemeValue {
        if (s1 != null && s2 != null) {
            getAttrOrNull(ref.copy(group = "${ref.group}:$s1:$s2"))?.let { return it }
            getAttrOrNull(ref.copy(group = "${ref.group}::$s2"))?.let { return it }
            getAttrOrNull(ref.copy(group = "${ref.group}:$s1"))?.let { return it }
        } else if (s1 != null && s2 == null) {
            getAttrOrNull(ref.copy(group = "${ref.group}:$s1"))?.let { return it }
        } else if (s1 == null && s2 != null) {
            getAttrOrNull(ref.copy(group = "${ref.group}::$s2"))?.let { return it }
        }
        getAttrOrNull(ref)?.let { return it }
        return ThemeValue.SolidColor(0)
    }

    private fun getAttrOrNull(ref: ThemeValue.Reference): ThemeValue? {
        if (attributes.containsKey(ref.group)) {
            val group = attributes[ref.group] ?: return null
            if (group.containsKey(ref.attr)) {
                return group[ref.attr]
            }
        }
        return null
    }

    @Suppress("unused")
    abstract class Attr {
        companion object {
            val WINDOW_COLOR_PRIMARY = ThemeValue.Reference("window", "colorPrimary")
            val WINDOW_COLOR_PRIMARY_DARK = ThemeValue.Reference("window", "colorPrimaryDark")
            val WINDOW_COLOR_ACCENT = ThemeValue.Reference("window", "colorAccent")
            val WINDOW_NAVIGATION_BAR_COLOR = ThemeValue.Reference("window", "navigationBarColor")
            val WINDOW_NAVIGATION_BAR_LIGHT = ThemeValue.Reference("window", "navigationBarLight")
            val WINDOW_SEMI_TRANSPARENT_COLOR = ThemeValue.Reference("window", "semiTransparentColor")
            val WINDOW_TEXT_COLOR = ThemeValue.Reference("window", "textColor")

            val KEYBOARD_BACKGROUND = ThemeValue.Reference("keyboard", "background")

            val KEY_BACKGROUND = ThemeValue.Reference("key", "background")
            val KEY_BACKGROUND_PRESSED = ThemeValue.Reference("key", "backgroundPressed")
            val KEY_FOREGROUND = ThemeValue.Reference("key", "foreground")
            val KEY_FOREGROUND_PRESSED = ThemeValue.Reference("key", "foregroundPressed")
            val KEY_SHOW_BORDER = ThemeValue.Reference("key", "showBorder")

            val MEDIA_FOREGROUND = ThemeValue.Reference("media", "foreground")
            val MEDIA_FOREGROUND_ALT = ThemeValue.Reference("media", "foregroundAlt")

            val ONE_HANDED_BACKGROUND = ThemeValue.Reference("oneHanded", "background")
            val ONE_HANDED_FOREGROUND = ThemeValue.Reference("oneHanded", "foreground")

            val POPUP_BACKGROUND = ThemeValue.Reference("popup", "background")
            val POPUP_BACKGROUND_ACTIVE = ThemeValue.Reference("popup", "backgroundActive")
            val POPUP_FOREGROUND = ThemeValue.Reference("popup", "foreground")

            val PRIVATE_MODE_BACKGROUND = ThemeValue.Reference("privateMode", "background")
            val PRIVATE_MODE_FOREGROUND = ThemeValue.Reference("privateMode", "foreground")

            val SMARTBAR_BACKGROUND = ThemeValue.Reference("smartbar", "background")
            val SMARTBAR_FOREGROUND = ThemeValue.Reference("smartbar", "foreground")
            val SMARTBAR_FOREGROUND_ALT = ThemeValue.Reference("smartbar", "foregroundAlt")

            val SMARTBAR_BUTTON_BACKGROUND = ThemeValue.Reference("smartbarButton", "background")
            val SMARTBAR_BUTTON_FOREGROUND = ThemeValue.Reference("smartbarButton", "foreground")
        }
    }

    enum class ValidationField {
        THEME_LABEL,
        GROUP_NAME,
        ATTR_NAME
    }
}

/**
 * Bridge data class so Moshi can handle Theme serialization/deserialization properly.
 */
class ThemeJson(
    override val name: String,
    override val label: String = name,
    override val authors: List<String>,
    private val isNightTheme: Boolean = false,
    private val attributes: Map<String, Map<String, String>>
) : Asset {
    companion object {
        fun fromTheme(theme: Theme): ThemeJson {
            return with(theme) {
                ThemeJson(name, label, authors, isNightTheme, attributes.mapValues { group ->
                    group.component2().mapValues { entry -> entry.component2().toString() }
                })
            }
        }
    }
    fun toTheme(): Theme {
        return Theme(name, label, authors, isNightTheme, attributes.mapValues { group ->
            group.component2().mapValues { entry -> ThemeValue.fromString(entry.component2()) }
        })
    }
}

/**
 * Data class which is used to quickly parse only the relevant meta data to
 * display a theme in a selection list.
 *
 * @see [Theme] for details regarding the attributes and the theme structure.
 */
data class ThemeMetaOnly(
    override val name: String,
    override val label: String,
    override val authors: List<String>,
    val isNightTheme: Boolean = false
) : Asset
