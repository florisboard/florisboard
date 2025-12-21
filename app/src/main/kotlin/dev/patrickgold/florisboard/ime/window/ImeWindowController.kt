/*
 * Copyright (C) 2025 The FlorisBoard Contributors
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

package dev.patrickgold.florisboard.ime.window

import android.content.res.Configuration
import dev.patrickgold.florisboard.app.FlorisPreferenceStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.florisboard.lib.android.isOrientationPortrait
import org.florisboard.lib.kotlin.collectIn

class ImeWindowController(val scope: CoroutineScope) {
    private val prefs by FlorisPreferenceStore

    val activeOrientation: StateFlow<ImeOrientation>
        field = MutableStateFlow(ImeOrientation.PORTRAIT)

    val activeImeInsets: StateFlow<ImeInsets?>
        field = MutableStateFlow(null)

    val activeFontSizeMultiplier: StateFlow<Float>
        field = MutableStateFlow(1.0f)

    val activeWindowConfig: StateFlow<ImeWindowConfig>
        field = MutableStateFlow(ImeWindowConfig.DefaultPortrait)

    val isWindowShown: StateFlow<Boolean>
        field = MutableStateFlow(false)

    init {
        val windowConfigFlow = combine(
            activeOrientation,
            prefs.keyboard.windowConfigPortrait.asFlow(),
            prefs.keyboard.windowConfigLandscape.asFlow(),
        ) { orientation, windowConfigP, windowConfigL ->
            when (orientation) {
                ImeOrientation.PORTRAIT -> windowConfigP
                ImeOrientation.LANDSCAPE -> windowConfigL
            }
        }

        val fontSizeMultiplierFlow = combine(
            activeOrientation,
            prefs.keyboard.fontSizeMultiplierPortrait.asFlow(),
            prefs.keyboard.fontSizeMultiplierLandscape.asFlow(),
        ) { orientation, multiplierP, multiplierL ->
            when (orientation) {
                ImeOrientation.PORTRAIT -> multiplierP / 100f
                ImeOrientation.LANDSCAPE -> multiplierL / 100f
            }
        }

        windowConfigFlow.collectIn(scope) { windowConfig ->
            activeWindowConfig.value = windowConfig
        }

        combine(
            fontSizeMultiplierFlow,
            windowConfigFlow,
        ) { multiplier, windowConfig ->
            // TODO: Scale the fontsize with the keyboard
            multiplier
        }.collectIn(scope) { multiplier ->
            activeFontSizeMultiplier.value = multiplier
        }
    }

    fun updateImeInsets(imeInsets: ImeInsets) {
        activeImeInsets.value = imeInsets
    }

    val tempUpdateGuard = Mutex() // TODO remove once JetPref is updated
    suspend fun updateWindowConfig(function: (ImeWindowConfig) -> ImeWindowConfig) {
        val pref = windowConfigPref(activeOrientation.value)
        // this function is only thread safe assuming the pref is written
        // exclusively by this function!
        // TODO rework JetPref pref data impl to support a thread-safe compareAndSet function
        tempUpdateGuard.withLock {
            pref.set(function(pref.get()))
        }
    }

    fun onConfigurationChanged(newConfig: Configuration) {
        activeOrientation.value = if (newConfig.isOrientationPortrait()) {
            ImeOrientation.PORTRAIT
        } else {
            ImeOrientation.LANDSCAPE
        }
    }

    fun onWindowShown(): Boolean {
        return isWindowShown.compareAndSet(expect = false, update = true)
    }

    fun onWindowHidden(): Boolean {
        return isWindowShown.compareAndSet(expect = true, update = false)
    }

    private fun windowConfigPref(orientation: ImeOrientation) =
        when (orientation) {
            ImeOrientation.PORTRAIT -> prefs.keyboard.windowConfigPortrait
            ImeOrientation.LANDSCAPE -> prefs.keyboard.windowConfigLandscape
        }
}
