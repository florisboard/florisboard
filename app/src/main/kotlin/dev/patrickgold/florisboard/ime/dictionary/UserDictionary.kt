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

package dev.patrickgold.florisboard.ime.dictionary

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.UserDictionary
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.Update
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.lib.FlorisLocale
import dev.patrickgold.florisboard.lib.ValidationRule
import dev.patrickgold.florisboard.lib.android.readText
import dev.patrickgold.florisboard.lib.android.writeText
import dev.patrickgold.florisboard.lib.kotlin.tryOrNull
import java.lang.ref.WeakReference

private const val WORDS_TABLE = "words"

const val FREQUENCY_MIN = 1
const val FREQUENCY_MAX = 255
const val FREQUENCY_DEFAULT = 128

private const val SORT_BY_WORD_ASC = "${UserDictionary.Words.WORD} ASC"
private const val SORT_BY_WORD_DESC = "${UserDictionary.Words.WORD} DESC"
private const val SORT_BY_FREQ_ASC = "${UserDictionary.Words.FREQUENCY} ASC"
private const val SORT_BY_FREQ_DESC = "${UserDictionary.Words.FREQUENCY} DESC"

private val PROJECTIONS: Array<String> = arrayOf(
    UserDictionary.Words._ID,
    UserDictionary.Words.WORD,
    UserDictionary.Words.FREQUENCY,
    UserDictionary.Words.LOCALE,
    UserDictionary.Words.SHORTCUT,
)

private val PROJECTIONS_LANGUAGE: Array<String> = arrayOf(
    UserDictionary.Words.LOCALE,
)

@Entity(tableName = WORDS_TABLE)
data class UserDictionaryEntry(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = UserDictionary.Words._ID, index = true)
    val id: Long,
    @ColumnInfo(name = UserDictionary.Words.WORD)
    val word: String,
    @ColumnInfo(name = UserDictionary.Words.FREQUENCY)
    val freq: Int,
    @ColumnInfo(name = UserDictionary.Words.LOCALE)
    val locale: String?,
    @ColumnInfo(name = UserDictionary.Words.SHORTCUT)
    val shortcut: String?,
)

@Dao
interface UserDictionaryDao {
    companion object {
        private const val SELECT_ALL_FROM_WORDS =
            "SELECT * FROM $WORDS_TABLE"
        private const val LOCALE_MATCHES =
            "(${UserDictionary.Words.LOCALE} = :locale OR ${UserDictionary.Words.LOCALE} IS NULL)"
    }

    @Query("$SELECT_ALL_FROM_WORDS WHERE ${UserDictionary.Words.WORD} LIKE '%' || :word || '%'")
    fun query(word: String): List<UserDictionaryEntry>

    @Query("$SELECT_ALL_FROM_WORDS WHERE ${UserDictionary.Words.WORD} LIKE '%' || :word || '%' AND $LOCALE_MATCHES")
    fun query(word: String, locale: FlorisLocale?): List<UserDictionaryEntry>

    @Query("$SELECT_ALL_FROM_WORDS WHERE ${UserDictionary.Words.SHORTCUT} = :shortcut")
    fun queryShortcut(shortcut: String): List<UserDictionaryEntry>

    @Query("$SELECT_ALL_FROM_WORDS WHERE ${UserDictionary.Words.SHORTCUT} = :shortcut AND $LOCALE_MATCHES")
    fun queryShortcut(shortcut: String, locale: FlorisLocale?): List<UserDictionaryEntry>

    @Query(SELECT_ALL_FROM_WORDS)
    fun queryAll(): List<UserDictionaryEntry>

    @Query("$SELECT_ALL_FROM_WORDS WHERE (${UserDictionary.Words.LOCALE} = :locale AND :locale IS NOT NULL) OR (${UserDictionary.Words.LOCALE} IS NULL AND :locale IS NULL)")
    fun queryAll(locale: FlorisLocale?): List<UserDictionaryEntry>

    @Query("$SELECT_ALL_FROM_WORDS WHERE ${UserDictionary.Words.WORD} = :word")
    fun queryExact(word: String): List<UserDictionaryEntry>

