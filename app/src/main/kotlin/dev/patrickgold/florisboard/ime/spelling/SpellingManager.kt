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
import android.util.LruCache
import android.view.textservice.SuggestionsInfo
import androidx.lifecycle.MutableLiveData
import dev.patrickgold.florisboard.appContext
import dev.patrickgold.florisboard.assetManager
import dev.patrickgold.florisboard.extensionManager
import dev.patrickgold.florisboard.lib.FlorisLocale
import dev.patrickgold.florisboard.lib.android.read
import dev.patrickgold.florisboard.lib.devtools.flogDebug
import dev.patrickgold.florisboard.lib.devtools.flogError
import dev.patrickgold.florisboard.lib.devtools.flogInfo
import dev.patrickgold.florisboard.lib.ext.ExtensionDefaults
import dev.patrickgold.florisboard.lib.ext.ExtensionMaintainer
import dev.patrickgold.florisboard.lib.ext.ExtensionMeta
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.io.File
import java.util.concurrent.atomic.AtomicInteger
import java.util.zip.ZipFile

class SpellingManager(context: Context) {
    companion object {
        private const val SOURCE_ID_RAW: String = "raw"

        private const val IMPORT_ARCHIVE_MAX_SIZE: Long = 25_165_820 // 24 MiB
        private const val IMPORT_ARCHIVE_TEMP_NAME: String = "__temp000__ime_spelling_import_archive"
        private const val IMPORT_NEW_DICT_TEMP_NAME: String = "__temp000__ime_spelling_import_new_dict"

        // Possible license file names, always in lowercase
        private val LICENSE_FILE_MATCHER =
            """^((copying|license)|((mpl-|gpl-|lgpl-|apache-)(\d\.\d)))(\.txt|\.md)?${'$'}""".toRegex(RegexOption.IGNORE_CASE)

        private const val FIREFOX_MANIFEST_JSON = "manifest.json"
        private const val FIREFOX_DICTIONARIES_FOLDER = "dictionaries"

        @Serializable
        private data class FirefoxManifestJson(
            @SerialName("manifest_version")
            val manifestVersion: Int,
            val name: String? = null,
            val description: String? = null,
            val version: String? = null,
            val author: String? = null,
            val dictionaries: Map<String, String>
        )

        private const val FREE_OFFICE_DICT_INI = "dict.ini"
        private const val FREE_OFFICE_DICT_INI_FILE_NAME_BASE = "FileNameBase="
        private const val FREE_OFFICE_DICT_INI_SUPPORTED_LOCALES = "SupportedLocales="

        const val AFF_EXT: String = "aff"
        const val DIC_EXT: String = "dic"

        val Config = SpellingConfig(
            basePath = "ime/spelling",
            importSources = listOf(
                SpellingConfig.ImportSource(
                    id = "mozilla_firefox",
                    label = "Mozilla Firefox Add-ons",
                    url = "https://addons.mozilla.org/firefox/language-tools/",
                    format = SpellingConfig.ImportFormat.Archive(
                        file = SpellingConfig.FileInput(
                            fileNameRegex = """^.+\.xpi${'$'}""".toRegex(),
                            isRequired = true,
                        ),
                    ),
                ),
                SpellingConfig.ImportSource(
                    id = "libre_office",
                    label = "LibreOffice [CURRENTLY UNSUPPORTED]",
                    url = "https://extensions.libreoffice.org/?Tags%5B%5D=50",
                    format = SpellingConfig.ImportFormat.Archive(
                        file = SpellingConfig.FileInput(
                            fileNameRegex = """^.+\.oxt${'$'}""".toRegex(),
                            isRequired = true,
                        ),
                    ),
                ),
                SpellingConfig.ImportSource(
                    id = "open_office",
                    label = "Apache OpenOffice [CURRENTLY UNSUPPORTED]",
                    url = "https://extensions.openoffice.org/en/search?f%5B0%5D=field_project_tags%3A157",
                    format = SpellingConfig.ImportFormat.Archive(
                        file = SpellingConfig.FileInput(
                            fileNameRegex = """^.+\.oxt${'$'}""".toRegex(),
                            isRequired = true,
                        ),
                    ),
                ),
                SpellingConfig.ImportSource(
                    id = "free_office",
                    label = "SoftMaker FreeOffice",
                    url = "https://www.freeoffice.com/en/download/dictionaries",
                    format = SpellingConfig.ImportFormat.Archive(
                        file = SpellingConfig.FileInput(
                            fileNameRegex = """^.+\.sox${'$'}""".toRegex(),
                            isRequired = true,
                        ),
                    ),
                ),
                SpellingConfig.ImportSource(
                    id = "gh_wooorm",
                    label = "GitHub collection by Titus Wormer",
                    url = "https://github.com/wooorm/dictionaries",
                    format = SpellingConfig.ImportFormat.Raw(
                        affFile = SpellingConfig.FileInput(
                            fileNameRegex = """^.+\.aff${'$'}""".toRegex(),
                            isRequired = true,
                        ),
                        dicFile = SpellingConfig.FileInput(
                            fileNameRegex = """^.+\.dic${'$'}""".toRegex(),
                            isRequired = true,
                        ),
                    ),
                ),
            ),
        )
    }

