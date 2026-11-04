/*
 * Copyright (C) 2021-2026 The FlorisBoard Contributors
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

package dev.patrickgold.florisboard.ime.keyboard3

import dev.patrickgold.florisboard.ime.io.FlorisRef
import dev.patrickgold.florisboard.ime.nlp.latin.LatinLanguageProvider
import dev.patrickgold.florisboard.lib.FlorisLocale
import dev.patrickgold.jetpref.datastore.model.PreferenceSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.florisboard.lib.kotlin.tryOrNull

/**
 * Data class which represents a user-specified set of language and layout. String representations
 * of this object are stored as a JSON array in the preference datastore.
 *
 * @property id The ID of this subtype.
 * @property primaryLocale The primary locale of this subtype.
 * @property secondaryLocales The secondary locales of this subtype. Can be an empty list.
 * @property nlpProviders The NLP provider map to instantiate the correct provider for each category.
 */
@Serializable
data class Subtype(
    val id: Long,
    val primaryLocale: FlorisLocale,
    val secondaryLocales: List<FlorisLocale>,
    val keyboard3: FlorisRef,
    val nlpProviders: SubtypeNlpProviderMap = SubtypeNlpProviderMap(),
) {
    /**
     * Returns an accumulated list of all locales of this subtype.
     */
    fun locales(): List<FlorisLocale> {
        val locales = mutableListOf(primaryLocale)
        locales.addAll(secondaryLocales)
        return locales
    }

    /**
     * Converts this object into its short string representation, used for debugging. Format:
     *  <id>/<language_tag>/<currency_set_name>
     */
    fun toShortString(): String {
        val languageTag = primaryLocale.languageTag()
        return "$id/$languageTag"
    }

    companion object {
        /**
         * Subtype to use when prefs do not contain any valid subtypes.
         */
        val DEFAULT = Subtype(
            id = -1,
            primaryLocale = FlorisLocale.from("en", "US"),
            secondaryLocales = emptyList(),
            keyboard3 = FlorisRef.from("flex://org.florisboard.k3.foundation/keyboards/qwerty.xml"),
        )
    }

    object ListSerializer : PreferenceSerializer<List<Subtype>> {
        private val SubtypeJsonConfig = Json {
            encodeDefaults = true
            ignoreUnknownKeys = true
            isLenient = false
        }

        override fun serialize(value: List<Subtype>): String {
            return SubtypeJsonConfig.encodeToString(value)
        }

        override fun deserialize(value: String): List<Subtype>? {
            return tryOrNull {
                SubtypeJsonConfig.decodeFromString(value)
            }
        }
    }
}

@Serializable
data class SubtypeNlpProviderMap(
    val spelling: String = LatinLanguageProvider.ProviderId,
    val suggestion: String = LatinLanguageProvider.ProviderId,
) {
    inline fun forEach(action: (String, String) -> Unit) {
        action("spelling", spelling)
        action("suggestion", suggestion)
    }
}