    @Query("$SELECT_ALL_FROM_WORDS WHERE ${UserDictionary.Words.WORD} = :word AND (${UserDictionary.Words.LOCALE} = :locale OR (${UserDictionary.Words.LOCALE} IS NULL AND :locale IS NULL))")
    fun queryExact(word: String, locale: FlorisLocale?): List<UserDictionaryEntry>

    @Query("$SELECT_ALL_FROM_WORDS WHERE ${UserDictionary.Words.WORD} = :word AND $LOCALE_MATCHES")
    fun queryExactFuzzyLocale(word: String, locale: FlorisLocale?): List<UserDictionaryEntry>

    @Query("SELECT DISTINCT ${UserDictionary.Words.LOCALE} FROM $WORDS_TABLE")
    fun queryLanguageList(): List<FlorisLocale?>

    @Insert
    fun insert(entry: UserDictionaryEntry)

    @Update
    fun update(entry: UserDictionaryEntry)

    @Delete
    fun delete(entry: UserDictionaryEntry)

    @Query("DELETE FROM $WORDS_TABLE")
    fun deleteAll()
}

interface UserDictionaryDatabase {
    fun userDictionaryDao(): UserDictionaryDao

    fun reset()

    fun importCombinedList(context: Context, uri: Uri) {
        context.contentResolver.readText(uri) { src ->
            var isFirstLine = true
            src.forEachLine { line ->
                if (isFirstLine) {
                    // Ignore
                    isFirstLine = false
                } else {
                    var word: String? = null
                    var freq: Int? = null
                    var locale: String? = null
                    var shortcut: String? = null
                    for (property in line.split(';')) {
                        val keyValuePair = property.split('=')
                        check(keyValuePair.size == 2) { "Error at source line `$line`: Key-Value pair expected, but either only key or too many values provided" }
                        val key = keyValuePair[0].trim().lowercase()
                        val value = keyValuePair[1].trim()
                        when (key) {
                            "w", "word" -> word = value.ifBlank { null }
                            "f", "freq" -> {
                                val number = value.toIntOrNull(10)
                                checkNotNull(number) { "Error at source line `$line`: Freq is not a valid decimal number" }
                                check(number in FREQUENCY_MIN..FREQUENCY_MAX) {
                                    "Error at source line `$line`: Freq not within range of $FREQUENCY_MIN and $FREQUENCY_MAX"
                                }
                                freq = number
                            }
                            "l", "locale" -> locale = when (value) {
                                "all", "null", "" -> null
                                else -> value.ifBlank { null }
                            }
                            "s", "shortcut" -> shortcut = value.ifBlank { null }
                        }
                    }
                    checkNotNull(word) { "Error at source line `$line`: Word cannot be empty or missing" }
                    checkNotNull(freq) { "Error at source line `$line`: Freq cannot be empty or missing" }
                    val alreadyExistingEntries = userDictionaryDao().queryExact(
                        word, locale?.let { FlorisLocale.fromTag(it) },
                    )
                    if (alreadyExistingEntries.isNotEmpty()) {
                        userDictionaryDao().update(UserDictionaryEntry(alreadyExistingEntries[0].id, word, freq, locale, shortcut))
                    } else {
                        userDictionaryDao().insert(UserDictionaryEntry(0, word, freq, locale, shortcut))
                    }
                }
            }
        }
    }

    fun exportCombinedList(context: Context, uri: Uri) {
        context.contentResolver.writeText(uri) { dst ->
            StringBuilder().apply {
                append("dictionary=")
                append(uri.lastPathSegment)
                append(";date=")
                append(System.currentTimeMillis())
                append(";generated-by=")
                append(context.packageName)
                append(";version=1")
                appendLine()
                dst.write(toString())
            }
            for (entry in userDictionaryDao().queryAll()) {
                StringBuilder().apply {
                    append(" w=")
                    append(entry.word)
                    append(";f=")
                    append(entry.freq)
                    append(";l=")
                    append(entry.locale) // always append locale even if null
                    if (entry.shortcut != null) {
                        append(";s=")
                        append(entry.shortcut)
                    }
                    appendLine()
                    dst.write(toString())
                }
            }
        }
    }
}

