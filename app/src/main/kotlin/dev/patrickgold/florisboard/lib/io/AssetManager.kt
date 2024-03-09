/*
 * Copyright (C) 2020 Patrick Goldinger
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

package dev.patrickgold.florisboard.lib.io

import android.content.Context
import dev.patrickgold.florisboard.appContext
import dev.patrickgold.florisboard.ime.keyboard.AbstractKeyData
import dev.patrickgold.florisboard.ime.keyboard.CaseSelector
import dev.patrickgold.florisboard.ime.keyboard.CharWidthSelector
import dev.patrickgold.florisboard.ime.keyboard.KanaSelector
import dev.patrickgold.florisboard.ime.keyboard.KeyData
import dev.patrickgold.florisboard.ime.keyboard.LayoutDirectionSelector
import dev.patrickgold.florisboard.ime.keyboard.ShiftStateSelector
import dev.patrickgold.florisboard.ime.keyboard.VariationSelector
import dev.patrickgold.florisboard.ime.text.keyboard.AutoTextKeyData
import dev.patrickgold.florisboard.ime.text.keyboard.MultiTextKeyData
import dev.patrickgold.florisboard.ime.text.keyboard.TextKeyData
import dev.patrickgold.florisboard.lib.android.reader
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import org.florisboard.lib.kotlin.resultErr
import org.florisboard.lib.kotlin.resultErrStr
import org.florisboard.lib.kotlin.resultOk
import java.io.File

val DefaultJsonConfig = Json {
    classDiscriminator = "$"
    encodeDefaults = true
    ignoreUnknownKeys = true
    isLenient = true
    serializersModule = SerializersModule {
        polymorphic(AbstractKeyData::class) {
            subclass(TextKeyData::class, TextKeyData.serializer())
            subclass(AutoTextKeyData::class, AutoTextKeyData.serializer())
            subclass(MultiTextKeyData::class, MultiTextKeyData.serializer())
            subclass(CaseSelector::class, CaseSelector.serializer())
            subclass(ShiftStateSelector::class, ShiftStateSelector.serializer())
            subclass(VariationSelector::class, VariationSelector.serializer())
            subclass(LayoutDirectionSelector::class, LayoutDirectionSelector.serializer())
            subclass(CharWidthSelector::class, CharWidthSelector.serializer())
            subclass(KanaSelector::class, KanaSelector.serializer())
            defaultDeserializer { TextKeyData.serializer() }
        }
        polymorphic(KeyData::class) {
            subclass(TextKeyData::class, TextKeyData.serializer())
            subclass(AutoTextKeyData::class, AutoTextKeyData.serializer())
            subclass(MultiTextKeyData::class, MultiTextKeyData.serializer())
            defaultDeserializer { TextKeyData.serializer() }
        }
    }
}

// TODO: fully deprecate and remove this class, should be substituted by extension funs on the actual file and stream
//       instances
class AssetManager(context: Context) {
    val appContext by context.appContext()

    fun delete(ref: FlorisRef) {
        when {
            ref.isCache || ref.isInternal -> {
                ref.absoluteFile(appContext).delete()
            }
            else -> error("Can not delete directory/file in location '${ref.scheme}'.")
        }
    }

    fun hasAsset(ref: FlorisRef): Boolean {
        return when {
            ref.isAssets -> {
                try {
                    val file = File(ref.relativePath)
                    val list = appContext.assets.list(file.parent?.toString() ?: "")
                    list?.contains(file.name) == true
                } catch (e: Exception) {
                    false
                }
            }
            ref.isCache || ref.isInternal -> {
                val file = File(ref.absolutePath(appContext))
                file.exists() && file.isFile
            }
            else -> false
        }
    }

    fun list(ref: FlorisRef) = list(ref, files = true, dirs = true)

    fun listFiles(ref: FlorisRef) = list(ref, files = true, dirs = false)

    fun listDirs(ref: FlorisRef) = list(ref, files = false, dirs = true)

    private fun list(ref: FlorisRef, files: Boolean, dirs: Boolean) = runCatching<List<FlorisRef>> {
        when {
            !files && !dirs -> listOf()
            ref.isAssets -> {
                appContext.assets.list(ref.relativePath)?.mapNotNull { fileName ->
                    val subList = appContext.assets.list("${ref.relativePath}/$fileName") ?: return@mapNotNull null
                    when {
                        files && dirs || files && subList.isEmpty() || dirs && subList.isNotEmpty() -> {
                            ref.subRef(fileName)
                        }
                        else -> null
                    }
                } ?: listOf()
            }
            ref.isCache || ref.isInternal -> {
                val dir = ref.absoluteFile(appContext)
                if (dir.isDirectory) {
                    when {
                        files && dirs -> dir.listFiles()?.toList()
                        files -> dir.listFiles()?.filter { it.isFile }
                        dirs -> dir.listFiles()?.filter { it.isDirectory }
                        else -> null
                    }!!.map { ref.subRef(it.name) }
                } else {
                    listOf()
                }
            }
            else -> error("Unsupported FlorisRef source!")
        }
    }

    fun <T> loadJsonAsset(
        ref: FlorisRef,
        serializer: KSerializer<T>,
        jsonConfig: Json = DefaultJsonConfig,
    ) = runCatching<T> {
        val jsonStr = loadTextAsset(ref).getOrThrow()
        jsonConfig.decodeFromString(serializer, jsonStr)
    }

    inline fun <reified T> loadJsonAsset(jsonStr: String, jsonConfig: Json = DefaultJsonConfig): Result<T> {
        return runCatching { jsonConfig.decodeFromString(jsonStr) }
    }

    fun <T> loadJsonAsset(
        jsonStr: String,
        serializer: KSerializer<T>,
        jsonConfig: Json = DefaultJsonConfig,
    ) = runCatching<T> {
        jsonConfig.decodeFromString(serializer, jsonStr)
    }

    fun loadTextAsset(ref: FlorisRef): Result<String> {
        return when {
            ref.isAssets -> runCatching {
                appContext.assets.reader(ref.relativePath).use { it.readText() }
            }
            ref.isCache || ref.isInternal -> {
                val file = File(ref.absolutePath(appContext))
                val contents = readTextFile(file).getOrElse { return resultErr(it) }
                if (contents.isBlank()) {
                    resultErrStr("File is blank!")
                } else {
                    resultOk(contents)
                }
            }
            else -> resultErrStr("Unsupported asset ref!")
        }
    }

    /**
     * Reads a given [file] and returns its content.
     *
     * @param file The file object.
     * @return The contents of the file or an empty string, if the file does not exist.
     */
    private fun readTextFile(file: File) = runCatching {
        file.readText(Charsets.UTF_8)
    }
}
