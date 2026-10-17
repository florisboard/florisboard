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
import dev.patrickgold.florisboard.app.components.segmentedErrorColors
import org.florisboard.lib.compose.stringRes

@Composable
fun ExtensionNotFoundCard(
    extensionId: String,
    modifier: Modifier = Modifier,
) {
    SegmentedListColumn(modifier = modifier) {
        val count = 1
        SegmentedListItem(
            shapes = ListItemDefaults.segmentedShapes(0, count),
            colors = ListItemDefaults.segmentedErrorColors(),
            content = {
                Text(stringRes(R.string.ext__error__not_found_title))
            },
            supportingContent = {
                Text(stringRes(R.string.ext__error__not_found_description, "id" to extensionId))
            },
        )
    }
}
