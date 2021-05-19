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

package dev.patrickgold.florisboard.ime.extension

import android.content.Context
import android.net.Uri
import dev.patrickgold.florisboard.ime.keyboard.CaseSelector
import dev.patrickgold.florisboard.ime.keyboard.KeyData
import dev.patrickgold.florisboard.ime.keyboard.VariationSelector
import dev.patrickgold.florisboard.ime.media.emoji.EmojiKeyData
import dev.patrickgold.florisboard.ime.text.composing.*
import dev.patrickgold.florisboard.ime.text.keyboard.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import timber.log.Timber
import java.io.File

class AssetManager private constructor(val applicationContext: Context) {
    private val json = Json {
        classDiscriminator = "$"
        ignoreUnknownKeys = true
        isLenient = true
        serializersModule = SerializersModule {
            polymorphic(KeyData::class) {
                subclass(BasicTextKeyData::class, BasicTextKeyData.serializer())
                subclass(AutoTextKeyData::class, AutoTextKeyData.serializer())
                subclass(MultiTextKeyData::class, MultiTextKeyData.serializer())
                subclass(EmojiKeyData::class, EmojiKeyData.serializer())
                subclass(CaseSelector::class, CaseSelector.serializer())
                subclass(VariationSelector::class, VariationSelector.serializer())
                default { BasicTextKeyData.serializer() }
            }
            polymorphic(TextKeyData::class) {
                subclass(BasicTextKeyData::class, BasicTextKeyData.serializer())
                subclass(AutoTextKeyData::class, AutoTextKeyData.serializer())
                subclass(MultiTextKeyData::class, MultiTextKeyData.serializer())
                default { BasicTextKeyData.serializer() }
            }
            polymorphic(Composer::class) {
                subclass(Appender::class, Appender.serializer())
                subclass(HangulUnicode::class, HangulUnicode.serializer())
                subclass(WithRules::class, WithRules.serializer())
                default { Appender.serializer() }
            }
        }
    }

    companion object {
        private var defaultInstance: AssetManager? = null

        fun init(applicationContext: Context): AssetManager {
            val instance = AssetManager(applicationContext)
            defaultInstance = instance
            return instance
        }

        fun default(): AssetManager {
            val instance = defaultInstance
            if (instance != null) {
                return instance
            } else {
                throw UninitializedPropertyAccessException(
                    "${this::class.simpleName} has not been initialized previously. Make sure to call init(applicationContext) before using default()."
                )
            }
        }

        fun defaultOrNull(): AssetManager? = defaultInstance
    }

    fun jsonBuilder(): Json = json

    fun deleteAsset(ref: AssetRef): Result<Unit> {
        return when (ref.source) {
            AssetSource.Internal -> {
                val file = File(applicationContext.filesDir.absolutePath + "/" + ref.path)
                if (file.isFile) {
                    val success = file.delete()
                    if (success) {
                        Result.success(Unit)
                    } else {
                        Result.failure(Exception("Could not delete file."))
                    }
                } else {
                    Result.failure(Exception("Provided reference is not a file."))
                }
            }
            else -> Result.failure(Exception("Can not delete an asset in source '${ref.source}'"))
        }
    }

    fun hasAsset(ref: AssetRef): Boolean {
        return when (ref.source) {
            AssetSource.Assets -> {
                try {
                    val file = File(ref.path)
                    val list = applicationContext.assets.list(file.parent?.toString() ?: "")
                    list?.contains(file.name) == true
                } catch (e: Exception) {
                    false
                }
            }
            AssetSource.Internal -> {
                val file = File(applicationContext.filesDir.absolutePath + "/" + ref.path)
                file.exists() && file.isFile
            }
            else -> false
        }
    }

