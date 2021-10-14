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

package dev.patrickgold.florisboard.res

import android.content.Context
import android.net.Uri
import dev.patrickgold.florisboard.appContext
import dev.patrickgold.florisboard.common.resultErr
import dev.patrickgold.florisboard.common.resultErrStr
import dev.patrickgold.florisboard.common.resultOk
import dev.patrickgold.florisboard.debug.LogTopic
import dev.patrickgold.florisboard.debug.flogError
import dev.patrickgold.florisboard.ime.keyboard.AbstractKeyData
import dev.patrickgold.florisboard.ime.keyboard.CaseSelector
import dev.patrickgold.florisboard.ime.keyboard.KanaSelector
import dev.patrickgold.florisboard.ime.keyboard.CharWidthSelector
import dev.patrickgold.florisboard.ime.keyboard.KeyData
import dev.patrickgold.florisboard.ime.keyboard.VariationSelector
import dev.patrickgold.florisboard.ime.media.emoji.EmojiKeyData
import dev.patrickgold.florisboard.ime.spelling.SpellingConfig
import dev.patrickgold.florisboard.ime.text.composing.Appender
import dev.patrickgold.florisboard.ime.text.composing.Composer
import dev.patrickgold.florisboard.ime.text.composing.HangulUnicode
import dev.patrickgold.florisboard.ime.text.composing.KanaUnicode
import dev.patrickgold.florisboard.ime.text.composing.WithRules
import dev.patrickgold.florisboard.ime.text.keyboard.AutoTextKeyData
import dev.patrickgold.florisboard.ime.text.keyboard.MultiTextKeyData
import dev.patrickgold.florisboard.ime.text.keyboard.TextKeyData
import kotlinx.serialization.KSerializer
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonBuilder
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import java.io.File

class AssetManager(context: Context) {
    val appContext by context.appContext()

    val jsonConfig = Json {
        classDiscriminator = "$"
        encodeDefaults = true
        ignoreUnknownKeys = true
        isLenient = true
        serializersModule = SerializersModule {
            polymorphic(AbstractKeyData::class) {
                subclass(TextKeyData::class, TextKeyData.serializer())
                subclass(AutoTextKeyData::class, AutoTextKeyData.serializer())
                subclass(MultiTextKeyData::class, MultiTextKeyData.serializer())
                subclass(EmojiKeyData::class, EmojiKeyData.serializer())
                subclass(CaseSelector::class, CaseSelector.serializer())
                subclass(VariationSelector::class, VariationSelector.serializer())
                subclass(CharWidthSelector::class, CharWidthSelector.serializer())
                subclass(KanaSelector::class, KanaSelector.serializer())
                default { TextKeyData.serializer() }
            }
            polymorphic(KeyData::class) {
                subclass(TextKeyData::class, TextKeyData.serializer())
                subclass(AutoTextKeyData::class, AutoTextKeyData.serializer())
                subclass(MultiTextKeyData::class, MultiTextKeyData.serializer())
                subclass(EmojiKeyData::class, EmojiKeyData.serializer())
                default { TextKeyData.serializer() }
            }
            polymorphic(Composer::class) {
                subclass(Appender::class, Appender.serializer())
                subclass(HangulUnicode::class, HangulUnicode.serializer())
                subclass(KanaUnicode::class, KanaUnicode.serializer())
                subclass(WithRules::class, WithRules.serializer())
                default { Appender.serializer() }
            }
            polymorphic(SpellingConfig.ImportFormat::class) {
                subclass(SpellingConfig.ImportFormat.Archive::class, SpellingConfig.ImportFormat.Archive.serializer())
                subclass(SpellingConfig.ImportFormat.Raw::class, SpellingConfig.ImportFormat.Raw.serializer())
            }
        }
    }

    fun jsonConfig(action: JsonBuilder.() -> Unit) = Json(jsonConfig) {
        this.action()
    }

    @Deprecated(message = "deleteAsset is deprecated in favor of delete")
    fun deleteAsset(ref: FlorisRef): Result<Unit> {
        return when {
            ref.isCache || ref.isInternal -> {
                val file = File(ref.absolutePath(appContext))
                if (file.isFile) {
                    try {
                        val success = file.delete()
                        if (success) {
                            resultOk()
                        } else {
                            resultErrStr("Could not delete file.")
                        }
                    } catch (e: Exception) {
                        resultErr(e)
                    }
                } else {
                    resultErrStr("Provided reference is not a file.")
                }
            }
            else -> resultErrStr("Can not delete an asset in source '${ref.scheme}'.")
        }
    }

