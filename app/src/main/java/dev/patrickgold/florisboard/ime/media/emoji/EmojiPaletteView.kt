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
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.GridCells
import androidx.compose.foundation.lazy.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Icon
import androidx.compose.material.LocalContentColor
import androidx.compose.material.Tab
import androidx.compose.material.TabRow
import androidx.compose.material.TabRowDefaults
import androidx.compose.material.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.emoji2.text.EmojiCompat
import dev.patrickgold.florisboard.app.ui.components.florisScrollbar
import dev.patrickgold.florisboard.ime.core.InputKeyEvent
import dev.patrickgold.florisboard.ime.keyboard.FlorisImeSizing
import dev.patrickgold.florisboard.ime.theme.FlorisImeTheme
import dev.patrickgold.florisboard.ime.theme.FlorisImeUi
import dev.patrickgold.florisboard.keyboardManager
import dev.patrickgold.florisboard.snygg.ui.solidColor
import kotlinx.coroutines.launch

private val EmojiBaseWidth = 42.dp
private val VariantsTriangleShape = GenericShape { size, _ ->
    moveTo(x = size.width, y = 0f)
    lineTo(x = size.width, y = size.height)
    lineTo(x = 0f, y = size.height)
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EmojiPaletteView(emojiMappings: EmojiLayoutDataMap) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val keyboardManager by context.keyboardManager()

    val systemFontPaint = remember(Typeface.DEFAULT) {
        Paint().apply {
            typeface = Typeface.DEFAULT
        }
    }

    var activeCategory by remember { mutableStateOf(EmojiCategory.RECENTLY_USED) }
    var lazyListState = rememberLazyListState()
    val tabStyle = FlorisImeTheme.style.get(element = FlorisImeUi.EmojiTab)
    val tabStyleFocused = FlorisImeTheme.style.get(element = FlorisImeUi.EmojiTab, isFocus = true)
    val unselectedContentColor = tabStyle.foreground.solidColor(default = Color.White)
    val selectedContentColor = tabStyleFocused.foreground.solidColor(default = Color.White)

    Column {
        val selectedTabIndex = EmojiCategory.values().indexOf(activeCategory)
        TabRow(
            modifier = Modifier
                .fillMaxWidth()
                .height(FlorisImeSizing.smartbarHeight),
            selectedTabIndex = selectedTabIndex,
            backgroundColor = Color.Transparent,
            contentColor = selectedContentColor,
            indicator = { tabPositions ->
                Box(
                    modifier = Modifier
                        .tabIndicatorOffset(tabPositions[selectedTabIndex])
                        .padding(horizontal = 8.dp)
                        .height(TabRowDefaults.IndicatorHeight)
                        .background(LocalContentColor.current, CircleShape),
                )
            },
        ) {
            for (category in EmojiCategory.values()) {
                Tab(
                    onClick = {
                        scope.launch { lazyListState.scrollToItem(0) }
                        activeCategory = category
                    },
                    modifier = Modifier.weight(1f),
                    selected = activeCategory == category,
                    icon = { Icon(
                        modifier = Modifier.size(ButtonDefaults.IconSize),
                        painter = painterResource(category.iconId()),
                        contentDescription = null,
                    ) },
                    unselectedContentColor = unselectedContentColor,
                    selectedContentColor = selectedContentColor,
                )
            }
        }

        Box(modifier = Modifier.fillMaxWidth()) {
            val emojiKeyStyle = FlorisImeTheme.style.get(element = FlorisImeUi.EmojiKey)
            val triangleColor = emojiKeyStyle.foreground.solidColor(default = Color.White)
            val emojiMapping = remember(activeCategory, systemFontPaint) {
                emojiMappings[activeCategory]!!.filter { emojiSet ->
                    val base = emojiSet.base()
                    EmojiCompat.get().getEmojiMatch(base.value, 0) == EmojiCompat.EMOJI_SUPPORTED ||
                        systemFontPaint.hasGlyph(base.value)
                }
            }
            var showVariantsBox by remember { mutableStateOf(false) }
            var variantsBoxEmojiSet by remember { mutableStateOf(EmojiSet(listOf(Emoji("", "", emptyList())))) }
            var variantsBoxPosition by remember { mutableStateOf(Offset.Zero) }

            LazyVerticalGrid(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .florisScrollbar(lazyListState, color = triangleColor.copy(alpha = 0.28f), isVertical = true),
                cells = GridCells.Adaptive(minSize = EmojiBaseWidth),
                state = lazyListState,
            ) {
                items(emojiMapping) { emojiSet -> key(emojiSet) {
                    val base = emojiSet.base()
                    val variations = emojiSet.variations()
                    var position = Offset.Zero
                    BoxWithConstraints(
                        modifier = Modifier
                            .onGloballyPositioned { position = it.positionInParent() }
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onTap = {
                                        keyboardManager.inputEventDispatcher.send(InputKeyEvent.downUp(base))
                                    },
                                    onLongPress = if (variations.isEmpty()) null else ({
                                        variantsBoxEmojiSet = emojiSet
                                        variantsBoxPosition = position
                                        showVariantsBox = true
                                    }),
                                )
                            }
                            .height(FlorisImeSizing.smartbarHeight),
                    ) {
                        Text(
                            modifier = Modifier.align(Alignment.Center),
                            text = base.value,
                            fontSize = 24.sp,
                        )
                        if (variations.isNotEmpty()) {
                            Box(
                                modifier = Modifier
                                    .offset(x = maxWidth - 8.dp, y = maxHeight - 8.dp)
                                    .size(4.dp)
                                    .background(triangleColor, VariantsTriangleShape),
                            )
                        }
                    }
                } }
            }

            androidx.compose.animation.AnimatedVisibility(visible = showVariantsBox) {
                val variations = variantsBoxEmojiSet.variations()
            }
        }
    }
}
