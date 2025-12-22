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

enum class ImeWindowResizeHandle(
    val left: Boolean = false,
    val top: Boolean = false,
    val right: Boolean = false,
    val bottom: Boolean = false,
) {
    LEFT(left = true),
    TOP_LEFT(top = true, left = true),
    TOP(top = true),
    TOP_RIGHT(top = true, right = true),
    RIGHT(right = true),
    BOTTOM_RIGHT(bottom = true, right = true),
    BOTTOM(bottom = true),
    BOTTOM_LEFT(bottom = true, left = true),
}