@Database(entities = [UserDictionaryEntry::class], version = 1)
@TypeConverters(FlorisUserDictionaryDatabase.Converters::class)
abstract class FlorisUserDictionaryDatabase : RoomDatabase(), UserDictionaryDatabase {
    companion object {
        const val DB_FILE_NAME = "floris_user_dictionary"
    }

    abstract override fun userDictionaryDao(): UserDictionaryDao

    override fun reset() {
        TODO("Not yet implemented")
    }

    class Converters {
        @TypeConverter
        fun localeToString(locale: FlorisLocale?): String? {
            return when (locale) {
                null -> null
                else -> locale.localeTag()
            }
        }

        @TypeConverter
        fun stringToLocale(string: String?): FlorisLocale? {
            return when (string) {
                null, "all", "null", "" -> null
                else -> FlorisLocale.fromTag(string)
            }
        }
    }
}

class SystemUserDictionaryDatabase(context: Context) : UserDictionaryDatabase {
    private val applicationContext: WeakReference<Context> = WeakReference(context.applicationContext ?: context)

    private val dao = object : UserDictionaryDao {
        override fun query(word: String): List<UserDictionaryEntry> {
            return queryResolver(
                selection = "${UserDictionary.Words.WORD} LIKE ?",
                selectionArgs = arrayOf("%$word%"),
                sortOrder = SORT_BY_FREQ_DESC,
            )
        }

        override fun query(word: String, locale: FlorisLocale?): List<UserDictionaryEntry> {
            return if (locale == null) {
                queryResolver(
                    selection = "${UserDictionary.Words.WORD} LIKE ? AND ${UserDictionary.Words.LOCALE} IS NULL",
                    selectionArgs = arrayOf("%$word%"),
                    sortOrder = SORT_BY_FREQ_DESC,
                )
            } else {
                queryResolver(
                    selection = "${UserDictionary.Words.WORD} LIKE ? AND (${UserDictionary.Words.LOCALE} = ? OR ${UserDictionary.Words.LOCALE} = ? OR ${UserDictionary.Words.LOCALE} IS NULL)",
                    selectionArgs = arrayOf("%$word%", locale.localeTag(), locale.language),
                    sortOrder = SORT_BY_FREQ_DESC,
                )
            }
        }

        override fun queryShortcut(shortcut: String): List<UserDictionaryEntry> {
            return queryResolver(
                selection = "${UserDictionary.Words.SHORTCUT} = ?",
                selectionArgs = arrayOf(shortcut),
                sortOrder = SORT_BY_FREQ_DESC,
            )
        }

        override fun queryShortcut(shortcut: String, locale: FlorisLocale?): List<UserDictionaryEntry> {
            return if (locale == null) {
                queryResolver(
                    selection = "${UserDictionary.Words.SHORTCUT} = ? AND ${UserDictionary.Words.LOCALE} IS NULL",
                    selectionArgs = arrayOf(shortcut),
                    sortOrder = SORT_BY_FREQ_DESC,
                )
            } else {
                queryResolver(
                    selection = "${UserDictionary.Words.SHORTCUT} = ? AND (${UserDictionary.Words.LOCALE} = ? OR ${UserDictionary.Words.LOCALE} = ? OR ${UserDictionary.Words.LOCALE} IS NULL)",
                    selectionArgs = arrayOf(shortcut, locale.localeTag(), locale.language),
                    sortOrder = SORT_BY_FREQ_DESC,
                )
            }
        }

        override fun queryAll(): List<UserDictionaryEntry> {
            return queryResolver(
                selection = null,
                selectionArgs = null,
                sortOrder = SORT_BY_FREQ_DESC,
            )
        }

        override fun queryAll(locale: FlorisLocale?): List<UserDictionaryEntry> {
            return if (locale == null) {
                queryResolver(
                    selection = "${UserDictionary.Words.LOCALE} IS NULL",
                    selectionArgs = null,
                    sortOrder = SORT_BY_FREQ_DESC,
                )
            } else {
                queryResolver(
                    selection = "${UserDictionary.Words.LOCALE} = ?",
                    selectionArgs = arrayOf(locale.localeTag()),
                    sortOrder = SORT_BY_FREQ_DESC,
                )
            }
        }

        override fun queryExact(word: String): List<UserDictionaryEntry> {
            return queryResolver(
                selection = "${UserDictionary.Words.WORD} = ?",
                selectionArgs = arrayOf(word),
                sortOrder = null,
            )
        }

        override fun queryExact(word: String, locale: FlorisLocale?): List<UserDictionaryEntry> {
            return if (locale == null) {
                queryResolver(
                    selection = "${UserDictionary.Words.WORD} = ? AND ${UserDictionary.Words.LOCALE} IS NULL",
                    selectionArgs = arrayOf(word),
                    sortOrder = SORT_BY_FREQ_DESC,
                )
            } else {
                queryResolver(
                    selection = "${UserDictionary.Words.WORD} = ? AND ${UserDictionary.Words.LOCALE} = ?",
                    selectionArgs = arrayOf(word, locale.localeTag()),
                    sortOrder = SORT_BY_FREQ_DESC,
                )
            }
        }

        override fun queryExactFuzzyLocale(word: String, locale: FlorisLocale?): List<UserDictionaryEntry> {
            return if (locale == null) {
                queryResolver(
                    selection = "${UserDictionary.Words.WORD} = ? AND ${UserDictionary.Words.LOCALE} IS NULL",
                    selectionArgs = arrayOf(word),
                    sortOrder = SORT_BY_FREQ_DESC,
                )
            } else {
                queryResolver(
                    selection = "${UserDictionary.Words.WORD} = ? AND (${UserDictionary.Words.LOCALE} = ? OR ${UserDictionary.Words.LOCALE} IS NULL)",
                    selectionArgs = arrayOf(word, locale.localeTag()),
                    sortOrder = SORT_BY_FREQ_DESC,
                )
            }
        }

        override fun queryLanguageList(): List<FlorisLocale?> {
            val resolver = applicationContext.get()?.contentResolver ?: return listOf()
            val cursor = resolver.query(
                UserDictionary.Words.CONTENT_URI,
                PROJECTIONS_LANGUAGE,
                null,
                null,
                null
            ) ?: return listOf()
            if (cursor.count <= 0) {
                return listOf()
            }
            val localeIndex = cursor.getColumnIndex(UserDictionary.Words.LOCALE)
            val retList = mutableSetOf<FlorisLocale?>()
            while (cursor.moveToNext()) {
                val localeStr = cursor.getString(localeIndex)
                if (localeStr == null) {
                    retList.add(null)
                } else {
                    retList.add(FlorisLocale.fromTag(localeStr))
                }
            }
            cursor.close()
            return retList.toList()
        }

        private fun queryResolver(selection: String?, selectionArgs: Array<out String>?, sortOrder: String?): List<UserDictionaryEntry> {
            val resolver = applicationContext.get()?.contentResolver ?: return listOf()
            val cursor = resolver.query(
                UserDictionary.Words.CONTENT_URI,
                PROJECTIONS,
                selection,
                selectionArgs,
                sortOrder
            ) ?: return listOf()
            return parseEntries(cursor).also { cursor.close() }
        }

        private fun parseEntries(cursor: Cursor): List<UserDictionaryEntry> {
            if (cursor.count <= 0) {
                return listOf()
            }
            val idIndex = cursor.getColumnIndex(UserDictionary.Words._ID)
            val wordIndex = cursor.getColumnIndex(UserDictionary.Words.WORD)
            val freqIndex = cursor.getColumnIndex(UserDictionary.Words.FREQUENCY)
            val localeIndex = cursor.getColumnIndex(UserDictionary.Words.LOCALE)
            val shortcutIndex = cursor.getColumnIndex(UserDictionary.Words.SHORTCUT)
            val retList = mutableListOf<UserDictionaryEntry>()
            while (cursor.moveToNext()) {
                retList.add(
                    UserDictionaryEntry(
                        id = cursor.getLong(idIndex),
                        word = cursor.getString(wordIndex),
                        freq = cursor.getInt(freqIndex),
                        locale = cursor.getString(localeIndex),
                        shortcut = cursor.getString(shortcutIndex)
                    )
                )
            }
            return retList
        }

        override fun insert(entry: UserDictionaryEntry) {
            val resolver = applicationContext.get()?.contentResolver ?: return
            val contentValues = ContentValues(5).apply {
                put(UserDictionary.Words.WORD, entry.word)
                put(UserDictionary.Words.FREQUENCY, entry.freq)
                put(UserDictionary.Words.LOCALE, entry.locale)
                put(UserDictionary.Words.APP_ID, 0)
                put(UserDictionary.Words.SHORTCUT, entry.shortcut)
            }
            resolver.insert(UserDictionary.Words.CONTENT_URI, contentValues)
        }

        override fun update(entry: UserDictionaryEntry) {
            val resolver = applicationContext.get()?.contentResolver ?: return
            val contentValues = ContentValues(4).apply {
                put(UserDictionary.Words.WORD, entry.word)
                put(UserDictionary.Words.FREQUENCY, entry.freq)
                put(UserDictionary.Words.LOCALE, entry.locale)
                put(UserDictionary.Words.SHORTCUT, entry.shortcut)
            }
            resolver.update(UserDictionary.Words.CONTENT_URI, contentValues, "${UserDictionary.Words._ID} = ${entry.id}", null)
        }

        override fun delete(entry: UserDictionaryEntry) {
            val resolver = applicationContext.get()?.contentResolver ?: return
            resolver.delete(UserDictionary.Words.CONTENT_URI, "${UserDictionary.Words._ID} = ${entry.id}", null)
        }

        override fun deleteAll() {
            // Unsupported action
        }
    }

