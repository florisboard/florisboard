/*
 * Copyright (C) 2022 Patrick Goldinger
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

package dev.patrickgold.florisboard.ime.smartbar.quickaction

import android.content.Context
import dev.patrickgold.florisboard.editorInstance
import dev.patrickgold.florisboard.ime.keyboard.KeyData
import dev.patrickgold.florisboard.ime.text.key.KeyCode
import dev.patrickgold.florisboard.keyboardManager
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed class QuickAction {
    open fun onPointerDown(context: Context) = Unit

    open fun onPointerUp(context: Context) = Unit

    open fun onPointerCancel(context: Context) = Unit

    @SerialName("insert_key")
    data class InsertKey(val data: KeyData) : QuickAction() {
        override fun onPointerDown(context: Context) {
            val keyboardManager by context.keyboardManager()
            keyboardManager.inputEventDispatcher.sendDown(data)
        }

        override fun onPointerUp(context: Context) {
            val keyboardManager by context.keyboardManager()
            keyboardManager.inputEventDispatcher.sendUp(data)
            if (!keyboardManager.inputEventDispatcher.isRepeatable(data) && data.code != KeyCode.TOGGLE_ACTIONS_OVERFLOW) {
                keyboardManager.activeState.isActionsOverflowVisible = false
            }
        }

        override fun onPointerCancel(context: Context) {
            val keyboardManager by context.keyboardManager()
            keyboardManager.inputEventDispatcher.sendCancel(data)
        }
    }

    @SerialName("insert_text")
    data class InsertText(val data: String) : QuickAction() {
        override fun onPointerUp(context: Context) {
            val editorInstance by context.editorInstance()
            editorInstance.commitText(data)
        }
    }
}
