/*
 * Copyright (C) 2025-2026 The FlorisBoard Contributors
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

import dev.patrickgold.florisboard.lib.devtools.flogError
import dev.patrickgold.jetpref.datastore.model.PreferenceSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Alias for mapping form factor guesses to window configs.
 */
typealias ImeWindowConfigByType = Map<ImeFormFactor.Type, ImeWindowConfig>

/**
 * Describes the window configuration, which is used to persist the user-preferred modes and sizes to prefs. For
 * each [ImeFormFactor.Type], a separate window config is present.
 *
 * @property mode The current window mode. Determines if the fixed or floating mode and props should be used for
 *  calculations and logic decisions.
 * @property fixedMode Describes the fixed sub-mode.
 * @property fixedProps Describes the props per fixed sub-mode. May not have a mapping for a given sub-mode, in
 *  which case the window constraints should be queried for default props.
 * @property floatingMode Describes the floating sub-mode.
 * @property floatingProps Describes the props per floating sub-mode. May not have a mapping for a given sub-mode, in
 *  which case the window constraints should be queried for default props.
 */
@Serializable
data class ImeWindowConfig(
    val mode: ImeWindowMode,
    val fixedMode: ImeWindowMode.Fixed = ImeWindowMode.Fixed.NORMAL,
    val fixedProps: Map<ImeWindowMode.Fixed, ImeWindowProps.Fixed> = emptyMap(),
    val floatingMode: ImeWindowMode.Floating = ImeWindowMode.Floating.NORMAL,
    val floatingProps: Map<ImeWindowMode.Floating, ImeWindowProps.Floating> = emptyMap(),
) {
    /**
     * Helper for serializing [ImeWindowConfigByType] to prefs.
     */
    object ByTypeSerializer : PreferenceSerializer<ImeWindowConfigByType> {
        override fun serialize(value: ImeWindowConfigByType): String {
            return Json.encodeToString(value)
        }

        override fun deserialize(value: String): ImeWindowConfigByType {
            return try {
                Json.decodeFromString(value)
            } catch (e: Throwable) {
                flogError { "Failed to deserialize ImeWindowConfig.ByType: ${e.message}" }
                emptyMap()
            }
        }
    }

    companion object {
        /**
         * The default window config, which has empty prop mappings.
         */
        val Default = ImeWindowConfig(
            mode = ImeWindowMode.FIXED,
            fixedMode = ImeWindowMode.Fixed.NORMAL,
            floatingMode = ImeWindowMode.Floating.NORMAL,
        )
    }
}