    override fun userDictionaryDao(): UserDictionaryDao {
        return dao
    }

    override fun reset() {
        TODO("Not yet implemented")
    }
}

object UserDictionaryValidation {
    private val WordRegex = """^[^\s;,]+${'$'}""".toRegex()

    val Word = ValidationRule<String> {
        forKlass = UserDictionaryEntry::class
        forProperty = "word"
        validator { input ->
            val str = input.trim()
            when {
                input.isBlank() -> resultInvalid(error = R.string.settings__udm__dialog__word_error_empty)
                !str.matches(WordRegex) -> resultInvalid(error = R.string.settings__udm__dialog__word_error_invalid, "regex" to WordRegex)
                else -> resultValid()
            }
        }
    }

    val Freq = ValidationRule<String> {
        forKlass = UserDictionaryEntry::class
        forProperty = "freq"
        validator { input ->
            val freq = input.trim().toIntOrNull(10)
            when {
                input.isBlank() -> resultInvalid(error = R.string.settings__udm__dialog__freq_error_empty)
                freq == null -> resultInvalid(error = R.string.settings__udm__dialog__freq_error_empty)
                freq < FREQUENCY_MIN || freq > FREQUENCY_MAX -> resultInvalid(error = R.string.settings__udm__dialog__freq_error_invalid)
                else -> resultValid()
            }
        }
    }

    val Shortcut = ValidationRule<String> {
        forKlass = UserDictionaryEntry::class
        forProperty = "shortcut"
        validator { input ->
            val str = input.trim()
            when {
                input.isBlank() -> resultValid() // Is optional
                !str.matches(WordRegex) -> resultInvalid(error = R.string.settings__udm__dialog__shortcut_error_invalid, "regex" to WordRegex)
                else -> resultValid()
            }
        }
    }

    val Locale = ValidationRule<String> {
        forKlass = UserDictionaryEntry::class
        forProperty = "locale"
        validator { input ->
            val str = input.trim()
            when {
                input.isBlank() -> resultValid() // Is optional
                tryOrNull { FlorisLocale.fromTag(str) } == null -> resultInvalid(error = R.string.settings__udm__dialog__locale_error_invalid)
                else -> resultValid()
            }
        }
    }
}
