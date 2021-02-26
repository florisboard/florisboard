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
import com.github.michaelbull.result.*
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dev.patrickgold.florisboard.ime.text.key.KeyTypeAdapter
import dev.patrickgold.florisboard.ime.text.key.KeyVariationAdapter
import dev.patrickgold.florisboard.ime.text.layout.LayoutTypeAdapter
import timber.log.Timber
import java.io.File

class AssetManager private constructor(private val applicationContext: Context) {
    private val moshi: Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        /*.add(PolymorphicJsonAdapterFactory.of(Asset::class.java, "\$type")
            .withSubtype(PopupExtension::class.java, PopupExtension::class.qualifiedName)
            .withSubtype(Theme::class.java, Theme::class.qualifiedName)
        )*/
        .add(LayoutTypeAdapter())
        .add(KeyTypeAdapter())
        .add(KeyVariationAdapter())
        .build()

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
    }

    fun deleteAsset(ref: AssetRef): Result<Nothing?, Throwable> {
        return when (ref.source) {
            AssetSource.Internal -> {
                val file = File(applicationContext.filesDir.absolutePath + "/" + ref.path)
                if (file.isFile) {
                    val success = file.delete()
                    if (success) {
                        Ok(null)
                    } else {
                        Err(Exception("Could not delete file."))
                    }
                } else {
                    Err(Exception("Provided reference is not a file."))
                }
            }
            else -> Err(Exception("Can not delete an asset in source '${ref.source}'"))
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

    fun <T: Asset> listAssets(ref: AssetRef, assetClass: Class<T>): Result<Map<AssetRef, T>, Throwable> {
        val retMap = mutableMapOf<AssetRef, T>()
        return when (ref.source) {
            AssetSource.Assets -> {
                try {
                    val list = applicationContext.assets.list(ref.path)
                    if (list != null) {
                        for (file in list) {
                            val fileRef = ref.copy(path = ref.path + "/" + file)
                            val assetResult = loadAsset(fileRef, assetClass)
                            assetResult.onSuccess { asset ->
                                retMap[fileRef.copy()] = asset
                            }.onFailure { error ->
                                Timber.e(error.toString())
                            }
                        }
                    }
                    Ok(retMap.toMap())
                } catch (e: Exception) {
                    Err(e)
                }
            }
            AssetSource.Internal -> {
                val dir = File(applicationContext.filesDir.absolutePath + "/" + ref.path)
                if (dir.isDirectory) {
                    dir.listFiles()?.let {
                        it.forEach { file ->
                            if (file.isFile) {
                                val fileRef = ref.copy(path = ref.path + "/" + file.name)
                                val assetResult = loadAsset(fileRef, assetClass)
                                assetResult.onSuccess { asset ->
                                    retMap[fileRef.copy()] = asset
                                }.onFailure { error ->
                                    Timber.e(error.toString())
                                }
                            }
                        }
                    }
                }
                Ok(retMap.toMap())
            }
            else -> Ok(retMap.toMap())
        }
    }

    fun <T: Asset> loadAsset(ref: AssetRef, assetClass: Class<T>): Result<T, Throwable> {
        val rawJsonData = when (ref.source) {
            is AssetSource.Assets -> {
                try {
                    applicationContext.assets.open(ref.path).bufferedReader().use { it.readText() }
                } catch (e: Exception) {
                    return Err(e)
                }
            }
            is AssetSource.Internal -> {
                val file = File(applicationContext.filesDir.absolutePath + "/" + ref.path)
                val contents = readFile(file)
                if (contents.isBlank()) {
                    "{}"
                } else {
                    contents
                }
            }
            else -> "{}"
        }
        return try {
            val adapter = moshi.adapter(assetClass)
            val asset = adapter.fromJson(rawJsonData)
            if (asset != null) {
                Ok(asset)
            } else {
                Err(NullPointerException("Asset failed to load!"))
            }
        } catch (e: Exception) {
            Err(e)
        }
    }

    fun <T: Asset> writeAsset(ref: AssetRef, assetClass: Class<T>, asset: T): Result<Boolean, Throwable> {
        return when (ref.source) {
            AssetSource.Internal -> {
                val adapter = moshi.adapter(assetClass)
                val rawJson = adapter.toJson(asset)
                val file = File(applicationContext.filesDir.absolutePath + "/" + ref.path)
                writeToFile(file, rawJson)
                Ok(true)
            }
            else -> Err(Exception("Can not write an asset in source '${ref.source}'"))
        }
    }

    /**
     * Reads a given [file] and returns its content.
     *
     * @param file The file object.
     * @return The contents of the file or an empty string, if the file does not exist.
     */
    private fun readFile(file: File): String {
        val retText = StringBuilder()
        if (file.exists()) {
            val newLine = System.lineSeparator()
            file.forEachLine {
                retText.append(it)
                retText.append(newLine)
            }
        }
        return retText.toString()
    }

    /**
     * Writes given [text] to given [file]. If the file already exists, its current content
     * will be overwritten.
     *
     * @param file The file object.
     * @param text The text to write to the file.
     * @return The contents of the file or an empty string, if the file does not exist.
     */
    private fun writeToFile(file: File, text: String) {
        try {
            file.parent?.let {
                val dir = File(it)
                if (!dir.exists()) {
                    dir.mkdirs()
                }
            }
            file.writeText(text)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
