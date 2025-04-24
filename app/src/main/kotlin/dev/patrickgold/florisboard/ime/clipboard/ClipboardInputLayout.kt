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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Backspace
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ToggleOff
import androidx.compose.material.icons.filled.ToggleOn
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.ripple
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
import androidx.compose.ui.unit.dp
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.app.florisPreferenceModel
import dev.patrickgold.florisboard.clipboardManager
import dev.patrickgold.florisboard.ime.ImeUiMode
import dev.patrickgold.florisboard.ime.clipboard.provider.ClipboardFileStorage
import dev.patrickgold.florisboard.ime.clipboard.provider.ClipboardItem
import dev.patrickgold.florisboard.ime.clipboard.provider.ItemType
import dev.patrickgold.florisboard.ime.keyboard.FlorisImeSizing
import dev.patrickgold.florisboard.ime.media.KeyboardLikeButton
import dev.patrickgold.florisboard.ime.text.keyboard.TextKeyData
import dev.patrickgold.florisboard.ime.theme.FlorisImeUi
import dev.patrickgold.florisboard.keyboardManager
import dev.patrickgold.florisboard.lib.compose.FlorisStaggeredVerticalGrid
import dev.patrickgold.florisboard.lib.compose.autoMirrorForRtl
import dev.patrickgold.florisboard.lib.compose.florisVerticalScroll
import dev.patrickgold.florisboard.lib.compose.rippleClickable
import dev.patrickgold.florisboard.lib.compose.stringRes
import dev.patrickgold.florisboard.lib.observeAsNonNullState
import dev.patrickgold.florisboard.lib.util.NetworkUtils
import dev.patrickgold.jetpref.datastore.model.observeAsState
import org.florisboard.lib.android.AndroidKeyguardManager
import org.florisboard.lib.android.AndroidVersion
import org.florisboard.lib.android.showShortToast
import org.florisboard.lib.android.systemService
import org.florisboard.lib.snygg.ui.SnyggBox
import org.florisboard.lib.snygg.ui.SnyggButton
import org.florisboard.lib.snygg.ui.SnyggColumn
import org.florisboard.lib.snygg.ui.SnyggIcon
import org.florisboard.lib.snygg.ui.SnyggIconButton
import org.florisboard.lib.snygg.ui.SnyggRow
import org.florisboard.lib.snygg.ui.SnyggText

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

    var popupItem by remember(history) { mutableStateOf<ClipboardItem?>(null) }
    var showClearAllHistory by remember { mutableStateOf(false) }

    fun isPopupSurfaceActive() = popupItem != null || showClearAllHistory

    @Composable
    fun HeaderRow() {
        SnyggRow(FlorisImeUi.ClipboardHeader.elementName,
            modifier = Modifier
                .fillMaxWidth()
                .height(FlorisImeSizing.smartbarHeight),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SnyggIconButton(
                onClick = { keyboardManager.activeState.imeUiMode = ImeUiMode.TEXT },
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            )
            SnyggText(
                modifier = Modifier.weight(1f),
                text = stringRes(R.string.clipboard__header_title),
            )
            SnyggIconButton(
                onClick = { prefs.clipboard.historyEnabled.set(!historyEnabled) },
                modifier = Modifier.autoMirrorForRtl(),
                imageVector = if (historyEnabled) {
                    Icons.Default.ToggleOn
                } else {
                    Icons.Default.ToggleOff
                },
                enabled = !deviceLocked && !isPopupSurfaceActive(),
            )
            SnyggIconButton(
                onClick = { showClearAllHistory = true },
                modifier = Modifier.autoMirrorForRtl(),
                imageVector = Icons.Default.ClearAll,
                enabled = !deviceLocked && historyEnabled && history.all.isNotEmpty() && !isPopupSurfaceActive(),
            )
            SnyggIconButton(
                onClick = {
                    context.showShortToast("TODO: implement inline clip item editing")
                },
                imageVector = Icons.Default.Edit,
                enabled = !deviceLocked && historyEnabled && !isPopupSurfaceActive(),
            )
            KeyboardLikeButton(
                inputEventDispatcher = keyboardManager.inputEventDispatcher,
                keyData = TextKeyData.DELETE,
                element = FlorisImeUi.ClipboardHeader.elementName,
            ) {
                Icon(Icons.AutoMirrored.Outlined.Backspace, null)
            }
        }
    }

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    fun ClipItemView(
        item: ClipboardItem,
        contentScrollInsteadOfClip: Boolean,
        modifier: Modifier = Modifier,
    ) {
        SnyggBox(FlorisImeUi.ClipboardItem.elementName,
            modifier = modifier.fillMaxWidth(),
            clickAndSemanticsModifier = Modifier.combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(),
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
                    SnyggText(
                        modifier = Modifier.fillMaxWidth(),
                        text = bitmap.exceptionOrNull()?.message ?: "Unknown error",
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
                    SnyggText(
                        modifier = Modifier.fillMaxWidth(),
                        text = bitmap.exceptionOrNull()?.message ?: "Unknown error",
                    )
                }
            } else {
                val text = item.stringRepresentation()
                Column {
                    ClipTextItemDescription(text)
                    SnyggText(
                        modifier = Modifier
                            .fillMaxWidth()
                            .run { if (contentScrollInsteadOfClip) this.florisVerticalScroll() else this },
                        text = item.displayText(),
                    )
                }
            }
        }
    }

    @Composable
    fun HistoryMainView() {
        SnyggBox(FlorisImeUi.ClipboardContent.elementName,
            modifier = Modifier.fillMaxSize(),
        ) {
            val historyAlpha by animateFloatAsState(targetValue = if (isPopupSurfaceActive()) 0.12f else 1f)
            SnyggColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(historyAlpha)
                    .florisVerticalScroll(),
            ) {
                if (history.pinned.isNotEmpty()) {
                    ClipCategoryTitle(
                        text = stringRes(R.string.clipboard__group_pinned),
                    )
                    FlorisStaggeredVerticalGrid(maxColumnWidth = ItemWidth) {
                        for (item in history.pinned) {
                            ClipItemView(item, contentScrollInsteadOfClip = false)
                        }
                    }
                }
                if (history.recent.isNotEmpty()) {
                    ClipCategoryTitle(
                        text = stringRes(R.string.clipboard__group_recent),
                    )
                    FlorisStaggeredVerticalGrid(maxColumnWidth = ItemWidth) {
                        for (item in history.recent) {
                            ClipItemView(item, contentScrollInsteadOfClip = false)
                        }
                    }
                }
                if (history.other.isNotEmpty()) {
                    ClipCategoryTitle(
                        text = stringRes(R.string.clipboard__group_other),
                    )
                    FlorisStaggeredVerticalGrid(maxColumnWidth = ItemWidth) {
                        for (item in history.other) {
                            ClipItemView(item, contentScrollInsteadOfClip = false)
                        }
                    }
                }
            }
            if (popupItem != null) {
                SnyggRow(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTapGestures { popupItem = null }
                        },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceAround,
                ) {
                    ClipItemView(
                        modifier = Modifier.widthIn(max = ItemWidth),
                        item = popupItem!!,
                        contentScrollInsteadOfClip = true,
                    )
                    SnyggColumn(FlorisImeUi.ClipboardItemPopup.elementName) {
                        PopupAction(
                            iconId = R.drawable.ic_pin,
                            text = stringRes(if (popupItem!!.isPinned) {
                                R.string.clip__unpin_item
                            } else {
                                R.string.clip__pin_item
                            }),
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
                        ) {
                            clipboardManager.deleteClip(popupItem!!)
                            popupItem = null
                        }
                        PopupAction(
                            iconId = R.drawable.ic_content_paste,
                            text = stringRes(R.string.clip__paste_item),
                        ) {
                            clipboardManager.pasteItem(popupItem!!)
                            popupItem = null
                        }
                    }
                }
            }
            if (showClearAllHistory) {
                SnyggRow(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTapGestures { showClearAllHistory = false }
                        },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceAround,
                ) {
                    SnyggColumn(
                        modifier = Modifier
                            .pointerInput(Unit) {
                                detectTapGestures { /* Do nothing */ }
                            },
                    ) {
                        SnyggText(
                            modifier = Modifier.padding(all = 16.dp),
                            text = stringRes(R.string.clipboard__confirm_clear_history__message),
                        )
                        SnyggRow {
                            Spacer(modifier = Modifier.weight(1f))
                            SnyggButton(
                                onClick = {
                                    showClearAllHistory = false
                                },
                                text = stringRes(R.string.action__no),
                            )
                            SnyggButton(
                                onClick = {
                                    clipboardManager.clearHistory()
                                    context.showShortToast(R.string.clipboard__cleared_history)
                                    showClearAllHistory = false
                                },
                                text = stringRes(R.string.action__yes),
                            )
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun HistoryEmptyView() {
        SnyggColumn(FlorisImeUi.ClipboardContent.elementName,
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            SnyggText(
                text = stringRes(R.string.clipboard__empty__title),
            )
            SnyggText(
                text = stringRes(R.string.clipboard__empty__message),
            )
        }
    }

    @Composable
    fun HistoryDisabledView() {
        SnyggColumn(FlorisImeUi.ClipboardContent.elementName,
            modifier = Modifier.fillMaxSize(),
        ) {
            SnyggText(
                modifier = Modifier.padding(bottom = 8.dp),
                text = stringRes(R.string.clipboard__disabled__title),
            )
            SnyggText(
                text = stringRes(R.string.clipboard__disabled__message),
            )
            SnyggButton(FlorisImeUi.ClipboardEnableHistoryButton.elementName,
                onClick = { prefs.clipboard.historyEnabled.set(true) },
                text = stringRes(R.string.clipboard__disabled__enable_button),
            )
        }
    }

    @Composable
    fun HistoryLockedView() {
        SnyggColumn(FlorisImeUi.ClipboardContent.elementName,
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            SnyggText(
                text = stringRes(R.string.clipboard__locked__title),
            )
            SnyggText(
                text = stringRes(R.string.clipboard__locked__message),
            )
        }
    }

    SnyggColumn(
        modifier = modifier
            .fillMaxWidth()
            .height(FlorisImeSizing.imeUiHeight()),
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
    modifier: Modifier = Modifier,
) {
    SnyggText(FlorisImeUi.ClipboardSubheader.elementName,
        modifier = modifier.fillMaxWidth(),
        text = text.uppercase(),
    )
}

@Composable
private fun ClipTextItemDescription(
    text: String,
    modifier: Modifier = Modifier,
): Unit = with(LocalDensity.current) {
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
        SnyggRow(
            modifier = modifier,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SnyggIcon(
                painter = painterResource(id = iconId),
            )
            SnyggText(
                modifier = Modifier.weight(1f),
                text = description,
            )
        }
    }
}

@Composable
private fun PopupAction(
    @DrawableRes iconId: Int,
    text: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    SnyggRow(FlorisImeUi.ClipboardItemPopupAction.elementName,
        modifier = modifier.rippleClickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SnyggIcon(FlorisImeUi.ClipboardItemPopupActionIcon.elementName,
            painter = painterResource(iconId),
        )
        SnyggText(FlorisImeUi.ClipboardItemPopupActionText.elementName,
            modifier = Modifier.weight(1f),
            text = text,
        )
    }
}
