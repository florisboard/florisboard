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

package dev.patrickgold.florisboard.ime.onehanded

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.app.florisPreferenceModel
import dev.patrickgold.florisboard.ime.input.LocalInputFeedbackController
import dev.patrickgold.florisboard.ime.theme.FlorisImeTheme
import dev.patrickgold.florisboard.ime.theme.FlorisImeUi
import dev.patrickgold.florisboard.lib.compose.stringRes
import dev.patrickgold.florisboard.lib.snygg.ui.snyggBackground
import dev.patrickgold.florisboard.lib.snygg.ui.solidColor

@Composable
fun RowScope.OneHandedPanel(
    modifier: Modifier = Modifier,
    panelSide: OneHandedMode,
    weight: Float,
) {
    val prefs by florisPreferenceModel()
    val inputFeedbackController = LocalInputFeedbackController.current
    val oneHandedPanelStyle = FlorisImeTheme.style.get(FlorisImeUi.OneHandedPanel)
    Column(
        modifier = modifier
            .weight(weight)
            .snyggBackground(oneHandedPanelStyle),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly,
    ) {
        IconButton(onClick = {
            inputFeedbackController.keyPress()
            prefs.keyboard.oneHandedMode.set(OneHandedMode.OFF)
        }) {
            Icon(
                painter = painterResource(R.drawable.ic_zoom_out_map),
                contentDescription = stringRes(R.string.one_handed__close_btn_content_description),
                tint = oneHandedPanelStyle.foreground.solidColor(),
            )
        }
        IconButton(onClick = {
            inputFeedbackController.keyPress()
            prefs.keyboard.oneHandedMode.set(panelSide)
        }) {
            Icon(
                painter = painterResource(
                    if (panelSide == OneHandedMode.START) {
                        R.drawable.ic_keyboard_arrow_left
                    } else {
                        R.drawable.ic_keyboard_arrow_right
                    }
                ),
                contentDescription = stringRes(
                    if (panelSide == OneHandedMode.START) {
                        R.string.one_handed__move_start_btn_content_description
                    } else {
                        R.string.one_handed__move_end_btn_content_description
                    }
                ),
                tint = oneHandedPanelStyle.foreground.solidColor(),
            )
        }
    }
}
