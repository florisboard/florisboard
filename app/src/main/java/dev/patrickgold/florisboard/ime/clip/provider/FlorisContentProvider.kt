package dev.patrickgold.florisboard.ime.clip.provider

import android.content.ContentProvider
import android.content.ContentUris
import android.content.ContentValues
import android.content.UriMatcher
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.ParcelFileDescriptor
import androidx.room.Room
import timber.log.Timber
import java.util.concurrent.Executor
import java.util.concurrent.Executors


class FlorisContentProvider : ContentProvider() {

    private lateinit var fileUriDao: FileUriDao
    private val mimeTypes: HashMap<Long, Array<String>> = hashMapOf()
    private lateinit var executor:Executor

    override fun onCreate(): Boolean {
        Timber.d("oncreate?")

        return true
    }

    private fun initIfNotAlready(){
        if (this::fileUriDao.isInitialized){
            return
        }


        fileUriDao = Room.databaseBuilder(
            context!!,
            FileUriDatabase::class.java, "fileuridb"
        ).build().fileUriDao()

        executor = Executors.newSingleThreadExecutor()
        Timber.d("before execution")
        executor.execute {
            Timber.d("EXECUTED")
            for (fileUri in fileUriDao.getAll()) {
                mimeTypes[fileUri.fileName] = fileUri.mimeTypes
            }
        }
    }


    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor {
        val id = ContentUris.parseId(uri)
        val cursor = MatrixCursor(arrayOf("_data"), 1)

        cursor.addRow(arrayOf(FileStorage.getInstance().getAddress(id)))
        cursor.setNotificationUri(context?.contentResolver, uri)
        return cursor
    }

    override fun getType(uri: Uri): String {
        initIfNotAlready()
        Timber.d("HAVE ${mimeTypes}")
        return when (matcher.match(uri)) {
            CLIP_ITEM -> mimeTypes.getOrElse(ContentUris.parseId(uri), {throw IllegalArgumentException("Don't have this item!")})[0]
            CLIPS_TABLE -> "vnd.android.cursor.dir/$AUTHORITY.clip"
            else -> throw IllegalArgumentException("Don't know what this is $uri")
        }
    }

    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor {
        return openFileHelper(uri, mode)
    }
    override fun insert(uri: Uri, values: ContentValues?): Uri {
        initIfNotAlready()
        when (matcher.match(uri)){
            CLIPS_TABLE -> {
                val id = FileStorage.getInstance().cloneURI(Uri.parse(values?.getAsString("uri")))
                val mimes =  values?.getAsString("mimetypes")?.split(",")?.toTypedArray()
                mimes?.let {
                    mimeTypes[id] = mimes
                    executor.execute {
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
        initIfNotAlready()
        when (matcher.match(uri)){
            CLIP_ITEM -> {
                val id = ContentUris.parseId(uri)
                FileStorage.getInstance().deleteById(id)
                mimeTypes.remove(id)
                executor.execute {
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


    companion object {
        private var instance: FlorisContentProvider? = null
        const val AUTHORITY = "dev.patrickgold.florisboard.provider.clip"
        val CONTENT_URI: Uri = Uri.parse("content://$AUTHORITY")
        val CLIPS_URI: Uri = Uri.parse("content://$AUTHORITY/clips")

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
}
