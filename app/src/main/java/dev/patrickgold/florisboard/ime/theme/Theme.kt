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
import com.squareup.moshi.Json
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dev.patrickgold.florisboard.ime.core.PrefHelper

/**
 * Data class which holds a parsed theme json file. Used for loading a theme
 * preset in Settings.
 * Note: this implementation is generic and allows for any group/attr names.
 *       FlorisBoard itself expects certain groups and attrs to be able to
 *       color the controls accordingly. See 'ime/themes/floris_day.json'
 *       for a good example of which attributes FlorisBoard needs!
 *
 * @property name A unique id/name for this theme. Must only contain certain
 *  characters: upper/lower case letters, numbers (not at the beginning!) or
 *  an underline (_).
 * @property displayName The name of this theme when shown to the user. Can
 *  contain any valid Unicode character.
 * @property author The name of the author of this theme. Should be your
 *  username on GitHub/GitLab/BitBucket/... or your full name.
 * @property isNightTheme If this theme is meant for display at day (false)
 *  or night (true). This property is only used to auto-assign this theme to
 *  either the day or night theme list in Settings, which is used when the
 *  user wants to auto-set his theme based on the current time.
 * @property rawAttrs Map which holds the raw attributes of this theme. Note
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
 *     b. Referencing an previously defined attribute is fine.
 *     c. Referencing an attribute not-yet defined is also ok, as long as
 *        the reference can be resolved at the next iteration.
 *     d. If the next iteration cannot resolve a value, an exception is
 *        thrown.
 *  4. If the value is of any other format, an exception will be thrown.
 *
 * @throws IllegalArgumentException either at an invalid value or when a
 *  reference cannot be resolved.
 */
