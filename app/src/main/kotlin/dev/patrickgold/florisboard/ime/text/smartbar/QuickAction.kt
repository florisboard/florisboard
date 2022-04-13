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
import dev.patrickgold.florisboard.app.florisPreferenceModel
import dev.patrickgold.florisboard.ime.keyboard.KeyData
import dev.patrickgold.florisboard.ime.text.keyboard.TextKeyData
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val QuickActionJsonConfig = Json {
    classDiscriminator = "$"
    ignoreUnknownKeys = true
    isLenient = false
}

private val QuickActionSet = setOf<QuickAction>(
    QuickAction.Key(TextKeyData.UNDO),
    QuickAction.Key(TextKeyData.REDO),
    QuickAction.Key(TextKeyData.SETTINGS),
    QuickAction.Key(TextKeyData.IME_UI_MODE_MEDIA),
    QuickAction.Key(TextKeyData.COMPACT_LAYOUT_TO_RIGHT),
    QuickAction.Key(TextKeyData.IME_UI_MODE_CLIPBOARD),
)

@Serializable
sealed class QuickAction {
    companion object {
        fun from(str: String): QuickAction {
            return QuickActionJsonConfig.decodeFromString(str)
        }
    }

    override fun toString(): String {
        return QuickActionJsonConfig.encodeToString(this)
    }

    @SerialName("key")
    data class Key(val data: KeyData) : QuickAction()
}

class SmartbarActions : LiveData<List<QuickAction>>(QuickActionSet.toList()) {
    private val prefs by florisPreferenceModel()

    init {
        prefs.smartbar.quickActions.observeForever { strList ->
            parseStrList(strList)
        }
    }

    override fun getValue(): List<QuickAction> {
        return super.getValue()!!
    }

    override fun setValue(value: List<QuickAction>) {
        dispatchValue(value, persist = true)
    }

    override fun postValue(value: List<QuickAction>) {
        dispatchValue(value, persist = true)
    }

    private fun parseStrList(strList: String) {
        val list = QuickActionJsonConfig.decodeFromString<List<QuickAction>>(strList)
        val convList = if (list.isEmpty()) {
            QuickActionSet.toList()
        } else {
            list.intersect(QuickActionSet).union(QuickActionSet).toList()
        }
        dispatchValue(convList, persist = false)
    }

    private fun dispatchValue(value: List<QuickAction>, persist: Boolean) {
        try {
            super.setValue(value)
        } catch (e: Exception) {
            super.postValue(value)
        }
        if (persist) {
            prefs.smartbar.quickActions.set(QuickActionJsonConfig.encodeToString(value))
        }
    }
}
