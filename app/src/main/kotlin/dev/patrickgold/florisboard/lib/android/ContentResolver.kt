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

@file:Suppress("NOTHING_TO_INLINE")

package dev.patrickgold.florisboard.lib.android

import android.content.ContentResolver
import android.content.res.AssetFileDescriptor
import android.net.Uri
import dev.patrickgold.florisboard.lib.io.FsFile
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStream
import java.io.OutputStream
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/**
 * Shorthand function for querying a Uri without any other arguments.
 *
 * @see android.content.ContentResolver.query
 */
inline fun ContentResolver.query(uri: Uri) = this.query(uri, null, null, null, null)

/**
 * Shorthand function for querying a Uri and projection without any other arguments.
 *
 * @see android.content.ContentResolver.query
 */
inline fun ContentResolver.query(uri: Uri, projection: Array<String>) = this.query(uri, projection, null, null, null)

inline fun ContentResolver.read(uri: Uri, maxSize: Long = Long.MAX_VALUE, block: (InputStream) -> Unit) {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    require(maxSize > 0) { "Argument `maxSize` must be greater than 0" }
    val inputStream = this.openInputStream(uri)
        ?: error("Cannot open input stream for given uri '$uri'")
    val assetFileDescriptor = this.openAssetFileDescriptor(uri, "r")
        ?: error("Cannot open asset file descriptor for given uri '$uri'")
    assetFileDescriptor.use {
        val assetFileSize = assetFileDescriptor.length
        if (assetFileSize != AssetFileDescriptor.UNKNOWN_LENGTH) {
            if (assetFileSize > maxSize) {
                error("Contents of given uri '$uri' exceeds maximum size of $maxSize bytes!")
            }
        }
    }
    inputStream.use(block)
}

inline fun ContentResolver.readToFile(uri: Uri, file: FsFile) {
    this.read(uri) { inStream ->
        file.outputStream().use { outStream ->
            inStream.copyTo(outStream)
        }
    }
}

inline fun ContentResolver.readText(uri: Uri, block: (BufferedReader) -> Unit) {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    this.read(uri) { inStream ->
        inStream.bufferedReader().use(block)
    }
}

inline fun ContentResolver.readAllText(uri: Uri): String {
    val text: String
    this.read(uri) { inStream ->
        text = inStream.bufferedReader().use { it.readText() }
    }
    return text
}

inline fun ContentResolver.write(uri: Uri, block: (OutputStream) -> Unit) {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    val outputStream = this.openOutputStream(uri, "wt")
        ?: error("Cannot open input stream for given uri '$uri'")
    outputStream.use(block)
}

inline fun ContentResolver.writeFromFile(uri: Uri, file: FsFile) {
    this.write(uri) { outStream ->
        file.inputStream().use { inStream ->
            inStream.copyTo(outStream)
        }
    }
}

inline fun ContentResolver.writeText(uri: Uri, block: (BufferedWriter) -> Unit) {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    this.write(uri) { outStream ->
        outStream.bufferedWriter().use(block)
    }
}

inline fun ContentResolver.writeAllText(uri: Uri, text: String) {
    this.write(uri) { outStream ->
        outStream.bufferedWriter().use { it.write(text) }
    }
}
