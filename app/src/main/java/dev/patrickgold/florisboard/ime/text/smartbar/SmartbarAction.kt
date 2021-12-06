/*
 * Copyright (C) 2021 Patrick Goldinger
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

package dev.patrickgold.florisboard.ime.text.smartbar

import androidx.lifecycle.LiveData
import dev.patrickgold.florisboard.app.prefs.florisPreferenceModel
import dev.patrickgold.florisboard.ime.keyboard.KeyData
import dev.patrickgold.florisboard.ime.text.keyboard.TextKeyData
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val SmartbarActionJsonConfig = Json {
    classDiscriminator = "$"
    ignoreUnknownKeys = true
    isLenient = false
}

private val SmartbarActionSet = setOf<SmartbarAction>(
    SmartbarAction.Key(TextKeyData.UNDO),
    SmartbarAction.Key(TextKeyData.REDO),
    SmartbarAction.Key(TextKeyData.SETTINGS),
    SmartbarAction.Key(TextKeyData.IME_UI_MODE_MEDIA),
    SmartbarAction.Key(TextKeyData.COMPACT_LAYOUT_TO_RIGHT),
    SmartbarAction.Key(TextKeyData.IME_UI_MODE_CLIPBOARD),
    SmartbarAction.Key(TextKeyData.IME_UI_MODE_CLIPBOARD),
    SmartbarAction.Key(TextKeyData.IME_UI_MODE_CLIPBOARD),
    SmartbarAction.Key(TextKeyData.IME_UI_MODE_CLIPBOARD),
)

@Serializable
sealed class SmartbarAction {
    companion object {
        fun from(str: String): SmartbarAction {
            return SmartbarActionJsonConfig.decodeFromString(str)
        }
    }

    override fun toString(): String {
        return SmartbarActionJsonConfig.encodeToString(this)
    }

    @SerialName("key")
    data class Key(val data: KeyData) : SmartbarAction()
}

class SmartbarActions : LiveData<List<SmartbarAction>>(SmartbarActionSet.toList()) {
    private val prefs by florisPreferenceModel()

    init {
        prefs.smartbar.actions.observeForever { strList ->
            parseStrList(strList)
        }
    }

    override fun getValue(): List<SmartbarAction> {
        return super.getValue()!!
    }

    override fun setValue(value: List<SmartbarAction>) {
        dispatchValue(value, persist = true)
    }

    override fun postValue(value: List<SmartbarAction>) {
        dispatchValue(value, persist = true)
    }

    private fun parseStrList(strList: String) {
        val list = SmartbarActionJsonConfig.decodeFromString<List<SmartbarAction>>(strList)
        val convList = if (list.isEmpty()) {
            SmartbarActionSet.toList()
        } else {
            list.intersect(SmartbarActionSet).union(SmartbarActionSet).toList()
        }
        dispatchValue(convList, persist = false)
    }

    private fun dispatchValue(value: List<SmartbarAction>, persist: Boolean) {
        try {
            super.setValue(value)
        } catch (e: Exception) {
            super.postValue(value)
        }
        if (persist) {
            prefs.smartbar.actions.set(SmartbarActionJsonConfig.encodeToString(value))
        }
    }
}
