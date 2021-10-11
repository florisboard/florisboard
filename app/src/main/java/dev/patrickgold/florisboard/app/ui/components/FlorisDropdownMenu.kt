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

package dev.patrickgold.florisboard.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentSize
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import dev.patrickgold.florisboard.R

@Composable
fun FlorisDropdownMenu(
    items: List<String>,
    expanded: Boolean,
    selectedIndex: Int,
    modifier: Modifier = Modifier,
    onSelectItem: (Int) -> Unit = { },
    onExpandRequest: () -> Unit = { },
    onDismissRequest: () -> Unit = { },
) {
    Box(modifier = modifier.wrapContentSize(Alignment.TopStart)) {
        val indicatorRotation by animateFloatAsState(targetValue = if (expanded) 180f else 0f)
        val index = selectedIndex.coerceIn(items.indices)
        OutlinedButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = { onExpandRequest() },
        ) {
            Text(
                modifier = Modifier.weight(1.0f),
                text = items[index],
                textAlign = TextAlign.Start,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
                color = MaterialTheme.colors.onBackground,
            )
            Icon(
                modifier = Modifier.rotate(indicatorRotation),
                painter = painterResource(R.drawable.ic_keyboard_arrow_down),
                tint = MaterialTheme.colors.onBackground.copy(alpha = ContentAlpha.medium),
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
                    Text(text = item)
                }
            }
        }
    }
}
