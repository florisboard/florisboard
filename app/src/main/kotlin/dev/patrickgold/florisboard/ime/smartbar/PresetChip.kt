/*
 * Copyright (C) 2025 SpeekEZ
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

package dev.patrickgold.florisboard.ime.smartbar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.speekez.data.entity.Preset

private val SpeekEZTeal = Color(0xFF00D4AA)

@Composable
fun PresetChip(
    preset: Preset,
    isActive: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val borderModifier = if (isActive) {
        Modifier.border(2.dp, SpeekEZTeal, CircleShape)
    } else {
        Modifier
    }

    val glowModifier = if (isActive) {
        Modifier.shadow(
            elevation = 6.dp,
            shape = CircleShape,
            ambientColor = SpeekEZTeal,
            spotColor = SpeekEZTeal,
        )
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .size(34.dp)
            .then(glowModifier)
            .then(borderModifier)
            .clip(CircleShape)
            .background(if (isActive) SpeekEZTeal.copy(alpha = 0.15f) else Color.Gray.copy(alpha = 0.1f))
            .pointerInput(preset.id) {
                detectTapGestures(
                    onTap = { onClick() },
                    onLongPress = { onLongClick() }
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = preset.iconEmoji,
            fontSize = 18.sp,
        )
    }
}