data class Theme(
    val name: String,
    val displayName: String,
    val author: String,
    val isNightTheme: Boolean = false,
    @Json(name = "attributes")
    private val rawAttrs: Map<String, Map<String, String>>
) {
    /**
     * Holds the parsed attributes after init.
     */
    val parsedAttrs: MutableMap<String, MutableMap<String, Int>> = mutableMapOf()

    companion object {
        /**
         * Loads a theme from the specified [path].
         *
         * @param context A reference to the current [Context]. Used to request
         *  asset file.
         * @param path The path to the json theme file in the asset folder.
         * @return A parsed [Theme] or null. A null value may indicate that
         *  the file does not exist or that an error during the reading
         *  of the file occurred.
         */
        fun fromJsonFile(context: Context, path: String): Theme? {
            val rawJsonData: String = try {
                context.assets.open(path).bufferedReader().use { it.readText() }
            } catch (e: Exception) {
                null
            } ?: return null
            return fromJsonString(rawJsonData)
        }

        /**
         * Loads a theme from the given [rawData].
         *
         * @param rawData The raw json theme file as a string.
         * @return A parsed [Theme] or null. A null value may indicate that an error
         * during the reading of the [rawData] occurred.
         */
        fun fromJsonString(rawData: String): Theme? {
            val moshi = Moshi.Builder()
                .add(KotlinJsonAdapterFactory())
                .build()
            val layoutAdapter = moshi.adapter(Theme::class.java)
            return layoutAdapter.fromJson(rawData)
        }

        /**
         * Writes a given [theme] to the [prefs]. The default color values are based off the
         * Floris Day theme and are not intended to be modified. Instead, themes should be defined
         * in assets/ime/theme/<theme_id>.json
         *
         * @param theme The theme data.
         * @param prefs The preference object to write the theme to.
         */
        fun writeThemeToPrefs(prefs: PrefHelper, theme: Theme) {
            // Internal prefs part I
            prefs.internal.themeCurrentBasedOn = theme.name
            prefs.internal.themeCurrentIsNight = theme.isNightTheme

            // Theme attributes
            prefs.theme.colorPrimary = theme.getAttr("window/colorPrimary", "#4CAF50")
            prefs.theme.colorPrimaryDark = theme.getAttr("window/colorPrimaryDark", "#388E3C")
            prefs.theme.colorAccent = theme.getAttr("window/colorAccent", "#FF9800")
            prefs.theme.navBarColor = theme.getAttr("window/navigationBarColor", "#E0E0E0")
            prefs.theme.navBarIsLight = (theme.getAttrOrNull("window/navigationBarLight") ?: 0) > 0

            prefs.theme.keyboardBgColor = theme.getAttr("keyboard/bgColor", "#E0E0E0")

            prefs.theme.keyBgColor = theme.getAttr("key/bgColor", "#FFFFFF")
            prefs.theme.keyBgColorPressed = theme.getAttr("key/bgColorPressed", "#F5F5F5")
            prefs.theme.keyFgColor = theme.getAttr("key/fgColor", "#000000")

            prefs.theme.keyEnterBgColor = theme.getAttr("keyEnter/bgColor", "#4CAF50")
            prefs.theme.keyEnterBgColorPressed = theme.getAttr("keyEnter/bgColorPressed", "#388E3C")
            prefs.theme.keyEnterFgColor = theme.getAttr("keyEnter/fgColor", "#FFFFFF")

            prefs.theme.keyPopupBgColor = theme.getAttr("keyPopup/bgColor", "#EEEEEE")
            prefs.theme.keyPopupBgColorActive = theme.getAttr("keyPopup/bgColorActive", "#BDBDBD")
            prefs.theme.keyPopupFgColor = theme.getAttr("keyPopup/fgColor", "#000000")

            prefs.theme.keyShiftBgColor = theme.getAttr("keyShift/bgColor", "#FFFFFF")
            prefs.theme.keyShiftBgColorPressed = theme.getAttr("keyShift/bgColorPressed", "#F5F5F5")
            prefs.theme.keyShiftFgColor = theme.getAttr("keyShift/fgColor", "#000000")
            prefs.theme.keyShiftFgColorCapsLock = theme.getAttr("keyShift/fgColorCapsLock", "#FF9800")

            prefs.theme.mediaFgColor = theme.getAttr("media/fgColor", "#000000")
            prefs.theme.mediaFgColorAlt = theme.getAttr("media/fgColorAlt", "#757575")

            prefs.theme.oneHandedBgColor = theme.getAttr("oneHanded/bgColor", "#E8F5E9")

            prefs.theme.oneHandedButtonFgColor = theme.getAttr("oneHandedButton/fgColor", "#424242")

            prefs.theme.privateModeBgColor = theme.getAttr("privateMode/bgColor", "#A000FF")
            prefs.theme.privateModeFgColor = theme.getAttr("privateMode/fgColor", "#FFFFFF")

            prefs.theme.smartbarBgColor = theme.getAttr("smartbar/bgColor", "#E0E0E0")
            prefs.theme.smartbarFgColor = theme.getAttr("smartbar/fgColor", "#000000")
            prefs.theme.smartbarFgColorAlt = theme.getAttr("smartbar/fgColorAlt", "#4A000000")

            prefs.theme.smartbarButtonBgColor = theme.getAttr("smartbarButton/bgColor", "#FFFFFF")
            prefs.theme.smartbarButtonFgColor = theme.getAttr("smartbarButton/fgColor", "#000000")

            // Internal prefs part II (must be written at the end!!)
            prefs.internal.themeCurrentIsModified = false
        }
    }

    init {
        val listOfAttrsToReevaluate = mutableListOf<Triple<String, String, String>>()
        for (group in rawAttrs) {
            val groupMap = mutableMapOf<String, Int>()
            parsedAttrs[group.key] = groupMap
            for (attr in group.value) {
                val colorRegex = """[#]([0-9a-fA-F]{8}|[0-9a-fA-F]{6})""".toRegex()
                val refRegex = """[@]([a-zA-Z_][a-zA-Z0-9_]*)[/]([a-zA-Z_][a-zA-Z0-9_]*)""".toRegex()
                when {
                    attr.value.matches(colorRegex) -> {
                        groupMap[attr.key] = Color.parseColor(attr.value)
                    }
                    attr.value == "transparent" -> {
                        groupMap[attr.key] = Color.TRANSPARENT
                    }
                    attr.value == "true" -> {
                        groupMap[attr.key] = 0x1
                    }
                    attr.value == "false" -> {
                        groupMap[attr.key] = 0x0
                    }
                    attr.value.matches(refRegex) -> {
                        val attrValue = getAttrOrNull(attr.value.substring(1))
                        if (attrValue != null) {
                            groupMap[attr.key] = attrValue
                        } else {
                            listOfAttrsToReevaluate.add(Triple(group.key, attr.key, attr.value))
                        }
                    }
                    else -> {
                        throw IllegalArgumentException("The specified attr '${attr.key}' = '${attr.value}' is not valid!")
                    }
                }
            }
        }
        for (attrToReevaluate in listOfAttrsToReevaluate) {
            val attrValue = getAttrOrNull(attrToReevaluate.third.substring(1))
            if (attrValue != null) {
                parsedAttrs[attrToReevaluate.first]?.put(attrToReevaluate.second, attrValue)
            } else {
                throw IllegalArgumentException("The specified attr '${attrToReevaluate.second}' = '${attrToReevaluate.third}' is not valid!")
            }
        }
    }

    fun getAttr(key: String, defaultColor: String): Int {
        return getAttrOrNull(key) ?: Color.parseColor(defaultColor)
    }
    fun getAttr(group: String, attr: String, defaultColor: String): Int {
        return getAttrOrNull(group, attr) ?: Color.parseColor(defaultColor)
    }

    fun getAttrOrNull(key: String): Int? {
        val regex = """([a-zA-Z_][a-zA-Z0-9_]*)[/]([a-zA-Z_][a-zA-Z0-9_]*)""".toRegex()
        return if (key.matches(regex)) {
            val split = key.split("/")
            getAttrOrNull(split[0], split[1])
        } else {
            null
        }
    }
    fun getAttrOrNull(group: String, attr: String): Int? {
        return parsedAttrs[group]?.get(attr)
    }
}

