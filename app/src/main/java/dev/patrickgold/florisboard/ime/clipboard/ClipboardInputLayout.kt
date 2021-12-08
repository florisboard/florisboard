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

package dev.patrickgold.florisboard.ime.clipboard

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.app.prefs.florisPreferenceModel
import dev.patrickgold.florisboard.app.res.stringRes
import dev.patrickgold.florisboard.app.ui.components.FlorisIconButton
import dev.patrickgold.florisboard.app.ui.theme.Green500
import dev.patrickgold.florisboard.ime.ImeUiMode
import dev.patrickgold.florisboard.ime.keyboard.FlorisImeSizing
import dev.patrickgold.florisboard.ime.text.smartbar.SecondaryRowPlacement
import dev.patrickgold.florisboard.ime.theme.FlorisImeTheme
import dev.patrickgold.florisboard.ime.theme.FlorisImeUi
import dev.patrickgold.florisboard.keyboardManager
import dev.patrickgold.florisboard.snygg.ui.SnyggSurface
import dev.patrickgold.florisboard.snygg.ui.snyggBackground
import dev.patrickgold.florisboard.snygg.ui.solidColor
import dev.patrickgold.florisboard.snygg.ui.spSize
import dev.patrickgold.jetpref.datastore.model.observeAsState

private val HeaderIconPadding = PaddingValues(horizontal = 4.dp)
private val ContentPadding = PaddingValues(horizontal = 4.dp)
private val ItemMargin = PaddingValues(all = 6.dp)
private val ItemPadding = PaddingValues(vertical = 8.dp, horizontal = 12.dp)

@Composable
fun ClipboardInputLayout(
    modifier: Modifier = Modifier,
) {
    val prefs by florisPreferenceModel()
    val context = LocalContext.current
    val keyboardManager by context.keyboardManager()

    val activeState by keyboardManager.observeActiveState()
    val historyEnabled by prefs.clipboard.historyEnabled.observeAsState()

    val smartbarEnabled by prefs.smartbar.enabled.observeAsState()
    val secondaryRowEnabled by prefs.smartbar.secondaryRowEnabled.observeAsState()
    val secondaryRowExpanded by prefs.smartbar.secondaryRowExpanded.observeAsState()
    val secondaryRowPlacement by prefs.smartbar.secondaryRowPlacement.observeAsState()
    val innerHeight =
        if (smartbarEnabled && secondaryRowEnabled && secondaryRowExpanded &&
            secondaryRowPlacement != SecondaryRowPlacement.OVERLAY_APP_UI
        ) {
            FlorisImeSizing.smartbarHeight
        } else {
            0.dp
        } + (FlorisImeSizing.keyboardRowBaseHeight * 4)

    val headerStyle = FlorisImeTheme.style.get(FlorisImeUi.ClipboardHeader)
    val itemStyle = FlorisImeTheme.style.get(FlorisImeUi.ClipboardItem)
    val popupStyle = FlorisImeTheme.style.get(FlorisImeUi.ClipboardItemPopup)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(FlorisImeSizing.smartbarHeight)
                .snyggBackground(headerStyle.background),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FlorisIconButton(
                modifier = Modifier
                    .padding(HeaderIconPadding)
                    .fillMaxHeight()
                    .aspectRatio(1f),
                iconId = R.drawable.ic_arrow_back,
                iconColor = headerStyle.foreground.solidColor(),
                onClick = { activeState.imeUiMode = ImeUiMode.TEXT },
            )
            Text(
                modifier = Modifier.weight(1f),
                text = stringRes(R.string.clipboard__header_title),
                color = headerStyle.foreground.solidColor(),
                fontSize = headerStyle.fontSize.spSize(),
            )
            FlorisIconButton(
                modifier = Modifier
                    .padding(HeaderIconPadding)
                    .fillMaxHeight()
                    .aspectRatio(1f),
                iconId = if (historyEnabled) {
                    R.drawable.ic_toggle_on
                } else {
                    R.drawable.ic_toggle_off
                },
                iconColor = headerStyle.foreground.solidColor(),
                onClick = { prefs.clipboard.historyEnabled.set(!historyEnabled) },
            )
            FlorisIconButton(
                modifier = Modifier
                    .padding(HeaderIconPadding)
                    .fillMaxHeight()
                    .aspectRatio(1f),
                iconId = R.drawable.ic_clear_all,
                iconColor = headerStyle.foreground.solidColor(),
                enabled = historyEnabled,
                onClick = { /*TODO*/ },
            )
        }

        if (historyEnabled) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(innerHeight)
                    .padding(ContentPadding),
            ) {
                //
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(innerHeight)
                    .padding(ContentPadding),
                contentAlignment = Alignment.TopCenter,
            ) {
                SnyggSurface(
                    modifier = Modifier
                        .padding(ItemMargin)
                        .fillMaxWidth()
                        .wrapContentHeight(),
                    background = itemStyle.background,
                    shape = itemStyle.shape,
                    contentPadding = ItemPadding,
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            modifier = Modifier.padding(bottom = 8.dp),
                            text = stringRes(R.string.clipboard__disabled__title),
                            color = itemStyle.foreground.solidColor(),
                            fontSize = itemStyle.fontSize.spSize() * 1.1f,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = stringRes(R.string.clipboard__disabled__message),
                            color = itemStyle.foreground.solidColor(),
                            fontSize = itemStyle.fontSize.spSize(),
                        )
                        Button(
                            modifier = Modifier
                                .padding(top = 8.dp)
                                .align(Alignment.End),
                            onClick = { prefs.clipboard.historyEnabled.set(true) },
                            colors = ButtonDefaults.buttonColors(
                                backgroundColor = Green500,
                                contentColor = Color.White,
                            ),
                        ) {
                            Text(
                                text = stringRes(R.string.clipboard__disabled__enable_button),
                                fontSize = itemStyle.fontSize.spSize(),
                            )
                        }
                    }
                }
            }
        }
    }
}
