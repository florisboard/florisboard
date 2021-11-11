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

package dev.patrickgold.florisboard.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import dev.patrickgold.jetpref.datastore.model.PreferenceData
import dev.patrickgold.jetpref.datastore.model.PreferenceObserver

@Composable
fun <V : Any, R : Any> PreferenceData<V>.observeAsTransformingState(transform: (V) -> R): State<R> {
    val lifecycleOwner = LocalLifecycleOwner.current
    val state = remember(key) { mutableStateOf(transform(get())) }
    DisposableEffect(this, lifecycleOwner) {
        val observer = PreferenceObserver<V> { newValue -> state.value = transform(newValue) }
        observe(lifecycleOwner, observer)
        onDispose { removeObserver(observer) }
    }
    return state
}

@Composable
fun <V> LiveData<V>.observeAsNonNullState(): State<V> = observeAsTransformingState { it!! }

@Composable
fun <V, R> LiveData<V>.observeAsTransformingState(transform: (V?) -> R): State<R> {
    val lifecycleOwner = LocalLifecycleOwner.current
    val state = remember { mutableStateOf(transform(value)) }
    DisposableEffect(this, lifecycleOwner) {
        val observer = Observer<V> { state.value = transform(it) }
        observe(lifecycleOwner, observer)
        onDispose { removeObserver(observer) }
    }
    return state
}
