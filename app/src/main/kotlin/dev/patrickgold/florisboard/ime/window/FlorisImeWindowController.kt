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

import androidx.compose.ui.unit.IntRect
import dev.patrickgold.florisboard.FlorisImeService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class FlorisImeWindowController(val ims: FlorisImeService) {
    val activeImeInsets: StateFlow<FlorisImeInsets?>
        field = MutableStateFlow(null)

    val isWindowShown: StateFlow<Boolean>
        field = MutableStateFlow(false)

    fun updateImeInsets(rootBounds: IntRect, windowBounds: IntRect) {
        activeImeInsets.value = FlorisImeInsets(rootBounds, windowBounds)
    }

    fun onWindowShown(): Boolean {
        return isWindowShown.compareAndSet(expect = false, update = true)
    }

    fun onWindowHidden(): Boolean {
        return isWindowShown.compareAndSet(expect = true, update = false)
    }
}
