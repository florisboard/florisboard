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

package dev.patrickgold.florisboard.lib.util

object UnitUtils {
    private const val KiB = (1024).toFloat()
    private const val MiB = (1024 * 1024).toFloat()
    private const val GiB = (1024 * 1024 * 1024).toFloat()

    fun formatMemorySize(sizeBytes: Long): String {
        return when {
            sizeBytes >= GiB -> String.format("%.2f GiB", sizeBytes / GiB)
            sizeBytes >= MiB -> String.format("%.2f MiB", sizeBytes / MiB)
            sizeBytes >= KiB -> String.format("%.2f KiB", sizeBytes / KiB)
            else -> String.format("%d bytes")
        }
    }
}
