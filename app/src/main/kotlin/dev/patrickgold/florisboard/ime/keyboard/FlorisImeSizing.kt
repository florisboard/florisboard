/*
 * Copyright (C) 2021-2025 The FlorisBoard Contributors
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

package dev.patrickgold.florisboard.ime.keyboard

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.patrickgold.florisboard.app.FlorisPreferenceStore
import dev.patrickgold.florisboard.ime.smartbar.ExtendedActionsPlacement
import dev.patrickgold.florisboard.ime.smartbar.SmartbarLayout
import dev.patrickgold.florisboard.ime.text.keyboard.TextKeyboard
import dev.patrickgold.florisboard.ime.window.ImeWindowDefaults
import dev.patrickgold.florisboard.ime.window.LocalWindowController
import dev.patrickgold.florisboard.keyboardManager
import dev.patrickgold.jetpref.datastore.model.observeAsState

private val LocalKeyboardRowBaseHeight = compositionLocalOf { 65.dp }
private val LocalSmartbarHeight = compositionLocalOf { 40.dp }

object FlorisImeSizing {
    val keyboardRowBaseHeight: Dp
        @Composable
        @ReadOnlyComposable
        get() = LocalKeyboardRowBaseHeight.current

    val smartbarHeight: Dp
        @Composable
        @ReadOnlyComposable
        get() = LocalSmartbarHeight.current

    @Composable
    fun keyboardUiHeight(): Dp {
        val context = LocalContext.current
        val keyboardManager by context.keyboardManager()
        val evaluator by keyboardManager.activeEvaluator.collectAsState()
        val lastCharactersEvaluator by keyboardManager.lastCharactersEvaluator.collectAsState()
        val rowCount = when (evaluator.keyboard.mode) {
            KeyboardMode.CHARACTERS,
            KeyboardMode.NUMERIC_ADVANCED,
            KeyboardMode.SYMBOLS,
            KeyboardMode.SYMBOLS2 -> lastCharactersEvaluator.keyboard as TextKeyboard
            else -> evaluator.keyboard as TextKeyboard
        }.rowCount.coerceAtLeast(4)
        return (keyboardRowBaseHeight * rowCount)
    }

    @Composable
    fun smartbarUiHeight(): Dp {
        val prefs by FlorisPreferenceStore
        val smartbarEnabled by prefs.smartbar.enabled.observeAsState()
        val smartbarLayout by prefs.smartbar.layout.observeAsState()
        val extendedActionsExpanded by prefs.smartbar.extendedActionsExpanded.observeAsState()
        val extendedActionsPlacement by prefs.smartbar.extendedActionsPlacement.observeAsState()
        val height =
            if (smartbarEnabled) {
                if (smartbarLayout == SmartbarLayout.SUGGESTIONS_ACTIONS_EXTENDED && extendedActionsExpanded &&
                    extendedActionsPlacement != ExtendedActionsPlacement.OVERLAY_APP_UI) {
                    smartbarHeight * 2
                } else {
                    smartbarHeight
                }
            } else {
                0.dp
            }
        return height
    }

    @Composable
    fun imeUiHeight(): Dp {
        return keyboardUiHeight() + smartbarUiHeight()
    }

    object Static {
        var smartbarHeightPx: Int = 0
    }
}

@Deprecated("TODO: move logic fully into ImeWindow impl")
@Composable
fun ProvideKeyboardRowBaseHeight(content: @Composable () -> Unit) {
    val windowController = LocalWindowController.current
    val density = LocalDensity.current

    val windowSpec by windowController.activeWindowSpec.collectAsState()

    val heights by remember {
        derivedStateOf {
            val windowDefaults = ImeWindowDefaults.of(windowSpec.orientation)
            val rowHeight = windowSpec.props.rowHeight
            val smartbarHeight = rowHeight * windowDefaults.smartbarHeightFactor
            rowHeight to smartbarHeight
        }
    }
    val (rowHeight, smartbarHeight) = heights

    SideEffect {
        FlorisImeSizing.Static.smartbarHeightPx = with(density) { smartbarHeight.roundToPx() }
    }

    CompositionLocalProvider(
        LocalKeyboardRowBaseHeight provides rowHeight,
        LocalSmartbarHeight provides smartbarHeight,
    ) {
        content()
    }
}
