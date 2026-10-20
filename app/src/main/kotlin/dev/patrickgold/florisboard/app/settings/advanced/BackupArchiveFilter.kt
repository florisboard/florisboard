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

package dev.patrickgold.florisboard.app.settings.advanced

import androidx.compose.material3.ListItemDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.state.ToggleableState
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.app.components.CheckboxListItem
import dev.patrickgold.florisboard.app.components.SegmentedListColumn
import dev.patrickgold.florisboard.app.components.TriStateCheckboxListItem
import dev.patrickgold.florisboard.ime.extension.ExtensionType
import kotlinx.serialization.Serializable
import org.florisboard.lib.compose.stringRes

@Serializable
data class BackupArchiveFilter(
    val inclPrefs: Boolean = true,
    val inclThemeExtensions: Boolean = true,
    val inclKeyboard3Extensions: Boolean = true,
    val inclClipboardTextItems: Boolean = false,
    val inclClipboardImageItems: Boolean = false,
    val inclClipboardVideoItems: Boolean = false,
) {
    fun inclClipboard(): ToggleableState {
        return when {
            inclClipboardTextItems && inclClipboardImageItems && inclClipboardVideoItems -> {
                ToggleableState.On
            }
            inclClipboardTextItems || inclClipboardImageItems || inclClipboardVideoItems -> {
                ToggleableState.Indeterminate
            }
            else -> {
                ToggleableState.Off
            }
        }
    }

    fun includesAnyExtension(): Boolean {
        return inclThemeExtensions || inclKeyboard3Extensions
    }

    fun includes(type: ExtensionType): Boolean {
        return when (type) {
            ExtensionType.THEME -> inclThemeExtensions
            ExtensionType.KEYBOARD3 -> inclKeyboard3Extensions
        }
    }

    fun atLeastOneSelected(): Boolean {
        return inclPrefs ||
            inclThemeExtensions ||
            inclKeyboard3Extensions ||
            inclClipboardTextItems ||
            inclClipboardImageItems ||
            inclClipboardVideoItems
    }
}

@Composable
fun BackupArchiveFilterColumn(
    filter: BackupArchiveFilter,
    onUpdateFilter: (BackupArchiveFilter) -> Unit,
    title: String,
) {
    SegmentedListColumn(title = title) {
        val count = 7
        CheckboxListItem(
            onClick = {
                val newFilter = filter.copy(
                    inclPrefs = !filter.inclPrefs,
                )
                onUpdateFilter(newFilter)
            },
            shapes = ListItemDefaults.segmentedShapes(0, count),
            checked = filter.inclPrefs,
            text = stringRes(R.string.backup_and_restore__back_up__files_jetpref_datastore),
        )
        CheckboxListItem(
            onClick = {
                val newFilter = filter.copy(
                    inclThemeExtensions = !filter.inclThemeExtensions,
                )
                onUpdateFilter(newFilter)
            },
            shapes = ListItemDefaults.segmentedShapes(2, count),
            checked = filter.inclThemeExtensions,
            text = stringRes(R.string.backup_and_restore__back_up__files_ime_theme),
        )
        CheckboxListItem(
            onClick = {
                val newFilter = filter.copy(
                    inclKeyboard3Extensions = !filter.inclKeyboard3Extensions,
                )
                onUpdateFilter(newFilter)
            },
            shapes = ListItemDefaults.segmentedShapes(1, count),
            checked = filter.inclKeyboard3Extensions,
            text = stringRes(R.string.backup_and_restore__back_up__files_ime_keyboard),
        )
        TriStateCheckboxListItem(
            onClick = {
                val inclClipboard = filter.inclClipboard()
                val newFilter = if (inclClipboard == ToggleableState.On) {
                    filter.copy(
                        inclClipboardTextItems = false,
                        inclClipboardImageItems = false,
                        inclClipboardVideoItems = false,
                    )
                } else {
                    filter.copy(
                        inclClipboardTextItems = true,
                        inclClipboardImageItems = true,
                        inclClipboardVideoItems = true,
                    )
                }
                onUpdateFilter(newFilter)
            },
            shapes = ListItemDefaults.segmentedShapes(3, count),
            state = filter.inclClipboard(),
            text = stringRes(R.string.backup_and_restore__back_up__files_clipboard_history),
        )
        CheckboxListItem(
            onClick = {
                val newFilter = filter.copy(
                    inclClipboardTextItems = !filter.inclClipboardTextItems,
                )
                onUpdateFilter(newFilter)
            },
            shapes = ListItemDefaults.segmentedShapes(4, count),
            checked = filter.inclClipboardTextItems,
            text = stringRes(R.string.backup_and_restore__back_up__files_clipboard_history__clipboard_text_items),
            isSecondaryListItem = true,
        )
        CheckboxListItem(
            onClick = {
                val newFilter = filter.copy(
                    inclClipboardImageItems = !filter.inclClipboardImageItems,
                )
                onUpdateFilter(newFilter)
            },
            shapes = ListItemDefaults.segmentedShapes(5, count),
            checked = filter.inclClipboardImageItems,
            text = stringRes(R.string.backup_and_restore__back_up__files_clipboard_history__clipboard_image_items),
            isSecondaryListItem = true,
        )
        CheckboxListItem(
            onClick = {
                val newFilter = filter.copy(
                    inclClipboardVideoItems = !filter.inclClipboardVideoItems,
                )
                onUpdateFilter(newFilter)
            },
            shapes = ListItemDefaults.segmentedShapes(6, count),
            checked = filter.inclClipboardVideoItems,
            text = stringRes(R.string.backup_and_restore__back_up__files_clipboard_history__clipboard_video_items),
            isSecondaryListItem = true,
        )

    }
}
