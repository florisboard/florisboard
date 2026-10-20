/*
 * Copyright (C) 2026 The FlorisBoard Contributors
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

package dev.patrickgold.florisboard.app.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ListItemColors
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.ListItemShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.material3.TriStateCheckbox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.dp
import dev.patrickgold.florisboard.app.apptheme.SegmentedListColumnVerticalPadding

@Composable
fun SegmentedListTitle(
    title: String,
    modifier: Modifier = Modifier,
) {
    Text(
        modifier = modifier
            .padding(top = SegmentedListColumnVerticalPadding),
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
fun SegmentedListColumn(
    modifier: Modifier = Modifier,
    title: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    if (title != null) {
        SegmentedListTitle(title)
    }
    Column(
        modifier = modifier
            .padding(vertical = SegmentedListColumnVerticalPadding),
        verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
        content = content,
    )
}

@Composable
fun ListItemDefaults.segmentedErrorColors(): ListItemColors {
    return segmentedColors(
        leadingContentColor = MaterialTheme.colorScheme.error,
        contentColor = MaterialTheme.colorScheme.error,
        supportingContentColor = MaterialTheme.colorScheme.error,
    )
}

@Composable
fun SegmentedRadioListItem(
    onClick: () -> Unit,
    selected: Boolean,
    shapes: ListItemShapes,
    content: @Composable () -> Unit,
    supportingContent: @Composable (() -> Unit)? = null,
) {
    SegmentedListItem(
        onClick = onClick,
        shapes = shapes,
        verticalAlignment = Alignment.CenterVertically,
        leadingContent = {
            RadioButton(
                selected = selected,
                onClick = null,
            )
        },
        content = content,
        supportingContent = supportingContent,
    )
}

@Composable
internal fun CheckboxListItem(
    onClick: () -> Unit,
    shapes: ListItemShapes,
    checked: Boolean,
    text: String,
    isSecondaryListItem: Boolean = false
) {
    SegmentedListItem(
        onClick = onClick,
        shapes = shapes,
        leadingContent = {
            Row {
                if (isSecondaryListItem) {
                    Spacer(modifier = Modifier.width(40.dp))
                }
                Checkbox(
                    checked = checked,
                    onCheckedChange = null,
                )
            }
        },
        content = { Text(text) },
    )
}

@Composable
internal fun TriStateCheckboxListItem(
    onClick: () -> Unit,
    shapes: ListItemShapes,
    state: ToggleableState,
    text: String,
    isSecondaryListItem: Boolean = false,
) {
    SegmentedListItem(
        onClick = onClick,
        shapes = shapes,
        leadingContent = {
            Row {
                if (isSecondaryListItem) {
                    Spacer(modifier = Modifier.width(40.dp))
                }
                TriStateCheckbox(
                    state = state,
                    onClick = null,
                )
            }
        },
        content = { Text(text) },
    )
}
