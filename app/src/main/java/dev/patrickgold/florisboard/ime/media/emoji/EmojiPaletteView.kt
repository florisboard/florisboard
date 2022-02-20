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

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.GridCells
import androidx.compose.foundation.lazy.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Icon
import androidx.compose.material.Tab
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.patrickgold.florisboard.app.ui.components.rippleClickable
import dev.patrickgold.florisboard.ime.core.InputKeyEvent
import dev.patrickgold.florisboard.ime.keyboard.FlorisImeSizing
import dev.patrickgold.florisboard.keyboardManager

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EmojiPaletteView(emojiMappings: EmojiLayoutDataMap) {
    val context = LocalContext.current
    val keyboardManager by context.keyboardManager()

    var activeCategory by remember { mutableStateOf(EmojiCategory.RECENTLY_USED) }

    Column {
        Row(modifier = Modifier.fillMaxWidth()) {
            for (category in EmojiCategory.values()) {
                Tab(
                    onClick = { activeCategory = category },
                    modifier = Modifier.weight(1f),
                    selected = activeCategory == category,
                    icon = { Icon(painter = painterResource(category.iconId()), contentDescription = null) },
                )
            }
        }

        val emojiMapping = emojiMappings[activeCategory]!!
        LazyVerticalGrid(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            cells = GridCells.Fixed(10),
        ) {
            items(emojiMapping) { emojiSet ->
                val base = emojiSet.base()
                val variations = emojiSet.variations()
                Box(
                    modifier = Modifier
                        .rippleClickable {
                            keyboardManager.inputEventDispatcher.send(InputKeyEvent.downUp(base))
                        }
                        .height(FlorisImeSizing.smartbarHeight),
                ) {
                    Text(
                        modifier = Modifier.align(Alignment.Center),
                        text = base.value,
                        fontSize = 18.sp,
                    )
                    if (variations.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .offset(x = 4.dp, y = 4.dp)
                                .size(4.dp)
                                .background(Color.Red)
                                .clip(RectangleShape),
                        )
                    }
                }
            }
        }
    }
}
