/*
 * Copyright (C) 2025 The FlorisBoard Contributors
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

package dev.patrickgold.florisboard.ime.window

import dev.patrickgold.jetpref.datastore.model.PreferenceSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class ImeWindowConfig(
    val mode: ImeWindowMode,
    val fixedMode: ImeWindowMode.Fixed,
    val fixedSizes: Map<ImeWindowMode.Fixed, ImeWindowSize.Fixed> = emptyMap(),
    val floatingMode: ImeWindowMode.Floating,
    val floatingSizes: Map<ImeWindowMode.Floating, ImeWindowSize.Floating> = emptyMap(),
) {
    companion object {
        val DefaultPortrait = ImeWindowConfig(
            mode = ImeWindowMode.FIXED,
            fixedMode = ImeWindowMode.Fixed.NORMAL,
            floatingMode = ImeWindowMode.Floating.NORMAL,
        )

        val DefaultLandscape = ImeWindowConfig(
            mode = ImeWindowMode.FIXED,
            fixedMode = ImeWindowMode.Fixed.NORMAL,
            floatingMode = ImeWindowMode.Floating.NORMAL,
        )
    }

    object Serializer : PreferenceSerializer<ImeWindowConfig> {
        private val coercingJson = Json { coerceInputValues = true }

        override fun serialize(value: ImeWindowConfig): String {
            return coercingJson.encodeToString(value)
        }

        override fun deserialize(value: String): ImeWindowConfig {
            return coercingJson.decodeFromString(value)
        }
    }
}
