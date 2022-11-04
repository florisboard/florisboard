/*
 * Copyright (C) 2022 Patrick Goldinger
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

package dev.patrickgold.florisboard.ime.nlp.han

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import android.icu.text.BreakIterator
import android.net.Uri
import dev.patrickgold.florisboard.appContext
import dev.patrickgold.florisboard.ime.core.Subtype
import dev.patrickgold.florisboard.ime.editor.EditorContent
import dev.patrickgold.florisboard.ime.editor.EditorRange
import dev.patrickgold.florisboard.ime.nlp.BreakIteratorGroup
import dev.patrickgold.florisboard.ime.nlp.LanguagePack
import dev.patrickgold.florisboard.ime.nlp.SpellingProvider
import dev.patrickgold.florisboard.ime.nlp.SpellingResult
import dev.patrickgold.florisboard.ime.nlp.SuggestionCandidate
import dev.patrickgold.florisboard.ime.nlp.SuggestionProvider
import dev.patrickgold.florisboard.ime.nlp.WordSuggestionCandidate
import dev.patrickgold.florisboard.lib.android.readText
import dev.patrickgold.florisboard.lib.devtools.flogDebug
import dev.patrickgold.florisboard.lib.devtools.flogError
import dev.patrickgold.florisboard.lib.kotlin.guardedByLock
import dev.patrickgold.florisboard.lib.io.subFile
import dev.patrickgold.florisboard.lib.android.copy
import dev.patrickgold.florisboard.lib.android.readToFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

class HanShapeBasedLanguageProvider(context: Context) : SpellingProvider, SuggestionProvider {
    companion object {
        // Default user ID used for all subtypes, unless otherwise specified.
        // See `ime/core/Subtype.kt` Line 210 and 211 for the default usage
        const val ProviderId = "org.florisboard.nlp.providers.han.shape"

        const val DB_PATH = "ime/dict/han.sqlite3";
    }


    private val appContext by context.appContext()

    private val maxFreqBySubType = mutableMapOf<String, Int>();
    private var database = SQLiteDatabase.create(null);
    private var languagePack = buildMap {
        for (languagePackData in LanguagePack.load(context).items) {
            put(languagePackData.locale.localeTag(), languagePackData)
        }
    }.toMap()
    private val keyCode = buildMap {
        languagePack.forEach { (tag, languagePackData) ->
            put(tag, languagePackData.hanShapeBasedKeyCode.toSet())
        }
        put("default", "abcdefghijklmnopqrstuvwxyz".toSet())
    }.toMap()

    override val providerId = ProviderId

    override suspend fun create() {
        // Here we initialize our provider, set up all things which are not language dependent.
        appContext.filesDir.subFile("ime/dict").mkdirs()  // TODO: should this be .subFile(DB_PATH).parentDir?
        if (!appContext.filesDir.subFile(DB_PATH).exists()) {
            appContext.assets.copy(DB_PATH, appContext.filesDir.subFile(DB_PATH))
        }
        database = SQLiteDatabase.openDatabase(appContext.filesDir.subFile(DB_PATH).path, null, SQLiteDatabase.OPEN_READONLY);
    }

    override suspend fun preload(subtype: Subtype) = withContext(Dispatchers.IO) {
        // Here we have the chance to preload dictionaries and prepare a neural network for a specific language.
        // Is kept in sync with the active keyboard subtype of the user, however a new preload does not necessary mean
        // the previous language is not needed anymore (e.g. if the user constantly switches between two subtypes)

        // To read a file from the APK assets the following methods can be used:
        // appContext.assets.open()
        // appContext.assets.reader()
        // appContext.assets.bufferedReader()
        // appContext.assets.readText()
        // To copy an APK file/dir to the file system cache (appContext.cacheDir), the following methods are available:
        // appContext.assets.copy()
        // appContext.assets.copyRecursively()

        // The subtype we get here contains a lot of data, however we are only interested in subtype.primaryLocale and
        // subtype.secondaryLocales.
    }

    override suspend fun spell(
        subtype: Subtype,
        word: String,
        precedingWords: List<String>,
        followingWords: List<String>,
        maxSuggestionCount: Int,
        allowPossiblyOffensive: Boolean,
        isPrivateSession: Boolean,
    ): SpellingResult {
        return when (word.lowercase()) {
            // Use typo for typing errors
            "typo" -> SpellingResult.typo(arrayOf("typo1", "typo2", "typo3"))
            // Use grammar error if the algorithm can detect this. On Android 11 and lower grammar errors are visually
            // marked as typos due to a lack of support
            "gerror" -> SpellingResult.grammarError(arrayOf("grammar1", "grammar2", "grammar3"))
            // Use valid word for valid input
            else -> SpellingResult.validWord()
        }
    }

    override suspend fun suggest(
        subtype: Subtype,
        content: EditorContent,
        maxCandidateCount: Int,
        allowPossiblyOffensive: Boolean,
        isPrivateSession: Boolean,
    ): List<SuggestionCandidate> {
        if (content.composingText.isEmpty()) {
            return emptyList();
        }
        val layout: String = languagePack[subtype.primaryLocale.localeTag()]?.hanShapeBasedTable
            ?: subtype.primaryLocale.variant
        try {
            val cur = database.query(layout, arrayOf ( "code", "text" ), "code LIKE ? || '%'", arrayOf(content.composingText), "", "", "code ASC, weight DESC", "$maxCandidateCount");
            cur.moveToFirst();
            val rowCount = cur.getCount();
            flogDebug { "Query was '${content.composingText}'" }
            val suggestions = buildList {
                for (n in 0 until rowCount) {
                    val code = cur.getString(0);
                    val word = cur.getString(1);
                    cur.moveToNext();
                    add(WordSuggestionCandidate(
                        text = "$word",
                        secondaryText = code,
                        confidence = 0.5,
                        isEligibleForAutoCommit = n == 0,
                        // We set ourselves as the source provider so we can get notify events for our candidate
                        sourceProvider = this@HanShapeBasedLanguageProvider,
                    ))
                }
            }
            return suggestions
        } catch (e: IllegalStateException) {
            flogError { "Invalid layout '${layout}' not found" }
            return emptyList();
        } catch (e: SQLiteException) {
            flogError { "SQLiteException: layout=$layout, composing=${content.composingText}, error='${e}'" }
            return emptyList();
        }
    }

    override suspend fun notifySuggestionAccepted(subtype: Subtype, candidate: SuggestionCandidate) {
        // We can use flogDebug, flogInfo, flogWarning and flogError for debug logging, which is a wrapper for Logcat
        flogDebug { candidate.toString() }
    }

    override suspend fun notifySuggestionReverted(subtype: Subtype, candidate: SuggestionCandidate) {
        flogDebug { candidate.toString() }
    }

    override suspend fun removeSuggestion(subtype: Subtype, candidate: SuggestionCandidate): Boolean {
        flogDebug { candidate.toString() }
        return false
    }

    override suspend fun getListOfWords(subtype: Subtype): List<String> {
        return emptyList();
    }

    override suspend fun getFrequencyForWord(subtype: Subtype, word: String): Double {
        return 0.0;
    }

    override suspend fun destroy() {
        // Here we have the chance to de-allocate memory and finish our work. However this might never be called if
        // the app process is killed (which will most likely always be the case).
    }

    override suspend fun determineLocalComposing(subtype: Subtype, textBeforeSelection: CharSequence, breakIterators: BreakIteratorGroup): EditorRange {
        return breakIterators.character(subtype.primaryLocale) {
            it.setText(textBeforeSelection.toString())
            val end = it.last()
            var start = end
            var next = it.previous()
            val keyCodeLocale = keyCode[subtype.primaryLocale.localeTag()]?: keyCode["default"]?: emptySet()
            while (next != BreakIterator.DONE) {
                val sub = textBeforeSelection.substring(next, start)
                if (! sub.all { char -> char in keyCodeLocale })
                    break
                start = next
                next = it.previous()
            }
            if (start != end) {
                flogDebug { "Determined $start - $end as composing: ${textBeforeSelection.substring(start, end)}" }
                EditorRange(start, end)
            } else {
                flogDebug { "Determined Unspecified as composing" }
                EditorRange.Unspecified
            }
        }
    }

}