    inline fun <reified T> listAssets(ref: AssetRef): Result<Map<AssetRef, T>> {
        val retMap = mutableMapOf<AssetRef, T>()
        return when (ref.source) {
            AssetSource.Assets -> runCatching {
                val list = applicationContext.assets.list(ref.path)
                if (list != null) {
                    for (file in list) {
                        val fileRef = ref.copy(path = ref.path + "/" + file)
                        val assetResult = loadJsonAsset<T>(fileRef)
                        assetResult.onSuccess { asset ->
                            retMap[fileRef.copy()] = asset
                        }.onFailure { error ->
                            Timber.e(error.toString())
                        }
                    }
                }
                retMap.toMap()
            }
            AssetSource.Internal -> {
                val dir = File(applicationContext.filesDir.absolutePath + "/" + ref.path)
                if (dir.isDirectory) {
                    dir.listFiles()?.let {
                        it.forEach { file ->
                            if (file.isFile) {
                                val fileRef = ref.copy(path = ref.path + "/" + file.name)
                                val assetResult = loadJsonAsset<T>(fileRef)
                                assetResult.onSuccess { asset ->
                                    retMap[fileRef.copy()] = asset
                                }.onFailure { error ->
                                    Timber.e(error.toString())
                                }
                            }
                        }
                    }
                }
                Result.success(retMap.toMap())
            }
            else -> Result.success(retMap.toMap())
        }
    }

    inline fun <reified T> loadJsonAsset(ref: AssetRef): Result<T> {
        return loadTextAsset(ref).fold(
            onSuccess = { runCatching { jsonBuilder().decodeFromString(it) } },
            onFailure = { Result.failure(it) }
        )
    }

    inline fun <reified T> loadJsonAsset(uri: Uri, maxSize: Int): Result<T> {
        return loadTextAsset(uri, maxSize).fold(
            onSuccess = { runCatching { jsonBuilder().decodeFromString(it) } },
            onFailure = { Result.failure(it) }
        )
    }

    fun loadTextAsset(ref: AssetRef): Result<String> {
        return when (ref.source) {
            is AssetSource.Assets -> runCatching {
                applicationContext.assets.open(ref.path).bufferedReader().use { it.readText() }
            }
            is AssetSource.Internal -> {
                val file = File(applicationContext.filesDir.absolutePath + "/" + ref.path)
                val contents = readTextFile(file).getOrElse { return Result.failure(it) }
                if (contents.isBlank()) {
                    Result.failure(Exception("File is blank!"))
                } else {
                    Result.success(contents)
                }
            }
            else -> Result.failure(Exception("Unsupported asset ref!"))
        }
    }

    /**
     * Reads a given [file] and returns its content.
     *
     * @param file The file object.
     * @return The contents of the file or an empty string, if the file does not exist.
     */
    private fun readTextFile(file: File) = runCatching {
        val retText = StringBuilder()
        if (file.exists()) {
            val newLine = System.lineSeparator()
            file.forEachLine {
                retText.append(it)
                retText.append(newLine)
            }
        }
        retText.toString()
    }

    fun loadTextAsset(uri: Uri, maxSize: Int): Result<String> {
        return ExternalContentUtils.readTextFromUri(applicationContext, uri, maxSize)
    }

    inline fun <reified T> writeJsonAsset(ref: AssetRef, asset: T): Result<Unit> {
        return runCatching { jsonBuilder().encodeToString(asset) }.fold(
            onSuccess = { writeTextAsset(ref, it) },
            onFailure = { Result.failure(it) }
        )
    }

    inline fun <reified T> writeJsonAsset(uri: Uri, asset: T): Result<Unit> {
        return runCatching { jsonBuilder().encodeToString(asset) }.fold(
            onSuccess = { writeTextAsset(uri, it) },
            onFailure = { Result.failure(it) }
        )
    }

    fun writeTextAsset(ref: AssetRef, text: String): Result<Unit> {
        return when (ref.source) {
            AssetSource.Internal -> {
                val file = File(applicationContext.filesDir.absolutePath + "/" + ref.path)
                writeTextFile(file, text)
            }
            else -> Result.failure(Exception("Can not write an asset in source '${ref.source}'"))
        }
    }

    /**
     * Writes given [text] to given [file]. If the file already exists, its current content
     * will be overwritten.
     *
     * @param file The file object.
     * @param text The text to write to the file.
     * @return The contents of the file or an empty string, if the file does not exist.
     */
    private fun writeTextFile(file: File, text: String) = runCatching {
        file.parent?.let {
            val dir = File(it)
            if (!dir.exists()) {
                dir.mkdirs()
            }
        }
        file.writeText(text)
    }

    fun writeTextAsset(uri: Uri, text: String): Result<Unit> {
        return ExternalContentUtils.writeTextToUri(applicationContext, uri, text)
    }
}
