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

package dev.patrickgold.florisboard.lib.ext

import android.content.Context
import android.net.Uri
import android.os.FileObserver
import androidx.lifecycle.LiveData
import dev.patrickgold.florisboard.appContext
import dev.patrickgold.florisboard.assetManager
import dev.patrickgold.florisboard.ime.keyboard.KeyboardExtension
import dev.patrickgold.florisboard.ime.spelling.SpellingExtension
import dev.patrickgold.florisboard.ime.text.composing.Appender
import dev.patrickgold.florisboard.ime.text.composing.Composer
import dev.patrickgold.florisboard.ime.text.composing.HangulUnicode
import dev.patrickgold.florisboard.ime.text.composing.KanaUnicode
import dev.patrickgold.florisboard.ime.text.composing.WithRules
import dev.patrickgold.florisboard.ime.theme.ThemeExtension
import dev.patrickgold.florisboard.lib.android.AndroidVersion
import dev.patrickgold.florisboard.lib.devtools.LogTopic
import dev.patrickgold.florisboard.lib.devtools.flogDebug
import dev.patrickgold.florisboard.lib.devtools.flogError
import dev.patrickgold.florisboard.lib.io.FlorisRef
import dev.patrickgold.florisboard.lib.io.FsFile
import dev.patrickgold.florisboard.lib.io.ZipUtils
import dev.patrickgold.florisboard.lib.io.writeJson
import dev.patrickgold.florisboard.lib.kotlin.throwOnFailure
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic

@OptIn(ExperimentalSerializationApi::class)
val ExtensionJsonConfig = Json {
    classDiscriminator = "$"
    encodeDefaults = true
    ignoreUnknownKeys = true
    isLenient = true
    prettyPrint = true
    prettyPrintIndent = "  "
    encodeDefaults = false
    serializersModule = SerializersModule {
        polymorphic(Extension::class) {
            subclass(KeyboardExtension::class, KeyboardExtension.serializer())
            subclass(SpellingExtension::class, SpellingExtension.serializer())
            subclass(ThemeExtension::class, ThemeExtension.serializer())
        }
        polymorphic(Composer::class) {
            subclass(Appender::class, Appender.serializer())
            subclass(HangulUnicode::class, HangulUnicode.serializer())
            subclass(KanaUnicode::class, KanaUnicode.serializer())
            subclass(WithRules::class, WithRules.serializer())
            default { Appender.serializer() }
        }
    }
}

class ExtensionManager(context: Context) {
    companion object {
        const val IME_KEYBOARD_PATH = "ime/keyboard"
        const val IME_SPELLING_PATH = "ime/spelling"
        const val IME_THEME_PATH = "ime/theme"
    }

    private val appContext by context.appContext()
    private val assetManager by context.assetManager()
    private val ioScope = CoroutineScope(Dispatchers.IO)

    val keyboardExtensions = ExtensionIndex(KeyboardExtension.serializer(), IME_KEYBOARD_PATH)
    val spellingDicts = ExtensionIndex(SpellingExtension.serializer(), IME_SPELLING_PATH)
    val themes = ExtensionIndex(ThemeExtension.serializer(), IME_THEME_PATH)

    fun import(ext: Extension) {
        val workingDir = requireNotNull(ext.workingDir) { "No working dir specified" }
        val extFileName = ExtensionDefaults.createFlexName(ext.meta.id)
        val relGroupPath = when (ext) {
            is KeyboardExtension -> IME_KEYBOARD_PATH
            is SpellingExtension -> IME_SPELLING_PATH
            is ThemeExtension -> IME_THEME_PATH
            else -> error("Unknown extension type")
        }
        ext.sourceRef = FlorisRef.internal(relGroupPath).subRef(extFileName)
        FsFile(workingDir, ExtensionDefaults.MANIFEST_FILE_NAME).writeJson(ext, ExtensionJsonConfig)
        writeExtension(ext).throwOnFailure()
        ext.unload(appContext)
        ext.workingDir = null
    }

    fun export(ext: Extension, uri: Uri) {
        ext.load(appContext).throwOnFailure()
        val workingDir = requireNotNull(ext.workingDir) { "No working dir specified" }
        ZipUtils.zip(appContext, workingDir, uri).throwOnFailure()
        ext.unload(appContext)
    }

    private fun writeExtension(ext: Extension) = runCatching {
        val workingDir = requireNotNull(ext.workingDir) { "No working dir specified" }
        val sourceRef = requireNotNull(ext.sourceRef) { "No source ref specified" }
        ZipUtils.zip(appContext, workingDir, sourceRef).throwOnFailure()
    }

    fun getExtensionById(id: String): Extension? {
        keyboardExtensions.value?.find { it.meta.id == id }?.let { return it }
        spellingDicts.value?.find { it.meta.id == id }?.let { return it }
        themes.value?.find { it.meta.id == id }?.let { return it }
        return null
    }

    fun canDelete(ext: Extension): Boolean {
        return ext.sourceRef?.isInternal == true
    }

    fun delete(ext: Extension) {
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
        private val fileObserverMask =
            FileObserver.CLOSE_WRITE or FileObserver.DELETE or
            FileObserver.MOVED_FROM or FileObserver.MOVED_TO
        private val fileObserver = if (AndroidVersion.ATLEAST_API29_Q) {
            object : FileObserver(internalModuleDir, fileObserverMask) {
                override fun onEvent(event: Int, path: String?) = onEventCallback(event, path)
            }
        } else {
            @Suppress("DEPRECATION")
            object : FileObserver(internalModuleDir.absolutePath, fileObserverMask) {
                override fun onEvent(event: Int, path: String?) = onEventCallback(event, path)
            }
        }

        private fun onEventCallback(event: Int, path: String?) {
            flogDebug(LogTopic.EXT_INDEXING) { "FileObserver.onEvent { event=$event path=$path }" }
            if (path == null) return
            ioScope.launch {
                refresh()
            }
        }

        init {
            value = emptyList()
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
                        assetManager.loadJsonAsset(fileRef, serializer, ExtensionJsonConfig).fold(
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
                                assetManager.loadJsonAsset(metaStr, serializer, ExtensionJsonConfig).fold(
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
