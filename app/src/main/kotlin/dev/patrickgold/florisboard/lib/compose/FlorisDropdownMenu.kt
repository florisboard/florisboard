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

package dev.patrickgold.florisboard.lib.compose

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow

//TODO: Use JetPrefDropDownMenu instead
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
            MaterialTheme.colorScheme.outline
        } else if (isError) {
            MaterialTheme.colorScheme.error
        } else {
            MaterialTheme.colorScheme.onBackground
        }
        OutlinedButton(
            modifier = Modifier.fillMaxWidth(),
            border = if (isError && enabled) {
                BorderStroke(ButtonDefaults.outlinedButtonBorder.width, MaterialTheme.colorScheme.error)
            } else {
                ButtonDefaults.outlinedButtonBorder
            },
            shape = ShapeDefaults.ExtraSmall,
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
                imageVector = Icons.Filled.KeyboardArrowDown,
                tint = if (enabled) {
                    color.copy(alpha = 0.74f) //Also test 0.60f
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
                    text = {
                        Text(text = asString(item))
                    },
                    onClick = {
                        onSelectItem(n)
                        onDismissRequest()
                    },
                )
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
            MaterialTheme.colorScheme.error
        } else {
            MaterialTheme.colorScheme.onBackground
        }
        OutlinedButton(
            modifier = Modifier
                .fillMaxWidth(),
            border = if (isError) {
                BorderStroke(ButtonDefaults.outlinedButtonBorder.width, MaterialTheme.colorScheme.error)
            } else {
                ButtonDefaults.outlinedButtonBorder
            },
            shape = ShapeDefaults.ExtraSmall,
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
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                tint = color.copy(alpha = 0.74f), //Also test 0.60f
                contentDescription = "Dropdown indicator",
            )
        }
    }
}
