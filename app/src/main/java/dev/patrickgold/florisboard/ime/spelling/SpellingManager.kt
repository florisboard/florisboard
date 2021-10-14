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
import dev.patrickgold.florisboard.appContext
import dev.patrickgold.florisboard.assetManager
import dev.patrickgold.florisboard.common.FlorisLocale
import dev.patrickgold.florisboard.debug.flogDebug
import dev.patrickgold.florisboard.debug.flogError
import dev.patrickgold.florisboard.debug.flogInfo
import dev.patrickgold.florisboard.extensionManager
import dev.patrickgold.florisboard.res.ExternalContentUtils
import dev.patrickgold.florisboard.res.FlorisRef
import dev.patrickgold.florisboard.res.ext.ExtensionAuthor
import dev.patrickgold.florisboard.res.ext.ExtensionAuthorEditor
import dev.patrickgold.florisboard.res.ext.ExtensionDefaults
import dev.patrickgold.florisboard.res.ext.ExtensionMetaEditor
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.io.File
import java.util.zip.ZipFile

class SpellingManager(context: Context) {
    companion object {
        private const val SOURCE_ID_RAW: String = "raw"

        private const val IMPORT_ARCHIVE_MAX_SIZE: Int = 25_165_820 // 24 MiB
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
    }

    private val appContext by context.appContext()
    private val extensionManager by context.extensionManager()

    private val assetManager by appContext.assetManager()

    val config = assetManager.loadJsonAsset<SpellingConfig>(FlorisRef.assets("ime/spelling/config.json")).getOrDefault(SpellingConfig.default())
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
                    meta = ExtensionMetaEditor(
                        id = ExtensionDefaults.createIdForImport("spelling"),
                        version = manifest.version ?: "0.0.0",
                        title = manifest.name ?: "Imported spelling dict",
                        description = manifest.description ?: "",
                        authors = manifest.author?.let { mutableListOf(ExtensionAuthor.fromOrTakeRaw(it).edit()) }
                            ?: mutableListOf(ExtensionAuthorEditor(name = "Unknown")),
                        license = "unknown",
                    ),
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
                    meta = ExtensionMetaEditor(
                        id = ExtensionDefaults.createIdForImport("spelling"),
                        version = "0.0.0",
                        title = "LibreOffice import",
                        authors = mutableListOf(ExtensionAuthorEditor(name = "Unknown")),
                        license = "unknown",
                    ),
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
        ExternalContentUtils.readFromUri(appContext, uri, IMPORT_ARCHIVE_MAX_SIZE) { bis ->
            tempFile.outputStream().use { os -> bis.copyTo(os) }
        }.getOrThrow()
        tempFile
    }
}
