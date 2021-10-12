/*
 * Copyright (C) 2021 Patrick Goldinger
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

package dev.patrickgold.florisboard.util

import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import dev.patrickgold.florisboard.common.SystemSettingsObserver

object AndroidSettingsSecure {
    @Composable
    fun observeAsState(key: String) = observeAsState(key, null)

    @Composable
    fun observeAsState(key: String, initial: String?): State<String?> {
        val lifecycleOwner = LocalLifecycleOwner.current
        val context = LocalContext.current
        val contentResolver = context.contentResolver
        val state = remember(key) { mutableStateOf(initial) }
        DisposableEffect(lifecycleOwner) {
            val observer = SystemSettingsObserver(context) {
                state.value = Settings.Secure.getString(contentResolver, key)
            }
            contentResolver.registerContentObserver(
                Settings.Secure.getUriFor(key), false, observer
            )
            onDispose {
                contentResolver.unregisterContentObserver(observer)
            }
        }
        return state
    }
}
