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

package dev.patrickgold.florisboard.lib.compose

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.ContentAlpha
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.app.apptheme.outline

@Composable
fun <T : Any> FlorisDropdownMenu(
    items: List<T>,
    expanded: Boolean,
    selectedIndex: Int,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isError: Boolean = false,
    labelProvider: (@Composable (T) -> String)? = null,
    onSelectItem: (Int) -> Unit = { },
    onExpandRequest: () -> Unit = { },
    onDismissRequest: () -> Unit = { },
) {
    @Composable
    fun asString(v: T): String {
        return labelProvider?.invoke(v) ?: v.toString()
    }

    Box(modifier = modifier.wrapContentSize(Alignment.TopStart)) {
        val indicatorRotation by animateFloatAsState(targetValue = if (expanded) 180f else 0f)
        val index = selectedIndex.coerceIn(items.indices)
        val color = if (!enabled) {
            MaterialTheme.colors.outline
        } else if (isError) {
            MaterialTheme.colors.error
        } else {
            MaterialTheme.colors.onBackground
        }
        OutlinedButton(
            modifier = Modifier.fillMaxWidth(),
            border = if (isError && enabled) {
                BorderStroke(ButtonDefaults.OutlinedBorderSize, MaterialTheme.colors.error)
            } else {
                ButtonDefaults.outlinedBorder
            },
            enabled = enabled,
            onClick = onExpandRequest,
        ) {
            Text(
                modifier = Modifier.weight(1.0f),
                text = asString(items[index]),
                textAlign = TextAlign.Start,
                fontWeight = FontWeight.Normal,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
                color = color,
            )
            Icon(
                modifier = Modifier.rotate(indicatorRotation),
                painter = painterResource(R.drawable.ic_keyboard_arrow_down),
                tint = if (enabled) {
                    color.copy(alpha = ContentAlpha.medium)
                } else {
                    color
                },
                contentDescription = "Dropdown indicator",
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = onDismissRequest,
        ) {
            for ((n, item) in items.withIndex()) {
                DropdownMenuItem(
                    onClick = {
                        onSelectItem(n)
                        onDismissRequest()
                    },
                ) {
                    Text(text = asString(item))
                }
            }
        }
    }
}

@Composable
fun FlorisDropdownLikeButton(
    item: String,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    onClick: () -> Unit = { },
) {
    Box(modifier = modifier.wrapContentSize(Alignment.TopStart)) {
        val color = if (isError) {
            MaterialTheme.colors.error
        } else {
            MaterialTheme.colors.onBackground
        }
        OutlinedButton(
            modifier = Modifier
                .fillMaxWidth(),
            border = if (isError) {
                BorderStroke(ButtonDefaults.OutlinedBorderSize, MaterialTheme.colors.error)
            } else {
                ButtonDefaults.outlinedBorder
            },
            onClick = onClick,
        ) {
            Text(
                modifier = Modifier.weight(1.0f),
                text = item,
                textAlign = TextAlign.Start,
                fontWeight = FontWeight.Normal,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
                color = color,
            )
            Icon(
                modifier = Modifier.autoMirrorForRtl(),
                painter = painterResource(R.drawable.ic_keyboard_arrow_right),
                tint = color.copy(alpha = ContentAlpha.medium),
                contentDescription = "Dropdown indicator",
            )
        }
    }
}
