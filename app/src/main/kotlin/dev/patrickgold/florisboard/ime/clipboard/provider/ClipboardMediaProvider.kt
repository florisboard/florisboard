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

import android.content.ContentProvider
import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.content.Intent
import android.content.UriMatcher
import android.database.Cursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import dev.patrickgold.florisboard.BuildConfig
import dev.patrickgold.florisboard.lib.devtools.flogError
import dev.patrickgold.florisboard.lib.kotlin.tryOrNull
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Allows apps to access images and videos on the clipboard.
 *
 * This is sometimes called by the UI thread, so all functions are non blocking.
 * Database accesses are performed async.
 */
class ClipboardMediaProvider : ContentProvider() {
    private var clipboardFilesDao: ClipboardFilesDao? = null
    private val cachedFileInfos: HashMap<Long, ClipboardFileInfo> = hashMapOf()
    private val ioScope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    companion object {
        const val AUTHORITY = "${BuildConfig.APPLICATION_ID}.provider.clipboard"
        val IMAGE_CLIPS_URI: Uri = Uri.parse("content://$AUTHORITY/clips/images")
        val VIDEO_CLIPS_URI: Uri = Uri.parse("content://$AUTHORITY/clips/videos")

        private const val IMAGE_CLIP_ITEM = 0
        private const val IMAGE_CLIPS_TABLE = 1
        private const val VIDEO_CLIP_ITEM = 2
        private const val VIDEO_CLIPS_TABLE = 3

        private val Matcher = UriMatcher(UriMatcher.NO_MATCH).apply {
            addURI(AUTHORITY, "clips/images/#", IMAGE_CLIP_ITEM)
            addURI(AUTHORITY, "clips/images", IMAGE_CLIPS_TABLE)
            addURI(AUTHORITY, "clips/videos/#", VIDEO_CLIP_ITEM)
            addURI(AUTHORITY, "clips/videos", VIDEO_CLIPS_TABLE)
        }
    }

    object Columns {
        const val MediaUri = "media_uri"
        const val MimeTypes = "mime_types"
    }

    fun init() {
        clipboardFilesDao = ClipboardFilesDatabase.new(context!!).clipboardFilesDao()

        for (clipboardFileInfo in clipboardFilesDao?.getAll() ?: emptyList()) {
            cachedFileInfos[clipboardFileInfo.id] = clipboardFileInfo
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
        val id = tryOrNull { ContentUris.parseId(uri) } ?: return null
        return clipboardFilesDao?.getCursorById(id)
    }

    override fun getType(uri: Uri): String? {
        return when (Matcher.match(uri)) {
            IMAGE_CLIP_ITEM, VIDEO_CLIP_ITEM -> {
                cachedFileInfos.getOrDefault(ContentUris.parseId(uri), null)?.mimeTypes?.getOrNull(0)
            }
            IMAGE_CLIPS_TABLE -> "${ContentResolver.CURSOR_DIR_BASE_TYPE}/vnd.florisboard.image_clip_table"
            VIDEO_CLIPS_TABLE -> "${ContentResolver.CURSOR_DIR_BASE_TYPE}/vnd.florisboard.video_clip_table"
            else -> null
        }
    }

    override fun getStreamTypes(uri: Uri, mimeTypeFilter: String): Array<String>? {
        return when (Matcher.match(uri)) {
            IMAGE_CLIP_ITEM, VIDEO_CLIP_ITEM -> {
                cachedFileInfos.getOrDefault(ContentUris.parseId(uri), null)?.mimeTypes
            }
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
        when (val m = Matcher.match(uri)) {
            IMAGE_CLIPS_TABLE, VIDEO_CLIPS_TABLE -> {
                return try {
                    values as ContentValues
                    val mediaUri = Uri.parse(values.getAsString(Columns.MediaUri))
                    val id = ClipboardFileStorage.cloneUri(context!!, mediaUri)
                    val size = ClipboardFileStorage.getFileForId(context!!, id).length()
                    val mimeTypes = values.getAsString(Columns.MimeTypes).split(",").toTypedArray()
                    val displayName = values.getAsString(OpenableColumns.DISPLAY_NAME)
                    val fileInfo = ClipboardFileInfo(id, displayName, size, mimeTypes)
                    cachedFileInfos[id] = fileInfo
                    ioScope.launch {
                        clipboardFilesDao?.insert(fileInfo)
                    }
                    if (m == IMAGE_CLIPS_TABLE) {
                        ContentUris.withAppendedId(IMAGE_CLIPS_URI, id)
                    } else {
                        ContentUris.withAppendedId(VIDEO_CLIPS_URI, id)
                    }
                } catch (e: Exception) {
                    flogError { e.message.toString() }
                    uri.buildUpon().appendPath("0").build()
                }
            }
            else -> error("Unable to identify type of $uri")
        }
    }

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int {
        when (Matcher.match(uri)) {
            IMAGE_CLIP_ITEM, VIDEO_CLIP_ITEM -> {
                val id = ContentUris.parseId(uri)
                ClipboardFileStorage.deleteById(context!!, id)
                cachedFileInfos.remove(id)
                context?.revokeUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                ioScope.launch {
                    clipboardFilesDao?.delete(id)
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
