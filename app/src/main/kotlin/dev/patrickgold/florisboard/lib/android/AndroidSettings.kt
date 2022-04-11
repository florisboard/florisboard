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

package dev.patrickgold.florisboard.lib.android

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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import dev.patrickgold.florisboard.lib.kotlin.tryOrNull
import java.lang.reflect.Modifier
import kotlin.reflect.KClass

abstract class AndroidSettingsHelper(
    private val kClass: KClass<*>,
    val groupId: String,
) {
    abstract fun getString(context: Context, key: String): String?

    abstract fun getUriFor(key: String): Uri?

    private fun reflectionGetAllStaticFields(kClass: KClass<*>) = sequence<Pair<String, String>> {
        for (field in kClass.java.declaredFields) {
            if (Modifier.isStatic(field.modifiers)) {
                try {
                    val value = field.get(null) as? String ?: continue
                    yield(field.name to value)
                } catch (e: Exception) {
                    // Cannot access field, continue on to next one
                }
            }
        }
    }

    fun getAllKeys(): Sequence<Pair<String, String>> {
        return reflectionGetAllStaticFields(kClass)
    }

    private fun observe(context: Context, key: String, observer: SystemSettingsObserver) {
        getUriFor(key)?.let { uri ->
            context.contentResolver.registerContentObserver(uri, false, observer)
            observer.dispatchChange(false, uri)
        }
    }

    private fun removeObserver(context: Context, observer: SystemSettingsObserver) {
        context.contentResolver.unregisterContentObserver(observer)
    }

    @Composable
    fun observeAsState(
        key: String,
        foregroundOnly: Boolean = false,
    ) = observeAsState(
        key = key,
        foregroundOnly = foregroundOnly,
        transform = { it },
    )

    @Composable
    fun <R> observeAsState(
        key: String,
        foregroundOnly: Boolean = false,
        transform: (String?) -> R,
    ): State<R> {
        val lifecycleOwner = LocalLifecycleOwner.current
        val context = LocalContext.current.applicationContext
        val state = remember(key) { mutableStateOf(transform(getString(context, key))) }
        DisposableEffect(lifecycleOwner.lifecycle) {
            val observer = SystemSettingsObserver(context) {
                state.value = transform(getString(context, key))
            }
            if (foregroundOnly) {
                val eventObserver = LifecycleEventObserver { _, event ->
                    when (event) {
                        Lifecycle.Event.ON_RESUME -> {
                            observe(context, key, observer)
                        }
                        Lifecycle.Event.ON_PAUSE -> {
                            removeObserver(context, observer)
                        }
                        else -> {}
                    }
                }
                lifecycleOwner.lifecycle.addObserver(eventObserver)
                onDispose {
                    lifecycleOwner.lifecycle.removeObserver(eventObserver)
                    removeObserver(context, observer)
                }
            } else {
                observe(context, key, observer)
                onDispose {
                    removeObserver(context, observer)
                }
            }
        }
        return state
    }
}

object AndroidSettings {
    val Global = object : AndroidSettingsHelper(Settings.Global::class, "global") {
        override fun getString(context: Context, key: String): String? {
            return tryOrNull { Settings.Global.getString(context.contentResolver, key) }
        }

        override fun getUriFor(key: String): Uri? {
            return tryOrNull { Settings.Global.getUriFor(key) }
        }
    }

    val Secure = object : AndroidSettingsHelper(Settings.Secure::class, "secure") {
        override fun getString(context: Context, key: String): String? {
            return tryOrNull { Settings.Secure.getString(context.contentResolver, key) }
        }

        override fun getUriFor(key: String): Uri? {
            return tryOrNull { Settings.Secure.getUriFor(key) }
        }
    }

    val System = object : AndroidSettingsHelper(Settings.System::class, "system") {
        override fun getString(context: Context, key: String): String? {
            return tryOrNull { Settings.System.getString(context.contentResolver, key) }
        }

        override fun getUriFor(key: String): Uri? {
            return tryOrNull { Settings.System.getUriFor(key) }
        }
    }
}