    private val appContext by context.appContext()
    private val assetManager by context.assetManager()
    private val extensionManager by context.extensionManager()

    val importSourceLabels: List<String>
    val importSourceUrls: List<String?>

    val debugOverlaySuggestionsInfos = LruCache<Long, Pair<String, SuggestionsInfo>>(10)
    var debugOverlayVersion = MutableLiveData(0)
    private val debugOverlayVersionSource = AtomicInteger(0)

    init {
        Config.importSources.map { it.label }.toMutableList().let {
            it.add(0, "-")
            importSourceLabels = it.toList()
        }
        Config.importSources.map { it.url }.toMutableList().let {
            it.add(0, "-")
            importSourceUrls = it.toList()
        }
    }

    @Synchronized
    fun getSpellingDict(locale: FlorisLocale): SpellingDict? {
        flogDebug { locale.toString() }
        val dicts = extensionManager.spellingDicts.value ?: return null
        val ext = dicts.firstNotNullOfOrNull {
            if (it.spelling.locale.localeTag() == locale.localeTag()) it else null
        } ?: dicts.firstNotNullOfOrNull {
            if (it.spelling.locale.language == locale.language) it else null
        } ?: return null
        if (!ext.isLoaded()) {
            ext.load(appContext).onFailure { flogError { it.toString() } }
        }
        return ext.dict
    }

