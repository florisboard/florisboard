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

package dev.patrickgold.florisboard.lib.compose

import androidx.compose.ui.graphics.Color
import dev.patrickgold.jetpref.datastore.model.PreferenceSerializer

object ColorPreferenceSerializer : PreferenceSerializer<Color> {
    @OptIn(ExperimentalStdlibApi::class)
    override fun deserialize(value: String): Color {
        return Color(value.hexToULong())
    }

    @OptIn(ExperimentalStdlibApi::class)
    override fun serialize(value: Color): String = value.value.toHexString()
}
