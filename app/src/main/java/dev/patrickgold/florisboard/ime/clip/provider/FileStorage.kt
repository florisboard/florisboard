package dev.patrickgold.florisboard.ime.clip.provider

import android.net.Uri
import dev.patrickgold.florisboard.ime.core.FlorisBoard
import java.io.File
import java.util.*


class FileStorage private constructor() {


    companion object {
        private const val BUF_SIZE = 1024 * 8
        private var instance: FileStorage? = null

        fun getInstance() : FileStorage {
            return instance!!
        }

        fun init(){
            this.instance = FileStorage()
        }

        fun close(){
            this.instance = null
        }

    }


    /**
     * Clones a content URI to internal storage.
     * @param uri The URI
     * @return The name of the file (a random UUID)
     */
    fun cloneURI(uri: Uri) : UUID {
        val context = FlorisBoard.getInstance().context
        val source = context.contentResolver.openInputStream(uri)!!
        val name = UUID.randomUUID()
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


}
