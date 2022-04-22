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

package dev.patrickgold.florisboard

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.os.UserManagerCompat
import dev.patrickgold.florisboard.app.florisPreferenceModel
import dev.patrickgold.florisboard.ime.clipboard.ClipboardManager
import dev.patrickgold.florisboard.ime.core.SubtypeManager
import dev.patrickgold.florisboard.ime.dictionary.DictionaryManager
import dev.patrickgold.florisboard.ime.editor.EditorInstance
import dev.patrickgold.florisboard.ime.keyboard.KeyboardManager
import dev.patrickgold.florisboard.ime.nlp.NlpManager
import dev.patrickgold.florisboard.ime.spelling.SpellingManager
import dev.patrickgold.florisboard.ime.spelling.SpellingService
import dev.patrickgold.florisboard.ime.text.gestures.GlideTypingManager
import dev.patrickgold.florisboard.ime.theme.ThemeManager
import dev.patrickgold.florisboard.lib.NativeStr
import dev.patrickgold.florisboard.lib.cache.CacheManager
import dev.patrickgold.florisboard.lib.crashutility.CrashUtility
import dev.patrickgold.florisboard.lib.devtools.Flog
import dev.patrickgold.florisboard.lib.devtools.LogTopic
import dev.patrickgold.florisboard.lib.devtools.flogError
import dev.patrickgold.florisboard.lib.devtools.flogInfo
import dev.patrickgold.florisboard.lib.ext.ExtensionManager
import dev.patrickgold.florisboard.lib.io.AssetManager
import dev.patrickgold.florisboard.lib.io.deleteContentsRecursively
import dev.patrickgold.florisboard.lib.toNativeStr
import dev.patrickgold.jetpref.datastore.JetPref
import java.io.File

@Suppress("unused")
class FlorisApplication : Application() {
    companion object {
        private const val ICU_DATA_ASSET_PATH = "icu/icudt69l.dat"

        private external fun nativeInitICUData(path: NativeStr): Int

        init {
            try {
                System.loadLibrary("florisboard-native")
            } catch (_: Exception) {
            }
        }
    }

    private val prefs by florisPreferenceModel()

    val assetManager = lazy { AssetManager(this) }
    val cacheManager = lazy { CacheManager(this) }
    val clipboardManager = lazy { ClipboardManager(this) }
    val editorInstance = lazy { EditorInstance(this) }
    val extensionManager = lazy { ExtensionManager(this) }
    val glideTypingManager = lazy { GlideTypingManager(this) }
    val keyboardManager = lazy { KeyboardManager(this) }
    val nlpManager = lazy { NlpManager(this) }
    val spellingManager = lazy { SpellingManager(this) }
    val spellingService = lazy { SpellingService(this) }
    val subtypeManager = lazy { SubtypeManager(this) }
    val themeManager = lazy { ThemeManager(this) }

    override fun onCreate() {
        super.onCreate()
        try {
            JetPref.configure(saveIntervalMs = 500)
            Flog.install(
                context = this,
                isFloggingEnabled = BuildConfig.DEBUG,
                flogTopics = LogTopic.ALL,
                flogLevels = Flog.LEVEL_ALL,
                flogOutputs = Flog.OUTPUT_CONSOLE,
            )
            CrashUtility.install(this)

            if (!UserManagerCompat.isUserUnlocked(this)) {
                val context = createDeviceProtectedStorageContext()
                initICU(context)
                prefs.initializeBlocking(context)
                registerReceiver(BootComplete(), IntentFilter(Intent.ACTION_USER_UNLOCKED))
            } else {
                initICU(this)
                cacheDir?.deleteContentsRecursively()
                prefs.initializeBlocking(this)
                clipboardManager.value.initializeForContext(this)
            }

            DictionaryManager.init(this)
        } catch (e: Exception) {
            CrashUtility.stageException(e)
            return
        }
    }

    fun initICU(context: Context): Boolean {
        try {
            val androidAssetManager = context.assets ?: return false
            val icuTmpDataFile = File(context.cacheDir, "icudt.dat")
            icuTmpDataFile.outputStream().use { os ->
                androidAssetManager.open(ICU_DATA_ASSET_PATH).use { it.copyTo(os) }
            }
            val status = nativeInitICUData(icuTmpDataFile.absolutePath.toNativeStr())
            icuTmpDataFile.delete()
            return if (status != 0) {
                flogError { "Native ICU data initializing failed with error code $status!" }
                false
            } else {
                flogInfo { "Successfully loaded ICU data!" }
                true
            }
        } catch (e: Exception) {
            flogError { e.toString() }
            return false
        }
    }

    private inner class BootComplete : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent == null) return
            if (intent.action == Intent.ACTION_USER_UNLOCKED) {
                try {
                    unregisterReceiver(this)
                } catch (e: Exception) {
                    flogError { e.toString() }
                }
                cacheDir?.deleteContentsRecursively()
                prefs.initializeBlocking(this@FlorisApplication)
                clipboardManager.value.initializeForContext(this@FlorisApplication)
            }
        }
    }
}

private fun Context.florisApplication(): FlorisApplication {
    return when (this) {
        is FlorisApplication -> this
        else -> this.applicationContext as FlorisApplication
    }
}

fun Context.appContext() = lazy { this.florisApplication() }

fun Context.assetManager() = lazy { this.florisApplication().assetManager.value }

fun Context.cacheManager() = lazy { this.florisApplication().cacheManager.value }

fun Context.clipboardManager() = lazy { this.florisApplication().clipboardManager.value }

fun Context.editorInstance() = lazy { this.florisApplication().editorInstance.value }

fun Context.extensionManager() = lazy { this.florisApplication().extensionManager.value }

fun Context.glideTypingManager() = lazy { this.florisApplication().glideTypingManager.value }

fun Context.keyboardManager() = lazy { this.florisApplication().keyboardManager.value }

fun Context.nlpManager() = lazy { this.florisApplication().nlpManager.value }

fun Context.spellingManager() = lazy { this.florisApplication().spellingManager.value }

fun Context.spellingService() = lazy { this.florisApplication().spellingService.value }

fun Context.subtypeManager() = lazy { this.florisApplication().subtypeManager.value }

fun Context.themeManager() = lazy { this.florisApplication().themeManager.value }
