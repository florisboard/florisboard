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
import dev.patrickgold.florisboard.common.resultErrStr
import dev.patrickgold.florisboard.common.resultOk
import java.io.File

object FileUtils {
    fun copy(context: Context, srcRef: FlorisRef, dst: File): Result<Unit> {
        return when {
            srcRef.isAssets -> {
                val base = srcRef.relativePath.removeSuffix("/")
                copyApkAssets(context, base, "", dst)
                resultOk()
            }
            srcRef.isCache || srcRef.isInternal -> {
                val srcFile = File(srcRef.absolutePath(context))
                srcFile.copyRecursively(dst)
                resultOk()
            }
            else -> {
                resultErrStr("")
            }
        }
    }

    private fun copyApkAssets(context: Context, base: String, path: String, dst: File) {
        val apkAssetsPath = if (base.isBlank()) path else "$base/$path"
        val list = context.assets.list(apkAssetsPath) ?: return
        if (list.isEmpty()) {
            // Is file
            val file = File(dst, path)
            context.assets.open(apkAssetsPath).use { inStream ->
                file.outputStream().use { outStream ->
                    inStream.copyTo(outStream)
                }
            }
        } else {
            // Is directory
            for (entry in list) {
                copyApkAssets(context, base, "$apkAssetsPath/$entry", dst)
            }
        }
    }
}
