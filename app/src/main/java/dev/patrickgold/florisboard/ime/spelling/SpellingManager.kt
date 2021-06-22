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

package dev.patrickgold.florisboard.ime.spelling

import android.content.Context
import android.net.Uri
import android.view.textservice.SentenceSuggestionsInfo
import android.view.textservice.SpellCheckerSession
import android.view.textservice.SuggestionsInfo
import android.view.textservice.TextServicesManager
import dev.patrickgold.florisboard.debug.flogInfo
import dev.patrickgold.florisboard.debug.flogWarning
import dev.patrickgold.florisboard.res.AssetManager
import dev.patrickgold.florisboard.res.ExternalContentUtils
import dev.patrickgold.florisboard.res.FlorisRef
import dev.patrickgold.florisboard.res.ext.Extension
import dev.patrickgold.florisboard.util.LocaleUtils
import java.io.File
import java.lang.ref.WeakReference
import java.util.*
import java.util.zip.ZipFile

class SpellingManager private constructor(
    val applicationContext: WeakReference<Context>,
    configRef: FlorisRef
) {
    companion object {
        private var defaultInstance: SpellingManager? = null

        private const val IMPORT_ARCHIVE_MAX_SIZE: Int = 6_192_000
        private const val IMPORT_ARCHIVE_TEMP_NAME: String = "__temp000__ime_spelling_import_archive"
        private const val IMPORT_NEW_DICT_TEMP_NAME: String = "__temp000__ime_spelling_import_new_dict"

        private const val FREE_OFFICE_DICT_INI = "dict.ini"
        private const val FREE_OFFICE_DICT_INI_FILE_NAME_BASE = "FileNameBase="
        private const val FREE_OFFICE_DICT_INI_SUPPORTED_LOCALES = "SupportedLocales="

        const val AFF_EXT: String = "AFF"
        const val DIC_EXT: String = "DIC"
        const val HYPH_EXT: String = "HYPH"

        private val STUB_LISTENER = object : SpellCheckerSession.SpellCheckerSessionListener {
            override fun onGetSuggestions(results: Array<out SuggestionsInfo>?) {
                // Intentionally empty
            }

            override fun onGetSentenceSuggestions(results: Array<out SentenceSuggestionsInfo>?) {
                // Intentionally empty
            }
        }

        fun init(context: Context, configRef: FlorisRef): SpellingManager {
            val applicationContext = WeakReference(context.applicationContext ?: context)
            val instance = SpellingManager(applicationContext, configRef)
            defaultInstance = instance
            return instance
        }

        fun default() = defaultInstance!!

        fun defaultOrNull() = defaultInstance
    }

    private val tsm =
        applicationContext.get()?.getSystemService(Context.TEXT_SERVICES_MANAGER_SERVICE) as? TextServicesManager

    private val assetManager get() = AssetManager.default()
    private val spellingDictCache: MutableMap<FlorisRef, SpellingDict> = mutableMapOf()
    private val indexedSpellingDictMetas: MutableMap<FlorisRef, SpellingDict.Meta> = mutableMapOf()
    val indexedSpellingDicts: Map<FlorisRef, SpellingDict.Meta>
        get() = indexedSpellingDictMetas

    val config = assetManager.loadJsonAsset<SpellingConfig>(configRef).getOrDefault(SpellingConfig.default())
    val importSourceLabels: List<String>
    val importSourceUrls: List<String?>

    init {
        config.importSources.map { it.label }.toMutableList().let {
            it.add(0, "-")
            importSourceLabels = it.toList()
        }
        config.importSources.map { it.url }.toMutableList().let {
            it.add(0, "-")
            importSourceUrls = it.toList()
        }
        indexSpellingDicts()
    }

    fun getCurrentSpellingServiceName(): String? {
        try {
            val session = tsm?.newSpellCheckerSession(
                null, Locale.ENGLISH, STUB_LISTENER, false
            ) ?: return null
            val context = applicationContext.get() ?: return null
            val pm = context.packageManager
            return session.spellChecker.loadLabel(pm).toString()
        } catch (e: Exception) {
            flogWarning { e.toString() }
            return null
        }
    }

    @Synchronized
    fun getSpellingDict(locale: Locale): SpellingDict? {
        val entry = indexedSpellingDictMetas.firstNotNullOfOrNull {
            if (it.value.locale.toString() == locale.toString()) it else null
        } ?: indexedSpellingDictMetas.firstNotNullOfOrNull {
            if (it.value.locale.language == locale.language) it else null
        } ?: return null
        val ref = entry.key
        val meta = entry.value
        val cachedDict = spellingDictCache[ref]
        if (cachedDict != null) {
            return cachedDict
        }
        val newDict = SpellingDict.new(ref, meta)
        spellingDictCache[ref] = newDict
        return newDict
    }

    fun indexSpellingDicts(): Boolean {
        indexedSpellingDictMetas.clear()
        assetManager.listAssets<SpellingDict.Meta>(FlorisRef.internal(config.basePath)).onSuccess { map ->
            for ((ref, meta) in map) {
                indexedSpellingDictMetas[ref] = meta
            }
            return true
        }
        return false
    }

    fun prepareImport(sourceId: String, archiveUri: Uri): Result<Extension<SpellingDict.Meta>> {
        val context = applicationContext.get() ?: return Result.failure(Exception("Context is null"))
        return when (sourceId) {
            "free_office" -> {
                val tempFile = saveTempFile(archiveUri).getOrElse { return Result.failure(it) }
                val zipFile = ZipFile(tempFile)
                val dictIniEntry = zipFile.getEntry(FREE_OFFICE_DICT_INI) ?: return Result.failure(Exception("No dict.ini file found"))
                var fileNameBase: String? = null
                var supportedLocale: Locale? = null
                zipFile.getInputStream(dictIniEntry).bufferedReader(Charsets.UTF_8).forEachLine { line ->
                    if (line.startsWith(FREE_OFFICE_DICT_INI_FILE_NAME_BASE)) {
                        fileNameBase = line.substring(FREE_OFFICE_DICT_INI_FILE_NAME_BASE.length)
                    } else if (line.startsWith(FREE_OFFICE_DICT_INI_SUPPORTED_LOCALES)) {
                        supportedLocale = LocaleUtils.stringToLocale(line.substring(FREE_OFFICE_DICT_INI_SUPPORTED_LOCALES.length))
                    }
                }
                fileNameBase ?: return Result.failure(Exception("No valid file name base found"))
                supportedLocale ?: return Result.failure(Exception("No valid supported locale found"))

                val tempDictDir = File(context.cacheDir, IMPORT_NEW_DICT_TEMP_NAME)
                tempDictDir.deleteRecursively()
                tempDictDir.mkdirs()
                val entries = zipFile.entries()
                return SpellingDict.metaBuilder {
                    locale = supportedLocale
                    originalSourceId = sourceId
                    while (entries.hasMoreElements()) {
                        val entry = entries.nextElement()
                        flogInfo { entry.name }
                        when {
                            entry.name == "$fileNameBase.aff" -> {
                                val aff = File(tempDictDir, entry.name)
                                aff.outputStream().use { output ->
                                    zipFile.getInputStream(entry).use { input ->
                                        input.copyTo(output)
                                    }
                                }
                                affFile = entry.name
                            }
                            entry.name == "$fileNameBase.dic" -> {
                                val dic = File(tempDictDir, entry.name)
                                dic.outputStream().use { output ->
                                    zipFile.getInputStream(entry).use { input ->
                                        input.copyTo(output)
                                    }
                                }
                                dicFile = entry.name
                            }
                            entry.name.lowercase().contains("copying") || entry.name.lowercase().contains("license") -> {
                                val license = File(tempDictDir, SpellingDict.LICENSE_FILE_NAME)
                                license.outputStream().use { output ->
                                    zipFile.getInputStream(entry).use { input ->
                                        input.copyTo(output)
                                    }
                                }
                                licenseFile = SpellingDict.LICENSE_FILE_NAME
                            }
                            entry.name.lowercase().contains("readme") || entry.name.lowercase().contains("description") -> {
                                val readme = File(tempDictDir, SpellingDict.README_FILE_NAME)
                                readme.outputStream().use { output ->
                                    zipFile.getInputStream(entry).use { input ->
                                        input.copyTo(output)
                                    }
                                }
                                readmeFile = SpellingDict.README_FILE_NAME
                            }
                        }
                    }
                    val meta = build().getOrElse { return@metaBuilder Result.failure(it) }
                    Result.success(Extension(meta, tempDictDir, File(FlorisRef.internal(config.basePath).subRef("${meta.id}.flex").absolutePath(context))))
                }
            }
            else -> Result.failure(NotImplementedError())
        }
    }

    private fun saveTempFile(uri: Uri): Result<File> {
        val context = applicationContext.get() ?: return Result.failure(Exception("Context is null"))
        val tempFile = File(context.cacheDir, IMPORT_ARCHIVE_TEMP_NAME)
        tempFile.deleteRecursively() // JUst to make sure we clean up old mess
        ExternalContentUtils.readFromUri(context, uri, IMPORT_ARCHIVE_MAX_SIZE) { bis ->
            tempFile.outputStream().use { os -> bis.copyTo(os) }
        }.onFailure { return Result.failure(it) }
        return Result.success(tempFile)
    }
}
