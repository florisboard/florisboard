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

package dev.patrickgold.florisboard.ime.extension

import android.content.Context
import android.net.Uri

class ExternalContentUtils private constructor() {
    companion object {
        fun readTextFromUri(context: Context, uri: Uri): Result<String> {
            val contentResolver = context.contentResolver
                ?: return Result.failure(NullPointerException("System content resolver not available"))
            val inputStream = contentResolver.openInputStream(uri)
                ?: return Result.failure(NullPointerException("Cannot open input stream for given uri '$uri'"))
            val rawText = inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            return Result.success(rawText)
        }

        fun writeTextToUri(context: Context, uri: Uri, text: String): Result<Unit> {
            val contentResolver = context.contentResolver
                ?: return Result.failure(NullPointerException("System content resolver not available"))
            // Must use "rwt" mode to ensure destination file length is truncated after writing.
            val outputStream = contentResolver.openOutputStream(uri, "rwt")
                ?: return Result.failure(NullPointerException("Cannot open output stream for given uri '$uri'"))
            outputStream.bufferedWriter(Charsets.UTF_8).use { it.flush(); it.write(text) }
            return Result.success(Unit)
        }
    }
}
