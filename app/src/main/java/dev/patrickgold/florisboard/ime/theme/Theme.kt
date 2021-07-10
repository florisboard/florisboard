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
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.res.Asset
import kotlinx.serialization.Serializable

/**
 * Data class which holds a parsed theme json file. Used for loading a theme preset in Settings.
 * Note: this implementation is generic and allows for any group/attr names. FlorisBoard itself
 *       expects certain groups and attrs to be able to color the controls accordingly. See
 *       'ime/theme/floris_day.json' for a good example of which attributes FlorisBoard needs!
 *
 * @property isNightTheme If this theme is meant for display at day (false) or night (true). This
 *  property is used to auto-assign this theme to either the day or night theme list in Settings,
 *  which is used when the user wants to auto-set his theme based on the current time.
 * @property attributes Map which holds the raw attributes of this theme. Attributes are always
 *  grouped together. This ensures a better structure and easier storage. The group- as well as the
 *  attr-name must only consist of lowercase or uppercase Latin letters (a-z and/or A-Z).
 *  Attribute values can be of different format:
 *  1. A color
 *     Either #RRGGBB or #AARRGGBB (case-insensitive) -> e.g. #A034FF23
 *  2. A static word
 *     - transparent
 *     - true
 *     - false
 *  3. A reference to another attribute within the SAME theme, as follows:
 *     @group/attrName -> e.g. @window/textColor
 *     Note that referencing attributes has its limitations:
 *     a. Recursive references will cause an infinite loop and FlorisBoard will then not react.
 *  4. If the value is of any other format, it is treated as an Other value with a raw string.
 */
