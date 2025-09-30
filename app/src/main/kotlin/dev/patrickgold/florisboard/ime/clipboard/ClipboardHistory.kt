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

package dev.patrickgold.florisboard.ime.clipboard

import dev.patrickgold.florisboard.ime.clipboard.provider.ClipboardItem

data class ClipboardHistory(val all: List<ClipboardItem>) {
    companion object {
        private const val RECENT_TIMESPAN_MS = 300_000 // 300 sec = 5 min

        val EMPTY = ClipboardHistory(emptyList())
    }

    private val now = System.currentTimeMillis()

    val pinned = all.filter { it.isPinned }
    val unpinned = all.filter { !it.isPinned }
    val recent = unpinned.filter { (now - it.creationTimestampMs) < RECENT_TIMESPAN_MS }
    val other = unpinned.filter { (now - it.creationTimestampMs) >= RECENT_TIMESPAN_MS }
}
