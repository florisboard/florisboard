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

import android.content.ContentUris
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.media.ThumbnailUtils
import android.provider.MediaStore
import android.util.Size
import androidx.annotation.DrawableRes
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ToggleOff
import androidx.compose.material.icons.filled.ToggleOn
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.app.florisPreferenceModel
import dev.patrickgold.florisboard.clipboardManager
import dev.patrickgold.florisboard.ime.ImeUiMode
import dev.patrickgold.florisboard.ime.clipboard.provider.ClipboardFileStorage
import dev.patrickgold.florisboard.ime.clipboard.provider.ClipboardItem
import dev.patrickgold.florisboard.ime.clipboard.provider.ItemType
import dev.patrickgold.florisboard.ime.keyboard.FlorisImeSizing
import dev.patrickgold.florisboard.ime.theme.FlorisImeTheme
import dev.patrickgold.florisboard.ime.theme.FlorisImeUi
import dev.patrickgold.florisboard.keyboardManager
import dev.patrickgold.florisboard.lib.compose.FlorisIconButtonWithInnerPadding
import dev.patrickgold.florisboard.lib.compose.FlorisStaggeredVerticalGrid
import dev.patrickgold.florisboard.lib.compose.FlorisTextButton
import dev.patrickgold.florisboard.lib.compose.autoMirrorForRtl
import dev.patrickgold.florisboard.lib.compose.florisVerticalScroll
import dev.patrickgold.florisboard.lib.compose.rippleClickable
import dev.patrickgold.florisboard.lib.compose.safeTimes
import dev.patrickgold.florisboard.lib.compose.stringRes
import dev.patrickgold.florisboard.lib.observeAsNonNullState
import dev.patrickgold.florisboard.lib.util.NetworkUtils
import dev.patrickgold.jetpref.datastore.model.observeAsState
import org.florisboard.lib.android.AndroidKeyguardManager
import org.florisboard.lib.android.AndroidVersion
import org.florisboard.lib.android.showShortToast
import org.florisboard.lib.android.systemService
import org.florisboard.lib.snygg.SnyggPropertySet
import org.florisboard.lib.snygg.ui.SnyggButton
import org.florisboard.lib.snygg.ui.SnyggSurface
import org.florisboard.lib.snygg.ui.snyggBackground
import org.florisboard.lib.snygg.ui.snyggBorder
import org.florisboard.lib.snygg.ui.snyggClip
import org.florisboard.lib.snygg.ui.snyggShadow
import org.florisboard.lib.snygg.ui.solidColor
import org.florisboard.lib.snygg.ui.spSize

private val ContentPadding = PaddingValues(horizontal = 4.dp)
private val ItemMargin = PaddingValues(all = 6.dp)
private val ItemPadding = PaddingValues(vertical = 8.dp, horizontal = 12.dp)
private val DescriptionPadding = PaddingValues(top = 4.dp, start = 12.dp, end = 12.dp)
private val ItemWidth = 200.dp
private val DialogWidth = 240.dp

