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

import android.content.Context
import android.net.Uri
import dev.patrickgold.florisboard.debug.LogTopic
import dev.patrickgold.florisboard.debug.flogDebug
import java.io.File

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
    fun cloneURI(context: Context, uri: Uri) = runCatching<Long> {
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

        return@runCatching name
    }

    /**
     * Deletes the file corresponding to an id.
     */
    fun deleteById(context: Context, id: Long) {
        flogDebug(LogTopic.CLIPBOARD) { "Cleaning up $id" }
        val file = File(context.filesDir, id.toString())
        file.delete()
    }

    /**
     * Get the file address of an id.
     */
    fun getAddress(context: Context, id: Long): String {
        return context.filesDir.toString() + "/$id"
    }
}
