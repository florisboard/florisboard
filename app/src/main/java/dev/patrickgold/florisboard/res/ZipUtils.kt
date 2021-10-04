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
import dev.patrickgold.florisboard.common.resultErr
import dev.patrickgold.florisboard.common.resultErrStr
import dev.patrickgold.florisboard.common.resultOk
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

object ZipUtils {
    fun unzip(context: Context, srcRef: FlorisRef, dstRef: FlorisRef): Result<Unit> {
        return unzip(context, srcRef, File(dstRef.absolutePath(context)))
    }

    fun unzip(context: Context, srcRef: FlorisRef, dstDir: File): Result<Unit> {
        if (dstDir.exists() && dstDir.isFile) {
            return resultErrStr("Cannot unzip into file.")
        } else {
            dstDir.mkdirs()
        }
        return when {
            srcRef.isAssets -> {
                FileUtils.copy(context, srcRef, dstDir)
            }
            srcRef.isCache || srcRef.isInternal -> {
                val flexHandle = File(srcRef.absolutePath(context))
                if (!flexHandle.isFile) return resultErrStr("Given ref $srcRef is not a file!")
                try {
                    val flexFile = ZipFile(flexHandle)
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
                    flexFile.close()
                    resultOk()
                } catch (e: Exception) {
                    resultErr(e)
                }
            }
            else -> resultErrStr("Unsupported source!")
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
