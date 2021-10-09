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

package dev.patrickgold.florisboard.res.ext

import android.content.Context
import androidx.lifecycle.MutableLiveData
import dev.patrickgold.florisboard.appContext
import dev.patrickgold.florisboard.assetManager
import dev.patrickgold.florisboard.debug.LogTopic
import dev.patrickgold.florisboard.debug.flogError
import dev.patrickgold.florisboard.ime.spelling.SpellingExtension
import dev.patrickgold.florisboard.ime.theme.ThemeExtension
import dev.patrickgold.florisboard.res.FlorisRef
import dev.patrickgold.florisboard.res.ZipUtils
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import java.io.File

class ExtensionManager(context: Context) {
    companion object {
        private const val IME_SPELLING_PATH = "ime/spelling"
        private const val IME_THEME_PATH = "ime/theme"
    }

    private val appContext by context.appContext()
    private val assetManager by context.assetManager()

    private val jsonConfig = assetManager.jsonConfig {
        serializersModule = SerializersModule {
            polymorphic(Extension::class) {
                subclass(SpellingExtension::class, SpellingExtension.serializer())
                subclass(ThemeExtension::class, ThemeExtension.serializer())
            }
        }
    }

    val index = Index()

    init {
        indexExtensions()
    }

    private fun indexExtensions() {
        index.spellingDicts.postValue(listExtensions(IME_SPELLING_PATH))
        index.themes.postValue(listExtensions(IME_THEME_PATH))
    }

    private inline fun <reified T : Extension> listExtensions(path: String): List<T> {
        val retList = mutableListOf<T>()
        assetManager.listDirs(FlorisRef.assets(path)).onSuccess { extRefs ->
            for (extRef in extRefs) {
                val fileRef = extRef.subRef(ExtensionMetaDefaults.NAME)
                val assetResult = assetManager.loadJsonAsset<T>(fileRef, jsonConfig)
                assetResult.onSuccess { asset ->
                    retList.add(asset)
                }.onFailure { error ->
                    flogError(LogTopic.ASSET_MANAGER) { error.toString() }
                }
            }
        }
        assetManager.listFiles(FlorisRef.internal(path)).onSuccess { extRefs ->
            for (extRef in extRefs) {
                val fileRef = File(extRef.absolutePath(appContext))
                if (!fileRef.name.endsWith(ExtensionMetaDefaults.FILE_EXTENSION)) {
                    continue
                }
                val fileContents = ZipUtils.readFileFromArchive(appContext, extRef, ExtensionMetaDefaults.NAME)
                fileContents.onSuccess { metaStr ->
                    val assetResult = assetManager.loadJsonAsset<T>(metaStr, jsonConfig)
                    assetResult.onSuccess { asset ->
                        retList.add(asset)
                    }.onFailure { error ->
                        flogError(LogTopic.ASSET_MANAGER) { error.toString() }
                    }
                }
            }
        }
        return retList
    }

    fun getExtensionById(id: String): Extension? {
        index.spellingDicts.value?.find { it.meta.id == id }?.let { return it }
        index.themes.value?.find { it.meta.id == id }?.let { return it }
        return null
    }

    inner class Index {
        val spellingDicts: MutableLiveData<List<SpellingExtension>> = MutableLiveData()
        val themes: MutableLiveData<List<ThemeExtension>> = MutableLiveData()
    }
}
