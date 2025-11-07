/*
 * Copyright (C) 2021-2025 The OmniBoard Contributors
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

package dev.silo.omniboard

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.IntentFilter
import android.os.Handler
import android.util.Log
import androidx.core.os.UserManagerCompat
import dev.silo.omniboard.app.OmniPreferenceModel
import dev.silo.omniboard.app.OmniPreferenceStore
import dev.silo.omniboard.ime.clipboard.ClipboardManager
import dev.silo.omniboard.ime.core.SubtypeManager
import dev.silo.omniboard.ime.dictionary.DictionaryManager
import dev.silo.omniboard.ime.editor.EditorInstance
import dev.silo.omniboard.ime.keyboard.KeyboardManager
import dev.silo.omniboard.ime.media.emoji.OmniEmojiCompat
import dev.silo.omniboard.ime.nlp.NlpManager
import dev.silo.omniboard.ime.text.gestures.GlideTypingManager
import dev.silo.omniboard.ime.theme.ThemeManager
import dev.silo.omniboard.lib.cache.CacheManager
import dev.silo.omniboard.lib.crashutility.CrashUtility
import dev.silo.omniboard.lib.devtools.Flog
import dev.silo.omniboard.lib.devtools.LogTopic
import dev.silo.omniboard.lib.devtools.flogError
import dev.silo.omniboard.lib.ext.ExtensionManager
import dev.silo.jetpref.datastore.runtime.initAndroid
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import org.omniboard.lib.kotlin.io.deleteContentsRecursively
import org.omniboard.lib.kotlin.tryOrNull
import org.omniboard.libnative.dummyAdd
import java.lang.ref.WeakReference

/**
 * Global weak reference for the [OmniApplication] class. This is needed as in certain scenarios an application
 * reference is needed, but the Android framework hasn't finished setting up
 */
private var OmniApplicationReference = WeakReference<OmniApplication?>(null)

@Suppress("unused")
class OmniApplication : Application() {
    companion object {
        init {
            try {
                System.loadLibrary("fl_native")
            } catch (_: Exception) {
            }
        }
    }

    private val mainHandler by lazy { Handler(mainLooper) }
    private val scope = CoroutineScope(Dispatchers.Default)
    val preferenceStoreLoaded = MutableStateFlow(false)

    val cacheManager = lazy { CacheManager(this) }
    val clipboardManager = lazy { ClipboardManager(this) }
    val editorInstance = lazy { EditorInstance(this) }
    val extensionManager = lazy { ExtensionManager(this) }
    val glideTypingManager = lazy { GlideTypingManager(this) }
    val keyboardManager = lazy { KeyboardManager(this) }
    val nlpManager = lazy { NlpManager(this) }
    val subtypeManager = lazy { SubtypeManager(this) }
    val themeManager = lazy { ThemeManager(this) }

    override fun onCreate() {
        super.onCreate()
        OmniApplicationReference = WeakReference(this)
        try {
            Flog.install(
                context = this,
                isFloggingEnabled = BuildConfig.DEBUG,
                flogTopics = LogTopic.ALL,
                flogLevels = Flog.LEVEL_ALL,
                flogOutputs = Flog.OUTPUT_CONSOLE,
            )
            CrashUtility.install(this)
            OmniEmojiCompat.init(this)
            flogError { "dummy result: ${dummyAdd(3,4)}" }

            if (!UserManagerCompat.isUserUnlocked(this)) {
                cacheDir?.deleteContentsRecursively()
                extensionManager.value.init()
                registerReceiver(BootComplete(), IntentFilter(Intent.ACTION_USER_UNLOCKED))
                return
            }

            init()
        } catch (e: Exception) {
            CrashUtility.stageException(e)
            return
        }
    }

    fun init() {
        cacheDir?.deleteContentsRecursively()
        scope.launch {
            val result = OmniPreferenceStore.initAndroid(
                context = this@OmniApplication,
                datastoreName = OmniPreferenceModel.NAME,
            )
            Log.i("PREFS", result.toString())
            preferenceStoreLoaded.value = true
        }
        extensionManager.value.init()
        clipboardManager.value.initializeForContext(this)
        DictionaryManager.init(this)
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
                mainHandler.post { init() }
            }
        }
    }
}

private tailrec fun Context.omniApplication(): OmniApplication {
    return when (this) {
        is OmniApplication -> this
        is ContextWrapper -> when {
            this.baseContext != null -> this.baseContext.omniApplication()
            else -> OmniApplicationReference.get()!!
        }
        else -> tryOrNull { this.applicationContext as OmniApplication } ?: OmniApplicationReference.get()!!
    }
}

fun Context.appContext() = lazyOf(this.omniApplication())

fun Context.cacheManager() = this.omniApplication().cacheManager

fun Context.clipboardManager() = this.omniApplication().clipboardManager

fun Context.editorInstance() = this.omniApplication().editorInstance

fun Context.extensionManager() = this.omniApplication().extensionManager

fun Context.glideTypingManager() = this.omniApplication().glideTypingManager

fun Context.keyboardManager() = this.omniApplication().keyboardManager

fun Context.nlpManager() = this.omniApplication().nlpManager

fun Context.subtypeManager() = this.omniApplication().subtypeManager

fun Context.themeManager() = this.omniApplication().themeManager
