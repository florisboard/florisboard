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

package dev.patrickgold.florisboard.ime.core

import com.squareup.moshi.Json
import dev.patrickgold.florisboard.util.LocaleUtils
import java.util.*

/**
 * Data class which represents an user-specified set of language and layout. String representations
 * of this object are stored as an array in the shared prefs.
 * @property id The ID of this subtype. Although this can be any numeric value, its value
 *  typically matches the one of the [DefaultSubtype] with the same locale.
 * @property locale The locale this subtype is bound to.
 * @property layout The name of the layout the user wants to use within the bounds of this subtype.
 *  Must be a string which also exists in [FlorisBoard.ImeConfig.characterLayouts]. If the value is
 *  not included within this list, no layout will be shown to the user.
 */
data class Subtype(
    var id: Int,
    var locale: Locale,
    var layout: String
) {
    companion object {
        /**
         * Subtype to use when prefs do not contain any valid subtypes.
         */
        val DEFAULT = Subtype(-1, Locale.ENGLISH, "qwerty")

        /**
         * Converts the string representation of this object to a [Subtype]. Must be in the
         * following format:
         *  <id>/<language_code>/<layout_name>
         * or
         *  <id>/<language_tag>/<layout_name>
         * Eg: 101/en_US/qwerty
         *     201/de-DE/qwertz
         * If the given [string] does not match this format an [InvalidPropertiesFormatException]
         * will be thrown.
         */
        fun fromString(string: String): Subtype {
            val data = string.split("/")
            if (data.size != 3) {
                throw InvalidPropertiesFormatException(
                    "Given string contains more or less than 3 properties..."
                )
            } else {
                val locale = LocaleUtils.stringToLocale(data[1])
                return Subtype(
                    data[0].toInt(),
                    locale,
                    data[2]
                )
            }
        }
    }

    /**
     * Converts this object into its string representation. Format:
     *  <id>/<language_tag>/<layout_name>
     */
    override fun toString(): String {
        val languageTag = locale.toLanguageTag()
        return "$id/$languageTag/$layout"
    }
}

/**
 * Data class which represents a predefined set of language and preferred layout.
 * @property id The ID of this subtype.
 * @property locale The locale of this subtype. Beware its different name in json: 'languageTag'.
 * @property preferredLayout The preferred layout for this subtype's locale.
 *  Must be a string which also exists in [FlorisBoard.ImeConfig.characterLayouts]. If the value is
 *  not included within this list, no layout will be shown to the user if the user selects the
 *  predefined layout value.
 */
data class DefaultSubtype(
    var id: Int,
    @Json(name = "languageTag")
    var locale: Locale,
    var preferredLayout: String
)
