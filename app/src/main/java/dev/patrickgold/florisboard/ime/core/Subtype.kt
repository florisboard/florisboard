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

package dev.patrickgold.florisboard.ime.core

import dev.patrickgold.florisboard.common.FlorisLocale
import dev.patrickgold.florisboard.common.stringBuilder
import dev.patrickgold.florisboard.ime.keyboard.LayoutType
import dev.patrickgold.florisboard.res.ext.ExtensionComponentName
import kotlinx.serialization.*

/**
 * Data class which represents an user-specified set of language and layout. String representations
 * of this object are stored as an Json array in the preference datastore.
 *
 * @property id The ID of this subtype.
 * @property primaryLocale The primary locale of this subtype.
 * @property secondaryLocales The secondary locales of this subtype. May be an empty list.
 * @property composer The composer name to composer characters the way they should.
 * @property currencySet The currency set name to display the correct currency symbols for this subtype.
 * @property popupMapping The popup mapping name to correctly show popups for this subtype.
 * @property layoutMap The layout map to properly display the correct layout for each layout type.
 */
@Serializable
data class Subtype(
    val id: Int,
    val primaryLocale: FlorisLocale,
    val secondaryLocales: List<FlorisLocale>,
    val composer: ExtensionComponentName,
    val currencySet: ExtensionComponentName,
    val popupMapping: ExtensionComponentName,
    val layoutMap: SubtypeLayoutMap,
) {
    companion object {
        /**
         * Subtype to use when prefs do not contain any valid subtypes.
         */
        val DEFAULT = Subtype(
            id = -1,
            primaryLocale = FlorisLocale.from("en", "US"),
            secondaryLocales = emptyList(),
            composer = ExtensionComponentName("org.florisboard.composers", "appender"),
            currencySet = ExtensionComponentName("org.florisboard.currencysets", "dollar"),
            popupMapping = ExtensionComponentName("org.florisboard.localization", "en"),
            layoutMap = SubtypeLayoutMap(characters = ExtensionComponentName("org.florisboard.layouts", "qwerty")),
        )
    }

    /**
     * Converts this object into its short string representation, used for debugging. Format:
     *  <id>/<language_tag>/<currency_set_name>
     */
    fun toShortString(): String {
        val languageTag = primaryLocale.languageTag()
        return "$id/$languageTag/$currencySet"
    }
}

