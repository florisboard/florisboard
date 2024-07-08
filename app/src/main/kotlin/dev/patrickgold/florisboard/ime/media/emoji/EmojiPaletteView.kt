/*
 * Copyright (C) 2022 Patrick Goldinger
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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
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
import dev.patrickgold.florisboard.ime.theme.FlorisImeTheme
import dev.patrickgold.florisboard.ime.theme.FlorisImeUi
import dev.patrickgold.florisboard.keyboardManager
import dev.patrickgold.florisboard.lib.compose.florisScrollbar
import dev.patrickgold.florisboard.lib.compose.safeTimes
import dev.patrickgold.florisboard.lib.compose.stringRes
import dev.patrickgold.jetpref.datastore.model.observeAsState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.florisboard.lib.android.AndroidKeyguardManager
import org.florisboard.lib.android.showShortToast
import org.florisboard.lib.android.systemService
import org.florisboard.lib.snygg.ui.snyggBackground
import org.florisboard.lib.snygg.ui.snyggBorder
import org.florisboard.lib.snygg.ui.snyggShadow
import org.florisboard.lib.snygg.ui.solidColor
import org.florisboard.lib.snygg.ui.spSize
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

    var activeCategory by remember { mutableStateOf(EmojiCategory.RECENTLY_USED) }
    val lazyListState = rememberLazyGridState()
    val scope = rememberCoroutineScope()

    val preferredSkinTone by prefs.media.emojiPreferredSkinTone.observeAsState()
    val fontSizeMultiplier = prefs.keyboard.fontSizeMultiplier()
    val emojiKeyStyle = FlorisImeTheme.style.get(element = FlorisImeUi.EmojiKey)
    val emojiKeyFontSize = emojiKeyStyle.fontSize.spSize(default = EmojiDefaultFontSize) safeTimes fontSizeMultiplier
    val contentColor = emojiKeyStyle.foreground.solidColor(context, default = FlorisImeTheme.fallbackContentColor())

    Column(modifier = modifier) {
        EmojiCategoriesTabRow(
            activeCategory = activeCategory,
            onCategoryChange = { category ->
                scope.launch { lazyListState.scrollToItem(0) }
                activeCategory = category
            },
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            var recentlyUsedVersion by remember { mutableIntStateOf(0) }
            val emojiMapping = if (activeCategory == EmojiCategory.RECENTLY_USED) {
                // Purposely using remember here to prevent recomposition, as this would cause rapid
                // emoji changes for the user when in recently used category.
                remember(recentlyUsedVersion) {
                    prefs.media.emojiRecentlyUsed.get().map { EmojiSet(listOf(it)) }
                }
            } else {
                emojiMappings[activeCategory]!!
            }
            if (activeCategory == EmojiCategory.RECENTLY_USED && deviceLocked) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(all = 8.dp),
                ) {
                    Text(
                        text = stringRes(R.string.emoji__recently_used__phone_locked_message),
                        color = contentColor,
                    )
                }
            } else if (activeCategory == EmojiCategory.RECENTLY_USED && emojiMapping.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(all = 8.dp),
                ) {
                    Text(
                        text = stringRes(R.string.emoji__recently_used__empty_message),
                        color = contentColor,
                    )
                    Text(
                        modifier = Modifier.padding(top = 8.dp),
                        text = stringRes(R.string.emoji__recently_used__removal_tip),
                        color = contentColor,
                        fontStyle = FontStyle.Italic,
                    )
                }
            }
            else key(emojiMapping) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    LazyVerticalGrid(
                        modifier = Modifier
                            .fillMaxSize()
                            .florisScrollbar(lazyListState, color = contentColor.copy(alpha = 0.28f)),
                        columns = GridCells.Adaptive(minSize = EmojiBaseWidth),
                        state = lazyListState,
                    ) {
                        items(emojiMapping) { emojiSet ->
                            EmojiKey(
                                emojiSet = emojiSet,
                                emojiCompatInstance = emojiCompatInstance,
                                preferredSkinTone = preferredSkinTone,
                                contentColor = contentColor,
                                fontSize = emojiKeyFontSize,
                                fontSizeMultiplier = fontSizeMultiplier,
                                onEmojiInput = { emoji ->
                                    keyboardManager.inputEventDispatcher.sendDownUp(emoji)
                                    scope.launch {
                                        EmojiRecentlyUsedHelper.addEmoji(prefs, emoji)
                                    }
                                },
                                onLongPress = { emoji ->
                                    if (activeCategory == EmojiCategory.RECENTLY_USED) {
                                        scope.launch {
                                            EmojiRecentlyUsedHelper.removeEmoji(prefs, emoji)
                                            recentlyUsedVersion++
                                            withContext(Dispatchers.Main) {
                                                context.showShortToast(
                                                    R.string.emoji__recently_used__removal_success_message,
                                                    "emoji" to emoji.value,
                                                )
                                            }
                                        }
                                    }
                                },
                            )
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
) {
    val context = LocalContext.current
    val inputFeedbackController = LocalInputFeedbackController.current
    val tabStyle = FlorisImeTheme.style.get(element = FlorisImeUi.EmojiTab)
    val tabStyleFocused = FlorisImeTheme.style.get(element = FlorisImeUi.EmojiTab, isFocus = true)
    val unselectedContentColor = tabStyle.foreground.solidColor(context, default = FlorisImeTheme.fallbackContentColor())
    val selectedContentColor = tabStyleFocused.foreground.solidColor(context, default = FlorisImeTheme.fallbackContentColor())

    val selectedTabIndex = EmojiCategoryValues.indexOf(activeCategory)
    TabRow(
        modifier = Modifier
            .fillMaxWidth()
            .height(FlorisImeSizing.smartbarHeight),
        selectedTabIndex = selectedTabIndex,
        containerColor = Color.Transparent,
        contentColor = selectedContentColor,
        indicator = { tabPositions ->
            TabRowDefaults.PrimaryIndicator(
                Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                color = selectedContentColor,
                height = 4.dp
            )
        },
    ) {
        for (category in EmojiCategoryValues) {
            Tab(
                onClick = {
                    inputFeedbackController.keyPress(TextKeyData.UNSPECIFIED)
                    onCategoryChange(category)
                },
                selected = activeCategory == category,
                icon = { Icon(
                    modifier = Modifier.size(ButtonDefaults.IconSize),
                    imageVector = category.icon(),
                    contentDescription = null,
                ) },
                unselectedContentColor = unselectedContentColor,
                selectedContentColor = selectedContentColor,
            )
        }
    }
}

@Composable
private fun EmojiKey(
    emojiSet: EmojiSet,
    emojiCompatInstance: EmojiCompat?,
    preferredSkinTone: EmojiSkinTone,
    contentColor: Color,
    fontSize: TextUnit,
    fontSizeMultiplier: Float,
    onEmojiInput: (Emoji) -> Unit,
    onLongPress: (Emoji) -> Unit,
) {
    val inputFeedbackController = LocalInputFeedbackController.current
    val base = emojiSet.base(withSkinTone = preferredSkinTone)
    val variations = emojiSet.variations(withoutSkinTone = preferredSkinTone)
    var showVariantsBox by remember { mutableStateOf(false) }

    Box(
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
                        onLongPress(base)
                        if (variations.isNotEmpty()) {
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
            color = contentColor,
            fontSize = fontSize,
        )
        if (variations.isNotEmpty()) {
            val shape = when (LocalLayoutDirection.current) {
                LayoutDirection.Ltr -> VariantsTriangleShapeLtr
                LayoutDirection.Rtl -> VariantsTriangleShapeRtl
            }
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = (-4).dp, y = (-4).dp)
                    .size(4.dp)
                    .background(contentColor, shape),
            )
        }

        EmojiVariationsPopup(
            variations = variations,
            visible = showVariantsBox,
            emojiCompatInstance = emojiCompatInstance,
            fontSizeMultiplier = fontSizeMultiplier,
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EmojiVariationsPopup(
    variations: List<Emoji>,
    visible: Boolean,
    emojiCompatInstance: EmojiCompat?,
    fontSizeMultiplier: Float,
    onEmojiTap: (Emoji) -> Unit,
    onDismiss: () -> Unit,
) {
    val popupStyle = FlorisImeTheme.style.get(element = FlorisImeUi.EmojiKeyPopup)
    val emojiKeyHeight = FlorisImeSizing.smartbarHeight
    val context = LocalContext.current

    if (visible) {
        Popup(
            alignment = Alignment.TopCenter,
            offset = with(LocalDensity.current) {
                val y = -emojiKeyHeight * ceil(variations.size / 6f)
                IntOffset(x = 0, y = y.toPx().toInt())
            },
            onDismissRequest = onDismiss,
        ) {
            FlowRow(
                modifier = Modifier
                    .widthIn(max = EmojiBaseWidth * 6)
                    .snyggShadow(popupStyle)
                    .snyggBorder(context, popupStyle)
                    .snyggBackground(context, popupStyle, fallbackColor = FlorisImeTheme.fallbackSurfaceColor()),
            ) {
                for (emoji in variations) {
                    Box(
                        modifier = Modifier
                            .pointerInput(Unit) {
                                detectTapGestures { onEmojiTap(emoji) }
                            }
                            .width(EmojiBaseWidth)
                            .height(emojiKeyHeight)
                            .padding(all = 4.dp),
                    ) {
                        EmojiText(
                            modifier = Modifier.align(Alignment.Center),
                            text = emoji.value,
                            emojiCompatInstance = emojiCompatInstance,
                            color = popupStyle.foreground.solidColor(context, default = FlorisImeTheme.fallbackContentColor()),
                            fontSize = popupStyle.fontSize.spSize(default = EmojiDefaultFontSize) safeTimes fontSizeMultiplier,
                        )
                    }
                }
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
