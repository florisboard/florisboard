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

import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.app.components.SegmentedListColumn
import dev.patrickgold.florisboard.ime.extension.ExtensionComponent
import org.florisboard.lib.compose.stringRes

@Composable
fun ExtensionComponentListColumn(
    title: String,
    components: List<ExtensionComponent>,
    modifier: Modifier = Modifier,
    onDeleteComponent: ((ExtensionComponent) -> Unit)? = null,
    onEditComponent: ((ExtensionComponent) -> Unit)? = null,
) {
    SegmentedListColumn(modifier, title) {
        if (components.isNotEmpty()) {
            val count = components.size
            for ((index, component) in components.withIndex()) {
                ExtensionComponentListItem(
                    shapes = ListItemDefaults.segmentedShapes(index, count),
                    component = component,
                    onDelete = onDeleteComponent?.let { { it(component) } },
                    onEdit = onEditComponent?.let { { it(component) } },
                )
            }
        } else {
            SegmentedListItem(
                shapes = ListItemDefaults.segmentedShapes(0, 1),
                content = { Text(stringRes(R.string.ext__meta__components_none_found)) },
            )
        }
    }
}
