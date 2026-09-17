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
import dev.patrickgold.florisboard.ime.io.readJson
import dev.patrickgold.florisboard.ime.keyboard3.extension.Keyboard3Extension
import dev.patrickgold.florisboard.ime.theme.ThemeExtension
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

    fun delete(extension: Extension<*>): Boolean {
        TODO()
    }

    fun export(extension: Extension<*>, ref: FlorisRef) {
        TODO()
    }

    private sealed interface Event {
        data class ResetStorage(val newStorage: Storage) : Event
    }

    private class MutableIndex(initialStorage: Storage) {
        private val bundledRef = FlorisRef.assets(EXTENSIONS_PATH)
        private val internalRef = FlorisRef.internal(EXTENSIONS_PATH)

        private var storage: Storage = initialStorage
        private val extensions = mutableMapOf<String, Extension<*>>()
        private val reports = MutableReportList()

        fun build(): ExtensionIndex {
            return ExtensionIndex(
                storage = storage,
                extensions = extensions.toMap(),
                reports = reports.finalize(),
            )
        }

        suspend fun handle(event: Event) {
            when (event) {
                is Event.ResetStorage -> handleResetStorage(event.newStorage)
            }
        }

        private suspend fun handleResetStorage(newStorage: Storage) {
            storage = newStorage
            extensions.clear()
            reports.clear()
            indexExtensions(bundledRef)
            if (reports.numErrorReports == 0) {
                indexExtensions(internalRef)
            }
        }

        private fun indexExtensions(ref: FlorisRef) {
            if (!storage.canAccessUnderlyingMedium(ref)) {
                // If we do not have access to the underlying medium all operations below would fail.
                // This can happen if we are initialized before first device-unlock (in direct boot mode
                // on Android).
                return
            }
            val extRefList =
                try {
                    storage.list(ref)
                } catch (e: Throwable) {
                    reports.add(
                        ExtensionIndexViolation.UnexpectedError(
                            cause = e,
                            sourceRange = ref.asSourceRange(),
                        )
                    )
                    return
                }
            for (extRef in extRefList) {
                val extId =
                    try {
                        storage.basename(extRef)
                    } catch (e: Throwable) {
                        reports.add(
                            ExtensionIndexViolation.UnexpectedError(
                                cause = e,
                                sourceRange = extRef.asSourceRange(),
                            )
                        )
                        return
                    }
                val manifestRef = extRef.subRef(ExtensionDefaults.MANIFEST_FILE_NAME)
                val manifest =
                    try {
                        storage.readJson<ExtensionManifest>(manifestRef, ExtensionJson)
                    } catch (e: Throwable) {
                        // TODO fine-grained violations
                        reports.add(
                            ExtensionIndexViolation.UnexpectedError(
                                cause = e,
                                sourceRange = manifestRef.asSourceRange(),
                            )
                        )
                        continue
                    }
                if (extId != manifest.meta.id) {
                    reports.add(
                        ExtensionIndexViolation.IdMismatch(
                            idInDirName = extId,
                            idInManifest = manifest.meta.id,
                            sourceRange = extRef.asSourceRange(),
                        )
                    )
                    continue
                }
                val duplicateExtension = extensions[extId]
                if (duplicateExtension != null) {
                    reports.add(
                        ExtensionIndexViolation.DuplicateExtension(
                            extRefA = duplicateExtension.sourceRef,
                            extRefB = extRef,
                            sourceRange = extRef.asSourceRange(),
                        )
                    )
                    continue
                }
                extensions[extId] = when (manifest) {
                    is Keyboard3Extension.Manifest -> {
                        Keyboard3Extension(manifest, extRef)
                    }
                    is ThemeExtension.Manifest -> {
                        ThemeExtension(manifest, extRef)
                    }
                    else -> {
                        reports.add(
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