    fun delete(ref: FlorisRef) = runCatching {
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

    @Deprecated(message = "listAssets is deprecated in favor of list/listFiles/listDirs")
    inline fun <reified T> listAssets(ref: FlorisRef): Result<Map<FlorisRef, T>> {
        val retMap = mutableMapOf<FlorisRef, T>()
        return when {
            ref.isAssets -> runCatching {
                val list = appContext.assets.list(ref.relativePath)
                if (list != null) {
                    for (file in list) {
                        val fileRef = ref.subRef(file)
                        val assetResult = loadJsonAsset<T>(fileRef)
                        assetResult.onSuccess { asset ->
                            retMap[fileRef] = asset
                        }.onFailure { error ->
                            flogError(LogTopic.FILE_IO) { error.toString() }
                        }
                    }
                }
                retMap.toMap()
            }
            ref.isCache || ref.isInternal -> {
                val dir = File(ref.absolutePath(appContext))
                if (dir.isDirectory) {
                    dir.listFiles()?.let {
                        it.forEach { file ->
                            if (file.isFile) {
                                val fileRef = ref.subRef(file.name)
                                val assetResult = loadJsonAsset<T>(fileRef)
                                assetResult.onSuccess { asset ->
                                    retMap[fileRef] = asset
                                }.onFailure { error ->
                                    flogError(LogTopic.FILE_IO) { error.toString() }
                                }
                            }
                        }
                    }
                }
                resultOk(retMap.toMap())
            }
            else -> resultErrStr("Unsupported FlorisRef source!")
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

    inline fun <reified T> loadJsonAsset(ref: FlorisRef, jsonConfig: Json = this.jsonConfig): Result<T> {
        return loadTextAsset(ref).fold(
            onSuccess = { runCatching { jsonConfig.decodeFromString(it) } },
            onFailure = { resultErr(it) }
        )
    }

    fun <T> loadJsonAsset(
        ref: FlorisRef,
        serializer: KSerializer<T>,
        jsonConfig: Json = this.jsonConfig,
    ) = runCatching<T> {
        val jsonStr = loadTextAsset(ref).getOrThrow()
        jsonConfig.decodeFromString(serializer, jsonStr)
    }

    inline fun <reified T> loadJsonAsset(file: File, jsonConfig: Json = this.jsonConfig): Result<T> {
        return readTextFile(file).fold(
            onSuccess = { runCatching { jsonConfig.decodeFromString(it) } },
            onFailure = { resultErr(it) }
        )
    }

    inline fun <reified T> loadJsonAsset(uri: Uri, maxSize: Int, jsonConfig: Json = this.jsonConfig): Result<T> {
        return loadTextAsset(uri, maxSize).fold(
            onSuccess = { runCatching { jsonConfig.decodeFromString(it) } },
            onFailure = { resultErr(it) }
        )
    }

    inline fun <reified T> loadJsonAsset(jsonStr: String, jsonConfig: Json = this.jsonConfig): Result<T> {
        return runCatching { jsonConfig.decodeFromString(jsonStr) }
    }

    fun <T> loadJsonAsset(
        jsonStr: String,
        serializer: KSerializer<T>,
        jsonConfig: Json = this.jsonConfig,
    ) = runCatching<T> {
        jsonConfig.decodeFromString(serializer, jsonStr)
    }

    fun loadTextAsset(ref: FlorisRef): Result<String> {
        return when {
            ref.isAssets -> runCatching {
                appContext.assets.open(ref.relativePath).bufferedReader().use { it.readText() }
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

    fun loadTextAsset(uri: Uri, maxSize: Int): Result<String> {
        return ExternalContentUtils.readAllTextFromUri(appContext, uri, maxSize)
    }

    inline fun <reified T> writeJsonAsset(ref: FlorisRef, asset: T, jsonConfig: Json = this.jsonConfig): Result<Unit> {
        return runCatching { jsonConfig.encodeToString(asset) }.fold(
            onSuccess = { writeTextAsset(ref, it) },
            onFailure = { resultErr(it) }
        )
    }

    inline fun <reified T> writeJsonAsset(file: File, asset: T, jsonConfig: Json = this.jsonConfig): Result<Unit> {
        return runCatching { jsonConfig.encodeToString(asset) }.fold(
            onSuccess = { writeTextFile(file, it) },
            onFailure = { resultErr(it) }
        )
    }

    inline fun <reified T> writeJsonAsset(uri: Uri, asset: T, jsonConfig: Json = this.jsonConfig): Result<Unit> {
        return runCatching { jsonConfig.encodeToString(asset) }.fold(
            onSuccess = { writeTextAsset(uri, it) },
            onFailure = { resultErr(it) }
        )
    }

    fun writeTextAsset(ref: FlorisRef, text: String): Result<Unit> {
        return when {
            ref.isCache || ref.isInternal -> {
                val file = File(ref.absolutePath(appContext))
                writeTextFile(file, text)
            }
            else -> resultErrStr("Can not write an asset in source '${ref.scheme}'")
        }
    }

    fun writeTextAsset(uri: Uri, text: String): Result<Unit> {
        return ExternalContentUtils.writeAllTextToUri(appContext, uri, text)
    }

    /**
     * Reads a given [file] and returns its content.
     *
     * @param file The file object.
     * @return The contents of the file or an empty string, if the file does not exist.
     */
    fun readTextFile(file: File) = runCatching {
        file.readText(Charsets.UTF_8)
    }

    /**
     * Writes given [text] to given [file]. If the file already exists, its current content
     * will be overwritten.
     *
     * @param file The file object.
     * @param text The text to write to the file.
     * @return The contents of the file or an empty string, if the file does not exist.
     */
    fun writeTextFile(file: File, text: String) = runCatching {
        file.parentFile?.mkdirs()
        file.writeText(text)
    }

    /*inline fun <reified C : ExtensionConfig> listExtensions(ref: FlorisRef): Result<Map<FlorisRef, C>> {
        val retMap = mutableMapOf<FlorisRef, C>()
        return when {
            ref.isInternal -> {
                val dir = File(ref.absolutePath(applicationContext))
                if (dir.isDirectory) {
                    val tempConfigFile = File.createTempFile("__config_ext_index", null)
                    for (file in dir.listFiles() ?: arrayOf()) {
                        if (file.isFile && file.extension == ExtensionMetaDefaults.FILE_EXTENSION) {
                            val flexFile = ZipFile(file)
                            val configEntry = flexFile.getEntry(ExtensionMetaDefaults.NAME)
                            if (configEntry != null) {
                                tempConfigFile.delete()
                                flexFile.copy(configEntry, tempConfigFile)
                                loadJsonAsset<C>(tempConfigFile).fold(
                                    onSuccess = { config -> retMap.put(ref.subRef(file.name), config) },
                                    onFailure = { error -> flogError(LogTopic.ASSET_MANAGER) { error.toString() } }
                                )
                            }
                        }
                    }
                    tempConfigFile.delete()
                    resultOk { retMap.toMap() }
                } else {
                    resultErrStr { "Given path does not exist or is a file!" }
                }
            }
            else -> resultErrStr { "Unsupported asset source" }
        }
    }*/

    /*inline fun <X: ExtensionConfig<C>, reified C : ExtensionConfig> writeExtension(extension: X): Result<Unit> {
        if (extension.flexFile == null) return Result.failure(Exception("Flex file handle is null!"))
        extension.flexFile.parentFile?.mkdirs()
        extension.flexFile.deleteRecursively()
        return try {
            // Write extension file to working dir
            writeJsonAsset(File(extension.workingDir, ExtensionConfig.DEFAULT_NAME), extension.config)
            val fileOut = FileOutputStream(extension.flexFile.absolutePath)
            val zipOut = ZipOutputStream(fileOut)
            for (workingFile in extension.workingDir.listFiles() ?: arrayOf()) {
                zipOut.putNextEntry(ZipEntry(workingFile.name))
                workingFile.inputStream().use { it.copyTo(zipOut) }
                zipOut.closeEntry()
            }
            zipOut.close()
            return Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }*/
}