@Composable
fun ClipboardInputLayout(
    modifier: Modifier = Modifier,
) {
    val prefs by florisPreferenceModel()
    val context = LocalContext.current
    val clipboardManager by context.clipboardManager()
    val keyboardManager by context.keyboardManager()
    val androidKeyguardManager = remember { context.systemService(AndroidKeyguardManager::class) }

    val deviceLocked = androidKeyguardManager.let { it.isDeviceLocked || it.isKeyguardLocked }
    val historyEnabled by prefs.clipboard.historyEnabled.observeAsState()
    val history by clipboardManager.history.observeAsNonNullState()

    val innerHeight = FlorisImeSizing.imeUiHeight() - FlorisImeSizing.smartbarHeight
    var popupItem by remember(history) { mutableStateOf<ClipboardItem?>(null) }
    var showClearAllHistory by remember { mutableStateOf(false) }

    val headerStyle = FlorisImeTheme.style.get(FlorisImeUi.ClipboardHeader)
    val itemStyle = FlorisImeTheme.style.get(FlorisImeUi.ClipboardItem)
    val popupStyle = FlorisImeTheme.style.get(FlorisImeUi.ClipboardItemPopup)
    val enableHistoryButtonStyle = FlorisImeTheme.style.get(FlorisImeUi.ClipboardEnableHistoryButton)

    fun isPopupSurfaceActive() = popupItem != null || showClearAllHistory

    @Composable
    fun HeaderRow() {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(FlorisImeSizing.smartbarHeight)
                .snyggBackground(context, headerStyle),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FlorisIconButtonWithInnerPadding(
                onClick = { keyboardManager.activeState.imeUiMode = ImeUiMode.TEXT },
                icon = Icons.AutoMirrored.Filled.ArrowBack,
                iconColor = headerStyle.foreground.solidColor(context),
            )
            Text(
                modifier = Modifier.weight(1f),
                text = stringRes(R.string.clipboard__header_title),
                color = headerStyle.foreground.solidColor(context),
                fontSize = headerStyle.fontSize.spSize(),
            )
            FlorisIconButtonWithInnerPadding(
                onClick = { prefs.clipboard.historyEnabled.set(!historyEnabled) },
                modifier = Modifier.autoMirrorForRtl(),
                icon = if (historyEnabled) {
                    Icons.Default.ToggleOn
                } else {
                    Icons.Default.ToggleOff
                },
                iconColor = headerStyle.foreground.solidColor(context),
                enabled = !deviceLocked && !isPopupSurfaceActive(),
            )
            FlorisIconButtonWithInnerPadding(
                onClick = { showClearAllHistory = true },
                modifier = Modifier.autoMirrorForRtl(),
                icon = Icons.Default.ClearAll,
                iconColor = headerStyle.foreground.solidColor(context),
                enabled = !deviceLocked && historyEnabled && history.all.isNotEmpty() && !isPopupSurfaceActive(),
            )
            FlorisIconButtonWithInnerPadding(
                onClick = {
                    context.showShortToast("TODO: implement inline clip item editing")
                },
                icon = Icons.Default.Edit,
                iconColor = headerStyle.foreground.solidColor(context),
                enabled = !deviceLocked && historyEnabled && !isPopupSurfaceActive(),
            )
        }
    }

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    fun ClipItemView(
        item: ClipboardItem,
        style: SnyggPropertySet,
        contentScrollInsteadOfClip: Boolean,
        modifier: Modifier = Modifier,
    ) {
        SnyggSurface(
            modifier = modifier
                .fillMaxWidth()
                .padding(ItemMargin),
            style = style,
            clip = true,
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
            if (item.type == ItemType.IMAGE) {
                val id = ContentUris.parseId(item.uri!!)
                val file = ClipboardFileStorage.getFileForId(context, id)
                val bitmap = remember(id) {
                    runCatching {
                        check(file.exists()) { "Unable to resolve image at ${file.absolutePath}" }
                        val rawBitmap = BitmapFactory.decodeFile(file.absolutePath)
                        checkNotNull(rawBitmap) { "Unable to decode image at ${file.absolutePath}" }
                        rawBitmap.asImageBitmap()
                    }
                }
                if (bitmap.isSuccess) {
                    Image(
                        modifier = Modifier.fillMaxWidth(),
                        bitmap = bitmap.getOrThrow(),
                        contentDescription = null,
                        contentScale = ContentScale.FillWidth,
                    )
                } else {
                    Text(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(ItemPadding),
                        text = bitmap.exceptionOrNull()?.message ?: "Unknown error",
                        style = TextStyle(textDirection = TextDirection.Ltr),
                        color = Color.Red,
                        fontSize = style.fontSize.spSize(),
                    )
                }
            } else if (item.type == ItemType.VIDEO) {
                val id = ContentUris.parseId(item.uri!!)
                val file = ClipboardFileStorage.getFileForId(context, id)
                val bitmap = remember(id) {
                    runCatching {
                        check(file.exists()) { "Unable to resolve video at ${file.absolutePath}" }
                        val rawBitmap = if (AndroidVersion.ATLEAST_API29_Q) {
                            val dataRetriever = MediaMetadataRetriever()
                            dataRetriever.setDataSource(file.absolutePath)
                            val width = dataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
                            val height = dataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
                            ThumbnailUtils.createVideoThumbnail(file, Size(width!!.toInt(), height!!.toInt()), null)
                        } else {
                            @Suppress("DEPRECATION")
                            ThumbnailUtils.createVideoThumbnail(file.absolutePath, MediaStore.Video.Thumbnails.MINI_KIND)
                        }
                        checkNotNull(rawBitmap) { "Unable to decode video at ${file.absolutePath}" }
                        rawBitmap.asImageBitmap()
                    }
                }
                if (bitmap.isSuccess) {
                    Image(
                        modifier = Modifier.fillMaxWidth(),
                        bitmap = bitmap.getOrThrow(),
                        contentDescription = null,
                        contentScale = ContentScale.FillWidth,
                    )
                    Icon(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(start = 4.dp, bottom = 4.dp)
                            .background(Color.White, CircleShape),
                        imageVector = Icons.Default.Videocam,
                        contentDescription = null,
                        tint = Color.Black,
                    )
                } else {
                    Text(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(ItemPadding),
                        text = bitmap.exceptionOrNull()?.message ?: "Unknown error",
                        style = TextStyle(textDirection = TextDirection.Ltr),
                        color = Color.Red,
                        fontSize = style.fontSize.spSize(),
                    )
                }
            } else {
                val text = item.stringRepresentation()
                Column {
                    ClipTextItemDescription(text, style)
                    Text(
                        modifier = Modifier
                            .fillMaxWidth()
                            .run { if (contentScrollInsteadOfClip) this.florisVerticalScroll() else this }
                            .padding(ItemPadding),
                        text = text,
                        style = TextStyle(textDirection = TextDirection.ContentOrLtr),
                        color = style.foreground.solidColor(context),
                        fontSize = style.fontSize.spSize(),
                        maxLines = if (contentScrollInsteadOfClip) Int.MAX_VALUE else 5,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }

    @Composable
    fun HistoryMainView() {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(innerHeight),
        ) {
            val historyAlpha by animateFloatAsState(targetValue = if (isPopupSurfaceActive()) 0.12f else 1f)
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
                            ClipItemView(item, itemStyle, contentScrollInsteadOfClip = false)
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
                            ClipItemView(item, itemStyle, contentScrollInsteadOfClip = false)
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
                            ClipItemView(item, itemStyle, contentScrollInsteadOfClip = false)
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
                        contentScrollInsteadOfClip = true,
                    )
                    Column(
                        modifier = Modifier
                            .padding(ItemMargin)
                            .snyggShadow(popupStyle)
                            .snyggBorder(context, popupStyle)
                            .snyggBackground(context, popupStyle)
                            .snyggClip(popupStyle),
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
            if (showClearAllHistory) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(ContentPadding)
                        .pointerInput(Unit) {
                            detectTapGestures { showClearAllHistory = false }
                        },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceAround,
                ) {
                    Column(
                        modifier = Modifier
                            .width(DialogWidth)
                            .snyggShadow(popupStyle)
                            .snyggBorder(context, popupStyle)
                            .snyggBackground(context, popupStyle)
                            .snyggClip(popupStyle)
                            .pointerInput(Unit) {
                                detectTapGestures { /* Do nothing */ }
                            },
                    ) {
                        Text(
                            modifier = Modifier.padding(all = 16.dp),
                            text = stringRes(R.string.clipboard__confirm_clear_history__message),
                            color = popupStyle.foreground.solidColor(context),
                        )
                        Row(modifier = Modifier.padding(horizontal = 8.dp)) {
                            Spacer(modifier = Modifier.weight(1f))
                            FlorisTextButton(
                                onClick = {
                                    showClearAllHistory = false
                                },
                                modifier = Modifier.padding(end = 8.dp),
                                text = stringRes(R.string.action__no),
                                colors = ButtonDefaults.textButtonColors(contentColor = popupStyle.foreground.solidColor(context)),
                            )
                            FlorisTextButton(
                                onClick = {
                                    clipboardManager.clearHistory()
                                    context.showShortToast(R.string.clipboard__cleared_history)
                                    showClearAllHistory = false
                                },
                                text = stringRes(R.string.action__yes),
                                colors = ButtonDefaults.textButtonColors(contentColor = popupStyle.foreground.solidColor(context)),
                            )
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
                color = itemStyle.foreground.solidColor(context),
                fontSize = itemStyle.fontSize.spSize() safeTimes 1.1f,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = stringRes(R.string.clipboard__empty__message),
                color = itemStyle.foreground.solidColor(context),
                fontSize = itemStyle.fontSize.spSize(),
                textAlign = TextAlign.Center,
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
                style = itemStyle,
                contentPadding = ItemPadding,
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        modifier = Modifier.padding(bottom = 8.dp),
                        text = stringRes(R.string.clipboard__disabled__title),
                        color = itemStyle.foreground.solidColor(context),
                        fontSize = itemStyle.fontSize.spSize() safeTimes 1.1f,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = stringRes(R.string.clipboard__disabled__message),
                        color = itemStyle.foreground.solidColor(context),
                        fontSize = itemStyle.fontSize.spSize(),
                    )
                    SnyggButton(
                        modifier = Modifier
                            .padding(top = 8.dp)
                            .align(Alignment.End),
                        onClick = { prefs.clipboard.historyEnabled.set(true) },
                        style = enableHistoryButtonStyle,
                        text = stringRes(R.string.clipboard__disabled__enable_button)
                    )
                }
            }
        }
    }

    @Composable
    fun HistoryLockedView() {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(innerHeight)
                .padding(ContentPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                modifier = Modifier.padding(top = 24.dp, bottom = 8.dp),
                text = stringRes(R.string.clipboard__locked__title),
                color = itemStyle.foreground.solidColor(context),
                fontSize = itemStyle.fontSize.spSize() safeTimes 1.1f,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = stringRes(R.string.clipboard__locked__message),
                color = itemStyle.foreground.solidColor(context),
                fontSize = itemStyle.fontSize.spSize(),
                textAlign = TextAlign.Center,
            )
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight(),
    ) {
        HeaderRow()
        if (deviceLocked) {
            HistoryLockedView()
        } else {
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
}

@Composable
private fun ClipCategoryTitle(
    text: String,
    style: SnyggPropertySet,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    Text(
        modifier = modifier
            .padding(ItemMargin)
            .padding(top = 8.dp)
            .fillMaxWidth(),
        text = text.uppercase(),
        color = style.foreground.solidColor(context),
        fontWeight = FontWeight.Bold,
        fontSize = style.fontSize.spSize() safeTimes 0.8f,
    )
}

@Composable
private fun ClipTextItemDescription(
    text: String,
    style: SnyggPropertySet,
    modifier: Modifier = Modifier,
): Unit = with(LocalDensity.current) {
    val context = LocalContext.current
    val iconId: Int?
    val description: String?
    when {
        NetworkUtils.isEmailAddress(text) -> {
            iconId = R.drawable.ic_email
            description = stringRes(R.string.clipboard__item_description_email)
        }
        NetworkUtils.isUrl(text) -> {
            iconId = R.drawable.ic_link
            description = stringRes(R.string.clipboard__item_description_url)
        }
        NetworkUtils.isPhoneNumber(text) -> {
            iconId = R.drawable.ic_phone
            description = stringRes(R.string.clipboard__item_description_phone)
        }
        else -> {
            iconId = null
            description = null
        }
    }
    if (iconId != null && description != null) {
        Row(
            modifier = modifier
                .padding(DescriptionPadding)
                .offset(y = DescriptionPadding.calculateTopPadding()),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val fontSize = style.fontSize.spSize()
            Icon(
                modifier = Modifier
                    .padding(end = 8.dp)
                    .requiredSize(fontSize.toDp()),
                painter = painterResource(id = iconId),
                contentDescription = null,
                tint = style.foreground.solidColor(context),
            )
            Text(
                modifier = Modifier.weight(1f),
                text = description,
                color = style.foreground.solidColor(context),
                fontSize = fontSize safeTimes 0.8f,
                fontStyle = FontStyle.Italic,
            )
        }
    }
}

@Composable
private fun PopupAction(
    @DrawableRes iconId: Int,
    text: String,
    style: SnyggPropertySet,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val context = LocalContext.current
    Row(
        modifier = modifier
            .width(ItemWidth)
            .rippleClickable(onClick = onClick)
            .padding(all = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            modifier = Modifier.padding(end = 8.dp),
            painter = painterResource(iconId),
            contentDescription = null,
            tint = style.foreground.solidColor(context),
        )
        Text(
            modifier = Modifier.weight(1f),
            text = text,
            color = style.foreground.solidColor(context),
            fontSize = style.fontSize.spSize(),
        )
    }
}
