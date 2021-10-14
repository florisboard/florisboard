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

import android.content.Context
import android.net.Uri
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import dev.patrickgold.florisboard.common.SystemSettingsObserver

abstract class AndroidSettingsHelper {
    abstract fun getString(context: Context, key: String): String?

    abstract fun getUriFor(key: String): Uri?

    private fun observe(context: Context, key: String, observer: SystemSettingsObserver) {
        getUriFor(key)?.let { uri ->
            context.contentResolver.registerContentObserver(uri, false, observer)
        }
    }

    private fun removeObserver(context: Context, observer: SystemSettingsObserver) {
        context.contentResolver.unregisterContentObserver(observer)
    }

    @Composable
    fun observeAsState(key: String) = observeAsState(key, null)

    @Composable
    fun observeAsState(key: String, initial: String?): State<String?> {
        val lifecycleOwner = LocalLifecycleOwner.current
        val context = LocalContext.current
        val state = remember(key) { mutableStateOf(initial) }
        DisposableEffect(lifecycleOwner) {
            val observer = SystemSettingsObserver(context) {
                state.value = getString(context, key)
            }
            observe(context, key, observer)
            onDispose {
                removeObserver(context, observer)
            }
        }
        return state
    }
}

object AndroidSettings {
    val Secure = object : AndroidSettingsHelper() {
        override fun getString(context: Context, key: String): String? {
            return Settings.Secure.getString(context.contentResolver, key)
        }

        override fun getUriFor(key: String): Uri? {
            return Settings.Secure.getUriFor(key)
        }
    }
}
