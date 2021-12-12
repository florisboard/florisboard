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

package dev.patrickgold.florisboard.ime.clipboard.provider

import android.content.ClipData
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.BaseColumns
import androidx.lifecycle.LiveData
import androidx.room.*

private const val CLIPBOARD_HISTORY_TABLE = "clipboard_history"

enum class ItemType(val value: Int) {
    TEXT(1),
    IMAGE(2);

    companion object {
        fun fromInt(value : Int) : ItemType {
            return values().first { it.value == value }
        }
    }
}

/**
 * Represents an item on the clipboard.
 *
 * If type == ItemType.IMAGE there must be a uri set
 * if type == ItemType.TEXT there must be a text set
 */
@Entity(tableName = CLIPBOARD_HISTORY_TABLE)
data class ClipboardItem(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = BaseColumns._ID, index = true)
    val id: Long = 0,
    val type: ItemType,
    val text: String?,
    val uri: Uri?,
    val creationTimestampMs: Long,
    val isPinned: Boolean,
    val mimeTypes: Array<String>,
) {
    companion object {
        /**
         * So that every item doesn't have to allocate its own array.
         */
        private val TEXT_PLAIN = arrayOf("text/plain")

        const val FLORIS_CLIP_LABEL = "florisboard/clipboard_item"

        fun text(text: String): ClipboardItem {
            return ClipboardItem(
                type = ItemType.TEXT,
                text = text,
                uri = null,
                creationTimestampMs = System.currentTimeMillis(),
                isPinned = false,
                mimeTypes = TEXT_PLAIN,
            )
        }

        /**
         * Returns a new ClipboardItem based on a ClipData.
         *
         * @param data The ClipData to clone.
         * @param cloneUri Whether to store the image using [FlorisContentProvider].
         */
        fun fromClipData(context: Context, data: ClipData, cloneUri: Boolean) : ClipboardItem {
            val type = when {
                data.getItemAt(0)?.uri != null && data.description.hasMimeType("image/*") -> ItemType.IMAGE
                else -> ItemType.TEXT
            }

            val uri = if (type == ItemType.IMAGE) {
                if (data.getItemAt(0).uri.authority == FlorisContentProvider.CONTENT_URI.authority || !cloneUri){
                    data.getItemAt(0).uri
                }else {
                    val values = ContentValues().apply{
                        put("uri", data.getItemAt(0).uri.toString())
                        put("mimetypes", data.description.filterMimeTypes("*/*").joinToString(","))
                    }
                    context.contentResolver.insert(FlorisContentProvider.CLIPS_URI, values)
                }
            } else { null }

            val text = context.let { data.getItemAt(0).coerceToText(it).toString() }
            val mimeTypes = when (type) {
                ItemType.IMAGE -> {
                    (0 until data.description.mimeTypeCount).map {
                        data.description.getMimeType(it)
                    }.toTypedArray()
                }
                ItemType.TEXT -> { TEXT_PLAIN }
            }

            return ClipboardItem(0, type, text, uri, System.currentTimeMillis(), false, mimeTypes)
        }
    }

    infix fun isEqualTo(other: ClipData?): Boolean {
        if (other == null) return false
        return when (type) {
            ItemType.TEXT -> text == other.getItemAt(0).text
            ItemType.IMAGE -> uri == other.getItemAt(0).uri
        }
    }

    infix fun isNotEqualTo(other: ClipData?): Boolean = !(this isEqualTo other)

    /**
     * Creates a new ClipData which has the same contents as this.
     */
    fun toClipData(context: Context): ClipData {
        return when (type) {
            ItemType.IMAGE -> {
                ClipData.newUri(context.contentResolver, FLORIS_CLIP_LABEL, uri)
            }
            ItemType.TEXT -> {
                ClipData.newPlainText(FLORIS_CLIP_LABEL, text)
            }
        }
    }

    /**
     * Instructs the content provider to delete this URI. If not an image, is a noop
     */
    fun close(context: Context) {
        if (type == ItemType.IMAGE) {
            context.contentResolver.delete(this.uri!!, null, null)
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ClipboardItem

        if (id != other.id) return false
        if (type != other.type) return false
        if (text != other.text) return false
        if (uri != other.uri) return false
        if (creationTimestampMs != other.creationTimestampMs) return false
        if (!mimeTypes.contentEquals(other.mimeTypes)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + type.hashCode()
        result = 31 * result + (text?.hashCode() ?: 0)
        result = 31 * result + (uri?.hashCode() ?: 0)
        result = 31 * result + creationTimestampMs.hashCode()
        result = 31 * result + mimeTypes.contentHashCode()
        return result
    }

    fun stringRepresentation(): String {
        return when {
            text != null -> text
            uri != null -> "(Image) $uri"
            else -> "#ERROR"
        }
    }
}

class Converters {
    @TypeConverter
    fun uriFromString(value: String?): Uri? {
        return Uri.parse(value)
    }

    @TypeConverter
    fun stringFromUri(value: Uri?): String {
        return value.toString()
    }

    @TypeConverter
    fun itemTypeToInt(value: ItemType?): Int? {
        return value?.value
    }

    @TypeConverter
    fun intToItemType(value: Int?): ItemType? {
        return value?.let { ItemType.fromInt(it) }
    }

    /**
     * Only works because the string array is a mimetype.
     * DOES NOT USE A GENERALIZED FORMAT.
     */
    @TypeConverter
    fun mimeTypesToString(mimeTypes: Array<String>): String {
        return mimeTypes.joinToString(",")
    }

    @TypeConverter
    fun stringToMimeTypes(value: String): Array<String> {
        return value.split(",").toTypedArray()
    }
}

@Dao
interface ClipboardHistoryDao {
    @Query("SELECT * FROM $CLIPBOARD_HISTORY_TABLE")
    fun getAll(): List<ClipboardItem>

    @Query("SELECT * FROM $CLIPBOARD_HISTORY_TABLE")
    fun getAllLive(): LiveData<List<ClipboardItem>>

    @Insert
    fun insert(item: ClipboardItem): Long

    @Update
    fun update(item: ClipboardItem)

    @Update
    fun update(items: List<ClipboardItem>)

    @Delete
    fun delete(item: ClipboardItem)

    @Delete
    fun delete(items: List<ClipboardItem>)

    @Query("DELETE FROM $CLIPBOARD_HISTORY_TABLE")
    fun deleteAll()

    @Query("DELETE FROM $CLIPBOARD_HISTORY_TABLE WHERE NOT isPinned")
    fun deleteAllUnpinned()
}

@Database(entities = [ClipboardItem::class], version = 2)
@TypeConverters(Converters::class)
abstract class ClipboardHistoryDatabase : RoomDatabase() {
    abstract fun clipboardItemDao(): ClipboardHistoryDao

    companion object {
        fun new(context: Context): ClipboardHistoryDatabase {
            return Room.databaseBuilder(
                context, ClipboardHistoryDatabase::class.java, CLIPBOARD_HISTORY_TABLE,
            ).build()
        }
    }
}

@Entity(tableName = "file_uris")
data class FileUri(
    @PrimaryKey @ColumnInfo(name=BaseColumns._ID, index=true) val fileName: Long,
    val mimeTypes: Array<String>
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as FileUri

        if (fileName != other.fileName) return false
        if (!mimeTypes.contentEquals(other.mimeTypes)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = 31 + fileName.hashCode()
        result = 31 * result + mimeTypes.contentHashCode()
        return result
    }
}

@Dao
interface FileUriDao {
    @Query("SELECT * FROM file_uris WHERE ${BaseColumns._ID} == (:uid)")
    fun getById(uid: Long) : FileUri

    @Query("DELETE FROM file_uris WHERE ${BaseColumns._ID} == (:id)")
    fun delete(id: Long)

    @Insert
    fun insert(vararg fileUris: FileUri)

    @Query("SELECT COUNT(*) FROM file_uris WHERE ${BaseColumns._ID} == (:id)")
    fun numberWithId(id: Long): Int

    @Query("SELECT * FROM file_uris")
    fun getAll(): List<FileUri>
}

@Database(entities = [FileUri::class], version = 1)
@TypeConverters(Converters::class)
abstract class FileUriDatabase : RoomDatabase() {
    abstract fun fileUriDao() : FileUriDao
}
