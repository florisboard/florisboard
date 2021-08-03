package dev.patrickgold.florisboard.ime.clip.provider

import android.content.ClipData
import android.content.ContentValues
import android.net.Uri
import android.provider.BaseColumns
import androidx.room.*
import dev.patrickgold.florisboard.ime.clip.FlorisClipboardManager
import dev.patrickgold.florisboard.ime.core.FlorisBoard
import java.io.Closeable

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
@Entity(tableName = "pins")
data class ClipboardItem(
    /** Only used for pins */
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name=BaseColumns._ID, index=true) var uid: Long?,
    val type: ItemType,
    val uri: Uri?,
    val text: String?,
    val mimeTypes: Array<String>) : Closeable{

    /**
     * Creates a new ClipData which has the same contents as this.
     */
    fun toClipData(): ClipData {
        return when (type) {
            ItemType.IMAGE -> {
                ClipData.newUri(FlorisBoard.getInstance().contentResolver, "Clipboard data", uri)
            }
            ItemType.TEXT -> {
                ClipData.newPlainText("Clipboard data", text)
            }
        }
    }

    /**
     * Instructs the content provider to delete this URI. If not an image, is a noop
     */
    override fun close() {
        if (type == ItemType.IMAGE) {
            FlorisBoard.getInstance().contentResolver.delete(this.uri!!, null, null)
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ClipboardItem

        if (uid != other.uid) return false
        if (type != other.type) return false
        if (uri != other.uri) return false
        if (text != other.text) return false
        if (!mimeTypes.contentEquals(other.mimeTypes)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = uid.hashCode()
        result = 31 * result + type.hashCode()
        result = 31 * result + (uri?.hashCode() ?: 0)
        result = 31 * result + (text?.hashCode() ?: 0)
        result = 31 * result + mimeTypes.contentHashCode()
        return result
    }

    fun stringRepresentation(): String {
        return when {
            uri != null -> "(Image) $uri"
            text != null -> text
            else -> "#ERROR"
        }
    }

    companion object {
        /**
         * So that every item doesn't have to allocate its own array.
         */
        val TEXT_PLAIN = arrayOf("text/plain")

        /**
         * Returns a new ClipboardItem based on a ClipData
         *
         * @param data The ClipData to clone.
         * @param cloneUri Whether to store the image using [FlorisContentProvider].
         */
        fun fromClipData(data: ClipData, cloneUri: Boolean) : ClipboardItem {
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
                        FlorisBoard.getInstance().contentResolver.insert(FlorisContentProvider.CLIPS_URI, values)
                    }
            } else { null }

            val text = FlorisBoard.getInstanceOrNull()?.let { data.getItemAt(0).coerceToText(it).toString() } ?: "#ERROR"
            val mimeTypes = when (type) {
                ItemType.IMAGE -> {
                    (0 until data.description.mimeTypeCount).map {
                        data.description.getMimeType(it)
                    }.toTypedArray()
                }
                ItemType.TEXT -> { TEXT_PLAIN }
            }

            return ClipboardItem(null, type, uri, text, mimeTypes)
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
interface PinnedClipboardItemDao {
    @Query("SELECT * FROM pins")
    fun getAll(): List<ClipboardItem>

    @Insert
    fun insert(item: ClipboardItem) : Long

    @Delete
    fun delete(item: ClipboardItem)
}

@Database(entities = [ClipboardItem::class], version = 1)
@TypeConverters(Converters::class)
abstract class PinnedItemsDatabase : RoomDatabase() {
    abstract fun clipboardItemDao() : PinnedClipboardItemDao

    companion object {
        private var instance: PinnedItemsDatabase? = null

        fun getInstance(): PinnedItemsDatabase {

            if (instance == null) {
                instance = Room.databaseBuilder(
                    FlorisBoard.getInstance(),
                    PinnedItemsDatabase::class.java,
                    "pins").build()
            }

            return instance!!
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
