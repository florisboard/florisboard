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
import android.inputmethodservice.InputMethodService
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.coerceIn
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import dev.patrickgold.florisboard.app.FlorisPreferenceStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.florisboard.lib.android.isOrientationPortrait
import org.florisboard.lib.kotlin.collectIn

class ImeWindowController(val scope: CoroutineScope) {
    private val prefs by FlorisPreferenceStore

    val resizeMode = ResizeMode()

    val activeRootInsets: StateFlow<ImeInsets>
        field = MutableStateFlow(ImeInsets.Infinite)

    val activeWindowInsets: StateFlow<ImeInsets>
        field = MutableStateFlow(ImeInsets.Zero)

    val activeOrientation: StateFlow<ImeOrientation>
        field = MutableStateFlow(ImeOrientation.PORTRAIT)

    val activeFontSizeMultiplier: StateFlow<Float>
        field = MutableStateFlow(1.0f)

    val activeWindowSpec: StateFlow<ImeWindowSpec>
        field = MutableStateFlow<ImeWindowSpec>(ImeWindowDefaults.FallbackSpec)

    val isWindowShown: StateFlow<Boolean>
        field = MutableStateFlow(false)

    init {
        val prefsWindowConfig = combine(
            activeOrientation,
            prefs.keyboard.windowConfigPortrait.asFlow(),
            prefs.keyboard.windowConfigLandscape.asFlow(),
        ) { orientation, windowConfigP, windowConfigL ->
            when (orientation) {
                ImeOrientation.PORTRAIT -> windowConfigP
                ImeOrientation.LANDSCAPE -> windowConfigL
            }
        }

        val prefsFontSizeMultiplier = combine(
            activeOrientation,
            prefs.keyboard.fontSizeMultiplierPortrait.asFlow(),
            prefs.keyboard.fontSizeMultiplierLandscape.asFlow(),
        ) { orientation, multiplierP, multiplierL ->
            when (orientation) {
                ImeOrientation.PORTRAIT -> multiplierP / 100f
                ImeOrientation.LANDSCAPE -> multiplierL / 100f
            }
        }

        combine(
            activeRootInsets,
            activeOrientation,
            prefsWindowConfig,
            resizeMode.version,
        ) { rootInsets, orientation, windowConfig, _ ->
            doComputeWindowSpec(rootInsets, orientation, windowConfig)
        }.collectIn(scope) { windowSpec ->
            activeWindowSpec.value = windowSpec
        }

        combine(
            prefsWindowConfig,
            prefsFontSizeMultiplier,
        ) { windowConfig, multiplier ->
            // TODO: Scale the fontsize with the keyboard
            multiplier
        }.collectIn(scope) { multiplier ->
            activeFontSizeMultiplier.value = multiplier
        }
    }

    fun updateRootInsets(newInsets: ImeInsets) {
        activeRootInsets.value = newInsets
    }

