/*
 * Copyright (C) 2022-2025 The FlorisBoard Contributors
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
import android.database.sqlite.SQLiteException
import android.icu.text.BreakIterator
import dev.patrickgold.florisboard.appContext
import dev.patrickgold.florisboard.extensionManager
import dev.patrickgold.florisboard.ime.core.Subtype
import dev.patrickgold.florisboard.ime.editor.EditorContent
import dev.patrickgold.florisboard.ime.editor.EditorRange
import dev.patrickgold.florisboard.ime.nlp.BreakIteratorGroup
import dev.patrickgold.florisboard.ime.nlp.LanguagePackComponent
import dev.patrickgold.florisboard.ime.nlp.LanguagePackExtension
import dev.patrickgold.florisboard.ime.nlp.SpellingProvider
import dev.patrickgold.florisboard.ime.nlp.SpellingResult
import dev.patrickgold.florisboard.ime.nlp.SuggestionCandidate
import dev.patrickgold.florisboard.ime.nlp.SuggestionProvider
import dev.patrickgold.florisboard.ime.nlp.WordSuggestionCandidate
import dev.patrickgold.florisboard.lib.devtools.flogDebug
import dev.patrickgold.florisboard.lib.devtools.flogError
import dev.patrickgold.florisboard.subtypeManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HanShapeBasedLanguageProvider(val context: Context) : SpellingProvider, SuggestionProvider {
    companion object {
        // Default user ID used for all subtypes, unless otherwise specified.
        // See `ime/core/Subtype.kt` Line 210 and 211 for the default usage
        const val ProviderId = "org.florisboard.nlp.providers.han.shape"

        const val DB_PATH = "han.sqlite3";
    }


    private val appContext by context.appContext()

    private val maxFreqBySubType = mutableMapOf<String, Int>();
    private val extensionManager by context.extensionManager()
    private val subtypeManager by context.subtypeManager()
    private val allLanguagePacks: List<LanguagePackExtension>
        // Assume other types of extensions do not extend LanguagePackExtension
        get() = extensionManager.languagePacks.value ?: listOf()
    private var __connectedActiveLanguagePacks: Set<LanguagePackExtension> = setOf() // FIXME: hack for not able to observe extensionManager.languagePacks and subtypeManager.subtypes
    private var languagePackItems: Map<String, LanguagePackComponent> = mapOf() // init in refreshLanguagePacks()
    private var keyCode: Map<String, Set<Char>> = mapOf() // init in refreshLanguagePacks()
    private val activeLanguagePacks  // language packs referenced in subtypes
        get() = buildSet {
            val locales = subtypeManager.subtypes.map { it.primaryLocale.localeTag() }.toSet()
            for (languagePack in allLanguagePacks) {
                // FIXME: skip checking language pack type because it always is for now
                if (languagePack.items.any { it.locale.localeTag() in locales }) {
                    add(languagePack)
                }
            }
        }
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())  // same as NlpManager's preload()

    override val providerId = ProviderId

//    init {
//        // FIXME: observeForever only callable on the main thread.
//        extensionManager.languagePacks.observeForever { refreshLanguagePacks() }
//    }

    private fun refreshLanguagePacks() {
        scope.launch { create() }
    }

    override suspend fun create() {
        // Here we initialize our provider, set up all things which are not language dependent.
        // Refresh language pack parsing

        // build index of available language packs
        languagePackItems = buildMap {
            for (languagePack in allLanguagePacks) {
                // FIXME: skip checking language pack type because it always is for now
//                if (languagePack is HanShapeBasedLanguagePackExtensionImpl)
                for (languagePackItem in languagePack.items) {
                    put(languagePackItem.locale.localeTag(), languagePackItem)
                    // FIXME: how to put this in deserialization?
                    languagePackItem.parent = languagePack
                }
            }
        }.toMap()
        keyCode = buildMap {
            languagePackItems.forEach { (tag, languagePackItem) ->
                put(tag, languagePackItem.hanShapeBasedKeyCode.toSet())
            }
            put("default", "abcdefghijklmnopqrstuvwxyz".toSet())
        }.toMap()

        // Load all actively used language packs.
        val activeLanguagePacks = activeLanguagePacks
        for (activeLanguagePack in activeLanguagePacks) {
            if (!activeLanguagePack.isLoaded()) {
                // populates activeLanguagePack.hanShapeBasedSQLiteDatabase
                // FIXME: every time this is copied over to cache.
                activeLanguagePack.load(context)
            }
        }
        __connectedActiveLanguagePacks = activeLanguagePacks
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
        if (__connectedActiveLanguagePacks != activeLanguagePacks) {
            // FIXME: hack for not able to observe extensionManager.languagePacks
            refreshLanguagePacks()
        }
        if (content.composingText.isEmpty()) {
            return emptyList();
        }
        val (languagePackItem, languagePackExtension) = getLanguagePack(subtype) ?: return emptyList();
        val layout: String = languagePackItem.hanShapeBasedTable
        try {
            val database = languagePackExtension.hanShapeBasedSQLiteDatabase
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

    fun getLanguagePack(subtype: Subtype): Pair<LanguagePackComponent, LanguagePackExtension>? {
        val languagePackItem = languagePackItems[subtype.primaryLocale.localeTag()]
        val languagePackExtension = languagePackItem?.parent
        if (languagePackItem == null || languagePackExtension == null) {
            flogError { "Could not read language pack item / extension" }
            return null;
        }
        return Pair(languagePackItem, languagePackExtension)
    }

    override suspend fun getListOfWords(subtype: Subtype): List<String> {
        return emptyList()
//        val (languagePackItem, languagePackExtension) = getLanguagePack(subtype) ?: return emptyList();
//        val layout: String = languagePackItem.hanShapeBasedTable
//        try {
//            val database = languagePackExtension.hanShapeBasedSQLiteDatabase
//            val cur = database.query(layout, arrayOf ( "text" ), "", arrayOf(), "", "", "weight DESC, code ASC", "");
//            cur.moveToFirst();
//            val rowCount = cur.getCount();
//            val suggestions = buildList {
//                for (n in 0 until rowCount) {
//                    val word = cur.getString(0);
//                    cur.moveToNext();
//                    add(word)
//                }
//            }
//            flogDebug { "Read ${suggestions.size} words for ${subtype.primaryLocale.localeTag()}" }
//            return suggestions;
//        } catch (e: SQLiteException) {
//            flogError { "Encountered an SQL error: ${e}" }
//            return emptyList();
//        }
    }

    override suspend fun getFrequencyForWord(subtype: Subtype, word: String): Double {
        return 0.0
//        val (languagePackItem, languagePackExtension) = getLanguagePack(subtype) ?: return 0.0;
//        val layout: String = languagePackItem.hanShapeBasedTable
//        try {
//            val database = languagePackExtension.hanShapeBasedSQLiteDatabase
//            val cur = database.query(layout, arrayOf ( "weight" ), "code = ?", arrayOf(word), "", "", "", "");
//            cur.moveToFirst();
//            return try { cur.getDouble(0) } catch (e: Exception) { 0.0 };
//        } catch (e: SQLiteException) {
//            return 0.0;
//        }
    }

    override suspend fun destroy() {
        // Here we have the chance to de-allocate memory and finish our work. However this might never be called if
        // the app process is killed (which will most likely always be the case).
    }

    override suspend fun determineLocalComposing(
        subtype: Subtype,
        textBeforeSelection: CharSequence,
        breakIterators: BreakIteratorGroup,
        localLastCommitPosition: Int
    ): EditorRange {
        return breakIterators.character(subtype.primaryLocale) {
            it.setText(textBeforeSelection.toString())
            val end = it.last()
            var start = end
            var next = it.previous()
            val keyCodeLocale = keyCode[subtype.primaryLocale.localeTag()]?: keyCode["default"]?: emptySet()
            while (next != BreakIterator.DONE && start > localLastCommitPosition) {
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

    override val forcesSuggestionOn
        get() = true
}
