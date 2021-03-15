package dev.patrickgold.florisboard.ime.clip.provider

import android.content.ClipData
import android.net.Uri
import androidx.room.*
import dev.patrickgold.florisboard.ime.core.FlorisBoard
import java.util.*



enum class ItemType(val value: Int) {
    TEXT(1),
    IMAGE(2);

    companion object {
        fun fromInt(value : Int) : ItemType {
            return values().first { it.value == value }
        }
    }
}



@Entity
data class ClipboardItem(
    @PrimaryKey val uid: UUID,
    val type: ItemType,
    val uri: Uri?,
    val text: String?,
    val mimeTypes: Array<String>
){
    fun toClipData(): ClipData {
        return when (type) {
            ItemType.IMAGE -> {
                ClipData.newUri(FlorisBoard.getInstance().context.contentResolver, "Clipboard data", uri)
            }
            ItemType.TEXT -> {
                ClipData.newPlainText("Clipboard data", text)
            }
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

    companion object {
        /**
         * Returns a new ClipboardItem, resolving the URI.
         */
        fun fromClipData(data: ClipData) : ClipboardItem {

            val type = when {
                data.getItemAt(0)?.uri != null -> ItemType.IMAGE
                data.getItemAt(0)?.text != null -> ItemType.TEXT
                else -> null
            }!!

            val uri = if (type == ItemType.IMAGE) {
                    if (data.getItemAt(0).uri.authority == "dev.patrickgold.florisboard.provider.clips"){
                        data.getItemAt(0).uri
                    }else {
                        val uuid = FileStorage.getInstance().cloneURI(data.getItemAt(0).uri)
                        FlorisContentProvider.getInstance().uuidToUri(uuid)
                    }
            }else { null }

            val text = data.getItemAt(0).text.toString()
            val mimeTypes = Array(data.description.mimeTypeCount) { "" }

            (0 until data.description.mimeTypeCount).forEach {
                    mimeTypes[it] = data.description.getMimeType(it)
            }

            return ClipboardItem(UUID.randomUUID(), type, uri, text, mimeTypes)
        }
    }
}


@Dao
interface ClipboardItemDao {
 //   @Query("SELECT * FROM clipboard")
 //   fun getAll(): List<ClipboardItem>

    @Insert
    fun insert(vararg items: ClipboardItem)

}
