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
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ListItemColors
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.ListItemShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
    shapes: ListItemShapes,
    selected: Boolean,
    content: @Composable () -> Unit,
    supportingContent: @Composable (() -> Unit)? = null,
) {
    SegmentedListItem(
        onClick = onClick,
        shapes = shapes,
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