    fun prepareImport(sourceId: String, archiveUri: Uri) = runCatching<SpellingExtensionEditor> {
        when (sourceId) {
            "mozilla_firefox" -> {
                val tempFile = saveTempFile(archiveUri).getOrThrow()
                val zipFile = ZipFile(tempFile)
                val manifestEntry = zipFile.getEntry(FIREFOX_MANIFEST_JSON) ?: error("No $FIREFOX_MANIFEST_JSON file found")
                val manifest = zipFile.getInputStream(manifestEntry).bufferedReader(Charsets.UTF_8).use {
                    assetManager.loadJsonAsset<FirefoxManifestJson>(it.readText())
                }.getOrThrow()

                if (manifest.dictionaries.isEmpty()) {
                    error("No dictionary definitions provided!")
                }
                val supportedLocale: FlorisLocale
                val fileNameBase: String
                manifest.dictionaries.entries.first().let {
                    supportedLocale = FlorisLocale.fromTag(it.key)
                    fileNameBase = it.value.removeSuffix(".dic")
                }

                val tempDictDir = File(appContext.cacheDir, IMPORT_NEW_DICT_TEMP_NAME)
                tempDictDir.deleteRecursively()
                tempDictDir.mkdirs()
                val entries = zipFile.entries()
                val extensionEditor = SpellingExtensionEditor(
                    meta = ExtensionMeta(
                        id = ExtensionDefaults.createLocalId("spelling"),
                        version = manifest.version ?: "0.0.0",
                        title = manifest.name ?: "Imported spelling dict",
                        description = manifest.description ?: "",
                        maintainers = manifest.author?.let { mutableListOf(ExtensionMaintainer.fromOrTakeRaw(it)) }
                            ?: mutableListOf(ExtensionMaintainer(name = "Unknown")),
                        license = "unknown",
                    ),
                    dependencies = mutableListOf(),
                    workingDir = tempDictDir,
                    spelling = SpellingExtensionConfigEditor().apply {
                        locale = supportedLocale.languageTag()
                        originalSourceId = sourceId
                        while (entries.hasMoreElements()) {
                            val entry = entries.nextElement()
                            flogInfo { entry.name }
                            when (entry.name) {
                                "$fileNameBase.aff" -> {
                                    val name = entry.name.removePrefix("$FIREFOX_DICTIONARIES_FOLDER/").replace('/', '_')
                                    val aff = File(tempDictDir, name)
                                    aff.outputStream().use { output ->
                                        zipFile.getInputStream(entry).use { input ->
                                            input.copyTo(output)
                                        }
                                    }
                                    affFile = name
                                }
                                "$fileNameBase.dic" -> {
                                    val name = entry.name.removePrefix("$FIREFOX_DICTIONARIES_FOLDER/").replace('/', '_')
                                    val dic = File(tempDictDir, name)
                                    dic.outputStream().use { output ->
                                        zipFile.getInputStream(entry).use { input ->
                                            input.copyTo(output)
                                        }
                                    }
                                    dicFile = name
                                }
                                else -> {
                                    if (LICENSE_FILE_MATCHER.matches(entry.name)) {
                                        val license = File(tempDictDir, SpellingDict.LICENSE_FILE_NAME)
                                        license.outputStream().use { output ->
                                            zipFile.getInputStream(entry).use { input ->
                                                input.copyTo(output)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                )
                tempFile.delete()
                extensionEditor
            }
            "free_office" -> {
                val tempFile = saveTempFile(archiveUri).getOrThrow()
                val zipFile = ZipFile(tempFile)
                val dictIniEntry = zipFile.getEntry(FREE_OFFICE_DICT_INI) ?: error("No dict.ini file found")
                var fileNameBase: String? = null
                var supportedLocale: FlorisLocale? = null
                zipFile.getInputStream(dictIniEntry).bufferedReader(Charsets.UTF_8).forEachLine { line ->
                    if (line.startsWith(FREE_OFFICE_DICT_INI_FILE_NAME_BASE)) {
                        fileNameBase = line.substring(FREE_OFFICE_DICT_INI_FILE_NAME_BASE.length)
                    } else if (line.startsWith(FREE_OFFICE_DICT_INI_SUPPORTED_LOCALES)) {
                        supportedLocale = FlorisLocale.fromTag(line.substring(FREE_OFFICE_DICT_INI_SUPPORTED_LOCALES.length))
                    }
                }
                fileNameBase ?: error("No valid file name base found")
                supportedLocale ?: error("No valid supported locale found")

                val tempDictDir = File(appContext.cacheDir, IMPORT_NEW_DICT_TEMP_NAME)
                tempDictDir.deleteRecursively()
                tempDictDir.mkdirs()
                val entries = zipFile.entries()
                val extensionEditor = SpellingExtensionEditor(
                    meta = ExtensionMeta(
                        id = ExtensionDefaults.createLocalId("spelling"),
                        version = "0.0.0",
                        title = "FreeOffice import",
                        maintainers = mutableListOf(ExtensionMaintainer(name = "Unknown")),
                        license = "unknown",
                    ),
                    dependencies = mutableListOf(),
                    workingDir = tempDictDir,
                    spelling = SpellingExtensionConfigEditor().apply {
                        locale = supportedLocale!!.languageTag()
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
                                LICENSE_FILE_MATCHER.matches(entry.name) -> {
                                    val license = File(tempDictDir, SpellingDict.LICENSE_FILE_NAME)
                                    license.outputStream().use { output ->
                                        zipFile.getInputStream(entry).use { input ->
                                            input.copyTo(output)
                                        }
                                    }
                                    //licenseFile = SpellingDict.LICENSE_FILE_NAME
                                }
                                entry.name.lowercase().contains("readme") || entry.name.lowercase().contains("description") -> {
                                    val readme = File(tempDictDir, SpellingDict.README_FILE_NAME)
                                    readme.outputStream().use { output ->
                                        zipFile.getInputStream(entry).use { input ->
                                            input.copyTo(output)
                                        }
                                    }
                                    //readmeFile = SpellingDict.README_FILE_NAME
                                }
                            }
                        }
                    }
                )
                tempFile.delete()
                extensionEditor
            }
            else -> error("Unsupported source!")
        }
    }

    /*
    fun prepareImportRaw(affUri: Uri, dicUri: Uri, localeStr: String): Result<Extension<SpellingDict.Meta>> {
        val context = applicationContext.get() ?: return Result.failure(Exception("Context is null"))

        val tempDictDir = File(context.cacheDir, IMPORT_NEW_DICT_TEMP_NAME)
        tempDictDir.deleteRecursively()
        tempDictDir.mkdirs()
        return SpellingDict.metaBuilder {
            title = "Manually imported dictionary"
            locale = FlorisLocale.fromTag(localeStr)
            originalSourceId = SOURCE_ID_RAW
            affFile = "$localeStr.$AFF_EXT"
            val affFileHandle = File(tempDictDir, affFile)
            ExternalContentUtils.readFromUri(context, affUri, IMPORT_ARCHIVE_MAX_SIZE) { bis ->
                affFileHandle.outputStream().use { os -> bis.copyTo(os) }
            }.onFailure { return Result.failure(it) }
            dicFile = "$localeStr.$DIC_EXT"
            val dicFileHandle = File(tempDictDir, dicFile)
            ExternalContentUtils.readFromUri(context, dicUri, IMPORT_ARCHIVE_MAX_SIZE) { bis ->
                dicFileHandle.outputStream().use { os -> bis.copyTo(os) }
            }.onFailure { return Result.failure(it) }
            val meta = build().getOrElse { return@metaBuilder Result.failure(it) }
            Result.success(Extension(meta, tempDictDir, File(FlorisRef.internal(config.basePath).subRef("${meta.id}.flex").absolutePath(context))))
        }
    }*/

    private fun saveTempFile(uri: Uri) = runCatching<File> {
        val tempFile = File(appContext.cacheDir, IMPORT_ARCHIVE_TEMP_NAME)
        tempFile.deleteRecursively() // Just to make sure we clean up old mess
        appContext.contentResolver.read(uri, IMPORT_ARCHIVE_MAX_SIZE) { bis ->
            tempFile.outputStream().use { os -> bis.copyTo(os) }
        }
        tempFile
    }

    fun addToDebugOverlay(word: String, info: SuggestionsInfo) {
        val version = debugOverlayVersionSource.incrementAndGet()
        debugOverlaySuggestionsInfos.put(System.currentTimeMillis(), word to info)
        debugOverlayVersion.postValue(version)
    }

    fun clearDebugOverlay() {
        val version = debugOverlayVersionSource.incrementAndGet()
        debugOverlaySuggestionsInfos.evictAll()
        debugOverlayVersion.postValue(version)
    }
}
