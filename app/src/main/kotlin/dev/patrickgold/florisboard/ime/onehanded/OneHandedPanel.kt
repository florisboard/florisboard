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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ZoomOutMap
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.app.florisPreferenceModel
import dev.patrickgold.florisboard.ime.input.LocalInputFeedbackController
import dev.patrickgold.florisboard.ime.keyboard.FlorisImeSizing
import dev.patrickgold.florisboard.ime.theme.FlorisImeTheme
import dev.patrickgold.florisboard.ime.theme.FlorisImeUi
import dev.patrickgold.florisboard.lib.compose.stringRes
import org.florisboard.lib.snygg.ui.snyggBackground
import org.florisboard.lib.snygg.ui.solidColor

@Composable
fun RowScope.OneHandedPanel(
    modifier: Modifier = Modifier,
    panelSide: OneHandedMode,
    weight: Float,
) {
    val prefs by florisPreferenceModel()
    val inputFeedbackController = LocalInputFeedbackController.current
    val oneHandedPanelStyle = FlorisImeTheme.style.get(FlorisImeUi.OneHandedPanel)
    val context = LocalContext.current

    Column(
        modifier = modifier
            .weight(weight)
            .snyggBackground(context, oneHandedPanelStyle),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly,
    ) {
        IconButton(
            onClick = {
                inputFeedbackController.keyPress()
                prefs.keyboard.oneHandedMode.set(OneHandedMode.OFF)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Default.ZoomOutMap,
                contentDescription = stringRes(R.string.one_handed__close_btn_content_description),
                tint = oneHandedPanelStyle.foreground.solidColor(context),
            )
        }
        IconButton(
            onClick = {
                inputFeedbackController.keyPress()
                prefs.keyboard.oneHandedMode.set(panelSide)
            },
            modifier = Modifier.height(FlorisImeSizing.keyboardUiHeight()).fillMaxWidth()
        ) {
            Icon(
                imageVector = if (panelSide == OneHandedMode.START) {
                    Icons.AutoMirrored.Filled.KeyboardArrowLeft
                } else {
                    Icons.AutoMirrored.Filled.KeyboardArrowRight
                },
                contentDescription = stringRes(
                    if (panelSide == OneHandedMode.START) {
                        R.string.one_handed__move_start_btn_content_description
                    } else {
                        R.string.one_handed__move_end_btn_content_description
                    }
                ),
                tint = oneHandedPanelStyle.foreground.solidColor(context),
            )
        }
    }
}
