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
import androidx.compose.ui.unit.max
import androidx.compose.ui.unit.min
import dev.patrickgold.florisboard.app.FlorisPreferenceStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.launch
import org.florisboard.lib.kotlin.collectIn

class ImeWindowController(val scope: CoroutineScope) {
    private val prefs by FlorisPreferenceStore

    val actions = Actions()
    val editor = Editor()

    val activeRootInsets: StateFlow<ImeInsets.Root?>
        field = MutableStateFlow(null)

    val activeWindowInsets: StateFlow<ImeInsets.Window?>
        field = MutableStateFlow(null)

    val activeWindowConfig: StateFlow<ImeWindowConfig>
        field = MutableStateFlow(ImeWindowConfig.DefaultPortrait)

    val activeWindowSpec: StateFlow<ImeWindowSpec>
        field = MutableStateFlow<ImeWindowSpec>(ImeWindowConstraints.FallbackSpec)

    val isWindowShown: StateFlow<Boolean>
        field = MutableStateFlow(false)

    init {
        combine(
            activeRootInsets,
            prefs.keyboard.windowConfigPortrait.asFlow(),
            prefs.keyboard.windowConfigLandscape.asFlow(),
        ) { rootInsets, windowConfigP, windowConfigL ->
            when (rootInsets.inferredOrientation) {
                ImeOrientation.PORTRAIT -> windowConfigP
                ImeOrientation.LANDSCAPE -> windowConfigL
            }
        }.collectIn(scope) { windowConfig ->
            activeWindowConfig.value = windowConfig
        }

        val userFontScale = combine(
            activeRootInsets,
            prefs.keyboard.fontSizeMultiplierPortrait.asFlow(),
            prefs.keyboard.fontSizeMultiplierLandscape.asFlow(),
        ) { rootInsets, multiplierP, multiplierL ->
            when (rootInsets.inferredOrientation) {
                ImeOrientation.PORTRAIT -> multiplierP / 100f
                ImeOrientation.LANDSCAPE -> multiplierL / 100f
            }
        }

        combine(
            activeRootInsets,
            activeWindowConfig,
            userFontScale,
            editor.version,
        ) { rootInsets, windowConfig, userFontScale, _ ->
            rootInsets?.let { doComputeWindowSpec(rootInsets, windowConfig, userFontScale) }
        }.collectIn(scope) { windowSpec ->
            windowSpec?.let { activeWindowSpec.value = windowSpec }
        }
    }

    fun updateRootInsets(newInsets: ImeInsets.Root) {
        activeRootInsets.value = newInsets
    }

    fun updateWindowInsets(newInsets: ImeInsets.Window) {
        activeWindowInsets.value = newInsets
    }

    fun updateWindowConfig(function: (ImeWindowConfig) -> ImeWindowConfig) {
        val pref = windowConfigPref(activeRootInsets.value.inferredOrientation)
        val newWindowConfig = activeWindowConfig.updateAndGet(function)
        scope.launch {
            pref.set(newWindowConfig)
        }
    }

