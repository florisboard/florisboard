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

package dev.patrickgold.florisboard

import android.service.textservice.SpellCheckerService
import android.view.textservice.SuggestionsInfo
import android.view.textservice.TextInfo
import dev.patrickgold.florisboard.debug.LogTopic
import dev.patrickgold.florisboard.debug.flogInfo
import dev.patrickgold.florisboard.ime.core.Preferences
import dev.patrickgold.florisboard.ime.core.Subtype
import dev.patrickgold.florisboard.ime.core.SubtypeManager
import dev.patrickgold.florisboard.ime.dictionary.DictionaryManager
import dev.patrickgold.florisboard.ime.spelling.SpellingDict
import dev.patrickgold.florisboard.ime.spelling.SpellingManager
import java.util.*

class FlorisSpellCheckerService : SpellCheckerService() {
    override fun onCreate() {
        flogInfo(LogTopic.SPELL_EVENTS)

        super.onCreate()
    }

    override fun createSession(): Session {
        flogInfo(LogTopic.SPELL_EVENTS)

        return FlorisSpellCheckerSession()
    }

    override fun onDestroy() {
        flogInfo(LogTopic.SPELL_EVENTS)

        super.onDestroy()
    }

    class FlorisSpellCheckerSession : Session() {
        companion object {
            private const val USE_FLORIS_SUBTYPES_LOCALE: String = "zz"

            private val DEFAULT_SUGGESTIONS_INFO = SuggestionsInfo(0, arrayOf())
            private val EMPTY_STRING_ARRAY: Array<out String> = arrayOf()
        }

        private var spellingDict: SpellingDict? = null
        private val spellingManager get() = SpellingManager.default()
        private val subtypeManager get() = SubtypeManager.default()

        override fun onCreate() {
            flogInfo(LogTopic.SPELL_EVENTS) { "Session locale: $locale" }

            val localeToUse = when (locale) {
                null -> Subtype.DEFAULT.locale
                USE_FLORIS_SUBTYPES_LOCALE -> (subtypeManager.getActiveSubtype() ?: Subtype.DEFAULT).locale
                else -> Locale(locale)
            }

            spellingDict = spellingManager.getSpellingDict(localeToUse)
        }

        override fun onGetSuggestions(textInfo: TextInfo?, suggestionsLimit: Int): SuggestionsInfo {
            flogInfo(LogTopic.SPELL_EVENTS)

            val spellingDict = spellingDict ?: return DEFAULT_SUGGESTIONS_INFO

            val word = textInfo?.text ?: return DEFAULT_SUGGESTIONS_INFO
            val isWordOk = spellingDict.spell(word)
            return if (isWordOk) {
                SuggestionsInfo(SuggestionsInfo.RESULT_ATTR_IN_THE_DICTIONARY, EMPTY_STRING_ARRAY)
            } else {
                val suggestions = spellingDict.suggest(word)
                SuggestionsInfo(SuggestionsInfo.RESULT_ATTR_LOOKS_LIKE_TYPO, suggestions)
            }
        }
    }
}
