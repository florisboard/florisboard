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

package dev.patrickgold.florisboard.ime.keyboard3.touch

import dev.patrickgold.florisboard.ime.keyboard3.ImeActions
import dev.patrickgold.jetpref.datastore.model.PreferenceSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.florisboard.lib.kotlin.tryOrNull

@Serializable
data class FnKeyArrangement(
    val simpleAction: FnKeyAction,
    val longPressActions: List<FnKeyAction>,
    val layeredActions: List<List<FnKeyAction>>,
) {
    object Serializer : PreferenceSerializer<FnKeyArrangement> {
        override fun serialize(value: FnKeyArrangement): String? {
            return tryOrNull { Json.encodeToString(value) }
        }

        override fun deserialize(value: String): FnKeyArrangement? {
            return tryOrNull { Json.decodeFromString(value) }
        }
    }

    companion object {
        val Default = FnKeyArrangement(
            simpleAction = FnKeyAction(ImeActions.SwitchToNextSubtype),
            longPressActions = listOf(
                FnKeyAction(ImeActions.ShowInputMethodPicker),
            ),
            layeredActions = emptyList(),
        )
    }
}
