/*
 * Copyright (C) 2022-2026 The FlorisBoard Contributors
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

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ListItemShapes
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.ime.extension.ExtensionComponent
import dev.patrickgold.florisboard.ime.extension.ThemeExtension
import org.florisboard.lib.compose.FlorisButton
import org.florisboard.lib.compose.stringRes

@Composable
fun ExtensionComponentListItem(
    shapes: ListItemShapes,
    component: ExtensionComponent,
    onDelete: (() -> Unit)? = null,
    onEdit: (() -> Unit)? = null,
) {
    SegmentedListItem(
        shapes = shapes,
        overlineContent = { Text(component.id) },
        content = { Text(component.name) },
        supportingContent = {
            Column {
                when (component) {
                    is ThemeExtension.ThemeComponent -> {
                        val text = remember(
                            component.authors, component.isNightTheme, component.stylesheetPath(),
                        ) {
                            buildString {
                                appendLine("authors = ${component.authors}")
                                appendLine("isNightTheme = ${component.isNightTheme}")
                                append("stylesheetPath = ${component.stylesheetPath()}")
                            }
                        }
                        Text(
                            text = text,
                            style = MaterialTheme.typography.bodyMedium,
                            color = LocalContentColor.current,
                        )
                    }

                    else -> {}
                }
                if (onDelete != null || onEdit != null) {
                    Row(Modifier.fillMaxWidth()) {
                        if (onDelete != null) {
                            FlorisButton(
                                onClick = onDelete,
                                icon = Icons.Default.Delete,
                                text = stringRes(R.string.action__delete),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer,
                                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                                ),
                            )
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        if (onEdit != null) {
                            FlorisButton(
                                onClick = onEdit,
                                icon = Icons.Default.Edit,
                                text = stringRes(R.string.action__edit),
                                colors = ButtonDefaults.filledTonalButtonColors(),
                            )
                        }
                    }
                }
            }
        },
    )
}
