package dev.patrickgold.florisboard.ime.clip.provider

import android.net.Uri
import dev.patrickgold.florisboard.debug.*
import dev.patrickgold.florisboard.ime.core.FlorisBoard
import java.io.File
import java.io.InputStream

/**
 * Backend helper object which is used by [FlorisContentProvider] to serve content.
 */
object FileStorage {
    private const val BUF_SIZE = 1024 * 8
    private var offset = 0

    /**
     * Clones a content URI to internal storage.
     *
     * @param uri The URI
     * @return The file's name which is a unique long
     */
    @Synchronized
    fun cloneURI(uri: Uri): Result<Long> {
        val context = FlorisBoard.getInstance()
        // nanoTime + the number of items created so that it's unique.
        val name = (System.nanoTime() + offset)

        // Just a normal copy from input stream to output stream.
        val source: InputStream
        try {
            source = context.contentResolver.openInputStream(uri) ?: return Result.failure(
                NullPointerException("Input stream for given URI '$uri' is null!")
            )
        } catch (e: Exception) {
            return Result.failure(e)
        }
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

        return Result.success(name)
    }

    /**
     * Deletes the file corresponding to an id.
     */
    fun deleteById(id: Long) {
        flogDebug(LogTopic.CLIPBOARD) { "Cleaning up $id" }
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
