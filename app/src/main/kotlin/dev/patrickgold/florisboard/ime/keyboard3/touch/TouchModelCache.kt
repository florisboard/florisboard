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

package dev.patrickgold.florisboard.ime.keyboard3.touch

import io.github.reactivecircus.cache4k.Cache
import kotlinx.coroutines.CoroutineScope
import org.k3lp.model.K3Model

class TouchModelCache {
    private val byK3Model = Cache.Builder<Pair<K3Model, Boolean>, TouchModel>().build()

    fun getFor(
        model: K3Model,
        showNumberRow: Boolean,
    ): TouchModel? {
        return byK3Model.get(model to showNumberRow)
    }

    context(scope: CoroutineScope)
    suspend fun getOrComputeFor(
        model: K3Model,
        showNumberRow: Boolean,
    ): TouchModel {
        return byK3Model.get(model to showNumberRow) { computeTouchModel(model, showNumberRow) }
    }
}
