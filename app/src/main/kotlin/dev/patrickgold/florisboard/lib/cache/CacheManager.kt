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

package dev.patrickgold.florisboard.lib.cache

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.neverEqualPolicy
import androidx.compose.runtime.setValue
import dev.patrickgold.florisboard.app.ext.EditorAction
import dev.patrickgold.florisboard.app.settings.advanced.Backup
import dev.patrickgold.florisboard.appContext
import dev.patrickgold.florisboard.ime.nlp.NATIVE_NULLPTR
import dev.patrickgold.florisboard.ime.theme.ThemeExtensionEditor
import dev.patrickgold.florisboard.lib.android.query
import dev.patrickgold.florisboard.lib.android.readToFile
import dev.patrickgold.florisboard.lib.ext.Extension
import dev.patrickgold.florisboard.lib.ext.ExtensionDefaults
import dev.patrickgold.florisboard.lib.ext.ExtensionEditor
import dev.patrickgold.florisboard.lib.ext.ExtensionJsonConfig
import dev.patrickgold.florisboard.lib.io.FileRegistry
import dev.patrickgold.florisboard.lib.io.FsDir
import dev.patrickgold.florisboard.lib.io.FsFile
import dev.patrickgold.florisboard.lib.io.ZipUtils
import dev.patrickgold.florisboard.lib.io.readJson
import dev.patrickgold.florisboard.lib.io.subDir
import dev.patrickgold.florisboard.lib.io.subFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.Closeable
import java.util.*

class CacheManager(context: Context) {
    companion object {
        private const val InputDirName = "input"
        private const val OutputDirName = "output"

        private const val ImporterDirName = "importer"
        private const val ExporterDirName = "exporter"
        private const val EditorDirName = "editor"
        private const val BackupAndRestoreDirName = "backup-and-restore"
    }

    private val appContext by context.appContext()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val importer = WorkspacesContainer(ImporterDirName) { ImporterWorkspace(it) }
    val exporter = WorkspacesContainer(ExporterDirName) { ExporterWorkspace(it) }
    val themeExtEditor = WorkspacesContainer(EditorDirName) { ExtEditorWorkspace<ThemeExtensionEditor>(it) }
    val backupAndRestore = WorkspacesContainer(BackupAndRestoreDirName) { BackupAndRestoreWorkspace(it) }

    fun readFromUriIntoCache(uri: Uri) = readFromUriIntoCache(listOf(uri))

    fun readFromUriIntoCache(uriList: List<Uri>): ImporterWorkspace {
        val contentResolver = appContext.contentResolver ?: error("Content resolver is null.")
        val workspace = ImporterWorkspace(uuid = UUID.randomUUID().toString()).also { it.mkdirs() }
        workspace.inputFileInfos = buildList {
            for (uri in uriList) {
                val info = contentResolver.query(uri)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                    cursor.moveToFirst()
                    val file = workspace.inputDir.subFile(cursor.getString(nameIndex))
                    contentResolver.readToFile(uri, file)
                    val ext = runCatching {
                        val extWorkingDir = workspace.outputDir.subDir(file.nameWithoutExtension)
                        ZipUtils.unzip(srcFile = file, dstDir = extWorkingDir)
                        val extJsonFile = extWorkingDir.subFile(ExtensionDefaults.MANIFEST_FILE_NAME)
                        extJsonFile.readJson<Extension>(ExtensionJsonConfig).also { it.workingDir = extWorkingDir }
                    }
                    FileInfo(
                        file = file,
                        mediaType = FileRegistry.guessMediaType(file, contentResolver.getType(uri)),
                        size = cursor.getLong(sizeIndex),
                        ext = ext.getOrNull(),
                    )
                } ?: error("Unable to fetch info about one or more resources to be imported.")
                add(info)
            }
        }
        importer.add(workspace)
        return workspace
    }

    open inner class WorkspacesContainer<T : Workspace> internal constructor(
        val dirName: String,
        val factory: (uuid: String) -> T,
    ) {
        private val workspacesGuard = Mutex(locked = false)
        private val workspaces = mutableListOf<T>()

        val dir: FsDir = appContext.cacheDir.subDir(dirName)

        fun new(uuid: String = UUID.randomUUID().toString()): T {
            return factory(uuid).also { it.mkdirs(); add(it) }
        }

        internal fun add(workspace: T) = scope.launch {
            workspacesGuard.withLock {
                workspaces.add(workspace)
            }
        }

        internal fun remove(workspace: T) = scope.launch {
            workspacesGuard.withLock {
                workspaces.remove(workspace)
            }
        }

        fun getWorkspaceByUuid(uuid: String) = runBlocking { getWorkspaceByUuidAsync(uuid).await() }

        fun getWorkspaceByUuidAsync(uuid: String): Deferred<T?> = scope.async {
            workspacesGuard.withLock {
                workspaces.find { it.uuid == uuid }
            }
        }
    }

    abstract inner class Workspace(val uuid: String) : Closeable {
        abstract val dir: FsDir

        open fun mkdirs() {
            dir.mkdirs()
        }

        fun isOpen() = dir.exists()

        fun isClosed() = !dir.exists()

        override fun close() {
            dir.deleteRecursively()
        }
    }

    inner class ImporterWorkspace(uuid: String) : Workspace(uuid) {
        override val dir: FsDir = importer.dir.subDir(uuid)

        val inputDir: FsDir = dir.subDir(InputDirName)
        val outputDir: FsDir = dir.subDir(OutputDirName)

        var inputFileInfos = emptyList<FileInfo>()

        override fun mkdirs() {
            super.mkdirs()
            inputDir.mkdirs()
            outputDir.mkdirs()
        }

        override fun close() {
            super.close()
            importer.remove(this)
        }
    }

    inner class ExporterWorkspace(uuid: String) : Workspace(uuid) {
        override val dir: FsDir = exporter.dir.subDir(uuid)
    }

    inner class ExtEditorWorkspace<T : ExtensionEditor>(uuid: String) : Workspace(uuid) {
        override val dir: FsDir = themeExtEditor.dir.subDir(uuid)

        val extDir: FsDir = dir.subDir("ext")
        val saverDir: FsDir = dir.subDir("saver")

        var currentAction by mutableStateOf<EditorAction?>(null)
        var ext: Extension? = null
        var editor by mutableStateOf<T?>(null, neverEqualPolicy())
        var version by mutableStateOf(0)

        val isModified get() = version > 0

        override fun mkdirs() {
            super.mkdirs()
            extDir.mkdirs()
            saverDir.mkdirs()
        }

        inline fun <R> update(block: T.() -> R): R {
            // Method is designed to only be called when editor has been previously initialized
            val ret = block(editor!!)
            editor = editor
            version++
            return ret
        }
    }

    inner class BackupAndRestoreWorkspace(uuid: String) : Workspace(uuid) {
        override val dir: FsDir = backupAndRestore.dir.subDir(uuid)

        val inputDir: FsDir = dir.subDir(InputDirName)
        val outputDir: FsDir = dir.subDir(OutputDirName)

        lateinit var zipFile: FsFile
        lateinit var metadata: Backup.Metadata
        var restoreWarningId: Int? = null
        var restoreErrorId: Int? = null

        override fun mkdirs() {
            super.mkdirs()
            inputDir.mkdirs()
            outputDir.mkdirs()
        }

        override fun close() {
            super.close()
            backupAndRestore.remove(this)
        }
    }

    data class FileInfo(
        val file: FsFile,
        val mediaType: String?,
        val size: Long,
        val ext: Extension?,
        var skipReason: Int = NATIVE_NULLPTR,
    )
}
