package dev.patrickgold.florisboard.ime.clip.provider

import android.content.*
import android.database.Cursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.room.Room
import dev.patrickgold.florisboard.BuildConfig
import dev.patrickgold.florisboard.debug.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File

/**
 * Allows apps to access images on the clipboard.
 *
 * This is sometimes called by the UI thread, so all functions are non blocking.
 * Database accesses are performed async.
 */
class FlorisContentProvider : ContentProvider() {
    private lateinit var fileUriDao: FileUriDao
    private val mimeTypes: HashMap<Long, Array<String>> = hashMapOf()
    private val ioScope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    companion object {
        const val AUTHORITY = "${BuildConfig.APPLICATION_ID}.provider.clip"
        val CONTENT_URI: Uri = Uri.parse("content://$AUTHORITY")
        val CLIPS_URI: Uri = Uri.parse("content://$AUTHORITY/clips")

        private var instance: FlorisContentProvider? = null

        fun getInstance(): FlorisContentProvider {
            return instance!!
        }

        private const val CLIPS_TABLE = 1
        private const val CLIP_ITEM = 0

        val matcher: UriMatcher = UriMatcher(UriMatcher.NO_MATCH).apply {
            addURI(AUTHORITY, "clips/#", CLIP_ITEM)
            addURI(AUTHORITY, "clips", CLIPS_TABLE)
        }
    }

    override fun onCreate(): Boolean {
        instance = this
        return true
    }

    fun initIfNotAlready(){
        if (this::fileUriDao.isInitialized){
            return
        }

        fileUriDao = Room.databaseBuilder(
            context!!,
            FileUriDatabase::class.java, "fileuridb"
        ).build().fileUriDao()

        for (fileUri in fileUriDao.getAll()) {
            mimeTypes[fileUri.fileName] = fileUri.mimeTypes
        }
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor? {
        // just return nothing, nothing should call this function at all.
        return null
    }

    override fun getType(uri: Uri): String {
        return when (matcher.match(uri)) {
            CLIP_ITEM -> mimeTypes.getOrElse(ContentUris.parseId(uri), { throw IllegalArgumentException("Don't have this item!") })[0]
            CLIPS_TABLE -> "vnd.android.cursor.dir/$AUTHORITY.clip"
            else -> throw IllegalArgumentException("Don't know what this is $uri")
        }
    }

    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor {
        val id = ContentUris.parseId(uri)
        val path = File(FileStorage.getAddress(id))

        // Nothing has permission to write anyway.
        return ParcelFileDescriptor.open(path, ParcelFileDescriptor.MODE_READ_ONLY)
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri {
        when (matcher.match(uri)){
            CLIPS_TABLE -> {
                val id = FileStorage.cloneURI(Uri.parse(values?.getAsString("uri"))).getOrElse {
                    flogError(LogTopic.CLIPBOARD) { it.toString() }
                    return uri.buildUpon().appendPath("0").build()
                }
                val mimes =  values?.getAsString("mimetypes")?.split(",")?.toTypedArray()
                mimes?.let {
                    mimeTypes[id] = mimes
                    ioScope.launch {
                        Timber.d("Inserted file uri $id")
                        fileUriDao.insert(FileUri(id, mimes))
                    }
                }
                return ContentUris.withAppendedId(CLIPS_URI, id)
            }
            else -> throw IllegalArgumentException("Don't know what this is $uri")
        }
    }

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int {
        when (matcher.match(uri)){
            CLIP_ITEM -> {
                val id = ContentUris.parseId(uri)
                FileStorage.deleteById(id)
                mimeTypes.remove(id)
                context?.revokeUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                ioScope.launch {
                    fileUriDao.delete(id)
                }
                return 1
            }
            else -> throw IllegalArgumentException("Don't know what this is $uri")
        }
    }

    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int {
        throw IllegalArgumentException("This ContentProvider does not support update.")
    }
}