@Serializable
data class SubtypeLayoutMap(
    @SerialName(CHARACTERS_CODE)
    val characters: ExtensionComponentName = CHARACTERS_DEFAULT,
    @SerialName(SYMBOLS_CODE)
    val symbols: ExtensionComponentName = SYMBOLS_DEFAULT,
    @SerialName(SYMBOLS2_CODE)
    val symbols2: ExtensionComponentName = SYMBOLS2_DEFAULT,
    @SerialName(NUMERIC_CODE)
    val numeric: ExtensionComponentName = NUMERIC_DEFAULT,
    @SerialName(NUMERIC_ADVANCED_CODE)
    val numericAdvanced: ExtensionComponentName = NUMERIC_ADVANCED_DEFAULT,
    @SerialName(NUMERIC_ROW_CODE)
    val numericRow: ExtensionComponentName = NUMERIC_ROW_DEFAULT,
    @SerialName(PHONE_CODE)
    val phone: ExtensionComponentName = PHONE_DEFAULT,
    @SerialName(PHONE2_CODE)
    val phone2: ExtensionComponentName = PHONE2_DEFAULT,
) {
    @Transient private var _hashCode: Int = 0

    companion object {
        private const val CHARACTERS_CODE =             "c"
        private const val SYMBOLS_CODE =                "s"
        private const val SYMBOLS2_CODE =               "s2"
        private const val NUMERIC_CODE =                "n"
        private const val NUMERIC_ADVANCED_CODE =       "na"
        private const val NUMERIC_ROW_CODE =            "nr"
        private const val PHONE_CODE =                  "p"
        private const val PHONE2_CODE =                 "p2"

        private const val EQUALS =                      "="
        private const val DELIMITER =                   ","

        private val CHARACTERS_DEFAULT =          ExtensionComponentName("org.florisboard.layouts", "qwerty")
        private val SYMBOLS_DEFAULT =             ExtensionComponentName("org.florisboard.layouts", "western")
        private val SYMBOLS2_DEFAULT =            ExtensionComponentName("org.florisboard.layouts", "western")
        private val NUMERIC_DEFAULT =             ExtensionComponentName("org.florisboard.layouts", "western_arabic")
        private val NUMERIC_ADVANCED_DEFAULT =    ExtensionComponentName("org.florisboard.layouts", "western_arabic")
        private val NUMERIC_ROW_DEFAULT =         ExtensionComponentName("org.florisboard.layouts", "western_arabic")
        private val PHONE_DEFAULT =               ExtensionComponentName("org.florisboard.layouts", "telpad")
        private val PHONE2_DEFAULT =              ExtensionComponentName("org.florisboard.layouts", "telpad")
    }

    init {
        var result = characters.hashCode()
        result = 31 * result + symbols.hashCode()
        result = 31 * result + symbols2.hashCode()
        result = 31 * result + numeric.hashCode()
        result = 31 * result + numericAdvanced.hashCode()
        result = 31 * result + numericRow.hashCode()
        result = 31 * result + phone.hashCode()
        result = 31 * result + phone2.hashCode()
        _hashCode = result
    }

    operator fun get(layoutType: LayoutType): ExtensionComponentName? {
        return when (layoutType) {
            LayoutType.CHARACTERS -> characters
            LayoutType.SYMBOLS -> symbols
            LayoutType.SYMBOLS2 -> symbols2
            LayoutType.NUMERIC -> numeric
            LayoutType.NUMERIC_ADVANCED -> numericAdvanced
            LayoutType.NUMERIC_ROW -> numericRow
            LayoutType.PHONE -> phone
            LayoutType.PHONE2 -> phone2
            else -> null
        }
    }

    override fun toString() = stringBuilder(128) {
        append(CHARACTERS_CODE)
        append(EQUALS)
        append(characters)

        append(DELIMITER)

        append(SYMBOLS_CODE)
        append(EQUALS)
        append(symbols)

        append(DELIMITER)

        append(SYMBOLS2_CODE)
        append(EQUALS)
        append(symbols2)

        append(DELIMITER)

        append(NUMERIC_ROW_CODE)
        append(EQUALS)
        append(numericRow)

        append(DELIMITER)

        append(NUMERIC_CODE)
        append(EQUALS)
        append(numeric)

        append(DELIMITER)

        append(NUMERIC_ADVANCED_CODE)
        append(EQUALS)
        append(numericAdvanced)

        append(DELIMITER)

        append(PHONE_CODE)
        append(EQUALS)
        append(phone)

        append(DELIMITER)

        append(PHONE2_CODE)
        append(EQUALS)
        append(phone2)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SubtypeLayoutMap

        if (characters != other.characters) return false
        if (symbols != other.symbols) return false
        if (symbols2 != other.symbols2) return false
        if (numeric != other.numeric) return false
        if (numericAdvanced != other.numericAdvanced) return false
        if (numericRow != other.numericRow) return false
        if (phone != other.phone) return false
        if (phone2 != other.phone2) return false

        return true
    }

    override fun hashCode(): Int {
        return _hashCode
    }
}

/**
 * Data class which represents a predefined set of language and preferred layout.
 *
 * @property locale The locale of this subtype. Beware its different name in json: 'languageTag'.
 * @property currencySet The currency set name of this subtype.
 * @property preferred The preferred layout map for this subtype's locale.
 */
@Serializable
data class SubtypePreset(
    @Serializable(with = FlorisLocale.Serializer::class)
    @SerialName("languageTag")
    val locale: FlorisLocale,
    val composer: ExtensionComponentName,
    val currencySet: ExtensionComponentName,
    val popupMapping: ExtensionComponentName? = null,
    val preferred: SubtypeLayoutMap,
)
