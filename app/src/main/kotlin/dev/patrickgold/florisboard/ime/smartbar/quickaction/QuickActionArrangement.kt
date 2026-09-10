/*
 * Copyright (C) 2022-2025 The FlorisBoard Contributors
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

import dev.patrickgold.florisboard.ime.keyboard3.ImeActions
import dev.patrickgold.florisboard.lib.io.DefaultJsonConfig
import dev.patrickgold.jetpref.datastore.model.PreferenceSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.plus
import kotlinx.serialization.modules.polymorphic
import kotlin.contracts.contract

val QuickActionJsonConfig = Json(DefaultJsonConfig) {
    classDiscriminator = "$"
    encodeDefaults = false
    ignoreUnknownKeys = true
    isLenient = false

    serializersModule += SerializersModule {
        polymorphic(QuickAction::class) {
            subclass(QuickAction.InsertKey::class, QuickAction.InsertKey.serializer())
            subclass(QuickAction.InsertText::class, QuickAction.InsertText.serializer())
            subclass(QuickAction.InsertK3Descriptor::class, QuickAction.InsertK3Descriptor.serializer())
            defaultDeserializer { QuickAction.InsertKey.serializer() }
        }
    }
}

@Serializable
data class QuickActionArrangement(
    val stickyAction: QuickAction?,
    val dynamicActions: List<QuickAction>,
    val hiddenActions: List<QuickAction>,
) {
    operator fun contains(action: QuickAction): Boolean {
        return stickyAction == action || dynamicActions.contains(action) || hiddenActions.contains(action)
    }

    inline fun forEach(block: (QuickAction) -> Unit) {
        contract {
            callsInPlace(block)
        }
        stickyAction?.let { block(it) }
        dynamicActions.forEach { block(it) }
        hiddenActions.forEach { block(it) }
    }

    fun distinct(): QuickActionArrangement {
        val distinctSet = mutableSetOf<QuickAction>()
        if (stickyAction != null) {
            distinctSet.add(stickyAction)
        }
        val distinctDynamicActions = dynamicActions.filter { distinctSet.add(it) }
        val distinctHiddenActions = hiddenActions.filter { distinctSet.add(it) }
        return QuickActionArrangement(
            stickyAction = stickyAction,
            dynamicActions = distinctDynamicActions,
            hiddenActions = distinctHiddenActions,
        )
    }

    fun migrateToK3Descriptors(): QuickActionArrangement {
        return QuickActionArrangement(
            stickyAction = stickyAction?.migrateToK3DescriptorOrNull(),
            dynamicActions = dynamicActions.mapNotNull { it.migrateToK3DescriptorOrNull() },
            hiddenActions = hiddenActions.mapNotNull { it.migrateToK3DescriptorOrNull() },
        )
    }

    companion object {
        val Default = QuickActionArrangement(
            stickyAction = QuickAction.InsertK3Descriptor(ImeActions.ExternalVoiceInput),
            dynamicActions = listOf(
                QuickAction.InsertK3Descriptor(ImeActions.ToggleFloatingWindow),
                QuickAction.InsertK3Descriptor(ImeActions.ToggleResizeMode),
                QuickAction.InsertK3Descriptor(ImeActions.Undo),
                QuickAction.InsertK3Descriptor(ImeActions.Redo),
                QuickAction.InsertK3Descriptor(ImeActions.ShowClipboardPanel),
                QuickAction.InsertK3Descriptor(ImeActions.Settings),
                QuickAction.InsertK3Descriptor(ImeActions.ShowMediaPanel),
                QuickAction.InsertK3Descriptor(ImeActions.ToggleCompactLayout),
                QuickAction.InsertK3Descriptor(ImeActions.ToggleAutocorrect),
                QuickAction.InsertK3Descriptor(ImeActions.TogglePersonalizedLearning),
                QuickAction.InsertK3Descriptor(ImeActions.ArrowUp),
                QuickAction.InsertK3Descriptor(ImeActions.ArrowDown),
                QuickAction.InsertK3Descriptor(ImeActions.ArrowLeft),
                QuickAction.InsertK3Descriptor(ImeActions.ArrowRight),
                QuickAction.InsertK3Descriptor(ImeActions.ClipboardClearPrimaryClip),
                QuickAction.InsertK3Descriptor(ImeActions.ClipboardCopy),
                QuickAction.InsertK3Descriptor(ImeActions.ClipboardCut),
                QuickAction.InsertK3Descriptor(ImeActions.ClipboardPaste),
                QuickAction.InsertK3Descriptor(ImeActions.SelectAll),
                QuickAction.InsertK3Descriptor(ImeActions.LanguageSwitch),
                QuickAction.InsertK3Descriptor(ImeActions.Delete),
                QuickAction.InsertK3Descriptor(ImeActions.HideImeWindow),
            ),
            hiddenActions = emptyList(),
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
