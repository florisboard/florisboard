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

package dev.patrickgold.florisboard.res.importer

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import dev.patrickgold.florisboard.common.android.query
import dev.patrickgold.florisboard.common.android.readToFile
import dev.patrickgold.florisboard.res.io.subFile

object Importer {
    fun readFromUriIntoCache(context: Context, uri: Uri) = readFromUriIntoCache(context, listOf(uri))

    fun readFromUriIntoCache(context: Context, uriList: List<Uri>) = runCatching<ImportWorkspace> {
        val contentResolver = context.contentResolver ?: error("Content resolver of provided context $context is null.")
        val workspace = ImportWorkspace(context).also { it.mkdirs() }
        for (uri in uriList) {
            val info = contentResolver.query(uri)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                cursor.moveToFirst()
                ImportFileInfo(
                    file = workspace.wsInputDir.subFile(cursor.getString(nameIndex)),
                    mediaType = contentResolver.getType(uri),
                    size = cursor.getLong(sizeIndex),
                )
            } ?: error("Unable to fetch info about one or more resources to be imported.")
            contentResolver.readToFile(uri, info.file)
            workspace.wsInputFiles.add(info)
        }
        workspace
    }
}
