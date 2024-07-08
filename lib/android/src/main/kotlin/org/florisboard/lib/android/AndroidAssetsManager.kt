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

package org.florisboard.lib.android

import org.florisboard.lib.kotlin.io.FsDir
import org.florisboard.lib.kotlin.io.FsFile
import org.florisboard.lib.kotlin.io.subDir
import org.florisboard.lib.kotlin.io.subFile
import java.io.BufferedReader
import java.io.IOException
import java.io.Reader
import java.nio.charset.Charset

/**
 * Public typealias for the Android AssetManager class, which provides a basic API to read files which are statically
 * shipped in the APK assets. The typealias is used to allow both the FlorisBoard and Android asset managers to coexist
 * without name clashes.
 */
typealias AndroidAssetManager = android.content.res.AssetManager

/**
 * Creates a reader using specified [charset] or UTF-8 on the APK file with [path] and returns it.
 *
 * @param path The relative path of the APK assets file.
 * @param charset The charset used for decoding the raw bytes. Defaults to UTF-8.
 *
 * @return A new reader with given [charset].
 *
 * @throws IOException If the file does not exist or another IO error occurred.
 */
fun AndroidAssetManager.reader(path: String, charset: Charset = Charsets.UTF_8): Reader {
    return this.open(path).reader(charset)
}

/**
 * Creates a buffered reader using specified [charset] or UTF-8 on the APK file with [path] and returns it.
 *
 * @param path The relative path of the APK assets file.
 * @param charset The charset used for decoding the raw bytes. Defaults to UTF-8.
 *
 * @return A new buffered reader with given [charset].
 *
 * @throws IOException If the file does not exist or another IO error occurred.
 */
fun AndroidAssetManager.bufferedReader(path: String, charset: Charset = Charsets.UTF_8): BufferedReader {
    return this.open(path).bufferedReader(charset)
}

/**
 * Reads an entire APK assets file using specified [charset] or UTF-8 into memory and returns it. This method is not
 * recommended for huge files and must stay within the heap size limits, or it will result in an OOM error.
 *
 * @param path The relative path of the APK assets file.
 * @param charset The charset used for decoding the raw bytes. Defaults to UTF-8.
 *
 * @return The full text of the given file.
 *
 * @throws IOException If the file does not exist or another IO error occurred.
 */
fun AndroidAssetManager.readText(path: String, charset: Charset = Charsets.UTF_8): String {
    return this.reader(path, charset).use { it.readText() }
}

/**
 * Read an APK asset file using ACCESS_STREAMING mode and copy it to given [dst] file.
 *
 * @param path The relative path of the file to read.
 * @param dst The file object which is used to obtain a file output stream on.
 *
 * @throws IOException If the file does not exist or another IO error occurred.
 */
fun AndroidAssetManager.copy(path: String, dst: FsFile) {
    this.open(path).use { inStream ->
        dst.outputStream().use { outStream ->
            inStream.copyTo(outStream)
        }
    }
}

/**
 * Read an APK asset directory using ACCESS_STREAMING mode for each file and recursively copy it to the given [dst]
 * directory. If the given path is just a file, this method behaves exactly as [copy].
 *
 * @param path The relative path of the file to read.
 * @param dst The file object which is used to obtain sub references and their respective file output streams.
 *
 * @throws IOException If the directory/file does not exist or another IO error occurred. Even in the event of an
 *  exception a partial copy may have been done.
 */
fun AndroidAssetManager.copyRecursively(path: String, dst: FsDir) {
    this.copyApkAssets(path, "", dst)
}

private fun AndroidAssetManager.copyApkAssets(base: String, path: String, dst: FsDir) {
    val apkAssetsPath = if (base.isBlank()) path else if (path.isBlank()) base else "$base/$path"
    val list = this.list(apkAssetsPath) ?: return
    if (list.isEmpty()) {
        // Is file
        val file = dst.subFile(path)
        this.copy(apkAssetsPath, file)
    } else {
        // Is directory
        val dir = dst.subDir(path)
        dir.mkdirs()
        for (entry in list) {
            this.copyApkAssets(base, if (path.isBlank()) entry else "$path/$entry", dst)
        }
    }
}
