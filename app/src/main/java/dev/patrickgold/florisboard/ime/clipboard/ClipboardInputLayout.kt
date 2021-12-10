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

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.app.prefs.florisPreferenceModel
import dev.patrickgold.florisboard.app.res.stringRes
import dev.patrickgold.florisboard.app.ui.components.FlorisIconButton
import dev.patrickgold.florisboard.app.ui.components.FlorisStaggeredVerticalGrid
import dev.patrickgold.florisboard.app.ui.components.florisVerticalScroll
import dev.patrickgold.florisboard.app.ui.components.rippleClickable
import dev.patrickgold.florisboard.app.ui.theme.Green500
import dev.patrickgold.florisboard.clipboardManager
import dev.patrickgold.florisboard.common.android.showShortToast
import dev.patrickgold.florisboard.common.observeAsNonNullState
import dev.patrickgold.florisboard.ime.ImeUiMode
import dev.patrickgold.florisboard.ime.clipboard.provider.ClipboardItem
import dev.patrickgold.florisboard.ime.keyboard.FlorisImeSizing
import dev.patrickgold.florisboard.ime.text.smartbar.SecondaryRowPlacement
import dev.patrickgold.florisboard.ime.theme.FlorisImeTheme
import dev.patrickgold.florisboard.ime.theme.FlorisImeUi
import dev.patrickgold.florisboard.keyboardManager
import dev.patrickgold.florisboard.snygg.SnyggPropertySet
import dev.patrickgold.florisboard.snygg.ui.SnyggSurface
import dev.patrickgold.florisboard.snygg.ui.snyggBackground
import dev.patrickgold.florisboard.snygg.ui.solidColor
import dev.patrickgold.florisboard.snygg.ui.spSize
import dev.patrickgold.jetpref.datastore.model.observeAsState

private val HeaderIconPadding = PaddingValues(horizontal = 4.dp)
private val ContentPadding = PaddingValues(horizontal = 4.dp)
private val ItemMargin = PaddingValues(all = 6.dp)
private val ItemPadding = PaddingValues(vertical = 8.dp, horizontal = 12.dp)
private val ItemWidth = 200.dp