@Serializable
open class Theme(
    override val name: String,
    override val label: String,
    override val authors: List<String>,
    val isNightTheme: Boolean = false,
    val attributes: Map<String, Map<String, ThemeValue>>
) : Asset {
    companion object {
        private val VALIDATION_REGEX_THEME_LABEL = """^.+${'$'}""".toRegex()
        private val VALIDATION_REGEX_GROUP_NAME = """^[a-zA-Z]+((:[a-zA-Z0-9_~]+)|(::[a-zA-Z]+)|(:[a-zA-Z0-9_~]+:[a-zA-Z]+))?${'$'}""".toRegex()
        private val VALIDATION_REGEX_ATTR_NAME = """^[a-zA-Z]+${'$'}""".toRegex()

        /**
         * A static base theme for fallback when a theme is absolutely needed but no theme can be
         * loaded from the AssetManager, etc.
         */
        val BASE_THEME: Theme = baseTheme(
            name = "__base__",
            label = "Base Theme",
            authors = listOf("patrickgold"),
            isNightTheme = true
        )

        /**
         * Gets the Ui string for a given [attrName]. Used in the theme editor to properly display
         * attributes for non-advanced users.
         *
         * @param context The current activity context, used for retrieving the Ui string.
         * @param attrName The attribute name, which is used to determine which Ui string should be
         *  fetched.
         * @return The Ui string representation, which is localized and can be shown to the user.
         */
        fun getUiAttrNameString(context: Context, attrName: String): String {
            val strId = when (attrName) {
                "background" ->                     R.string.settings__theme__attr_background
                "backgroundActive" ->               R.string.settings__theme__attr_backgroundActive
                "backgroundPressed" ->              R.string.settings__theme__attr_backgroundPressed
                "foreground" ->                     R.string.settings__theme__attr_foreground
                "foregroundAlt" ->                  R.string.settings__theme__attr_foregroundAlt
                "foregroundPressed" ->              R.string.settings__theme__attr_foregroundPressed
                "showBorder" ->                     R.string.settings__theme__attr_showBorder
                "colorPrimary" ->                   R.string.settings__theme__attr_colorPrimary
                "colorPrimaryDark" ->               R.string.settings__theme__attr_colorPrimaryDark
                "colorAccent" ->                    R.string.settings__theme__attr_colorAccent
                "navigationBarColor" ->             R.string.settings__theme__attr_navBarColor
                "navigationBarLight" ->             R.string.settings__theme__attr_navBarLight
                "semiTransparentColor" ->           R.string.settings__theme__attr_semiTransparentColor
                "textColor" ->                      R.string.settings__theme__attr_textColor
                else -> null
            }
            return if (strId != null) {
                context.resources.getString(strId)
            } else {
                context.resources.getString(
                    R.string.settings__theme__attr_custom, attrName
                )
            }
        }

        /**
         * Gets the Ui string for a given [groupName]. Used in the theme editor to properly display
         * group names for non-advanced users.
         *
         * @param context The current activity context, used for retrieving the Ui string.
         * @param groupName The group name, which is used to determine which Ui string should be
         *  fetched.
         * @return The Ui string representation, which is localized and can be shown to the user.
         */
        fun getUiGroupNameString(context: Context, groupName: String): String {
            return when {
                groupName.startsWith("key:") -> context.resources.getString(
                    R.string.settings__theme__group_key_specific, groupName.substring(4)
                )
                else -> {
                    val strId = when (groupName) {
                        "window" ->                 R.string.settings__theme__group_window
                        "keyboard" ->               R.string.settings__theme__group_keyboard
                        "key" ->                    R.string.settings__theme__group_key
                        "media" ->                  R.string.settings__theme__group_media
                        "oneHanded" ->              R.string.settings__theme__group_oneHanded
                        "popup" ->                  R.string.settings__theme__group_popup
                        "privateMode" ->            R.string.settings__theme__group_privateMode
                        "smartbar" ->               R.string.settings__theme__group_smartbar
                        "smartbarButton" ->         R.string.settings__theme__group_smartbarButton
                        "extractEditLayout" ->      R.string.settings__theme__group_extractEditLayout
                        "extractActionButton" ->    R.string.settings__theme__group_extractActionButton
                        "glideTrail" ->             R.string.settings__theme__group_glideTrail
                        else -> null
                    }
                    if (strId != null) {
                        context.resources.getString(strId)
                    } else {
                        context.resources.getString(
                            R.string.settings__theme__group_custom, groupName
                        )
                    }
                }
            }
        }

        /**
         * Generate a base theme with the given meta data. For the argument info see [Theme].
         *
         * @return A generated base theme which has its colors set according to [isNightTheme].
         */
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
                    )),
                    Pair("extractEditLayout", mapOf(
                        Pair("background",              bgColor),
                        Pair("foreground",              ThemeValue.fromString("@window/textColor")),
                        Pair("foregroundAlt",           ThemeValue.fromString("#73FFFFFF")),
                    )),
                    Pair("extractActionButton", mapOf(
                        Pair("background",              ThemeValue.fromString("@smartbarButton/background")),
                        Pair("foreground",              ThemeValue.fromString("@smartbarButton/foreground")),
                    ))
                )
            )
        }

        /**
         * Generates an empty theme and returns it.
         *
         * @return The generated empty theme.
         */
        fun empty(): Theme {
            return Theme("", "", listOf(),
                isNightTheme = true,
                attributes = mapOf()
            )
        }

        /**
         * Validates a given [value] if it meets the [field] requirements. Useful for validation of
         * input in the Settings.
         *
         * @param field Which type of field's requirements the [value] should match.
         * @param value The value the user inputted.
         * @return True if the value meets the requirements, false otherwise.
         */
        fun validateField(field: ValidationField, value: String): Boolean {
            return when (field) {
                ValidationField.THEME_LABEL -> value.matches(VALIDATION_REGEX_THEME_LABEL)
                ValidationField.GROUP_NAME -> value.matches(VALIDATION_REGEX_GROUP_NAME)
                ValidationField.ATTR_NAME -> value.matches(VALIDATION_REGEX_ATTR_NAME)
            }
        }
    }

    /**
     * Copies this theme to a new one while giving the option to modify some properties. For the
     * argument info see [Theme].
     *
     * @return The copied theme.
     */
    fun copy(
        name: String = this.name,
        label: String = this.label,
        authors: List<String> = this.authors.toList(),
        isNightTheme: Boolean = this.isNightTheme,
        attributes: Map<String, Map<String, ThemeValue>> = this.attributes.toMap()
    ): Theme = Theme(name, label, authors, isNightTheme, attributes)

    /**
     * Gets an attribute from this theme. Allows to specify "specifics" ([s1] and [s2]).
     *
     * @param ref The `group/attrName` pair which is a reference to the attribute which should be
     *  resolved.
     * @param s1 "Specific 1": This only properly works for the `key` group and can be filled with
     *  the label of a key. If the specific has no matches, a default resolve without the specific
     *  will be made.
     * @param s2 "Specific 2": Allows for the values `caps` and `capslock`. This is useful top have
     *  specific themes for a key (or all keys) when caps/caps lock is activated. If the specific
     *  has no matches, a default resolve without the specific will be made.
     * @return The theme value for specified [ref]. If no value can be found within this theme, the
     *  default value of type [ThemeValue.SolidColor] with a transparent color set will be returned.
     */
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

    /**
     * Internal processing of the [getAttr] method. See [getAttr] for detailed info about the
     * method's input arguments.
     */
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
        return BASE_THEME.getAttrOrNull(ref) ?: ThemeValue.SolidColor(0)
    }

    /**
     * Internal processing of the [getAttr] method. See [getAttr] for detailed info about the
     * method's input arguments.
     */
    private fun getAttrOrNull(ref: ThemeValue.Reference): ThemeValue? {
        if (attributes.containsKey(ref.group)) {
            val group = attributes[ref.group] ?: return null
            if (group.containsKey(ref.attr)) {
                return group[ref.attr]
            }
        }
        return null
    }

    /**
     * Detailed list of all attributes FlorisBoard needs to properly display a theme. Is used
     * within the project to fetch an attribute from the current theme.
     * Note: Suppressing some warnings as Kotlin cannot properly identify if an attribute is only
     *       used via [ThemeValue.Reference] or directly.
     */
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

            val EXTRACT_EDIT_LAYOUT_BACKGROUND = ThemeValue.Reference("extractEditLayout", "background")
            val EXTRACT_EDIT_LAYOUT_FOREGROUND = ThemeValue.Reference("extractEditLayout", "foreground")
            val EXTRACT_EDIT_LAYOUT_FOREGROUND_ALT = ThemeValue.Reference("extractEditLayout", "foregroundAlt")

            val EXTRACT_ACTION_BUTTON_BACKGROUND = ThemeValue.Reference("extractActionButton", "background")
            val EXTRACT_ACTION_BUTTON_FOREGROUND = ThemeValue.Reference("extractActionButton", "foreground")

            val GLIDE_TRAIL_COLOR = ThemeValue.Reference("glideTrail", "foreground")
        }
    }

    /**
     * Enum class for all validation fields [validateField] accepts.
     */
    enum class ValidationField {
        THEME_LABEL,
        GROUP_NAME,
        ATTR_NAME
    }
}

/**
 * Data class which is used to quickly parse only the relevant meta data to
 * display a theme in a selection list.
 *
 * @see [Theme] for details regarding the attributes and the theme structure.
 */
@Serializable
data class ThemeMetaOnly(
    override val name: String,
    override val label: String,
    override val authors: List<String>,
    val isNightTheme: Boolean = false
) : Asset
