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

package org.florisboard.lib.snygg.ui.v2

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import org.florisboard.lib.snygg.SnyggStylesheet
import org.florisboard.lib.snygg.SnyggTheme

internal val LocalSnyggTheme: ProvidableCompositionLocal<SnyggTheme> =
    staticCompositionLocalOf {
        error("SnyggStylesheet not initialized.")
    }

// TODO: rememberSnyggStylesheet or similar API

@Composable
fun ProvideSnyggTheme(
    snyggTheme: SnyggTheme,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(
        LocalSnyggTheme provides snyggTheme,
        content = content,
    )
}
