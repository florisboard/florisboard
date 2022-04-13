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

import dev.patrickgold.florisboard.ime.keyboard.LayoutType
import dev.patrickgold.florisboard.ime.keyboard.LayoutTypeId
import dev.patrickgold.florisboard.ime.keyboard.extCoreComposer
import dev.patrickgold.florisboard.ime.keyboard.extCoreCurrencySet
import dev.patrickgold.florisboard.ime.keyboard.extCoreLayout
import dev.patrickgold.florisboard.ime.keyboard.extCorePopupMapping
import dev.patrickgold.florisboard.lib.FlorisLocale
import dev.patrickgold.florisboard.lib.ext.ExtensionComponentName
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

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
    val id: Long,
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
            composer = extCoreComposer("appender"),
            currencySet = extCoreCurrencySet("dollar"),
            popupMapping = extCorePopupMapping("en"),
            layoutMap = SubtypeLayoutMap(characters = extCoreLayout("qwerty")),
        )
    }

    /**
     * Converts this object into its short string representation, used for debugging. Format:
     *  <id>/<language_tag>/<currency_set_name>
     */
    fun toShortString(): String {
        val languageTag = primaryLocale.languageTag()
        return "$id/$languageTag/$currencySet/${layoutMap.characters}"
    }

    fun equalsExcludingId(other: Subtype): Boolean {
        if (other.primaryLocale != primaryLocale) return false
        if (other.secondaryLocales != secondaryLocales) return false
        if (other.composer != composer) return false
        if (other.currencySet != currencySet) return false
        if (other.popupMapping != popupMapping) return false
        if (other.layoutMap != layoutMap) return false

        return true
    }
}

@Serializable
data class SubtypeLayoutMap(
    @SerialName(LayoutTypeId.CHARACTERS)
    val characters: ExtensionComponentName = CHARACTERS_DEFAULT,
    @SerialName(LayoutTypeId.SYMBOLS)
    val symbols: ExtensionComponentName = SYMBOLS_DEFAULT,
    @SerialName(LayoutTypeId.SYMBOLS2)
    val symbols2: ExtensionComponentName = SYMBOLS2_DEFAULT,
    @SerialName(LayoutTypeId.NUMERIC)
    val numeric: ExtensionComponentName = NUMERIC_DEFAULT,
    @SerialName(LayoutTypeId.NUMERIC_ADVANCED)
    val numericAdvanced: ExtensionComponentName = NUMERIC_ADVANCED_DEFAULT,
    @SerialName(LayoutTypeId.NUMERIC_ROW)
    val numericRow: ExtensionComponentName = NUMERIC_ROW_DEFAULT,
    @SerialName(LayoutTypeId.PHONE)
    val phone: ExtensionComponentName = PHONE_DEFAULT,
    @SerialName(LayoutTypeId.PHONE2)
    val phone2: ExtensionComponentName = PHONE2_DEFAULT,
) {
    @Transient private var _hashCode: Int = 0

    companion object {
        private const val EQUALS =                      "="
        private const val DELIMITER =                   ","

        private val CHARACTERS_DEFAULT =          extCoreLayout("qwerty")
        private val SYMBOLS_DEFAULT =             extCoreLayout("western")
        private val SYMBOLS2_DEFAULT =            extCoreLayout("western")
        private val NUMERIC_DEFAULT =             extCoreLayout("western_arabic")
        private val NUMERIC_ADVANCED_DEFAULT =    extCoreLayout("western_arabic")
        private val NUMERIC_ROW_DEFAULT =         extCoreLayout("western_arabic")
        private val PHONE_DEFAULT =               extCoreLayout("telpad")
        private val PHONE2_DEFAULT =              extCoreLayout("telpad")
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

    fun copy(layoutType: LayoutType, componentName: ExtensionComponentName): SubtypeLayoutMap? {
        return when (layoutType) {
            LayoutType.CHARACTERS -> copy(characters = componentName)
            LayoutType.SYMBOLS -> copy(symbols = componentName)
            LayoutType.SYMBOLS2 -> copy(symbols2 = componentName)
            LayoutType.NUMERIC -> copy(numeric = componentName)
            LayoutType.NUMERIC_ADVANCED -> copy(numericAdvanced = componentName)
            LayoutType.NUMERIC_ROW -> copy(numericRow = componentName)
            LayoutType.PHONE -> copy(phone = componentName)
            LayoutType.PHONE2 -> copy(phone2 = componentName)
            else -> null
        }
    }

    override fun toString() = buildString(128) {
        append(LayoutTypeId.CHARACTERS)
        append(EQUALS)
        append(characters)

        append(DELIMITER)

        append(LayoutTypeId.SYMBOLS)
        append(EQUALS)
        append(symbols)

        append(DELIMITER)

        append(LayoutTypeId.SYMBOLS2)
        append(EQUALS)
        append(symbols2)

        append(DELIMITER)

        append(LayoutTypeId.NUMERIC_ROW)
        append(EQUALS)
        append(numericRow)

        append(DELIMITER)

        append(LayoutTypeId.NUMERIC)
        append(EQUALS)
        append(numeric)

        append(DELIMITER)

        append(LayoutTypeId.NUMERIC_ADVANCED)
        append(EQUALS)
        append(numericAdvanced)

        append(DELIMITER)

        append(LayoutTypeId.PHONE)
        append(EQUALS)
        append(phone)

        append(DELIMITER)

        append(LayoutTypeId.PHONE2)
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
    val popupMapping: ExtensionComponentName = extCorePopupMapping("default"),
    val preferred: SubtypeLayoutMap,
) {
    fun toSubtype(): Subtype {
        return Subtype(
            id = -1,
            primaryLocale = locale,
            secondaryLocales = listOf(),
            composer = composer,
            currencySet = currencySet,
            popupMapping = popupMapping,
            layoutMap = preferred,
        )
    }
}
