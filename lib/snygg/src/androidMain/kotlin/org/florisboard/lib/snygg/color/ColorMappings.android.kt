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

package org.florisboard.lib.snygg.color

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.isUnspecified
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import org.florisboard.lib.android.AndroidVersion

@Composable
actual fun systemAccentOrDefault(default: Color): Color {
    return when {
        default.isUnspecified && AndroidVersion.ATLEAST_API31_S -> {
            getSystemAccent()
        }
        default.isUnspecified -> {
            DEFAULT_GREEN
        }
        else -> {
            default
        }
    }
}

@Composable
@RequiresApi(Build.VERSION_CODES.S)
fun getSystemAccent(): Color {
    val context = LocalContext.current
    val resources = LocalResources.current

    return Color(resources.getColor(android.R.color.system_accent1_500, context.theme))
}

