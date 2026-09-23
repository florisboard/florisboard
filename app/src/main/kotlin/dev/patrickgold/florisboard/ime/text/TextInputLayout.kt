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

package dev.patrickgold.florisboard.ime.text

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.app.FlorisPreferenceStore
import dev.patrickgold.florisboard.ime.keyboard3.LocalImeController
import dev.patrickgold.florisboard.ime.keyboard3.ui.ImeKeyboardBox
import dev.patrickgold.florisboard.ime.smartbar.IncognitoDisplayMode
import dev.patrickgold.florisboard.ime.smartbar.InlineSuggestionsStyleCache
import dev.patrickgold.florisboard.ime.smartbar.Smartbar
import dev.patrickgold.florisboard.ime.smartbar.quickaction.QuickActionsOverflowPanel
import dev.patrickgold.florisboard.ime.theme.FlorisImeUi
import dev.patrickgold.jetpref.datastore.model.collectAsState
import org.florisboard.lib.snygg.ui.SnyggIcon

@Composable
fun TextInputLayout(
    modifier: Modifier = Modifier,
) {
    val prefs by FlorisPreferenceStore
    val imeController = LocalImeController.current

    val imeState by imeController.activeState.collectAsState()
    val isActionsOverflowVisible by remember {
        derivedStateOf { imeState.flags.isActionsOverflowVisible }
    }
    val isIncognitoMode by remember {
        derivedStateOf { imeState.flags.isIncognitoMode }
    }

    InlineSuggestionsStyleCache()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight(),
    ) {
        Smartbar()
        if (isActionsOverflowVisible) {
            QuickActionsOverflowPanel()
        } else {
            Box {
                val incognitoDisplayMode by prefs.keyboard.incognitoDisplayMode.collectAsState()
                val showIncognitoIcon = isIncognitoMode &&
                    incognitoDisplayMode == IncognitoDisplayMode.DISPLAY_BEHIND_KEYBOARD
                if (showIncognitoIcon) {
                    SnyggIcon(
                        FlorisImeUi.IncognitoModeIndicator.elementName,
                        modifier = Modifier
                            .matchParentSize()
                            .align(Alignment.Center),
                        painter = painterResource(R.drawable.ic_incognito),
                    )
                }
                ImeKeyboardBox()
            }
        }
    }
}
