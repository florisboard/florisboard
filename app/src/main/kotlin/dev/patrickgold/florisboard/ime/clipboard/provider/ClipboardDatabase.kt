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

package dev.patrickgold.florisboard.ime.clipboard.provider

import android.content.ClipData
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.BaseColumns
import android.provider.OpenableColumns
import androidx.core.database.getStringOrNull
import androidx.lifecycle.LiveData
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.Update
import dev.patrickgold.florisboard.lib.android.query
import dev.patrickgold.florisboard.lib.kotlin.tryOrNull

private const val CLIPBOARD_HISTORY_TABLE = "clipboard_history"
private const val CLIPBOARD_FILES_TABLE = "clipboard_files"

enum class ItemType(val value: Int) {
    TEXT(1),
    IMAGE(2),
    VIDEO(3);

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
    var id: Long = 0,
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
        private val MEDIA_PROJECTION = arrayOf(OpenableColumns.DISPLAY_NAME)

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
         * @param cloneUri Whether to store the image using [ClipboardMediaProvider].
         */
        fun fromClipData(context: Context, data: ClipData, cloneUri: Boolean) : ClipboardItem {
            val dataItem = data.getItemAt(0)
            val type = when {
                dataItem?.uri != null && data.description.hasMimeType("image/*") -> ItemType.IMAGE
                dataItem?.uri != null && data.description.hasMimeType("video/*") -> ItemType.VIDEO
                else -> ItemType.TEXT
            }

            val uri = if (type == ItemType.IMAGE || type == ItemType.VIDEO) {
                if (dataItem.uri.authority == ClipboardMediaProvider.AUTHORITY || !cloneUri) {
                    dataItem.uri
                } else {
                    var displayName = when (type) {
                        ItemType.IMAGE -> "Image"
                        ItemType.VIDEO -> "Video"
                        else -> "Unknown"
                    }
                    tryOrNull {
                        context.contentResolver.query(dataItem.uri, MEDIA_PROJECTION)?.use { cursor ->
                            val displayNameColumn = cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME)
                            if (cursor.moveToNext()) {
                                cursor.getStringOrNull(displayNameColumn)?.let { displayName = it }
                            }
                        }
                    }
                    val values = ContentValues(3).apply {
                        put(OpenableColumns.DISPLAY_NAME, displayName)
                        put(ClipboardMediaProvider.Columns.MediaUri, dataItem.uri.toString())
                        put(ClipboardMediaProvider.Columns.MimeTypes, data.description.filterMimeTypes("*/*").joinToString(","))
                    }
                    context.contentResolver.insert(when (type) {
                        ItemType.IMAGE -> ClipboardMediaProvider.IMAGE_CLIPS_URI
                        ItemType.VIDEO -> ClipboardMediaProvider.VIDEO_CLIPS_URI
                        else -> error("Impossible.")
                    }, values)
                }
            } else { null }

            val text = dataItem.text?.toString()
            val mimeTypes = when (type) {
                ItemType.TEXT -> TEXT_PLAIN
                ItemType.IMAGE, ItemType.VIDEO -> {
                    Array(data.description.mimeTypeCount) { data.description.getMimeType(it) }
                }
            }

            return ClipboardItem(0, type, text, uri, System.currentTimeMillis(), false, mimeTypes)
        }
    }

    infix fun isEqualTo(other: ClipData?): Boolean {
        if (other == null) return false
        return when (type) {
            ItemType.TEXT -> text == other.getItemAt(0).text
            ItemType.IMAGE, ItemType.VIDEO -> uri == other.getItemAt(0).uri
        }
    }

    /**
     * Creates a new ClipData which has the same contents as this.
     */
    fun toClipData(context: Context): ClipData {
        return when (type) {
            ItemType.TEXT -> {
                ClipData.newPlainText(FLORIS_CLIP_LABEL, text)
            }
            ItemType.IMAGE, ItemType.VIDEO -> {
                ClipData.newUri(context.contentResolver, FLORIS_CLIP_LABEL, uri)
            }
        }
    }

    /**
     * Instructs the content provider to delete this URI. If not an image, is a noop
     */
    fun close(context: Context) {
        if (type == ItemType.IMAGE) {
            tryOrNull { context.contentResolver.delete(this.uri!!, null, null) }
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

    @Query("DELETE FROM $CLIPBOARD_HISTORY_TABLE WHERE ${BaseColumns._ID} = :id")
    fun delete(id: Long)

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
            return Room
                .databaseBuilder(
                    context, ClipboardHistoryDatabase::class.java, CLIPBOARD_HISTORY_TABLE,
                )
                .fallbackToDestructiveMigration()
                .build()
        }
    }
}

@Entity(tableName = CLIPBOARD_FILES_TABLE)
data class ClipboardFileInfo(
    @PrimaryKey @ColumnInfo(name=BaseColumns._ID, index=true) val id: Long,
    @ColumnInfo(name=OpenableColumns.DISPLAY_NAME) val displayName: String,
    @ColumnInfo(name=OpenableColumns.SIZE) val size: Long,
    val mimeTypes: Array<String>,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ClipboardFileInfo

        if (id != other.id) return false
        if (displayName != other.displayName) return false
        if (size != other.size) return false
        if (!mimeTypes.contentEquals(other.mimeTypes)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + displayName.hashCode()
        result = 31 * result + size.hashCode()
        result = 31 * result + mimeTypes.contentHashCode()
        return result
    }
}

@Dao
interface ClipboardFilesDao {
    @Query("SELECT * FROM $CLIPBOARD_FILES_TABLE WHERE ${BaseColumns._ID} == (:uid)")
    fun getById(uid: Long) : ClipboardFileInfo

    @Query("SELECT * FROM $CLIPBOARD_FILES_TABLE WHERE ${BaseColumns._ID} == (:uid)")
    fun getCursorById(uid: Long) : Cursor

    @Query("DELETE FROM $CLIPBOARD_FILES_TABLE WHERE ${BaseColumns._ID} == (:id)")
    fun delete(id: Long)

    @Insert
    fun insert(vararg clipboardFileInfos: ClipboardFileInfo)

    @Query("SELECT * FROM $CLIPBOARD_FILES_TABLE")
    fun getAll(): List<ClipboardFileInfo>
}

@Database(entities = [ClipboardFileInfo::class], version = 1)
@TypeConverters(Converters::class)
abstract class ClipboardFilesDatabase : RoomDatabase() {
    abstract fun clipboardFilesDao() : ClipboardFilesDao

    companion object {
        fun new(context: Context): ClipboardFilesDatabase {
            return Room
                .databaseBuilder(
                    context, ClipboardFilesDatabase::class.java, CLIPBOARD_FILES_TABLE,
                )
                .fallbackToDestructiveMigration()
                .build()
        }
    }
}
