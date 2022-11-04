package dev.patrickgold.florisboard.ime.nlp

import android.content.Context
import dev.patrickgold.florisboard.appContext
import dev.patrickgold.florisboard.assetManager
import dev.patrickgold.florisboard.ime.nlp.han.HanShapeBasedLanguageProvider
import dev.patrickgold.florisboard.lib.io.AssetManager
import dev.patrickgold.florisboard.lib.FlorisLocale
import dev.patrickgold.florisboard.lib.android.copy
import dev.patrickgold.florisboard.lib.devtools.flogError
import dev.patrickgold.florisboard.lib.ext.ExtensionJsonConfig
import dev.patrickgold.florisboard.lib.io.FlorisRef
import dev.patrickgold.florisboard.lib.io.parentDir
import dev.patrickgold.florisboard.lib.io.subFile
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class LanguagePackItem {
    val hanShapeBasedKeyCode: String = "abcdefghijklmnopqrstuvwxyz"
    val locale = FlorisLocale.from("en", "US")
    @SerialName("hanShapeBasedTable")
    private val _hanShapeBasedTable: String? = null  // Allows overriding the sqlite3 table to query in the json
    val hanShapeBasedTable
        get() = _hanShapeBasedTable ?: locale.variant
}

@SerialName(LanguagePack.SERIAL_TYPE)
@Serializable
class LanguagePack {
    val items = emptyList<LanguagePackItem>()
    companion object {
        const val SERIAL_TYPE = "ime.extension.languagepack"

        const val SERIAL_PATH = "ime/dict/languagepack.json";

        fun load(context: Context): LanguagePack {
            // TODO: async?
            val appContext by context.appContext()
            val assetManager by context.assetManager()
            appContext.filesDir.subFile(SERIAL_PATH).parentDir?.mkdirs()
            if (!appContext.filesDir.subFile(SERIAL_PATH).exists()) {
                appContext.assets.copy(SERIAL_PATH, appContext.filesDir.subFile(SERIAL_PATH))
            }
            assetManager.loadJsonAsset(FlorisRef.internal(SERIAL_PATH), serializer()).fold(
                onSuccess = { return it },
                onFailure = { error ->
                    flogError { error.toString() }
                    return LanguagePack()
                },
            )
        }
    }
}
