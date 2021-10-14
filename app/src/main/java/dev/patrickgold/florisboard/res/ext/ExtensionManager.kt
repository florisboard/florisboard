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
import android.os.FileObserver
import androidx.lifecycle.LiveData
import dev.patrickgold.florisboard.appContext
import dev.patrickgold.florisboard.assetManager
import dev.patrickgold.florisboard.debug.LogTopic
import dev.patrickgold.florisboard.debug.flogDebug
import dev.patrickgold.florisboard.debug.flogError
import dev.patrickgold.florisboard.ime.spelling.SpellingExtension
import dev.patrickgold.florisboard.ime.theme.ThemeExtension
import dev.patrickgold.florisboard.res.FlorisRef
import dev.patrickgold.florisboard.res.ZipUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
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
    private val ioScope = CoroutineScope(Dispatchers.IO)

    val spellingDicts = ExtensionIndex(SpellingExtension.serializer(), IME_SPELLING_PATH)
    val themes = ExtensionIndex(ThemeExtension.serializer(), IME_THEME_PATH)

    @OptIn(ExperimentalSerializationApi::class)
    private val jsonConfig = assetManager.jsonConfig {
        prettyPrint = true
        prettyPrintIndent = "  "
        encodeDefaults = false
        serializersModule = SerializersModule {
            polymorphic(Extension::class) {
                subclass(SpellingExtension::class, SpellingExtension.serializer())
                subclass(ThemeExtension::class, ThemeExtension.serializer())
            }
        }
    }

    fun import(ext: Extension) = runCatching {
        val extFileName = ExtensionDefaults.createFlexName(ext.meta.id)
        val relGroupPath = when (ext) {
            is SpellingExtension -> "ime/spelling"
            is ThemeExtension -> "ime/theme"
            else -> error("Unknown extension type")
        }
        ext.sourceRef = FlorisRef.internal(relGroupPath).subRef(extFileName)
        assetManager.writeJsonAsset(File(ext.workingDir!!, ExtensionDefaults.MANIFEST_FILE_NAME), ext, jsonConfig).getOrThrow()
        writeExtension(ext).getOrThrow()
        ext.unload(appContext)
        ext.workingDir = null
    }

    private fun writeExtension(ext: Extension) = runCatching {
        val workingDir = ext.workingDir ?: error("No working dir specified")
        val sourceRef = ext.sourceRef ?: error("No source ref specified")
        ZipUtils.zip(appContext, workingDir, sourceRef).getOrThrow()
    }

    fun getExtensionById(id: String): Extension? {
        spellingDicts.value?.find { it.meta.id == id }?.let { return it }
        themes.value?.find { it.meta.id == id }?.let { return it }
        return null
    }

    fun canDelete(ext: Extension): Boolean {
        return ext.sourceRef?.isInternal == true
    }

    fun delete(ext: Extension) = runCatching {
        check(canDelete(ext)) { "Cannot delete extension!" }
        ext.unload(appContext)
        assetManager.delete(ext.sourceRef!!)
    }

    inner class ExtensionIndex<T : Extension>(
        private val serializer: KSerializer<T>,
        modulePath: String,
    ) : LiveData<List<T>>() {

        private val assetsModuleRef = FlorisRef.assets(modulePath)
        private val internalModuleRef = FlorisRef.internal(modulePath)
        private val internalModuleDir = internalModuleRef.absoluteFile(appContext)

        private var staticExtensions = listOf<T>()
        private val fileObserver = object : FileObserver(
            internalModuleDir, CLOSE_WRITE or DELETE or MOVED_FROM or MOVED_TO,
        ) {
            override fun onEvent(event: Int, path: String?) {
                flogDebug(LogTopic.EXT_INDEXING) { "FileObserver.onEvent { event=$event path=$path }" }
                if (path == null) return
                ioScope.launch {
                    refresh()
                }
            }
        }

        init {
            ioScope.launch {
                internalModuleDir.mkdirs()
                staticExtensions = indexAssetsModule()
                refresh()
                fileObserver.startWatching()
            }
        }

        private fun refresh() {
            val dynamicExtensions = staticExtensions + indexInternalModule()
            postValue(dynamicExtensions)
        }

        private fun indexAssetsModule(): List<T> {
            val list = mutableListOf<T>()
            assetManager.listDirs(assetsModuleRef).fold(
                onSuccess = { extRefs ->
                    for (extRef in extRefs) {
                        val fileRef = extRef.subRef(ExtensionDefaults.MANIFEST_FILE_NAME)
                        assetManager.loadJsonAsset(fileRef, serializer, jsonConfig).fold(
                            onSuccess = { ext ->
                                ext.sourceRef = extRef
                                list.add(ext)
                            },
                            onFailure = { error ->
                                flogError { error.toString() }
                            },
                        )
                    }
                },
                onFailure = { error ->
                    flogError { error.toString() }
                },
            )
            return list.toList()
        }

        private fun indexInternalModule(): List<T> {
            val list = mutableListOf<T>()
            assetManager.listFiles(internalModuleRef).fold(
                onSuccess = { extRefs ->
                    for (extRef in extRefs) {
                        val fileRef = extRef.absoluteFile(appContext)
                        if (!fileRef.name.endsWith(ExtensionDefaults.FILE_EXTENSION)) {
                            continue
                        }
                        ZipUtils.readFileFromArchive(appContext, extRef, ExtensionDefaults.MANIFEST_FILE_NAME).fold(
                            onSuccess = { metaStr ->
                                assetManager.loadJsonAsset(metaStr, serializer, jsonConfig).fold(
                                    onSuccess = { ext ->
                                        ext.sourceRef = extRef
                                        list.add(ext)
                                    },
                                    onFailure = { error ->
                                        flogError { error.toString() }
                                    },
                                )
                            },
                            onFailure = { error ->
                                flogError { error.toString() }
                            },
                        )
                    }
                },
                onFailure = { error ->
                    flogError { error.toString() }
                },
            )
            return list.toList()
        }
    }
}
