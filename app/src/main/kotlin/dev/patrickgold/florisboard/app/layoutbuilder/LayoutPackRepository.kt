package dev.patrickgold.florisboard.app.layoutbuilder

import android.content.Context
import android.net.Uri
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.serialization.json.Json

class LayoutPackRepository(
    private val context: Context,
    private val json: Json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
    },
) {
    private val defaultAssetPath = "layouts/lcars_hacker_en_us.json"
    private val userDir: File by lazy {
        File(context.filesDir, "layouts/user").also { dir ->
            if (!dir.exists()) {
                dir.mkdirs()
            }
        }
    }
    private val userFile: File by lazy {
        File(userDir, "lcars_hacker_en_us.user.json")
    }

    fun loadDefault(): LayoutPack {
        val raw = context.assets.open(defaultAssetPath).bufferedReader().use { it.readText() }
        return json.decodeFromString(LayoutPack.serializer(), raw)
    }

    fun loadUserOrDefault(): LayoutPack {
        return runCatching { loadUser() }.getOrElse { loadDefault() }
    }

    fun loadUser(): LayoutPack {
        val raw = userFile.readText()
        return json.decodeFromString(LayoutPack.serializer(), raw)
    }

    fun save(pack: LayoutPack) {
        val serialized = json.encodeToString(LayoutPack.serializer(), pack)
        userDir.mkdirs()
        userFile.writeText(serialized)
        writeBackup(serialized)
    }

    fun load(uri: Uri): LayoutPack {
        context.contentResolver.openInputStream(uri).use { stream ->
            requireNotNull(stream) { "No input stream" }
            return stream.bufferedReader(Charsets.UTF_8).use { reader ->
                json.decodeFromString(LayoutPack.serializer(), reader.readText())
            }
        }
    }

    fun save(pack: LayoutPack, uri: Uri) {
        context.contentResolver.openOutputStream(uri).use { stream ->
            requireNotNull(stream) { "No output stream" }
            stream.bufferedWriter(Charsets.UTF_8).use { writer ->
                json.encodeToString(LayoutPack.serializer(), pack).let { writer.write(it) }
            }
        }
    }

    private fun writeBackup(serialized: String) {
        val formatter = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US)
        val timestamp = formatter.format(Date())
        val backup = File(userDir, "lcars_hacker_en_us.$timestamp.bak")
        backup.writeText(serialized)
    }
}
