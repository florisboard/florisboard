package dev.patrickgold.florisboard.ime.clip.provider

import android.net.Uri
import dev.patrickgold.florisboard.ime.core.FlorisBoard
import timber.log.Timber
import java.io.File

/**
 * Backend class which is used by [FlorisContentProvider] to serve content.
 */
class FileStorage private constructor() {


    companion object {
        private const val BUF_SIZE = 1024 * 8
        private var instance: FileStorage? = null
        private var offset = 0


        fun getInstance() : FileStorage {
            if (this.instance == null){
                this.instance = FileStorage()
            }
            return instance!!
        }

    }


    /**
     * Clones a content URI to internal storage.
     * @param uri The URI
     * @return  the file's name which is a unique long
     */
    @Synchronized
    fun cloneURI(uri: Uri) : Long {
        val context = FlorisBoard.getInstance().context
        // nanoTime + the number of items created so that it's unique.
        val name = (System.nanoTime() + offset)

        // Just a normal copy from input stream to output stream.
        val source = context.contentResolver.openInputStream(uri)!!
        val sink = File(context.filesDir, name.toString()).outputStream()
        var nread = 0L
        val buf = ByteArray(BUF_SIZE)
        var n: Int
        while (source.read(buf).also { n = it } > 0) {
            sink.write(buf, 0, n)
            nread += n.toLong()
        }

        source.close()
        sink.close()

        return name
    }

    /**
     * Deletes the file corresponding to an id.
     */
    fun deleteById(id: Long) {
        Timber.d("Cleaning up $id")
        val file = File(FlorisBoard.getInstance().filesDir, id.toString())
        file.delete()
    }

    /**
     * Get the file address of an id.
     */
    fun getAddress(id: Long): String {
        return FlorisBoard.getInstance().filesDir.toString() + "/$id"
    }


}
