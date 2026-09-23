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

package dev.patrickgold.florisboard.ime.keyboard3.hint

import androidx.compose.ui.Alignment

enum class KeyHintPlacement(val alignment: Alignment) {
    TOP_START(Alignment.TopStart),
    TOP_CENTER(Alignment.TopCenter),
    TOP_END(Alignment.TopEnd),
    CENTER_START(Alignment.CenterStart),
    CENTER_END(Alignment.CenterEnd),
    BOTTOM_START(Alignment.BottomStart),
    BOTTOM_CENTER(Alignment.BottomCenter),
    BOTTOM_END(Alignment.BottomEnd),
}
