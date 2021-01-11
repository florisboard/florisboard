/*
 * Copyright (C) 2020 Patrick Goldinger
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

package dev.patrickgold.florisboard.ime.theme

import android.content.Context
import android.content.res.Configuration
import com.github.michaelbull.result.*
import dev.patrickgold.florisboard.ime.core.PrefHelper
import dev.patrickgold.florisboard.ime.extension.AssetManager
import dev.patrickgold.florisboard.ime.extension.AssetRef
import dev.patrickgold.florisboard.ime.extension.AssetSource
import dev.patrickgold.florisboard.util.TimeUtil
import timber.log.Timber
import java.util.concurrent.CopyOnWriteArrayList

class ThemeManager private constructor(
    private val applicationContext: Context,
    private val assetManager: AssetManager,
    private val prefs: PrefHelper
) {
    private val callbackReceivers: CopyOnWriteArrayList<OnThemeUpdatedListener> = CopyOnWriteArrayList()
    var activeTheme: Theme = Theme.empty()
        private set
    var indexedDayThemeRefs: MutableMap<AssetRef, ThemeMetaOnly> = mutableMapOf()
    var indexedNightThemeRefs: MutableMap<AssetRef, ThemeMetaOnly> = mutableMapOf()
    private var themeCache: MutableMap<AssetRef, Theme> = mutableMapOf()

    companion object {
        private const val THEME_PATH_REL: String = "ime/theme"

        private var defaultInstance: ThemeManager? = null

        fun init(
            applicationContext: Context,
            assetManager: AssetManager,
            prefs: PrefHelper
        ): ThemeManager {
            val instance = ThemeManager(applicationContext, assetManager, prefs)
            defaultInstance = instance
            return instance
        }

        fun default(): ThemeManager {
            val instance = defaultInstance
            if (instance != null) {
                return instance
            } else {
                throw UninitializedPropertyAccessException(
                    "${this::class.simpleName} has not been initialized previously. Make sure to call init(prefs) before using default()."
                )
            }
        }
    }

    init {
        update()
    }

    fun update() {
        indexThemeRefs()
        val ref = evaluateActiveThemeRef()
        Timber.i(ref.toString())
        activeTheme = if (ref == null) {
            Theme.BASE_THEME
        } else {
            loadTheme(ref).getOr(Theme.BASE_THEME)
        }
        Timber.i(activeTheme.label)
        notifyCallbackReceivers()
    }

    fun requestThemeUpdate(onThemeUpdatedListener: OnThemeUpdatedListener): Boolean {
        onThemeUpdatedListener.onThemeUpdated(activeTheme)
        return true
    }

    @Synchronized
    fun registerOnThemeUpdatedListener(onThemeUpdatedListener: OnThemeUpdatedListener): Boolean {
        val ret = callbackReceivers.addIfAbsent(onThemeUpdatedListener)
        onThemeUpdatedListener.onThemeUpdated(activeTheme)
        return ret
    }

    @Synchronized
    fun unregisterOnThemeUpdatedListener(onThemeUpdatedListener: OnThemeUpdatedListener): Boolean {
        return callbackReceivers.remove(onThemeUpdatedListener)
    }

    @Synchronized
    fun notifyCallbackReceivers() {
        callbackReceivers.forEach {
            it.onThemeUpdated(activeTheme)
        }
    }

    private fun loadTheme(ref: AssetRef): Result<Theme, Throwable> {
        val cached = themeCache[ref]
        if (cached != null) {
           return Ok(cached)
        } else {
            assetManager.loadAsset(ref, ThemeJson::class.java).onSuccess { themeJson ->
                val theme = themeJson.toTheme()
                themeCache[ref.copy()] = theme
                return Ok(theme)
            }.onFailure {
                Timber.e(it.toString())
                return Err(it)
            }
        }
        return Err(Exception("Unreachable code"))
    }

    private fun evaluateActiveThemeRef(): AssetRef? {
        Timber.i(prefs.theme.mode.toString())
        Timber.i(prefs.theme.dayThemeRef)
        Timber.i(prefs.theme.nightThemeRef)
        return AssetRef.fromString(when (prefs.theme.mode) {
            ThemeMode.ALWAYS_DAY -> prefs.theme.dayThemeRef
            ThemeMode.ALWAYS_NIGHT -> prefs.theme.nightThemeRef
            ThemeMode.FOLLOW_SYSTEM -> if (applicationContext.resources.configuration.uiMode and
                Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
            ) {
                prefs.theme.nightThemeRef
            } else {
                prefs.theme.dayThemeRef
            }
            ThemeMode.FOLLOW_TIME -> {
                val current = TimeUtil.currentLocalTime()
                val sunrise = TimeUtil.decode(prefs.theme.sunriseTime)
                val sunset = TimeUtil.decode(prefs.theme.sunsetTime)
                if (TimeUtil.isNightTime(sunrise, sunset, current)) {
                    prefs.theme.nightThemeRef
                } else {
                    prefs.theme.dayThemeRef
                }
            }
        }).onFailure { Timber.e(it) }.getOr(null)
    }

    private fun indexThemeRefs() {
        assetManager.listAssets(
            AssetRef(AssetSource.Assets, THEME_PATH_REL),
            ThemeMetaOnly::class.java
        ).onSuccess {
            for ((ref, themeMetaOnly) in it) {
                if (themeMetaOnly.isNightTheme) {
                    indexedNightThemeRefs[ref] = themeMetaOnly
                } else {
                    indexedDayThemeRefs[ref] = themeMetaOnly
                }
            }
        }.onFailure {
            Timber.e(it.toString())
        }
        assetManager.listAssets(
            AssetRef(AssetSource.Internal, THEME_PATH_REL),
            ThemeMetaOnly::class.java
        ).onSuccess {
            for ((ref, themeMetaOnly) in it) {
                if (themeMetaOnly.isNightTheme) {
                    indexedNightThemeRefs[ref] = themeMetaOnly
                } else {
                    indexedDayThemeRefs[ref] = themeMetaOnly
                }
            }
        }.onFailure {
            Timber.e(it.toString())
        }
    }

    fun interface OnThemeUpdatedListener {
        fun onThemeUpdated(theme: Theme)
    }
}
