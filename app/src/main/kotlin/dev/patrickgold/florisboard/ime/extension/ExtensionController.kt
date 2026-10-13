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
import dev.patrickgold.florisboard.ime.io.FlorisRef
import dev.patrickgold.florisboard.ime.io.Storage
import dev.patrickgold.florisboard.ime.io.StorageController
import dev.patrickgold.florisboard.ime.io.createWorkspace
import dev.patrickgold.florisboard.ime.io.readJson
import dev.patrickgold.florisboard.ime.io.zip
import dev.patrickgold.florisboard.ime.keyboard3.extension.Keyboard3Extension
import dev.patrickgold.florisboard.ime.theme.ThemeExtension
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import org.florisboard.lib.kotlin.collectIn
import org.k3lp.lib.meta.report.MutableReportList

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

    private val eventChannel = Channel<Event>(Channel.RENDEZVOUS)
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

    suspend fun import(tmpExtRef: FlorisRef) {
        val event = Event.Import(tmpExtRef)
        eventChannel.send(event)
        return event.response.await()
    }

    suspend fun export(extensionId: String, zipUriRef: FlorisRef) {
        val event = Event.Export(extensionId, zipUriRef)
        eventChannel.send(event)
        return event.response.await()
    }

    fun delete(extension: Extension<*>): Boolean {
        TODO()
    }

    private sealed interface Event {
        data class ResetStorage(
            val newStorage: Storage,
        ) : Event

        data class Import(
            val tmpExtRef: FlorisRef,
            val response: CompletableDeferred<Unit> = CompletableDeferred(),
        ) : Event

        data class Export(
            val extensionId: String,
            val zipUriRef: FlorisRef,
            val response: CompletableDeferred<Unit> = CompletableDeferred(),
        ) : Event
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

        suspend fun handle(event: Event) {
            when (event) {
                is Event.ResetStorage -> handleResetStorage(event)
                is Event.Import -> {
                    try {
                        handleImport(event)
                        event.response.complete(Unit)
                    } catch (e: Throwable) {
                        event.response.completeExceptionally(e)
                    }
                }
                is Event.Export -> {
                    try {
                        handleExport(event)
                        event.response.complete(Unit)
                    } catch (e: Throwable) {
                        event.response.completeExceptionally(e)
                    }
                }
            }
        }

        private suspend fun handleResetStorage(event: Event.ResetStorage) {
            storage = event.newStorage
            indexExtensions(bundledSource)
            indexExtensions(userSource)
        }

        private suspend fun handleImport(event: Event.Import) {
            val manifestRef = event.tmpExtRef.subRef(ExtensionDefaults.MANIFEST_FILE_NAME)
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
                val extRef = userSource.ref.subRef(manifest.meta.id)
                storage.deleteRecursively(extRef)
                // TODO this destroys the currently indexed extension in error cases!!!
                storage.atomicMove(event.tmpExtRef, extRef)
            } else {
                val extRef = userSource.ref.subRef(manifest.meta.id)
                storage.atomicMove(event.tmpExtRef, extRef)
            }
            indexExtensions(userSource)
        }

        private suspend fun handleExport(event: Event.Export) {
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
            serializersModule = SerializersModule {
                polymorphic(ExtensionManifest::class) {
                    subclass(Keyboard3Extension.Manifest::class, Keyboard3Extension.Manifest.serializer())
                    subclass(ThemeExtension.Manifest::class, ThemeExtension.Manifest.serializer())
                }
            }
        }
    }
}