/**
 * Data class which is used to quickly parse only the relevant meta data to
 * display a theme in a selection list.
 *
 * @see [Theme] for details regarding the attributes and the theme structure.
 */
data class ThemeMetaOnly(
    val name: String,
    val displayName: String,
    val author: String,
    val isNightTheme: Boolean = false
) {
    companion object {
        /**
         * Loads the theme meta data from the specified [path].
         *
         * @param context A reference to the current [Context]. Used to request
         *  asset file.
         * @param path The path to the json theme file in the asset folder.
         * @return [ThemeMetaOnly] or null. A null value may indicate that
         *  the file does not exist or that an error during the reading
         *  of the file occurred.
         */
        fun loadFromJsonFile(context: Context, path: String): ThemeMetaOnly? {
            val rawJsonData: String = try {
                context.assets.open(path).bufferedReader().use { it.readText() }
            } catch (e: Exception) {
                null
            } ?: return null
            val moshi = Moshi.Builder()
                .add(KotlinJsonAdapterFactory())
                .build()
            val layoutAdapter = moshi.adapter(ThemeMetaOnly::class.java)
            return layoutAdapter.fromJson(rawJsonData)
        }

        /**
         * Loads all theme meta data from the specified [path].
         *
         * @param context A reference to the current [Context]. Used to request
         *  asset file.
         * @param path The path to the dir in the asset folder.
         * @return [ThemeMetaOnly] or null. A null value may indicate that
         *  the file does not exist or that an error during the reading
         *  of the file occurred.
         */
        fun loadAllFromDir(context: Context, path: String): List<ThemeMetaOnly> {
            val ret = mutableListOf<ThemeMetaOnly>()
            try {
                val list = context.assets.list(path)
                if (list != null && list.isNotEmpty()) {
                    // Is a folder
                    for (file in list) {
                        val subList = context.assets.list("$path/$file")
                        if (subList?.isEmpty() == true) {
                            // Is file
                            val metaData = loadFromJsonFile(context, "$path/$file")
                            if (metaData != null) {
                                ret.add(metaData)
                            }
                        }
                    }
                }
            } catch (e: java.lang.Exception) {}
            return ret
        }
    }
}
