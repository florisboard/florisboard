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

package dev.patrickgold.florisboard.app.ext

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Create
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.app.components.SegmentedListColumn
import dev.patrickgold.florisboard.ime.extension.ExtensionComponent
import org.florisboard.lib.compose.FlorisButton
import org.florisboard.lib.compose.stringRes

@Composable
fun ExtensionComponentListColumn(
    title: String,
    components: List<ExtensionComponent>,
    modifier: Modifier = Modifier,
    onCreateComponent: (() -> Unit)? = null,
    onEditComponent: ((ExtensionComponent) -> Unit)? = null,
    onDeleteComponent: ((ExtensionComponent) -> Unit)? = null,
) {
    SegmentedListColumn(modifier, title) {
        val offset = if (onCreateComponent != null) 1 else 0
        val count: Int = if (components.isNotEmpty()) {
            components.size + offset
        } else {
            1 + offset
        }
        if (onCreateComponent != null) {
            SegmentedListItem(
                onClick = onCreateComponent,
                shapes = ListItemDefaults.segmentedShapes(0, count),
                verticalAlignment = Alignment.CenterVertically,
                content = {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(stringRes(R.string.ext__editor__create_component__title))
                        Spacer(Modifier.weight(1f))
                        FlorisButton(
                            onClick = onCreateComponent,
                            icon = Icons.Default.Create,
                            text = stringRes(R.string.action__create),
                            colors = ButtonDefaults.filledTonalButtonColors(),
                        )
                    }
                },
            )
        }
        if (components.isNotEmpty()) {
            for ((index, component) in components.withIndex()) {
                ExtensionComponentListItem(
                    shapes = ListItemDefaults.segmentedShapes(offset + index, count),
                    component = component,
                    onDelete = onDeleteComponent?.let { { it(component) } },
                    onEdit = onEditComponent?.let { { it(component) } },
                )
            }
        } else {
            SegmentedListItem(
                shapes = ListItemDefaults.segmentedShapes(offset, count),
                content = { Text(stringRes(R.string.ext__meta__components_none_found)) },
            )
        }
    }
}
