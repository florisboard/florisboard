package dev.patrickgold.florisboard.ime.extension

import dev.patrickgold.florisboard.ime.keyboard3.extension.Keyboard3Extension
import dev.patrickgold.florisboard.ime.theme.ThemeExtension
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic

class ExtensionController {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    init {
        // TODO
    }

    companion object {
        val JsonConfig = Json {
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