@Composable
fun ClipboardInputLayout(
    modifier: Modifier = Modifier,
) {
    val prefs by florisPreferenceModel()
    val context = LocalContext.current
    val clipboardManager by context.clipboardManager()
    val keyboardManager by context.keyboardManager()

    val activeState by keyboardManager.observeActiveState()
    val historyEnabled by prefs.clipboard.historyEnabled.observeAsState()
    val history by clipboardManager.history.observeAsNonNullState()

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
    var popupItem by remember(history) { mutableStateOf<ClipboardItem?>(null) }

    val headerStyle = FlorisImeTheme.style.get(FlorisImeUi.ClipboardHeader)
    val itemStyle = FlorisImeTheme.style.get(FlorisImeUi.ClipboardItem)
    val popupStyle = FlorisImeTheme.style.get(FlorisImeUi.ClipboardItemPopup)

    @Composable
    fun HeaderRow() {
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
                enabled = popupItem == null,
                onClick = { prefs.clipboard.historyEnabled.set(!historyEnabled) },
            )
            FlorisIconButton(
                modifier = Modifier
                    .padding(HeaderIconPadding)
                    .fillMaxHeight()
                    .aspectRatio(1f),
                iconId = R.drawable.ic_clear_all,
                iconColor = headerStyle.foreground.solidColor(),
                enabled = historyEnabled && popupItem == null,
                onClick = {
                    clipboardManager.clearHistory()
                    context.showShortToast(R.string.clipboard__cleared_history)
                },
            )
        }
    }

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    fun ClipItemView(
        item: ClipboardItem,
        style: SnyggPropertySet,
        modifier: Modifier = Modifier,
    ) {
        SnyggSurface(
            modifier = modifier
                .fillMaxWidth()
                .padding(ItemMargin),
            background = style.background,
            shape = style.shape,
            clip = true,
            contentPadding = ItemPadding,
            clickAndSemanticsModifier = Modifier.combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = rememberRipple(),
                enabled = popupItem == null,
                onLongClick = {
                    popupItem = item
                },
                onClick = {
                    clipboardManager.pasteItem(item)
                },
            ),
        ) {
            Text(
                text = item.stringRepresentation(),
                color = style.foreground.solidColor(),
                fontSize = style.fontSize.spSize(),
            )
        }
    }

    @Composable
    fun HistoryMainView() {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(innerHeight),
        ) {
            val historyAlpha by animateFloatAsState(targetValue = if (popupItem != null) 0.12f else 1f)
            Column(
                modifier = Modifier
                    .padding(ContentPadding)
                    .fillMaxSize()
                    .alpha(historyAlpha)
                    .florisVerticalScroll(),
            ) {
                if (history.pinned.isNotEmpty()) {
                    ClipCategoryTitle(
                        text = stringRes(R.string.clipboard__group_pinned),
                        style = itemStyle,
                    )
                    FlorisStaggeredVerticalGrid(maxColumnWidth = ItemWidth) {
                        for (item in history.pinned) {
                            ClipItemView(item, itemStyle)
                        }
                    }
                }
                if (history.recent.isNotEmpty()) {
                    ClipCategoryTitle(
                        text = stringRes(R.string.clipboard__group_recent),
                        style = itemStyle,
                    )
                    FlorisStaggeredVerticalGrid(maxColumnWidth = ItemWidth) {
                        for (item in history.recent) {
                            ClipItemView(item, itemStyle)
                        }
                    }
                }
                if (history.other.isNotEmpty()) {
                    ClipCategoryTitle(
                        text = stringRes(R.string.clipboard__group_other),
                        style = itemStyle,
                    )
                    FlorisStaggeredVerticalGrid(maxColumnWidth = ItemWidth) {
                        for (item in history.other) {
                            ClipItemView(item, itemStyle)
                        }
                    }
                }
            }
            if (popupItem != null) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(ContentPadding)
                        .pointerInput(Unit) {
                            detectTapGestures { popupItem = null }
                        },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceAround,
                ) {
                    ClipItemView(
                        modifier = Modifier.widthIn(max = ItemWidth),
                        item = popupItem!!,
                        style = itemStyle,
                    )
                    Column(
                        modifier = Modifier
                            .padding(ItemMargin)
                            .snyggBackground(popupStyle.background, popupStyle.shape),
                    ) {
                        PopupAction(
                            iconId = R.drawable.ic_pin,
                            text = stringRes(if (popupItem!!.isPinned) {
                                R.string.clip__unpin_item
                            } else {
                                R.string.clip__pin_item
                            }),
                            style = popupStyle,
                        ) {
                            if (popupItem!!.isPinned) {
                                clipboardManager.unpinClip(popupItem!!)
                            } else {
                                clipboardManager.pinClip(popupItem!!)
                            }
                            popupItem = null
                        }
                        PopupAction(
                            iconId = R.drawable.ic_delete,
                            text = stringRes(R.string.clip__delete_item),
                            style = popupStyle,
                        ) {
                            clipboardManager.deleteClip(popupItem!!)
                            popupItem = null
                        }
                        PopupAction(
                            iconId = R.drawable.ic_content_paste,
                            text = stringRes(R.string.clip__paste_item),
                            style = popupStyle,
                        ) {
                            clipboardManager.pasteItem(popupItem!!)
                            popupItem = null
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun HistoryEmptyView() {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(innerHeight)
                .padding(ContentPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                modifier = Modifier.padding(top = 24.dp, bottom = 8.dp),
                text = stringRes(R.string.clipboard__empty__title),
                color = itemStyle.foreground.solidColor(),
                fontSize = itemStyle.fontSize.spSize() * 1.1f,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = stringRes(R.string.clipboard__empty__message),
                color = itemStyle.foreground.solidColor(),
                fontSize = itemStyle.fontSize.spSize(),
            )
        }
    }

    @Composable
    fun HistoryDisabledView() {
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

    Column(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight(),
    ) {
        HeaderRow()

        if (historyEnabled) {
            if (history.all.isNotEmpty()) {
                HistoryMainView()
            } else {
                HistoryEmptyView()
            }
        } else {
            HistoryDisabledView()
        }
    }
}

@Composable
private fun ClipCategoryTitle(
    text: String,
    style: SnyggPropertySet,
    modifier: Modifier = Modifier,
) {
    Text(
        modifier = modifier
            .padding(ItemMargin)
            .padding(top = 8.dp)
            .fillMaxWidth(),
        text = text.uppercase(),
        color = style.foreground.solidColor(),
        fontWeight = FontWeight.Bold,
        fontSize = style.fontSize.spSize() * 0.8f,
    )
}

@Composable
private fun PopupAction(
    @DrawableRes iconId: Int,
    text: String,
    style: SnyggPropertySet,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .width(ItemWidth)
            .rippleClickable(onClick = onClick)
            .padding(all = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            modifier = Modifier.padding(end = 8.dp),
            painter = painterResource(iconId),
            contentDescription = null,
            tint = style.foreground.solidColor(),
        )
        Text(
            modifier = Modifier.weight(1f),
            text = text,
            color = style.foreground.solidColor(),
            fontSize = style.fontSize.spSize(),
        )
    }
}
