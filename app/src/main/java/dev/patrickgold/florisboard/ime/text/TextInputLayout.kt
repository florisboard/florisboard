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

package dev.patrickgold.florisboard.ime.text

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.viewinterop.AndroidView
import dev.patrickgold.florisboard.app.prefs.florisPreferenceModel
import dev.patrickgold.florisboard.common.observeAsNonNullState
import dev.patrickgold.florisboard.ime.keyboard.LocalInputFeedbackController
import dev.patrickgold.florisboard.ime.keyboard.LocalKeyboardRowBaseHeight
import dev.patrickgold.florisboard.ime.text.keyboard.TextKeyboardView
import dev.patrickgold.florisboard.ime.text.smartbar.Smartbar
import dev.patrickgold.florisboard.keyboardManager
import dev.patrickgold.florisboard.subtypeManager

@Composable
fun TextInputLayout() = Column {
    val context = LocalContext.current
    val prefs by florisPreferenceModel()
    val keyboardManager by context.keyboardManager()
    val subtypeManager by context.subtypeManager()

    val activeState by keyboardManager.activeState.observeAsNonNullState()
    val computedKeyboard by keyboardManager.computedKeyboard.observeAsNonNullState()
    val keyboardRowBaseHeight = LocalKeyboardRowBaseHeight.current
    val keyboardRowBaseHeightPx = with(LocalDensity.current) { keyboardRowBaseHeight.toPx() }
    val inputFeedbackController = LocalInputFeedbackController.current

    Smartbar()
    AndroidView(
        modifier = Modifier.fillMaxWidth().wrapContentHeight(),
        factory = { ctx -> TextKeyboardView(ctx).also { view ->
            view.setComputingEvaluator(keyboardManager.computingEvaluator)
            view.inputFeedbackController = inputFeedbackController
        } },
        update = { view ->
            view.keyboardRowBaseHeight = keyboardRowBaseHeightPx
            view.onUpdateKeyboardState(activeState)
            view.setComputedKeyboard(computedKeyboard)
            view.sync()
            view.requestLayout()
        },
    )
}
