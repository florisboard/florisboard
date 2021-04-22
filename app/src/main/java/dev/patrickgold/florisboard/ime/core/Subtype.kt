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

import dev.patrickgold.florisboard.ime.text.layout.LayoutType
import dev.patrickgold.florisboard.util.LocaleUtils
import kotlinx.serialization.*
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.util.*

/**
 * Data class which represents an user-specified set of language and layout. String representations
 * of this object are stored as an array in the shared prefs.
 * @property id The ID of this subtype. Although this can be any numeric value, its value
 *  typically matches the one of the [DefaultSubtype] with the same locale.
 * @property locale The locale this subtype is bound to.
 * @property currencySetName The currency set name to display the correct currency symbols for this subtype.
 * @property layoutMap The layout map to properly display the correct layout for each layout type.
 */
data class Subtype(
    var id: Int,
    var locale: Locale,
    var currencySetName: String,
    var layoutMap: SubtypeLayoutMap,
) {
    companion object {
        /**
         * Subtype to use when prefs do not contain any valid subtypes.
         */
        val DEFAULT = Subtype(
            id = -1,
            locale = Locale.ENGLISH,
            currencySetName = "\$default",
            layoutMap = SubtypeLayoutMap(characters = "qwerty")
        )

        /**
         * Converts the string representation of this object to a [Subtype]. Must be in the
         * following format:
         *  <id>/<language_code>/<currency_set_name>/c=<layout_name>
         * or
         *  <id>/<language_tag>/<currency_set_name>/c=<layout_name>
         * Eg: 101/en_US/dollar/c=qwerty
         *     201/de-DE/euro/c=qwertz
         * If the given [str] does not match this format an [InvalidPropertiesFormatException]
         * will be thrown.
         */
        fun fromString(str: String): Subtype {
            val data = str.split("/")
            if (data.size != 4) {
                throw InvalidPropertiesFormatException(
                    "Given string contains more or less than 4 properties..."
                )
            } else {
                val locale = LocaleUtils.stringToLocale(data[1])
                return Subtype(
                    data[0].toInt(),
                    locale,
                    data[2],
                    SubtypeLayoutMap.fromString(data[3])
                )
            }
        }
    }

    /**
     * Converts this object into its string representation. Format:
     *  <id>/<language_tag>/<currency_set_name>/<layout_map>
     */
    override fun toString(): String {
        val languageTag = locale.toLanguageTag()
        return "$id/$languageTag/$currencySetName/$layoutMap"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Subtype

        if (id != other.id) return false
        if (locale != other.locale) return false
        if (currencySetName != other.currencySetName) return false
        if (layoutMap != other.layoutMap) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id
        result = 31 * result + locale.hashCode()
        result = 31 * result + currencySetName.hashCode()
        result = 31 * result + layoutMap.hashCode()
        return result
    }
}

@Serializable
data class SubtypeLayoutMap(
    val characters: String = CHARACTERS_DEFAULT,
    val symbols: String = SYMBOLS_DEFAULT,
    val symbols2: String = SYMBOLS2_DEFAULT,
    val numeric: String = NUMERIC_DEFAULT,
    val numericAdvanced: String = NUMERIC_ADVANCED_DEFAULT,
    val numericRow: String = NUMERIC_ROW_DEFAULT,
    val phone: String = PHONE_DEFAULT,
    val phone2: String = PHONE2_DEFAULT,
) {
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

        private const val CHARACTERS_DEFAULT =          "qwerty"
        private const val SYMBOLS_DEFAULT =             "western"
        private const val SYMBOLS2_DEFAULT =            "western"
        private const val NUMERIC_DEFAULT =             "western_arabic"
        private const val NUMERIC_ADVANCED_DEFAULT =    "western_arabic"
        private const val NUMERIC_ROW_DEFAULT =         "western_arabic"
        private const val PHONE_DEFAULT =               "telpad"
        private const val PHONE2_DEFAULT =              "telpad"

        fun fromString(str: String): SubtypeLayoutMap {
            var characters: String = CHARACTERS_DEFAULT
            var symbols: String = SYMBOLS_DEFAULT
            var symbols2: String = SYMBOLS2_DEFAULT
            var numeric: String = NUMERIC_DEFAULT
            var numericAdvanced: String = NUMERIC_ADVANCED_DEFAULT
            var numericRow: String = NUMERIC_ROW_DEFAULT
            var phone: String = PHONE_DEFAULT
            var phone2: String = PHONE2_DEFAULT
            for (layout in str.split(DELIMITER)) {
                val layoutSplit = layout.split(EQUALS)
                if (layoutSplit.size == 2) {
                    val code = layoutSplit[0].trim()
                    val layoutName = layoutSplit[1].trim()
                    when (code) {
                        CHARACTERS_CODE -> characters = layoutName
                        SYMBOLS_CODE -> symbols = layoutName
                        SYMBOLS2_CODE -> symbols2 = layoutName
                        NUMERIC_CODE -> numeric = layoutName
                        NUMERIC_ADVANCED_CODE -> numericAdvanced = layoutName
                        NUMERIC_ROW_CODE -> numericRow = layoutName
                        PHONE_CODE -> phone = layoutName
                        PHONE2_CODE -> phone2 = layoutName
                    }
                }
            }
            return SubtypeLayoutMap(
                characters, symbols, symbols2, numeric, numericAdvanced, numericRow, phone, phone2
            )
        }
    }

    operator fun get(layoutType: LayoutType): String? {
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

    override fun toString(): String {
        return StringBuilder(128).run {
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

            toString()
        }
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
        var result = characters.hashCode()
        result = 31 * result + symbols.hashCode()
        result = 31 * result + symbols2.hashCode()
        result = 31 * result + numeric.hashCode()
        result = 31 * result + numericAdvanced.hashCode()
        result = 31 * result + numericRow.hashCode()
        result = 31 * result + phone.hashCode()
        result = 31 * result + phone2.hashCode()
        return result
    }
}

/**
 * Data class which represents a predefined set of language and preferred layout.
 *
 * @property id The ID of this subtype.
 * @property locale The locale of this subtype. Beware its different name in json: 'languageTag'.
 * @property currencySetName The currency set name of this subtype. Beware its different name in json: 'currencySet'.
 * @property preferred The preferred layout map for this subtype's locale.
 */
@Serializable
data class DefaultSubtype(
    var id: Int,
    @Serializable(with = LocaleSerializer::class)
    @SerialName("languageTag")
    var locale: Locale,
    @SerialName("currencySet")
    var currencySetName: String,
    var preferred: SubtypeLayoutMap
)

class LocaleSerializer : KSerializer<Locale> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Locale", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Locale) {
        encoder.encodeString(value.toString())
    }

    override fun deserialize(decoder: Decoder): Locale {
        return LocaleUtils.stringToLocale(decoder.decodeString())
    }
}
