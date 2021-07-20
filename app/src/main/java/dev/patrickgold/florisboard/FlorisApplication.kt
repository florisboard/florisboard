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
import android.os.Build
import androidx.core.os.UserManagerCompat
import dev.patrickgold.florisboard.common.NativeStr
import dev.patrickgold.florisboard.common.toNativeStr
import dev.patrickgold.florisboard.crashutility.CrashUtility
import dev.patrickgold.florisboard.debug.Flog
import dev.patrickgold.florisboard.debug.LogTopic
import dev.patrickgold.florisboard.debug.flogError
import dev.patrickgold.florisboard.debug.flogInfo
import dev.patrickgold.florisboard.ime.core.Preferences
import dev.patrickgold.florisboard.ime.core.SubtypeManager
import dev.patrickgold.florisboard.ime.dictionary.DictionaryManager
import dev.patrickgold.florisboard.ime.spelling.SpellingManager
import dev.patrickgold.florisboard.ime.theme.ThemeManager
import dev.patrickgold.florisboard.res.AssetManager
import dev.patrickgold.florisboard.res.FlorisRef
import timber.log.Timber
import java.io.File
import kotlin.Exception

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

    override fun onCreate() {
        super.onCreate()
        try {
            if (BuildConfig.DEBUG) {
                Timber.plant(Timber.DebugTree())
            }
            Flog.install(
                applicationContext = this,
                isFloggingEnabled = BuildConfig.DEBUG,
                flogTopics = LogTopic.ALL,
                flogLevels = Flog.LEVEL_ALL,
                flogOutputs = Flog.OUTPUT_CONSOLE
            )
            initICU()
            CrashUtility.install(this)
            val prefs = Preferences.initDefault(this)
            val assetManager = AssetManager.init(this)
            SpellingManager.init(this, FlorisRef.assets("ime/spelling/config.json"))
            SubtypeManager.init(this)
            DictionaryManager.init(this)
            ThemeManager.init(this, assetManager)
            prefs.initDefaultPreferences()
        } catch (e: Exception) {
            CrashUtility.stageException(e)
            return
        }

        /*Register a receiver so user config can be applied once device protracted storage is available*/
        if(!UserManagerCompat.isUserUnlocked(this) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            registerReceiver(BootComplete(), IntentFilter(Intent.ACTION_USER_UNLOCKED))
        }
    }

    fun initICU(): Boolean {
        try {
            val context = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                createDeviceProtectedStorageContext()
            } else {
                this
            }
            val androidAssetManager = context.assets ?: return false
            val dstDataFile = File(context.cacheDir, "icudt.dat")
            dstDataFile.outputStream().use { os ->
                androidAssetManager.open(ICU_DATA_ASSET_PATH).use { it.copyTo(os) }
            }
            val status = nativeInitICUData(dstDataFile.absolutePath.toNativeStr())
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

    fun init() {
        CrashUtility.install(this)
        val prefs = Preferences.initDefault(this)
        val assetManager = AssetManager.init(this)
        SubtypeManager.init(this)
        DictionaryManager.init(this)
        ThemeManager.init(this, assetManager)
        prefs.initDefaultPreferences()
    }

    private class BootComplete : BroadcastReceiver() {

        override fun onReceive(context: Context?, intent: Intent?) {
            if(Intent.ACTION_USER_UNLOCKED == intent?.action){
                try {
                    (context as FlorisApplication).unregisterReceiver(this)
                    context.init()
                } catch (e : Exception) {
                    e.fillInStackTrace()
                }
            }
        }
    }
}