    fun onComputeInsets(
        outInsets: InputMethodService.Insets,
        isFullscreenInputRequired: Boolean,
    ) {
        val rootInsets = activeRootInsets.value ?: return
        val windowInsets = activeWindowInsets.value ?: return
        val rootBounds = rootInsets.boundsPx
        val windowBounds = windowInsets.boundsPx
        val windowSpec = activeWindowSpec.value
        val editorState = editor.state.value

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
            isFullscreenInputRequired || editorState.isEnabled -> {
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
        editor.disable()
    }

    fun onWindowShown(): Boolean {
        return isWindowShown.compareAndSet(expect = false, update = true)
    }

    fun onWindowHidden(): Boolean {
        editor.disable()
        return isWindowShown.compareAndSet(expect = true, update = false)
    }

    private fun windowConfigPref(orientation: ImeOrientation) =
        when (orientation) {
            ImeOrientation.PORTRAIT -> prefs.keyboard.windowConfigPortrait
            ImeOrientation.LANDSCAPE -> prefs.keyboard.windowConfigLandscape
        }

    private fun doComputeWindowSpec(
        rootInsets: ImeInsets.Root,
        windowConfig: ImeWindowConfig,
        userFontScale: Float,
    ): ImeWindowSpec {
        return when (windowConfig.mode) {
            ImeWindowMode.FIXED -> {
                val constraints = ImeWindowConstraints.of(windowConfig.fixedMode, rootInsets)
                val props = windowConfig.fixedProps[windowConfig.fixedMode] ?: constraints.defaultProps()
                val fontScale = props.calcFontScale(constraints) * userFontScale
                ImeWindowSpec.Fixed(
                    fixedMode = windowConfig.fixedMode,
                    props = props.constrained(constraints),
                    fontScale = fontScale,
                    constraints = constraints,
                )
            }
            ImeWindowMode.FLOATING -> {
                val constraints = ImeWindowConstraints.of(windowConfig.floatingMode, rootInsets)
                val props = windowConfig.floatingProps[windowConfig.floatingMode] ?: constraints.defaultProps()
                val fontScale = props.calcFontScale(constraints) * userFontScale
                ImeWindowSpec.Floating(
                    floatingMode = windowConfig.floatingMode,
                    props = props.constrained(constraints),
                    fontScale = fontScale,
                    constraints = constraints,
                )
            }
        }
    }

    inner class Actions {
        fun toggleFloatingWindow() {
            updateWindowConfig { config ->
                val newMode = when (config.mode) {
                    ImeWindowMode.FIXED -> ImeWindowMode.FLOATING
                    ImeWindowMode.FLOATING -> ImeWindowMode.FIXED
                }
                config.copy(mode = newMode)
            }
        }

        fun toggleCompactLayout() {
            updateWindowConfig { config ->
                when (config.mode) {
                    ImeWindowMode.FIXED -> {
                        val newFixedMode = when (config.fixedMode) {
                            ImeWindowMode.Fixed.COMPACT -> ImeWindowMode.Fixed.NORMAL
                            else -> ImeWindowMode.Fixed.COMPACT
                        }
                        config.copy(fixedMode = newFixedMode)
                    }
                    ImeWindowMode.FLOATING -> {
                        config.copy(
                            mode = ImeWindowMode.FIXED,
                            fixedMode = ImeWindowMode.Fixed.COMPACT,
                        )
                    }
                }
            }
        }

        fun resetFloatingSize() {
            updateWindowConfig { config ->
                when(config.mode) {
                    ImeWindowMode.FLOATING -> {
                        //TODO: Reset only the sizing and not the position
                        editor.toggleEnabled()
                        config.copy(
                            floatingProps = config.floatingProps.filterNot {
                                it.key == config.floatingMode
                            }
                        )
                    }
                    ImeWindowMode.FIXED -> {
                        config
                    }
                }
            }
        }

        private inline fun doCompactLayout(
            crossinline updateProps: (ImeWindowProps.Fixed) -> ImeWindowProps.Fixed,
        ) {
            val rootInsets = activeRootInsets.value ?: return
            val constraints = ImeWindowConstraints.of(ImeWindowMode.Fixed.COMPACT, rootInsets)
            updateWindowConfig { config ->
                val props = config.fixedProps[ImeWindowMode.Fixed.COMPACT] ?: constraints.defaultProps()
                val newProps = updateProps(props)
                config.copy(
                    mode = ImeWindowMode.FIXED,
                    fixedMode = ImeWindowMode.Fixed.COMPACT,
                    fixedProps = config.fixedProps.plus(ImeWindowMode.Fixed.COMPACT to newProps)
                )
            }
        }

        fun compactLayoutToLeft() {
            doCompactLayout { props ->
                props.copy(
                    paddingLeft = min(props.paddingLeft, props.paddingRight),
                    paddingRight = max(props.paddingLeft, props.paddingRight),
                )
            }
        }

        fun compactLayoutToRight() {
            doCompactLayout { props ->
                props.copy(
                    paddingLeft = max(props.paddingLeft, props.paddingRight),
                    paddingRight = min(props.paddingLeft, props.paddingRight),
                )
            }
        }

        fun compactLayoutFlipSide() {
            doCompactLayout { props ->
                props.copy(
                    paddingLeft = props.paddingRight,
                    paddingRight = props.paddingLeft,
                )
            }
        }
    }

    inner class Editor {
        val state: StateFlow<EditorState>
            field = MutableStateFlow(EditorState.INACTIVE)

        val version: StateFlow<Int>
            field = MutableStateFlow(0)

        private fun syncFromPrefs() {
            version.update { it + 1 }
        }

        fun enable() {
            state.value = EditorState.ACTIVE
            syncFromPrefs()
        }

        fun disable() {
            state.value = EditorState.INACTIVE
            syncFromPrefs()
        }

        fun disableIfNoGestureInProgress() {
            val newState = state.updateAndGet { state ->
                if (state.isAnyGesture) state else EditorState.INACTIVE
            }
            if (!newState.isEnabled) {
                syncFromPrefs()
            }
        }

        fun toggleEnabled() {
            state.update { state ->
                editorStateOf(!state.isEnabled)
            }
            syncFromPrefs()
        }

        fun beginMoveGesture() {
            state.value = EditorState.ACTIVE_MOVE_GESTURE
            syncFromPrefs()
        }

        fun moveBy(offset: DpOffset): DpOffset {
            var consumed = DpOffset.Zero
            activeWindowSpec.update { spec ->
                val (newSpec, newConsumed) = spec.movedBy(offset)
                consumed = newConsumed
                newSpec
            }
            return consumed
        }

        fun endMoveGesture() {
            val spec = activeWindowSpec.value
            var keepEnabled = true
            updateWindowConfig { config ->
                when (spec) {
                     is ImeWindowSpec.Fixed -> {
                         keepEnabled = true
                         config.copy(fixedProps = config.fixedProps.plus(spec.fixedMode to spec.props))
                     }
                    is ImeWindowSpec.Floating -> {
                        if (spec.props.offsetBottom <= spec.constraints.dockToFixedHeight) {
                            keepEnabled = false
                            config.copy(mode = ImeWindowMode.FIXED)
                        } else {
                            keepEnabled = true
                            config.copy(floatingProps = config.floatingProps.plus(spec.floatingMode to spec.props))
                        }
                    }
                }
            }
            state.value = editorStateOf(keepEnabled)
        }

        fun beginResizeGesture() {
            state.value = EditorState.ACTIVE_RESIZE_GESTURE
            syncFromPrefs()
        }

        fun resizeBy(handle: ImeWindowResizeHandle, offset: DpOffset): DpOffset {
            var consumed = DpOffset.Zero
            activeWindowSpec.update { spec ->
                val (newSpec, newConsumed) = spec.resizedBy(handle, offset)
                consumed = newConsumed
                newSpec
            }
            return consumed
        }

        fun endResizeGesture() {
            val spec = activeWindowSpec.value
            var keepEnabled = true
            updateWindowConfig { config ->
                when (spec) {
                    is ImeWindowSpec.Fixed -> {
                        keepEnabled = true
                        config.copy(fixedProps = config.fixedProps.plus(spec.fixedMode to spec.props))
                    }
                    is ImeWindowSpec.Floating -> {
                        keepEnabled = true
                        config.copy(floatingProps = config.floatingProps.plus(spec.floatingMode to spec.props))
                    }
                }
            }
            state.value = editorStateOf(keepEnabled)
        }

        fun cancelGesture() {
            state.value = EditorState.INACTIVE
            syncFromPrefs()
        }

        @Suppress("NOTHING_TO_INLINE")
        private inline fun editorStateOf(isEnabled: Boolean): EditorState {
            return if (isEnabled) {
                EditorState.ACTIVE
            } else {
                EditorState.INACTIVE
            }
        }
    }

    enum class EditorState(
        val isEnabled: Boolean = false,
        val isMoveGesture: Boolean = false,
        val isResizeGesture: Boolean = false,
    ) {
        INACTIVE,
        ACTIVE(isEnabled = true),
        ACTIVE_MOVE_GESTURE(isEnabled = true, isMoveGesture = true),
        ACTIVE_RESIZE_GESTURE(isEnabled = true, isResizeGesture = true);

        val isAnyGesture: Boolean
            get() = isMoveGesture || isResizeGesture
    }
}
