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

/**
 * Allows apps to access images on the clipboard.
 *
 * This is sometimes called by the UI thread, so all functions are non blocking.
 * Database accesses are performed async.
 */
class ClipboardImagesProvider : ContentProvider() {
    private var fileUriDao: FileUriDao? = null
    private val cachedMimeTypes: HashMap<Long, Array<String>> = hashMapOf()
    private val ioScope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    companion object {
        const val AUTHORITY = "${BuildConfig.APPLICATION_ID}.provider.clipboard"
        val IMAGE_CLIPS_URI: Uri = Uri.parse("content://$AUTHORITY/clips/images")

        private const val IMAGE_CLIP_ITEM = 0
        private const val IMAGE_CLIPS_TABLE = 1

        val matcher = UriMatcher(UriMatcher.NO_MATCH).apply {
            addURI(AUTHORITY, "clips/images/#", IMAGE_CLIP_ITEM)
            addURI(AUTHORITY, "clips/images", IMAGE_CLIPS_TABLE)
        }
    }

    object Columns {
        const val ImageUri = "image_uri"
        const val MimeTypes = "mime_types"
    }

    fun init() {
        fileUriDao = Room.databaseBuilder(
            context!!,
            FileUriDatabase::class.java,
            "fileuridb",
        ).build().fileUriDao()

        for (fileUri in fileUriDao?.getAll() ?: emptyList()) {
            cachedMimeTypes[fileUri.fileName] = fileUri.mimeTypes
        }
    }

    override fun onCreate(): Boolean {
        ioScope.launch {
            init()
        }
        return true
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor? {
        // Just return nothing, nothing should call this function at all.
        return null
    }

    override fun getType(uri: Uri): String? {
        return when (matcher.match(uri)) {
            IMAGE_CLIP_ITEM -> cachedMimeTypes.getOrDefault(ContentUris.parseId(uri), null)?.getOrNull(0)
            IMAGE_CLIPS_TABLE -> "${ContentResolver.CURSOR_DIR_BASE_TYPE}/vnd.florisboard.image_clip_table"
            else -> null
        }
    }

    override fun getStreamTypes(uri: Uri, mimeTypeFilter: String): Array<String>? {
        return when (matcher.match(uri)) {
            IMAGE_CLIP_ITEM -> cachedMimeTypes.getOrDefault(ContentUris.parseId(uri), null)
            else -> null
        }
    }

    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor {
        val id = ContentUris.parseId(uri)
        val file = ClipboardFileStorage.getFileForId(context!!, id)

        // Nothing has permission to write anyway.
        return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri {
        when (matcher.match(uri)) {
            IMAGE_CLIPS_TABLE -> {
                return try {
                    values as ContentValues
                    val imageUri = Uri.parse(values.getAsString(Columns.ImageUri))
                    val id = ClipboardFileStorage.cloneUri(context!!, imageUri)
                    val mimeTypes = values.getAsString(Columns.MimeTypes).split(",").toTypedArray()
                    cachedMimeTypes[id] = mimeTypes
                    ioScope.launch {
                        fileUriDao?.insert(FileUri(id, mimeTypes))
                    }
                    ContentUris.withAppendedId(IMAGE_CLIPS_URI, id)
                } catch (e: Exception) {
                    flogError { e.message.toString() }
                    uri.buildUpon().appendPath("0").build()
                }
            }
            else -> error("Unable to identify type of $uri")
        }
    }

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int {
        when (matcher.match(uri)){
            IMAGE_CLIP_ITEM -> {
                val id = ContentUris.parseId(uri)
                ClipboardFileStorage.deleteById(context!!, id)
                cachedMimeTypes.remove(id)
                context?.revokeUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                ioScope.launch {
                    fileUriDao?.delete(id)
                }
                return 1
            }
            else -> error("Unable to identify type of $uri")
        }
    }

    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int {
        error("This ContentProvider does not support update.")
    }
}
