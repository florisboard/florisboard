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

import android.content.Context
import android.database.Cursor
import android.provider.UserDictionary
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import dev.patrickgold.florisboard.util.LocaleUtils
import java.lang.ref.WeakReference
import java.util.*

private const val WORDS_TABLE = "words"

private const val SORT_BY_WORD_ASC = "${UserDictionary.Words.WORD} ASC"
private const val SORT_BY_WORD_DESC = "${UserDictionary.Words.WORD} DESC"
private const val SORT_BY_FREQ_ASC = "${UserDictionary.Words.FREQUENCY} ASC"
private const val SORT_BY_FREQ_DESC = "${UserDictionary.Words.FREQUENCY} DESC"

private val PROJECTIONS: Array<String> = arrayOf(
    UserDictionary.Words._ID,
    UserDictionary.Words.WORD,
    UserDictionary.Words.FREQUENCY,
    UserDictionary.Words.LOCALE,
    UserDictionary.Words.SHORTCUT
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
    @Query("SELECT * FROM $WORDS_TABLE")
    fun queryAll(): List<UserDictionaryEntry>

    @Query("SELECT * FROM $WORDS_TABLE WHERE ${UserDictionary.Words.WORD} LIKE :word")
    fun query(word: String): List<UserDictionaryEntry>

    @Query("SELECT * FROM $WORDS_TABLE WHERE ${UserDictionary.Words.WORD} LIKE :word AND (${UserDictionary.Words.LOCALE} = :locale OR ${UserDictionary.Words.LOCALE} IS NULL)")
    fun query(word: String, locale: Locale): List<UserDictionaryEntry>
}

interface UserDictionaryDatabase {
    fun userDictionaryDao(): UserDictionaryDao
}

@Database(entities = [UserDictionaryEntry::class], version = 1)
@TypeConverters(FlorisUserDictionaryDatabase.Converters::class)
abstract class FlorisUserDictionaryDatabase : RoomDatabase(), UserDictionaryDatabase {
    companion object {
        const val DB_FILE_NAME = "floris_user_dictionary"
    }

    abstract override fun userDictionaryDao(): UserDictionaryDao

    class Converters {
        @TypeConverter
        fun localeToString(locale: Locale): String {
            return locale.toString()
        }

        @TypeConverter
        fun stringToLocale(string: String): Locale {
            return LocaleUtils.stringToLocale(string)
        }
    }
}

class SystemUserDictionaryDatabase(context: Context) : UserDictionaryDatabase {
    private val applicationContext: WeakReference<Context> = WeakReference(context.applicationContext ?: context)

    private val dao = object : UserDictionaryDao {
        override fun queryAll(): List<UserDictionaryEntry> {
            TODO("Not yet implemented")
        }

        override fun query(word: String): List<UserDictionaryEntry> {
            TODO("Not yet implemented")
        }

        override fun query(word: String, locale: Locale): List<UserDictionaryEntry> {
            val resolver = applicationContext.get()?.contentResolver ?: return listOf()
            val cursor = resolver.query(
                UserDictionary.Words.CONTENT_URI,
                PROJECTIONS,
                "${UserDictionary.Words.WORD} LIKE '%$word%' AND (${UserDictionary.Words.LOCALE} = '$locale' OR ${UserDictionary.Words.LOCALE} = '${locale.language}' OR ${UserDictionary.Words.LOCALE} IS NULL)",
                null,
                SORT_BY_FREQ_DESC
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
    }

    override fun userDictionaryDao(): UserDictionaryDao {
        return dao
    }
}
