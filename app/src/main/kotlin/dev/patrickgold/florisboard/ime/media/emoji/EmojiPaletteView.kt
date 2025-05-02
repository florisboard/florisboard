/*
 * Copyright (C) 2022-2025 The FlorisBoard Contributors
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

package dev.patrickgold.florisboard.ime.media.emoji

import android.graphics.Paint
import android.graphics.Typeface
import android.util.TypedValue
import android.widget.TextView
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Popup
import androidx.emoji2.text.EmojiCompat
import androidx.emoji2.widget.EmojiTextView
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.app.florisPreferenceModel
import dev.patrickgold.florisboard.editorInstance
import dev.patrickgold.florisboard.ime.input.LocalInputFeedbackController
import dev.patrickgold.florisboard.ime.keyboard.FlorisImeSizing
import dev.patrickgold.florisboard.ime.text.keyboard.TextKeyData
import dev.patrickgold.florisboard.ime.theme.FlorisImeUi
import dev.patrickgold.florisboard.keyboardManager
import dev.patrickgold.florisboard.lib.compose.florisScrollbar
import dev.patrickgold.florisboard.lib.compose.header
import dev.patrickgold.florisboard.lib.compose.stringRes
import dev.patrickgold.jetpref.datastore.model.observeAsState
import kotlinx.coroutines.launch
import org.florisboard.lib.android.AndroidKeyguardManager
import org.florisboard.lib.android.showShortToast
import org.florisboard.lib.android.systemService
import org.florisboard.lib.snygg.SnyggSelector
import org.florisboard.lib.snygg.ui.SnyggBox
import org.florisboard.lib.snygg.ui.SnyggIcon
import org.florisboard.lib.snygg.ui.SnyggRow
import org.florisboard.lib.snygg.ui.SnyggText
import org.florisboard.lib.snygg.ui.rememberSnyggThemeQuery
import kotlin.math.ceil

private val EmojiCategoryValues = EmojiCategory.entries
private val EmojiBaseWidth = 42.dp
private val EmojiDefaultFontSize = 22.sp

private val VariantsTriangleShapeLtr = GenericShape { size, _ ->
    moveTo(x = size.width, y = 0f)
    lineTo(x = size.width, y = size.height)
    lineTo(x = 0f, y = size.height)
}

private val VariantsTriangleShapeRtl = GenericShape { size, _ ->
    moveTo(x = 0f, y = 0f)
    lineTo(x = size.width, y = size.height)
    lineTo(x = 0f, y = size.height)
}

data class EmojiMappingForView(
    val pinned: List<EmojiSet>,
    val recent: List<EmojiSet>,
    val simple: List<EmojiSet>,
)

@Composable
fun EmojiPaletteView(
    fullEmojiMappings: EmojiData,
    modifier: Modifier = Modifier,
) {
    val prefs by florisPreferenceModel()
    val context = LocalContext.current
    val editorInstance by context.editorInstance()
    val keyboardManager by context.keyboardManager()

    val activeEditorInfo by editorInstance.activeInfoFlow.collectAsState()
    val systemFontPaint = remember(Typeface.DEFAULT) {
        Paint().apply {
            typeface = Typeface.DEFAULT
        }
    }
    val metadataVersion = activeEditorInfo.emojiCompatMetadataVersion
    val replaceAll = activeEditorInfo.emojiCompatReplaceAll
    val emojiCompatInstance by FlorisEmojiCompat.getAsFlow(replaceAll).collectAsState()
    val emojiMappings = remember(emojiCompatInstance, fullEmojiMappings, metadataVersion, systemFontPaint) {
        fullEmojiMappings.byCategory.mapValues { (_, emojiSetList) ->
            emojiSetList.mapNotNull { emojiSet ->
                emojiSet.emojis.filter { emoji ->
                    emojiCompatInstance?.getEmojiMatch(emoji.value, metadataVersion) == EmojiCompat.EMOJI_SUPPORTED ||
                        systemFontPaint.hasGlyph(emoji.value)
                }.let { if (it.isEmpty()) null else EmojiSet(it) }
            }
        }
    }
    val androidKeyguardManager = remember { context.systemService(AndroidKeyguardManager::class) }

    val deviceLocked = androidKeyguardManager.let { it.isDeviceLocked || it.isKeyguardLocked }

    val preferredSkinTone by prefs.emoji.preferredSkinTone.observeAsState()
    val emojiHistoryEnabled by prefs.emoji.historyEnabled.observeAsState()

    var activeCategory by remember(emojiHistoryEnabled) {
        if (emojiHistoryEnabled) {
            mutableStateOf(EmojiCategory.RECENTLY_USED)
        } else {
            mutableStateOf(EmojiCategory.SMILEYS_EMOTION)
        }
    }
    var recentlyUsedVersion by remember { mutableIntStateOf(0) }
    val lazyListState = rememberLazyGridState()
    val scope = rememberCoroutineScope()

    @Composable
    fun GridHeader(text: String) {
        SnyggText(
            elementName = FlorisImeUi.MediaEmojiSubheader.elementName,
            text = text,
        )
    }

    @Composable
    fun EmojiKeyWrapper(
        emojiSet: EmojiSet,
        isPinned: Boolean = false,
        isRecent: Boolean = false,
    ) {
        EmojiKey(
            emojiSet = emojiSet,
            emojiCompatInstance = emojiCompatInstance,
            preferredSkinTone = preferredSkinTone,
            isPinned = isPinned,
            isRecent = isRecent,
            onEmojiInput = { emoji ->
                keyboardManager.inputEventDispatcher.sendDownUp(emoji)
                scope.launch {
                    EmojiHistoryHelper.markEmojiUsed(prefs, emoji)
                }
            },
            onHistoryAction = {
                recentlyUsedVersion++
            },
        )
    }

    Column(modifier = modifier) {
        EmojiCategoriesTabRow(
            activeCategory = activeCategory,
            onCategoryChange = { category ->
                scope.launch { lazyListState.scrollToItem(0) }
                activeCategory = category
            },
            emojiHistoryEnabled = emojiHistoryEnabled,
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            val emojiMapping = if (activeCategory == EmojiCategory.RECENTLY_USED) {
                // Purposely using remember here to prevent recomposition, as this would cause rapid
                // emoji changes for the user when in recently used category.
                remember(recentlyUsedVersion) {
                    val data = prefs.emoji.historyData.get()
                    EmojiMappingForView(
                        pinned = data.pinned.map { EmojiSet(listOf(it)) },
                        recent = data.recent.map { EmojiSet(listOf(it)) },
                        simple = emptyList(),
                    )
                }
            } else {
                EmojiMappingForView(
                    pinned = emptyList(),
                    recent = emptyList(),
                    simple = emojiMappings[activeCategory]!!,
                )
            }
            val isEmojiHistoryEmpty = emojiMapping.pinned.isEmpty() && emojiMapping.recent.isEmpty()
            if (activeCategory == EmojiCategory.RECENTLY_USED && deviceLocked) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(all = 8.dp),
                ) {
                    Text(
                        text = stringRes(R.string.emoji__history__phone_locked_message),
                    )
                }
            } else if (activeCategory == EmojiCategory.RECENTLY_USED && isEmojiHistoryEmpty) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(all = 8.dp),
                ) {
                    Text(
                        text = stringRes(R.string.emoji__history__empty_message),
                    )
                    Text(
                        modifier = Modifier.padding(top = 8.dp),
                        text = stringRes(R.string.emoji__history__usage_tip),
                        fontStyle = FontStyle.Italic,
                    )
                }
            } else key(emojiMapping) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    LazyVerticalGrid(
                        modifier = Modifier
                            .fillMaxSize()
                            .florisScrollbar(lazyListState),
                        columns = GridCells.Adaptive(minSize = EmojiBaseWidth),
                        state = lazyListState,
                    ) {
                        if (emojiMapping.pinned.isNotEmpty()) {
                            header("header_pinned") {
                                GridHeader(text = stringRes(R.string.emoji__history__pinned))
                            }
                            items(emojiMapping.pinned) { emojiSet ->
                                EmojiKeyWrapper(emojiSet, isPinned = true)
                            }
                        }
                        if (emojiMapping.recent.isNotEmpty()) {
                            header("header_recent") {
                                GridHeader(text = stringRes(R.string.emoji__history__recent))
                            }
                            items(emojiMapping.recent) { emojiSet ->
                                EmojiKeyWrapper(emojiSet, isRecent = true)
                            }
                        }
                        if (emojiMapping.simple.isNotEmpty()) {
                            items(emojiMapping.simple) { emojiSet ->
                                EmojiKeyWrapper(emojiSet)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmojiCategoriesTabRow(
    activeCategory: EmojiCategory,
    onCategoryChange: (EmojiCategory) -> Unit,
    emojiHistoryEnabled: Boolean,
) {
    val inputFeedbackController = LocalInputFeedbackController.current
    val selectedTabIndex = if (emojiHistoryEnabled) {
        EmojiCategoryValues.indexOf(activeCategory)
    } else {
        EmojiCategoryValues.indexOf(activeCategory) - 1
    }
    val style = rememberSnyggThemeQuery(FlorisImeUi.MediaEmojiTab.elementName)
    TabRow(
        modifier = Modifier
            .fillMaxWidth()
            .height(FlorisImeSizing.smartbarHeight),
        selectedTabIndex = selectedTabIndex,
        containerColor = Color.Transparent,
        contentColor = style.foreground(),
        indicator = { tabPositions ->
            val style = rememberSnyggThemeQuery(
                elementName = FlorisImeUi.MediaEmojiTab.elementName,
                selector = SnyggSelector.FOCUS,
            )
            TabRowDefaults.PrimaryIndicator(
                Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                height = 4.dp,
                color = style.foreground(),
            )
        },
    ) {
        for (category in EmojiCategoryValues) {
            if (category == EmojiCategory.RECENTLY_USED && !emojiHistoryEnabled) {
                continue
            }
            Tab(
                onClick = {
                    inputFeedbackController.keyPress(TextKeyData.UNSPECIFIED)
                    onCategoryChange(category)
                },
                selected = activeCategory == category,
                icon = { SnyggIcon(
                    elementName = FlorisImeUi.MediaEmojiTab.elementName,
                    selector = if (activeCategory == category) SnyggSelector.FOCUS else SnyggSelector.NONE,
                    modifier = Modifier.size(ButtonDefaults.IconSize),
                    imageVector = category.icon(),
                ) },
            )
        }
    }
}

@Composable
private fun EmojiKey(
    emojiSet: EmojiSet,
    emojiCompatInstance: EmojiCompat?,
    preferredSkinTone: EmojiSkinTone,
    isPinned: Boolean,
    isRecent: Boolean,
    onEmojiInput: (Emoji) -> Unit,
    onHistoryAction: () -> Unit,
) {
    val inputFeedbackController = LocalInputFeedbackController.current
    val base = emojiSet.base(withSkinTone = preferredSkinTone)
    val variations = emojiSet.variations(withoutSkinTone = preferredSkinTone)
    var showVariantsBox by remember { mutableStateOf(false) }

    SnyggBox(FlorisImeUi.MediaEmojiKey.elementName,
        modifier = Modifier
            .aspectRatio(1f)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        inputFeedbackController.keyPress(TextKeyData.UNSPECIFIED)
                    },
                    onTap = {
                        onEmojiInput(base)
                    },
                    onLongPress = {
                        inputFeedbackController.keyLongPress(TextKeyData.UNSPECIFIED)
                        if (variations.isNotEmpty() || isPinned || isRecent) {
                            showVariantsBox = true
                        }
                    },
                )
            },
    ) {
        EmojiText(
            modifier = Modifier.align(Alignment.Center),
            text = base.value,
            emojiCompatInstance = emojiCompatInstance,
        )
        if (variations.isNotEmpty() || isPinned || isRecent) {
            val style = rememberSnyggThemeQuery(FlorisImeUi.MediaEmojiKeyPopupExtendedIndicator.elementName)
            val shape = when (LocalLayoutDirection.current) {
                LayoutDirection.Ltr -> VariantsTriangleShapeLtr
                LayoutDirection.Rtl -> VariantsTriangleShapeRtl
            }
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = (-4).dp, y = (-4).dp)
                    .size(4.dp)
                    .background(style.foreground(), shape),
            )
        }

        if (isPinned || isRecent) {
            EmojiHistoryPopup(
                emoji = base,
                visible = showVariantsBox,
                isCurrentlyPinned = isPinned,
                onHistoryAction = {
                    onHistoryAction()
                    showVariantsBox = false
                },
                onDismiss = {
                    showVariantsBox = false
                },
            )
        } else {
            EmojiVariationsPopup(
                variations = variations,
                visible = showVariantsBox,
                emojiCompatInstance = emojiCompatInstance,
                onEmojiTap = { emoji ->
                    onEmojiInput(emoji)
                    showVariantsBox = false
                },
                onDismiss = {
                    showVariantsBox = false
                },
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EmojiVariationsPopup(
    variations: List<Emoji>,
    visible: Boolean,
    emojiCompatInstance: EmojiCompat?,
    onEmojiTap: (Emoji) -> Unit,
    onDismiss: () -> Unit,
) {
    val emojiKeyHeight = FlorisImeSizing.smartbarHeight

    if (visible) {
        Popup(
            alignment = Alignment.TopCenter,
            offset = with(LocalDensity.current) {
                val y = -emojiKeyHeight * ceil(variations.size / 6f)
                IntOffset(x = 0, y = y.toPx().toInt())
            },
            onDismissRequest = onDismiss,
        ) {
            SnyggRow(
                elementName = FlorisImeUi.MediaEmojiKeyPopupBox.elementName,
                modifier = Modifier
                    .widthIn(max = EmojiBaseWidth * 6),
            ) {
                for (emoji in variations) {
                    SnyggBox(
                        elementName = FlorisImeUi.MediaEmojiKeyPopupElement.elementName,
                        modifier = Modifier
                            .pointerInput(Unit) {
                                detectTapGestures { onEmojiTap(emoji) }
                            }
                            .width(EmojiBaseWidth)
                            .height(emojiKeyHeight),
                    ) {
                        EmojiText(
                            modifier = Modifier.align(Alignment.Center),
                            text = emoji.value,
                            emojiCompatInstance = emojiCompatInstance,
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EmojiHistoryPopup(
    emoji: Emoji,
    visible: Boolean,
    isCurrentlyPinned: Boolean,
    onHistoryAction: () -> Unit,
    onDismiss: () -> Unit,
) {
    val prefs by florisPreferenceModel()
    val scope = rememberCoroutineScope()
    val emojiKeyHeight = FlorisImeSizing.smartbarHeight
    val context = LocalContext.current
    val pinnedUS by prefs.emoji.historyPinnedUpdateStrategy.observeAsState()
    val recentUS by prefs.emoji.historyRecentUpdateStrategy.observeAsState()
    val showMoveLeft = isCurrentlyPinned && !pinnedUS.isAutomatic || !recentUS.isAutomatic
    val showMoveRight = isCurrentlyPinned && !pinnedUS.isAutomatic || !recentUS.isAutomatic

    @Composable
    fun Action(icon: ImageVector, action: suspend () -> Unit) {
        SnyggBox(
            elementName = FlorisImeUi.MediaEmojiKeyPopupElement.elementName,
            modifier = Modifier
                .pointerInput(Unit) {
                    detectTapGestures {
                        scope.launch {
                            action()
                            onHistoryAction()
                        }
                    }
                }
                .width(EmojiBaseWidth)
                .height(emojiKeyHeight),
        ) {
            SnyggIcon(
                modifier = Modifier.align(Alignment.Center),
                imageVector = icon,
            )
        }
    }

    val numActions = 1
    if (visible) {
        Popup(
            alignment = Alignment.TopCenter,
            offset = with(LocalDensity.current) {
                val y = -emojiKeyHeight * ceil(numActions / 6f)
                IntOffset(x = 0, y = y.toPx().toInt())
            },
            onDismissRequest = onDismiss,
        ) {
            SnyggRow(
                elementName = FlorisImeUi.MediaEmojiKeyPopupBox.elementName,
                modifier = Modifier
                    .widthIn(max = EmojiBaseWidth * 6),
            ) {
                if (isCurrentlyPinned) {
                    Action(
                        icon = Icons.Outlined.PushPin,
                        action = {
                            EmojiHistoryHelper.unpinEmoji(prefs, emoji)
                        },
                    )
                } else {
                    Action(
                        icon = Icons.Outlined.PushPin,
                        action = {
                            EmojiHistoryHelper.pinEmoji(prefs, emoji)
                        },
                    )
                }
                if (showMoveLeft) {
                    Action(
                        icon = Icons.AutoMirrored.Default.KeyboardArrowLeft,
                        action = {
                            EmojiHistoryHelper.moveEmoji(prefs, emoji, -1)
                        },
                    )
                }
                if (showMoveRight) {
                    Action(
                        icon = Icons.AutoMirrored.Default.KeyboardArrowRight,
                        action = {
                            EmojiHistoryHelper.moveEmoji(prefs, emoji, 1)
                        },
                    )
                }
                Action(
                    icon = Icons.Outlined.Delete,
                    action = {
                        EmojiHistoryHelper.removeEmoji(prefs, emoji)
                        context.showShortToast(
                            R.string.emoji__history__removal_success_message,
                            "emoji" to emoji.value,
                        )
                    },
                )
            }
        }
    }
}

@Composable
fun EmojiText(
    text: String,
    emojiCompatInstance: EmojiCompat?,
    modifier: Modifier = Modifier,
    color: Color = Color.Black,
    fontSize: TextUnit = EmojiDefaultFontSize,
) {
    if (emojiCompatInstance != null) {
        AndroidView(
            modifier = modifier,
            factory = { context ->
                EmojiTextView(context).also {
                    it.setTextSize(TypedValue.COMPLEX_UNIT_SP, fontSize.value)
                    it.setTextColor(color.toArgb())
                }
            },
            update = { view ->
                view.text = text
            },
        )
    } else {
        AndroidView(
            modifier = modifier,
            factory = { context ->
                TextView(context).also {
                    it.setTextSize(TypedValue.COMPLEX_UNIT_SP, fontSize.value)
                    it.setTextColor(color.toArgb())
                }
            },
            update = { view ->
                view.text = text
            },
        )
    }
}
