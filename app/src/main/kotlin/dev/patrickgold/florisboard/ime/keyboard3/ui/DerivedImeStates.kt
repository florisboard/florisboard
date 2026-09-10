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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import dev.patrickgold.florisboard.clipboardManager
import dev.patrickgold.florisboard.ime.keyboard3.ImeActions
import dev.patrickgold.florisboard.ime.keyboard3.LocalImeController
import dev.patrickgold.florisboard.subtypeManager
import org.florisboard.lib.android.AndroidKeyguardManager
import org.florisboard.lib.android.systemService
import org.k3lp.lib.text.K3Descriptor
import org.k3lp.lib.text.K3String
import org.k3lp.lib.text.K3StringOrDescriptor

@Composable
fun rememberDerivedEnabledState(
    output: K3StringOrDescriptor,
): State<Boolean> {
    val context = LocalContext.current

    val clipboardManager by context.clipboardManager()
    val primaryClip by clipboardManager.primaryClipFlow.collectAsState()

    val imeController = LocalImeController.current
    val imeState by imeController.activeState.collectAsState()

    val subtypeManager by context.subtypeManager()
    val subtypes by subtypeManager.subtypesFlow.collectAsState()

    val androidKeyguardManager = remember {
        context.systemService(AndroidKeyguardManager::class)
    }
    // TODO observe it.isDeviceLocked || it.isKeyguardLocked
    val isDeviceLocked by remember { mutableStateOf(false) }

    return remember(output) {
        derivedStateOf {
            val isSelectionCollapsed = imeState.content.selection.isCollapsed()
            val isPrimaryClipPastable = clipboardManager.canBePasted(primaryClip)
            val isRichInputEditor = imeState.editor.info.isRichInputEditor
            val isMultiLingual = subtypes.size > 1
            val isDeviceLocked = isDeviceLocked
            when (output) {
                is K3Descriptor -> when (output.name) {
                    ImeActions.ClipboardCopy.name, ImeActions.ClipboardCut.name -> {
                        !isSelectionCollapsed
                    }
                    ImeActions.ClipboardPaste.name -> {
                        !isDeviceLocked && isPrimaryClipPastable
                    }
                    ImeActions.ClipboardClearPrimaryClip.name -> {
                        isPrimaryClipPastable
                    }
                    ImeActions.SelectAll.name -> {
                        isRichInputEditor
                    }
                    ImeActions.LanguageSwitch.name -> {
                        isMultiLingual
                    }
                    else -> true
                }
                is K3String -> true
            }
        }
    }
}
