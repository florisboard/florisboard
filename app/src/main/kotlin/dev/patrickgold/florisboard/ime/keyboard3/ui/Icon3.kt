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

package dev.patrickgold.florisboard.ime.keyboard3.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowRightAlt
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.KeyboardReturn
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.automirrored.outlined.Assignment
import androidx.compose.material.icons.automirrored.outlined.Backspace
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.ContentPasteGo
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.FontDownload
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.KeyboardDoubleArrowUp
import androidx.compose.material.icons.filled.KeyboardHide
import androidx.compose.material.icons.filled.KeyboardVoice
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.SentimentSatisfiedAlt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SpaceBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.ime.editor.ImeOptions
import dev.patrickgold.florisboard.ime.keyboard3.ImeIcons
import dev.patrickgold.florisboard.ime.keyboard3.LocalImeController
import dev.patrickgold.florisboard.ime.window.ImeWindowMode
import dev.patrickgold.florisboard.ime.window.LocalWindowController
import dev.patrickgold.florisboard.lib.compose.vectorResource
import org.florisboard.lib.compose.icons.ForwardDelete
import org.florisboard.lib.snygg.ui.SnyggIcon
import org.k3lp.lib.text.K3Descriptor

@Composable
fun Icon3(
    value: K3Descriptor,
    modifier: Modifier = Modifier,
    elementName: String? = null,
) {
    val context = LocalContext.current
    val imeController = LocalImeController.current
    val windowController = LocalWindowController.current

    val imeState by imeController.activeState.collectAsState()
    val imeOptions by remember { derivedStateOf { imeState.editor.info.imeOptions } }
    val inputAttributes by remember { derivedStateOf { imeState.editor.info.inputAttributes } }
    val debugShowDragAndDropHelpers by remember {
        derivedStateOf { imeState.flags.debugShowDragAndDropHelpers }
    }

    val windowConfig by windowController.activeWindowConfig.collectAsState()
    val windowMode by remember { derivedStateOf { windowConfig.mode } }

    val imageVector = remember(value, imeOptions, inputAttributes, windowMode) {
        when (value) {
            ImeIcons.ArrowDown -> Icons.Default.KeyboardArrowDown
            ImeIcons.ArrowLeft -> Icons.AutoMirrored.Filled.KeyboardArrowLeft
            ImeIcons.ArrowRight -> Icons.AutoMirrored.Filled.KeyboardArrowRight
            ImeIcons.ArrowUp -> Icons.Default.KeyboardArrowUp
            ImeIcons.Backspace -> Icons.AutoMirrored.Outlined.Backspace
            ImeIcons.ClipboardClearPrimaryClip -> Icons.Default.DeleteSweep
            ImeIcons.ClipboardCopy -> Icons.Default.ContentCopy
            ImeIcons.ClipboardCut -> Icons.Default.ContentCut
            ImeIcons.ClipboardPaste -> Icons.Default.ContentPasteGo
            ImeIcons.Close -> Icons.Default.Close
            ImeIcons.Delete -> Icons.AutoMirrored.Default.ForwardDelete
            ImeIcons.DragMarker -> {
                if (debugShowDragAndDropHelpers) Icons.Default.Close else null
            }
            ImeIcons.Enter -> {
                if (imeOptions.flagNoEnterAction || inputAttributes.flagTextMultiLine) {
                    Icons.AutoMirrored.Filled.KeyboardReturn
                } else {
                    when (imeOptions.action) {
                        ImeOptions.Action.DONE -> Icons.Default.Done
                        ImeOptions.Action.GO -> Icons.AutoMirrored.Filled.ArrowRightAlt
                        ImeOptions.Action.NEXT -> Icons.AutoMirrored.Filled.ArrowRightAlt
                        ImeOptions.Action.NONE -> Icons.AutoMirrored.Filled.KeyboardReturn
                        ImeOptions.Action.PREVIOUS -> Icons.AutoMirrored.Filled.ArrowRightAlt
                        ImeOptions.Action.SEARCH -> Icons.Default.Search
                        ImeOptions.Action.SEND -> Icons.AutoMirrored.Filled.Send
                        ImeOptions.Action.UNSPECIFIED -> Icons.AutoMirrored.Filled.KeyboardReturn
                    }
                }
            }
            ImeIcons.HideKeyboard -> Icons.Default.KeyboardHide
            ImeIcons.LanguageSwitch -> Icons.Default.Language
            ImeIcons.ClipboardPanel -> Icons.AutoMirrored.Outlined.Assignment
            ImeIcons.MediaPanel -> Icons.Default.SentimentSatisfiedAlt
            ImeIcons.TextPanel -> context.vectorResource(R.drawable.ic_abc)
            ImeIcons.Noop -> Icons.Default.Close
            ImeIcons.Redo -> Icons.AutoMirrored.Filled.Redo
            ImeIcons.ShowKeyboard -> Icons.Default.KeyboardDoubleArrowUp // TODO
            ImeIcons.SelectAll -> Icons.Default.SelectAll
            ImeIcons.Settings -> Icons.Default.Settings
            ImeIcons.SpaceBar -> Icons.Default.SpaceBar
            ImeIcons.ToggleActionsOverflow -> Icons.Default.MoreHoriz
            ImeIcons.ToggleAutocorrect -> Icons.Default.FontDownload
            ImeIcons.ToggleCompactLayout -> context.vectorResource(R.drawable.ic_accessibility_one_handed)
            ImeIcons.ToggleFloatingWindow -> when (windowMode) {
                ImeWindowMode.FIXED -> context.vectorResource(R.drawable.ic_floating_keyboard)
                ImeWindowMode.FLOATING -> context.vectorResource(R.drawable.ic_floating_keyboard_disable)
            }
            ImeIcons.ToggleResizeMode -> context.vectorResource(R.drawable.ic_resize)
            ImeIcons.Undo -> Icons.AutoMirrored.Filled.Undo
            ImeIcons.Voice -> Icons.Default.KeyboardVoice
            // TODO shift???
            // TODO incognito mode???
            // TODO char width/kata/hira icons???
            else -> null
        }
    }

    if (imageVector != null) {
        SnyggIcon(
            imageVector = imageVector,
            modifier = modifier,
            elementName = elementName,
        )
    }
}
