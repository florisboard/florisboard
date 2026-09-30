/*
 * Copyright (C) 2026 The FlorisBoard Contributors
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

package dev.patrickgold.florisboard.ime.io

import android.net.Uri
import android.provider.OpenableColumns
import kotlinx.io.asSource
import kotlinx.io.buffered
import org.florisboard.lib.android.query
import org.florisboard.lib.android.read

fun AndroidStorage.readFromUri(
    srcUri: Uri,
    dstDirRef: FlorisRef,
) {
    val contentResolver = context.contentResolver ?: error("Content resolver is null.")
    contentResolver.query(srcUri)?.use { cursor ->
        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        cursor.moveToFirst()
        val dstFileRef = dstDirRef.subRef(cursor.getString(nameIndex))
        contentResolver.read(srcUri) { inputStream ->
            inputStream.asSource().use { source ->
                sink(dstFileRef).buffered().use { sink ->
                    sink.transferFrom(source)
                }
            }
        }
    }
}
