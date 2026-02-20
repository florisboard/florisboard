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
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.speekez.data.entity.Preset
import kotlinx.coroutines.withTimeoutOrNull

private val SpeekEZTeal = Color(0xFF00D4AA)

@Composable
fun PresetChip(
    preset: Preset,
    isActive: Boolean,
    onClick: () -> Unit,
    onHoldStart: () -> Unit,
    onHoldEnd: () -> Unit,
    onHoldCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentOnClick by rememberUpdatedState(onClick)
    val currentOnHoldStart by rememberUpdatedState(onHoldStart)
    val currentOnHoldEnd by rememberUpdatedState(onHoldEnd)
    val currentOnHoldCancel by rememberUpdatedState(onHoldCancel)

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
                awaitEachGesture {
                    awaitFirstDown()
                    var up: PointerInputChange? = null
                    val isTimeout = withTimeoutOrNull(200) {
                        up = waitForUpOrCancellation()
                        false
                    } ?: true

                    if (isTimeout) {
                        currentOnHoldStart()
                        val finalUp = waitForUpOrCancellation()
                        if (finalUp != null) {
                            currentOnHoldEnd()
                            finalUp.consume()
                        } else {
                            currentOnHoldCancel()
                        }
                    } else if (up != null) {
                        if (up!!.pressed != up!!.previousPressed) {
                            currentOnClick()
                            up!!.consume()
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = preset.iconEmoji,
            fontSize = 18.sp,
        )
    }
}
