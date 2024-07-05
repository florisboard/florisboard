package dev.patrickgold.florisboard.ime.nlp

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import dev.patrickgold.florisboard.lib.FlorisLocale
import dev.patrickgold.florisboard.lib.devtools.flogError
import dev.patrickgold.florisboard.lib.ext.Extension
import dev.patrickgold.florisboard.lib.ext.ExtensionComponent
import dev.patrickgold.florisboard.lib.ext.ExtensionEditor
import dev.patrickgold.florisboard.lib.ext.ExtensionMeta
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.florisboard.lib.kotlin.io.FsDir
import org.florisboard.lib.kotlin.io.subFile

@Serializable
class LanguagePackComponent(
    override val id: String,
    override val label: String,
    override val authors: List<String>,
    val locale: FlorisLocale = FlorisLocale.fromTag(id),
    val hanShapeBasedKeyCode: String = "abcdefghijklmnopqrstuvwxyz",
) : ExtensionComponent {
    @Transient var parent: LanguagePackExtension? = null

    @SerialName("hanShapeBasedTable")
    private val _hanShapeBasedTable: String? = null  // Allows overriding the sqlite3 table to query in the json
    val hanShapeBasedTable
        get() = _hanShapeBasedTable ?: locale.variant
}

@SerialName(LanguagePackExtension.SERIAL_TYPE)
@Serializable
class LanguagePackExtension( // FIXME: how to make this support multiple types of language packs, and selectively load?
    override val meta: ExtensionMeta,
    override val dependencies: List<String>? = null,
    val items: List<LanguagePackComponent> = listOf(),
    val hanShapeBasedSQLite: String = "han.sqlite3",
) : Extension() {

    override fun components(): List<ExtensionComponent> = items

    override fun edit(): ExtensionEditor {
        TODO("LOL LMAO")
    }

    companion object {
        const val SERIAL_TYPE = "ime.extension.languagepack"
    }

    override fun serialType() = SERIAL_TYPE

    @Transient var hanShapeBasedSQLiteDatabase: SQLiteDatabase = SQLiteDatabase.create(null)

    override fun onAfterLoad(context: Context, cacheDir: FsDir) {
        // FIXME: this is loading language packs of all subtypes when they load.
        super.onAfterLoad(context, cacheDir)

        val databasePath = workingDir?.subFile(hanShapeBasedSQLite)?.path
        if (databasePath == null) {
            flogError { "Han shape-based language pack not found or loaded" }
        } else try {
            // TODO: use lock on database?
            hanShapeBasedSQLiteDatabase.takeIf { it.isOpen }?.close()
            hanShapeBasedSQLiteDatabase =
                SQLiteDatabase.openDatabase(databasePath, null, SQLiteDatabase.OPEN_READONLY);
        } catch (e: SQLiteException) {
            flogError { "SQLiteException in openDatabase: path=$databasePath, error='${e}'" }
        }
    }

    override fun onBeforeUnload(context: Context, cacheDir: FsDir) {
        super.onBeforeUnload(context, cacheDir)
        hanShapeBasedSQLiteDatabase.takeIf { it.isOpen }?.close()
    }
}
