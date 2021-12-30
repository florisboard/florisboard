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

package dev.patrickgold.florisboard.res

import android.content.Context
import android.net.Uri
import dev.patrickgold.florisboard.assetManager
import dev.patrickgold.florisboard.res.io.FsFileUtils
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

object ZipUtils {
    fun readFileFromArchive(context: Context, zipRef: FlorisRef, relPath: String) = runCatching<String> {
        val assetManager by context.assetManager()
        when {
            zipRef.isAssets -> {
                assetManager.loadTextAsset(zipRef.subRef(relPath)).getOrThrow()
            }
            zipRef.isCache || zipRef.isInternal -> {
                val flexHandle = File(zipRef.absolutePath(context))
                check(flexHandle.isFile) { "Given ref $zipRef is not a file!" }
                var fileContents: String? = null
                ZipFile(flexHandle).use { flexFile ->
                    flexFile.getEntry(relPath)?.let { flexEntry ->
                        fileContents = flexFile.getInputStream(flexEntry).bufferedReader().use { it.readText() }
                    }
                }
                fileContents ?: error("Failed to load requested file $relPath")
            }
            else -> error("Unsupported source!")
        }
    }

    fun zip(context: Context, srcRef: FlorisRef, dstRef: FlorisRef) =
        zip(context, File(srcRef.absolutePath(context)), dstRef)

    fun zip(context: Context, srcDir: File, dstRef: FlorisRef) = runCatching {
        check(srcDir.exists() && srcDir.isDirectory) { "Cannot zip standalone file." }
        when {
            dstRef.isCache || dstRef.isInternal -> {
                val flexFile = File(dstRef.absolutePath(context))
                flexFile.delete()
                flexFile.parentFile?.mkdirs()
                FileOutputStream(flexFile).use { fileOut ->
                    ZipOutputStream(fileOut).use { zipOut ->
                        for (file in srcDir.listFiles() ?: arrayOf()) {
                            zipOut.putNextEntry(ZipEntry(file.name))
                            file.inputStream().use { it.copyTo(zipOut) }
                            zipOut.closeEntry()
                        }
                    }
                }
            }
            else -> error("Unsupported destination!")
        }
    }

    fun zip(context: Context, srcDir: File, uri: Uri) = runCatching {
        check(srcDir.exists() && srcDir.isDirectory) { "Cannot zip standalone file." }
        ExternalContentUtils.writeToUri(context, uri) { fileOut ->
            ZipOutputStream(fileOut).use { zipOut ->
                zip(srcDir, zipOut, "")
            }
        }.getOrThrow()
    }

    internal fun zip(srcDir: File, zipOut: ZipOutputStream, base: String) {
        val dir = File(srcDir, base)
        for (file in dir.listFiles() ?: arrayOf()) {
            val path = if (base.isBlank()) file.name else "$base/${file.name}"
            if (file.isDirectory) {
                zipOut.putNextEntry(ZipEntry("$path/"))
                zipOut.closeEntry()
                zip(srcDir, zipOut, path)
            } else {
                zipOut.putNextEntry(ZipEntry(path))
                file.inputStream().use { it.copyTo(zipOut) }
                zipOut.closeEntry()
            }
        }
    }

    fun unzip(context: Context, srcRef: FlorisRef, dstRef: FlorisRef) =
        unzip(context, srcRef, File(dstRef.absolutePath(context)))

    fun unzip(context: Context, srcRef: FlorisRef, dstDir: File) = runCatching {
        check(dstDir.exists() && dstDir.isDirectory) { "Cannot unzip into file." }
        dstDir.mkdirs()
        when {
            srcRef.isAssets -> {
                FsFileUtils.copy(context, srcRef, dstDir).getOrThrow()
            }
            srcRef.isCache || srcRef.isInternal -> {
                val flexHandle = File(srcRef.absolutePath(context))
                check(flexHandle.isFile) { "Given ref $srcRef is not a file!" }
                ZipFile(flexHandle).use { flexFile ->
                    val flexEntries = flexFile.entries()
                    while (flexEntries.hasMoreElements()) {
                        val flexEntry = flexEntries.nextElement()
                        val tempFile = File(dstDir, flexEntry.name)
                        if (flexEntry.isDirectory) {
                            tempFile.mkdirs()
                        } else {
                            flexFile.copy(flexEntry, tempFile)
                        }
                    }
                }
            }
            else -> error("Unsupported source!")
        }
    }

    fun ZipFile.copy(srcEntry: ZipEntry, dstFile: File) {
        dstFile.outputStream().use { output ->
            this.getInputStream(srcEntry).use { input ->
                input.copyTo(output)
            }
        }
    }
}
