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
import android.util.Log
import dev.patrickgold.florisboard.crashutility.CrashUtility
import dev.patrickgold.florisboard.debug.Flog
import dev.patrickgold.florisboard.debug.LogTopic
import dev.patrickgold.florisboard.ime.core.Preferences
import dev.patrickgold.florisboard.ime.core.SubtypeManager
import dev.patrickgold.florisboard.ime.dictionary.DictionaryManager
import dev.patrickgold.florisboard.ime.spelling.SpellingManager
import dev.patrickgold.florisboard.ime.theme.ThemeManager
import dev.patrickgold.florisboard.res.AssetManager
import dev.patrickgold.florisboard.res.FlorisRef
import timber.log.Timber
import java.util.*

@Suppress("unused")
class FlorisApplication : Application() {
    companion object {
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
            for (locale in Locale.getAvailableLocales()) {
                Log.d(
                    "LOCALES",
                    locale.getLanguage().toString() + "_" + locale.getCountry() + " [" + locale.getDisplayName() + "]"
                )
            }
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
    }
}
