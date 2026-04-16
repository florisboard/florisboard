/*
 * Copyright (C) 2025-2026 The FlorisBoard Contributors
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
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.max
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.width
import dev.patrickgold.florisboard.app.FlorisPreferenceModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.florisboard.lib.kotlin.collectIn

/**
 * The window controller is responsible for managing everything related to window config, spec, insets,
 * actions, editor move, and editor resize.
 *
 * This class is designed so it does not contain any references to Android framework classes, allowing it
 * and its inner classes to be unit tested on JVM desktop.
 *
 * @property prefs The preference data store from which the window config is read from / where the window
 *  config is written to.
 * @property scope The coroutine scope in which suspending operations and flow collection should run in.
 */
class ImeWindowController(
    private val prefs: FlorisPreferenceModel,
    val scope: CoroutineScope,
) {
    /**
     * Access to window-related actions.
     */
    val actions = Actions()

    /**
     * Access to window config editor.
     */
    val editor = Editor()

    /**
     * The active root insets describe the size and position of the root window.
     *
     * Typically, the root insets corresponds to the screen bounds, however this is not guaranteed. For
     * consistency reasons, all sub sizes and positions should be derived from the root insets.
     *
     * @see ImeRootWindow
     */
    val activeRootInsets: StateFlow<ImeInsets.Root>
        field = MutableStateFlow(ImeInsets.Root.Zero)

    /**
     * The active window insets describe the size and position of the window within the root window.
     *
     * This value is used for responding to [onComputeInsets] by the accompanying IME service class.
     *
     * May be null. If null, this indicates the window insets are unknown, and no insets will be reported
     * back to the IME service class in this case.
     *
     * @see ImeWindow
     */
    val activeWindowInsets: StateFlow<ImeInsets.Window?>
        field = MutableStateFlow(null)

    /**
     * Holds the active window config for the current type guess based on the root insets.
     */
    val activeWindowConfig: StateFlow<ImeWindowConfig>
        field = MutableStateFlow(ImeWindowConfig.Default)

    /**
     * Holds the active window spec, which is computed based on the root insets and window config.
     */
    val activeWindowSpec: StateFlow<ImeWindowSpec>
        field = MutableStateFlow<ImeWindowSpec>(ImeWindowSpec.Fallback)

    /**
     * Holds the state if the window is currently shown to the user, as reported by the IME service class.
     */
    val isWindowShown: StateFlow<Boolean>
        field = MutableStateFlow(false)

    private val updateConfigMutex = Mutex()

    init {
        combine(
            activeRootInsets,
            prefs.keyboard.windowConfig.asFlow(),
        ) { rootInsets, windowConfigByType ->
            val typeGuess = rootInsets.formFactor.typeGuess
            windowConfigByType[typeGuess] ?: ImeWindowConfig.Default
        }.collectIn(scope) { windowConfig ->
            activeWindowConfig.value = windowConfig
        }

        val userPreferredOptions = combine(
            activeRootInsets,
            prefs.keyboard.keySpacingHorizontal.asFlow(),
            prefs.keyboard.keySpacingVertical.asFlow(),
            prefs.keyboard.fontSizeMultiplierPortrait.asFlow(),
            prefs.keyboard.fontSizeMultiplierLandscape.asFlow(),
        ) { rootInsets, keySpacingFactorH, keySpacingFactorV, multiplierP, multiplierL ->
            // TODO: this should adhere to form factor
            // TODO: font scale needs a rework anyways, change this in font scale rework PR!
            val rootBounds = rootInsets.boundsDp
            ImeWindowSpec.UserPreferredOptions(
                keySpacingFactorH = keySpacingFactorH / 100f,
                keySpacingFactorV = keySpacingFactorV / 100f,
                fontScale = when {
                    rootBounds.width <= rootBounds.height -> multiplierP / 100f
                    else -> multiplierL / 100f
                },
            )
        }

        combine(
            activeRootInsets,
            activeWindowConfig,
            userPreferredOptions,
            editor.version,
        ) { rootInsets, windowConfig, userConfig, _ ->
            doComputeWindowSpec(rootInsets, windowConfig, userConfig)
        }.collectIn(scope) { windowSpec ->
            activeWindowSpec.value = windowSpec
        }
    }

    /**
     * Updates the active root window insets. Should be called exclusively by [ImeRootWindow].
     */
    fun updateRootInsets(newInsets: ImeInsets.Root) {
        activeRootInsets.value = newInsets
    }

    /**
     * Updates the active window insets. Should be called exclusively by [ImeWindow].
     */
    fun updateWindowInsets(newInsets: ImeInsets.Window) {
        activeWindowInsets.value = newInsets
    }

    /**
     * Updates the window config in the preferences. It will only update the window config for the type guess of
     * the active root insets, and will keep other window configs intact.
     *
     * This function is thread-safe under the assumption that:
     * a) The window config pref is only written to by this update function. On other concurrent write accesses
     *    to the underlying pref the behavior is undefined.
     * b) The active root insets do not change while the update in ongoing. A snapshot of the active root insets
     *    will be taken once before any update attempt, and any inset change afterward will not be reflected.
     */
    fun updateWindowConfig(function: (ImeWindowConfig) -> ImeWindowConfig) {
        val rootInsets = activeRootInsets.value
        val typeGuess = rootInsets.formFactor.typeGuess
        scope.launch {
            // not bullet-proof sync, but good enough considering this is only triggered by tap actions
            updateConfigMutex.withLock {
                val newWindowConfig = activeWindowConfig.updateAndGet(function)
                val byType = prefs.keyboard.windowConfig.get()
                prefs.keyboard.windowConfig.set(byType.plus(typeGuess to newWindowConfig))
            }
        }
    }

    /**
     * Called by the accompanying IME service class to request the current window insets.
     *
     * The window controller will honor the request for computation only if it knows where the window is
     * located within the root window. If unknown, no response will be given.
     *
     * The response's touchable insets mode will always be [InputMethodService.Insets.TOUCHABLE_INSETS_REGION],
     * even if the touchable area needs to be fullscreen, or matches the visible/content top. This is due to a
     * bug that affects other modes, where no touch input is propagated to the root window in some cases.
     *
     * If the current window spec is of floating mode, the reported visible/content bounds will be empty. This
     * causes the underlying application to not resize at all when the floating window is shown.
     *
     * @param outInsets The out insets to write the response into.
     * @param isFullscreenInputRequired Flag indicating if, despite the current window config not requiring
     *  it, a fullscreen touch area should be reported back. This can be used e.g. for bottom sheets that
     *  cover a good portion of the screen, or other overlays requiring touch interaction.
     */
    fun onComputeInsets(
        outInsets: InputMethodService.Insets,
        isFullscreenInputRequired: Boolean,
    ) {
        val rootInsets = activeRootInsets.value
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

    fun onConfigurationChanged(@Suppress("UNUSED_PARAMETER") newConfig: Configuration) {
        // As of writing newConfig is unused, but kept for forward-compatibility.
        editor.disable()
    }

    fun onWindowShown(): Boolean {
        return isWindowShown.compareAndSet(expect = false, update = true)
    }

    fun onWindowHidden(): Boolean {
        editor.disable()
        return isWindowShown.compareAndSet(expect = true, update = false)
    }

    private fun doComputeWindowSpec(
        rootInsets: ImeInsets.Root,
        windowConfig: ImeWindowConfig,
        userPreferredOptions: ImeWindowSpec.UserPreferredOptions,
    ): ImeWindowSpec {
        return when (windowConfig.mode) {
            ImeWindowMode.FIXED -> {
                val constraints = ImeWindowConstraints.of(rootInsets, windowConfig.fixedMode)
                val props = (windowConfig.fixedProps[windowConfig.fixedMode] ?: constraints.defaultProps)
                    .constrained(constraints)
                ImeWindowSpec.Fixed(
                    fixedMode = windowConfig.fixedMode,
                    props = props,
                    userPreferredOptions = userPreferredOptions,
                    constraints = constraints,
                )
            }
            ImeWindowMode.FLOATING -> {
                val constraints = ImeWindowConstraints.of(rootInsets, windowConfig.floatingMode)
                val props = (windowConfig.floatingProps[windowConfig.floatingMode] ?: constraints.defaultProps)
                    .constrained(constraints)
                ImeWindowSpec.Floating(
                    floatingMode = windowConfig.floatingMode,
                    props = props,
                    userPreferredOptions = userPreferredOptions,
                    constraints = constraints,
                )
            }
        }
    }

    /**
     * Wrapper class for window-related actions.
     */
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

        private inline fun doCompactLayout(
            crossinline updateProps: (ImeWindowProps.Fixed) -> ImeWindowProps.Fixed,
        ) {
            val rootInsets = activeRootInsets.value
            val constraints = ImeWindowConstraints.of(rootInsets, ImeWindowMode.Fixed.COMPACT)
            updateWindowConfig { config ->
                val props = config.fixedProps[ImeWindowMode.Fixed.COMPACT] ?: constraints.defaultProps
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

        fun resetFixedSize() {
            updateWindowConfig { config ->
                config.copy(
                    fixedProps = config.fixedProps.minus(config.fixedMode),
                )
            }
        }

        fun resetFloatingSize() {
            val rootInsets = activeRootInsets.value
            updateWindowConfig { config ->
                when (config.mode) {
                    ImeWindowMode.FLOATING -> {
                        val constraints = ImeWindowConstraints.of(rootInsets, config.floatingMode)
                        val defaultProps = constraints.defaultProps
                        val newProps = config.floatingProps[config.floatingMode]?.copy(
                            keyboardHeight = defaultProps.keyboardHeight,
                            keyboardWidth = defaultProps.keyboardWidth,
                        ) ?: defaultProps
                        config.copy(
                            floatingProps = config.floatingProps.plus(
                                config.floatingMode to newProps.constrained(constraints)
                            ),
                        )
                    }
                    ImeWindowMode.FIXED -> {
                        config
                    }
                }
            }
        }
    }

    /**
     * The window config editor is the in-window UI for moving and/or resizing the window without having to
     * go to the settings. It both manages the state of the editor, and contains some utility functions.
     */
    inner class Editor {
        /**
         * The state of the editor.
         *
         * @see EditorState
         */
        val state: StateFlow<EditorState>
            field = MutableStateFlow(EditorState.INACTIVE)

        /**
         * The version of the editor. Is used to force spec re-computation.
         */
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

        fun beginMoveGesture(): ImeWindowSpec {
            state.value = EditorState.ACTIVE_MOVE_GESTURE
            return activeWindowSpec.value
        }

        fun endMoveGesture(spec: ImeWindowSpec) {
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

        fun beginResizeGesture(): ImeWindowSpec {
            state.value = EditorState.ACTIVE_RESIZE_GESTURE
            return activeWindowSpec.value
        }

        fun endResizeGesture(spec: ImeWindowSpec) {
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

        fun onSpecUpdated(spec: ImeWindowSpec) {
            if (state.value.isEnabled) {
                activeWindowSpec.value = spec
            }
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

    /**
     * The window config editor state.
     *
     * @property isEnabled If the editor is currently enabled.
     * @property isMoveGesture If an editor move gesture is ongoing. If true, implies that the editor is enabled.
     * @property isResizeGesture If an editor resize gesture is ongoing. If true, implies that the editor is enabled.
     *
     * @see Editor
     */
    enum class EditorState(
        val isEnabled: Boolean = false,
        val isMoveGesture: Boolean = false,
        val isResizeGesture: Boolean = false,
    ) {
        /**
         * The editor is disabled.
         */
        INACTIVE,

        /**
         * The editor is enabled without any ongoing gesture.
         */
        ACTIVE(isEnabled = true),

        /**
         * The editor is enabled and a move gesture is ongoing.
         */
        ACTIVE_MOVE_GESTURE(isEnabled = true, isMoveGesture = true),

        /**
         * The editor is enabled and a resize gesture is ongoing.
         */
        ACTIVE_RESIZE_GESTURE(isEnabled = true, isResizeGesture = true);

        /**
         * Indicates if any editor gesture is ongoing. If true, implies that the editor is enabled.
         */
        val isAnyGesture: Boolean
            get() = isMoveGesture || isResizeGesture
    }
}