    fun updateWindowInsets(newInsets: ImeInsets) {
        activeWindowInsets.value = newInsets
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

    fun onComputeInsets(
        outInsets: InputMethodService.Insets,
        isFullscreenInputRequired: Boolean,
    ) {
        val rootBounds = activeRootInsets.value.boundsPx
        val windowBounds = activeWindowInsets.value.boundsPx
        val windowSpec = activeWindowSpec.value

        when (windowSpec) {
            is ImeWindowSpec.Fixed -> {
                outInsets.contentTopInsets = windowBounds.top
                outInsets.visibleTopInsets = windowBounds.top
            }
            is ImeWindowSpec.Floating -> {
                outInsets.contentTopInsets = rootBounds.bottom
                outInsets.visibleTopInsets = rootBounds.bottom
            }
        }
        when {
            isFullscreenInputRequired -> {
                outInsets.touchableRegion.set(
                    rootBounds.left,
                    rootBounds.top,
                    rootBounds.right,
                    rootBounds.bottom,
                )
            }
            else -> {
                outInsets.touchableRegion.set(
                    windowBounds.left,
                    windowBounds.top,
                    windowBounds.right,
                    windowBounds.bottom,
                )
            }
        }
        outInsets.touchableInsets = InputMethodService.Insets.TOUCHABLE_INSETS_REGION
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

    private fun doComputeWindowSpec(
        rootInsets: ImeInsets,
        orientation: ImeOrientation,
        windowConfig: ImeWindowConfig,
    ): ImeWindowSpec {
        return when (windowConfig.mode) {
            ImeWindowMode.FIXED -> {
                val props = windowConfig.getFixedPropsOrDefault(orientation)
                ImeWindowSpec.Fixed(
                    mode = windowConfig.fixedMode,
                    props = props.constrained(rootInsets),
                    rootInsets = rootInsets,
                )
            }
            ImeWindowMode.FLOATING -> {
                val props = windowConfig.getFloatingPropsOrDefault(orientation)
                ImeWindowSpec.Floating(
                    mode = windowConfig.floatingMode,
                    props = props.constrained(rootInsets),
                    rootInsets = rootInsets,
                )
            }
        }
    }

    private fun ImeWindowProps.Fixed.constrained(
        rootInsets: ImeInsets,
    ): ImeWindowProps.Fixed = let { props ->
        val rootBounds = rootInsets.boundsDp
        // TODO
        return props
    }

    private fun ImeWindowProps.Floating.constrained(
        rootInsets: ImeInsets,
    ): ImeWindowProps.Floating = let { props ->
        val rootBounds = rootInsets.boundsDp
        val rowHeight = props.rowHeight.coerceIn(
            minimumValue = (rootBounds.height / (3f * ImeWindowDefaults.KeyboardHeightFactor)),
            maximumValue = (rootBounds.height / (2f * ImeWindowDefaults.KeyboardHeightFactor)),
        )
        val keyboardWidth = props.keyboardWidth.coerceIn(
            minimumValue = ImeWindowDefaults.MinKeyboardWidth,
            maximumValue = rootBounds.width,
        )
        val offsetLeft = props.offsetLeft.coerceIn(
            minimumValue = 0.dp,
            maximumValue = rootBounds.width - keyboardWidth,
        )
        val offsetBottom = props.offsetBottom.coerceIn(
            minimumValue = 0.dp,
            maximumValue = rootBounds.height - (rowHeight * ImeWindowDefaults.KeyboardHeightFactor),
        )
        return ImeWindowProps.Floating(rowHeight, keyboardWidth, offsetLeft, offsetBottom)
    }

    inner class ResizeMode {
        val active: StateFlow<Boolean>
            field = MutableStateFlow(false)

        val version: StateFlow<Int>
            field = MutableStateFlow(0)

        fun start() {
            active.value = true
            version.update { it + 1 }
        }

        fun moveBy(offset: DpOffset) {
            activeWindowSpec.update { spec ->
                when (spec) {
                    is ImeWindowSpec.Fixed -> {
                        // TODO: moving in fixed means adjusting the paddings really
                        spec
                    }
                    is ImeWindowSpec.Floating -> {
                        val newProps = spec.props.copy(
                            offsetLeft = spec.props.offsetLeft + offset.x,
                            offsetBottom = spec.props.offsetBottom - offset.y,
                        )
                        spec.copy(props = newProps.constrained(spec.rootInsets))
                    }
                }
            }
        }

        fun end() {
            val spec = activeWindowSpec.value
            active.value = false
            scope.launch { // TODO once updateWindowConfig is fixed no coroutine scope should be needed here
                updateWindowConfig { config ->
                    when (spec) {
                         is ImeWindowSpec.Fixed -> {
                             config.copy(fixedProps = config.fixedProps.plus(spec.mode to spec.props))
                         }
                        is ImeWindowSpec.Floating -> {
                            config.copy(floatingProps = config.floatingProps.plus(spec.mode to spec.props))
                        }
                    }
                }
            }
        }

        fun cancel() {
            active.value = false
            version.update { it + 1 }
        }
    }
}
