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
import org.florisboard.lib.android.reader
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

fun FlorisRef.delete(context: Context) {
    when {
        isCache || isInternal -> {
            absoluteFile(context).delete()
        }
        else -> error("Can not delete directory/file in location '${scheme}'.")
    }
}

fun FlorisRef.hasAsset(context: Context): Boolean {
    return when {
        isAssets -> {
            try {
                val file = File(relativePath)
                val list = context.assets.list(file.parent?.toString() ?: "")
                list?.contains(file.name) == true
            } catch (e: Exception) {
                false
            }
        }
        isCache || isInternal -> {
            val file = File(absolutePath(context))
            file.exists() && file.isFile
        }
        else -> false
    }
}

fun FlorisRef.list(context: Context) = list(context, files = true, dirs = true)

fun FlorisRef.listFiles(context: Context) = list(context, files = true, dirs = false)

fun FlorisRef.listDirs(context: Context) = list(context, files = false, dirs = true)

private fun FlorisRef.list(appContext: Context, files: Boolean, dirs: Boolean) = runCatching<List<FlorisRef>> {
    when {
        !files && !dirs -> listOf()
        isAssets -> {
            appContext.assets.list(relativePath)?.mapNotNull { fileName ->
                val subList = appContext.assets.list("${relativePath}/$fileName") ?: return@mapNotNull null
                when {
                    files && dirs || files && subList.isEmpty() || dirs && subList.isNotEmpty() -> {
                        subRef(fileName)
                    }
                    else -> null
                }
            } ?: listOf()
        }
        isCache || isInternal -> {
            val dir = absoluteFile(appContext)
            if (dir.isDirectory) {
                when {
                    files && dirs -> dir.listFiles()?.toList()
                    files -> dir.listFiles()?.filter { it.isFile }
                    dirs -> dir.listFiles()?.filter { it.isDirectory }
                    else -> null
                }!!.map { subRef(it.name) }
            } else {
                listOf()
            }
        }
        else -> error("Unsupported FlorisRef source!")
    }
}

fun <T> FlorisRef.loadJsonAsset(
    context: Context,
    serializer: KSerializer<T>,
    jsonConfig: Json = DefaultJsonConfig,
) = runCatching<T> {
    val jsonStr = loadTextAsset(context).getOrThrow()
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

fun FlorisRef.loadTextAsset(context: Context): Result<String> {
    return when {
        isAssets -> runCatching {
            context.assets.reader(relativePath).use { it.readText() }
        }
        isCache || isInternal -> {
            val file = File(absolutePath(context))
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


