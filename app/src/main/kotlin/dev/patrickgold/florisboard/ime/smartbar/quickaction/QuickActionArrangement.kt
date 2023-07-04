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

import dev.patrickgold.florisboard.ime.text.keyboard.TextKeyData
import dev.patrickgold.florisboard.lib.io.DefaultJsonConfig
import dev.patrickgold.jetpref.datastore.model.PreferenceSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.plus
import kotlinx.serialization.modules.polymorphic

private val QuickActionJsonConfig = Json(DefaultJsonConfig) {
    classDiscriminator = "$"
    encodeDefaults = false
    ignoreUnknownKeys = true
    isLenient = false

    serializersModule += SerializersModule {
        polymorphic(QuickAction::class) {
            subclass(QuickAction.InsertKey::class, QuickAction.InsertKey.serializer())
            subclass(QuickAction.InsertText::class, QuickAction.InsertText.serializer())
            default { QuickAction.InsertKey.serializer() }
        }
    }
}

@Serializable
data class QuickActionArrangement(
    val stickyAction: QuickAction?,
    val dynamicActions: List<QuickAction>,
    val hiddenActions: List<QuickAction>,
) {
    fun contains(action: QuickAction): Boolean {
        return stickyAction == action || dynamicActions.contains(action) || hiddenActions.contains(action)
    }

    companion object {
        val Default = QuickActionArrangement(
            stickyAction = QuickAction.InsertKey(TextKeyData.VOICE_INPUT),
            dynamicActions = listOf(
                QuickAction.InsertKey(TextKeyData.UNDO),
                QuickAction.InsertKey(TextKeyData.REDO),
                QuickAction.InsertKey(TextKeyData.SETTINGS),
                QuickAction.InsertKey(TextKeyData.TOGGLE_INCOGNITO_MODE),
                QuickAction.InsertKey(TextKeyData.IME_UI_MODE_CLIPBOARD),
                QuickAction.InsertKey(TextKeyData.IME_UI_MODE_MEDIA),
                QuickAction.InsertKey(TextKeyData.COMPACT_LAYOUT_TO_RIGHT),
                QuickAction.InsertKey(TextKeyData.TOGGLE_AUTOCORRECT),
                QuickAction.InsertKey(TextKeyData.ARROW_UP),
                QuickAction.InsertKey(TextKeyData.ARROW_DOWN),
                QuickAction.InsertKey(TextKeyData.ARROW_LEFT),
                QuickAction.InsertKey(TextKeyData.ARROW_RIGHT),
                QuickAction.InsertKey(TextKeyData.CLIPBOARD_CLEAR_PRIMARY_CLIP),
                QuickAction.InsertKey(TextKeyData.CLIPBOARD_COPY),
                QuickAction.InsertKey(TextKeyData.CLIPBOARD_CUT),
                QuickAction.InsertKey(TextKeyData.CLIPBOARD_PASTE),
                QuickAction.InsertKey(TextKeyData.CLIPBOARD_SELECT_ALL),
            ),
            hiddenActions = listOf(
            ),
        )
    }

    object Serializer : PreferenceSerializer<QuickActionArrangement> {
        override fun serialize(value: QuickActionArrangement): String {
            return QuickActionJsonConfig.encodeToString(value)
        }

        override fun deserialize(value: String): QuickActionArrangement {
            return QuickActionJsonConfig.decodeFromString(value)
        }
    }
}
