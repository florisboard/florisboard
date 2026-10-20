/*
 * Copyright (C) 2026 The FlorisBoard Contributors
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

import androidx.compose.runtime.staticCompositionLocalOf
import dev.patrickgold.florisboard.app.settings.advanced.BackupArchiveFilter
import dev.patrickgold.florisboard.app.settings.advanced.BackupDefaults
import dev.patrickgold.florisboard.ime.io.FlorisRef
import dev.patrickgold.florisboard.ime.io.Storage
import dev.patrickgold.florisboard.ime.io.StorageController
import dev.patrickgold.florisboard.ime.io.createWorkspace
import dev.patrickgold.florisboard.ime.io.readJson
import dev.patrickgold.florisboard.ime.io.unzip
import dev.patrickgold.florisboard.ime.io.zip
import dev.patrickgold.jetpref.datastore.runtime.ImportStrategy
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import org.florisboard.lib.kotlin.collectIn
import org.k3lp.lib.meta.report.MutableReportList
import kotlin.uuid.Uuid

/**
 * Provides the [ExtensionController] instance this composition tree is associated with.
 */
val LocalExtensionController = staticCompositionLocalOf<ExtensionController> {
    error("No extension controller is associated with this composition tree.")
}

class ExtensionController(
    storageController: StorageController,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val eventChannel = Channel<Event<*>>(Channel.RENDEZVOUS)
    private val mutableIndex = MutableIndex(storageController.activeStorage.value)

    val activeIndex: StateFlow<ExtensionIndex>
        field = MutableStateFlow(mutableIndex.build())

    init {
        storageController.activeStorage.collectIn(scope) { newStorage ->
            eventChannel.send(Event.ResetStorage(newStorage))
        }
        scope.launch {
            while (true) {
                val event = eventChannel.receive()
                mutableIndex.handle(event)
                activeIndex.value = mutableIndex.build()
            }
        }
    }

    suspend fun import(extSrcRef: FlorisRef) {
        val event = Event.Import(extSrcRef)
        eventChannel.send(event)
        return event.response.await()
    }

    suspend fun export(extensionId: String, zipUriRef: FlorisRef) {
        val event = Event.Export(extensionId, zipUriRef)
        eventChannel.send(event)
        return event.response.await()
    }

    suspend fun delete(extensionId: String) {
        val event = Event.Delete(extensionId)
        eventChannel.send(event)
        return event.response.await()
    }

    suspend fun restore(
        backupDirRef: FlorisRef,
        importStrategy: ImportStrategy,
        filter: BackupArchiveFilter,
        isLegacyMode: Boolean,
    ) {
        val event = Event.Restore(backupDirRef, importStrategy, filter, isLegacyMode)
        eventChannel.send(event)
        return event.response.await()
    }

    private sealed class Event<R> {
        val response: CompletableDeferred<R> = CompletableDeferred()

        data class ResetStorage(
            val newStorage: Storage,
        ) : Event<Unit>()

        data class Import(
            val extSrcRef: FlorisRef,
        ) : Event<Unit>()

        data class Export(
            val extensionId: String,
            val zipUriRef: FlorisRef,
        ) : Event<Unit>()

        data class Delete(
            val extensionId: String,
        ) : Event<Unit>()

        data class Restore(
            val backupDirRef: FlorisRef,
            val importStrategy: ImportStrategy,
            val filter: BackupArchiveFilter,
            val isLegacyMode: Boolean,
        ) : Event<Unit>()
    }

    private class MutableSource(
        val ref: FlorisRef,
        val extensions: MutableMap<String, Extension<*>> = mutableMapOf(),
        val reports: MutableReportList = MutableReportList(),
    ) {
        fun clear() {
            extensions.clear()
            reports.clear()
        }

        fun removeWhere(storage: Storage, predicate: (Extension<*>) -> Boolean) {
            extensions.entries.removeAll { (_, extension) ->
                if (predicate(extension)) {
                    storage.deleteRecursively(extension.sourceRef)
                    true
                } else {
                    false
                }
            }
        }
    }

    private class MutableIndex(initialStorage: Storage) {
        private val bundledSource = MutableSource(ref = FlorisRef.assets(EXTENSIONS_PATH))
        private val userSource = MutableSource(ref = FlorisRef.internal(EXTENSIONS_PATH))

        private var storage: Storage = initialStorage

        fun build(): ExtensionIndex {
            return ExtensionIndex(
                storage = storage,
                extensions = buildMap {
                    putAll(bundledSource.extensions)
                    putAll(userSource.extensions)
                },
                reports = buildList {
                    addAll(bundledSource.reports.toReportList())
                    addAll(userSource.reports.toReportList())
                },
            )
        }

        fun handle(event: Event<*>) {
            try {
                when (event) {
                    is Event.ResetStorage -> event.response.complete(handleResetStorage(event))
                    is Event.Import -> event.response.complete(handleImport(event))
                    is Event.Export -> event.response.complete(handleExport(event))
                    is Event.Delete -> event.response.complete(handleDelete(event))
                    is Event.Restore -> event.response.complete(handleRestore(event))
                }
            } catch (e: Throwable) {
                event.response.completeExceptionally(e)
            }
        }

        private fun handleResetStorage(event: Event.ResetStorage) {
            storage = event.newStorage
            indexExtensions(bundledSource)
            indexExtensions(userSource)
        }

        private fun handleImport(event: Event.Import) {
            importExtension(event.extSrcRef)
            indexExtensions(userSource)
        }

        private fun handleExport(event: Event.Export) {
            val extension = getExtensionById(event.extensionId)
            requireNotNull(extension) { "No such extension: ${event.extensionId}" }
            val workspaceRef = storage.createWorkspace()
            try {
                val intermediateRef = workspaceRef.subRef("${event.extensionId}.flex")
                storage.zip(extension.sourceRef, intermediateRef)
                storage.copy(intermediateRef, event.zipUriRef)
            } finally {
                storage.deleteRecursively(workspaceRef)
            }
        }

        private fun handleDelete(event: Event.Delete) {
            val extension = getExtensionById(event.extensionId)
            requireNotNull(extension) { "No such extension: ${event.extensionId}" }
            require(extension.canDelete()) { "Delete not allowed" }
            storage.deleteRecursively(extension.sourceRef)
            indexExtensions(userSource)
        }

        private fun handleRestore(event: Event.Restore) {
            val (backupDirRef, importStrategy, filter, isLegacyMode) = event
            if (importStrategy == ImportStrategy.Erase) {
                userSource.removeWhere(storage) { extension ->
                    filter.includes(extension.type)
                }
            }
            val backupFilesDirRef = backupDirRef.subRef(BackupDefaults.FILES_DIR)
            val extensionsDirRef = when {
                // we can only import legacy theme extensions, legacy keyboard extensions are not compatible
                isLegacyMode -> backupFilesDirRef.subRef(BackupDefaults.OLD_EXT_IME_THEME_PATH)
                else -> backupFilesDirRef.subRef(EXTENSIONS_PATH)
            }
            // TODO better error reporting
            for (entryRef in storage.listOrNull(extensionsDirRef) ?: emptyList()) {
                val extSrcRef = when {
                    isLegacyMode -> {
                        val extSrcRef = entryRef.parentRef!!.subRef(Uuid.random().toHexDashString())
                        storage.unzip(entryRef, extSrcRef)
                        extSrcRef
                    }
                    else -> entryRef
                }
                if (storage.metadataOrNull(extSrcRef)?.isDirectory != true) {
                    continue
                }
                val manifestRef = extSrcRef.subRef(ExtensionDefaults.MANIFEST_FILE_NAME)
                if (!storage.exists(manifestRef)) {
                    continue
                }
                val manifest = try {
                    storage.readJson<ExtensionManifest>(manifestRef)
                } catch (e: Throwable) {
                    continue
                }
                if (isLegacyMode && entryRef.pathName != manifest.meta.id + ".flex") {
                    continue
                }
                if (!isLegacyMode && extSrcRef.pathName != manifest.meta.id) {
                    continue
                }
                if (!filter.includes(manifest.type)) {
                    continue
                }
                importExtension(extSrcRef)
            }
            indexExtensions(userSource)
        }

        private fun importExtension(extSrcRef: FlorisRef) {
            val manifestRef = extSrcRef.subRef(ExtensionDefaults.MANIFEST_FILE_NAME)
            val manifest = storage.readJson<ExtensionManifest>(manifestRef, ExtensionJson)
            require(manifest.meta.isValid()) { "Extension meta is malformed" }
            val existingExtension = getExtensionById(manifest.meta.id)
            if (existingExtension != null) {
                require(existingExtension.canDelete()) {
                    "Attempted to update bundled extension, which is not possible."
                }
                require(existingExtension.manifest::class == manifest::class) {
                    "Attempted to update extension to different type, which is not supported."
                }
                val extDstRef = userSource.ref.subRef(manifest.meta.id)
                storage.deleteRecursively(extDstRef)
                // TODO this destroys the currently indexed extension in error cases!!!
                storage.copyRecursively(extSrcRef, extDstRef)
            } else {
                val extDstRef = userSource.ref.subRef(manifest.meta.id)
                storage.copyRecursively(extSrcRef, extDstRef)
            }
        }

        private fun indexExtensions(source: MutableSource) {
            source.clear()
            if (!storage.canAccessStorage(source.ref)) {
                // If we do not have access to the underlying medium all operations below would fail.
                // This can happen if we are initialized before first device-unlock (in direct boot mode
                // on Android).
                return
            }
            if (source.ref.isInternal) {
                try {
                    storage.mkdirs(source.ref)
                } catch (e: Throwable) {
                    source.reports.add(
                        ExtensionIndexViolation.UnexpectedError(
                            cause = e,
                            sourceRange = source.ref.asSourceRange(),
                        )
                    )
                    return
                }
            }
            val extRefList =
                try {
                    storage.list(source.ref)
                } catch (e: Throwable) {
                    source.reports.add(
                        ExtensionIndexViolation.UnexpectedError(
                            cause = e,
                            sourceRange = source.ref.asSourceRange(),
                        )
                    )
                    return
                }
            for (extRef in extRefList) {
                val extId = extRef.pathName
                val manifestRef = extRef.subRef(ExtensionDefaults.MANIFEST_FILE_NAME)
                val manifest =
                    try {
                        storage.readJson<ExtensionManifest>(manifestRef, ExtensionJson)
                    } catch (e: Throwable) {
                        // TODO fine-grained violations
                        source.reports.add(
                            ExtensionIndexViolation.UnexpectedError(
                                cause = e,
                                sourceRange = manifestRef.asSourceRange(),
                            )
                        )
                        continue
                    }
                if (extId != manifest.meta.id) {
                    source.reports.add(
                        ExtensionIndexViolation.IdMismatch(
                            idInDirName = extId,
                            idInManifest = manifest.meta.id,
                            sourceRange = extRef.asSourceRange(),
                        )
                    )
                    continue
                }
                val duplicateExtension = getExtensionById(extId)
                if (duplicateExtension != null) {
                    source.reports.add(
                        ExtensionIndexViolation.DuplicateExtension(
                            extRefA = duplicateExtension.sourceRef,
                            extRefB = extRef,
                            sourceRange = extRef.asSourceRange(),
                        )
                    )
                    continue
                }
                source.extensions[extId] = when (manifest) {
                    is Keyboard3Extension.Manifest -> {
                        Keyboard3Extension(manifest, extRef)
                    }
                    is ThemeExtension.Manifest -> {
                        ThemeExtension(manifest, extRef)
                    }
                    else -> {
                        source.reports.add(
                            ExtensionIndexViolation.UnknownManifest(
                                klass = manifest::class,
                                sourceRange = manifestRef.asSourceRange(),
                            )
                        )
                        continue
                    }
                }
            }
        }

        private fun getExtensionById(id: String): Extension<*>? {
            return bundledSource.extensions.getOrElse(id) { userSource.extensions[id] }
        }
    }

    companion object {
        const val EXTENSIONS_PATH = "extensions"

        val ExtensionJson = Json {
            classDiscriminator = "$"
            encodeDefaults = false
            ignoreUnknownKeys = true
            isLenient = false
            prettyPrint = true
            prettyPrintIndent = "  "
        }
    }
}
