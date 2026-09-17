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

package dev.patrickgold.florisboard.ime.io

import android.content.Context
import androidx.core.os.UserManagerCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.concurrent.atomics.AtomicInt
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.concurrent.atomics.fetchAndIncrement

@OptIn(ExperimentalAtomicApi::class)
class AndroidStorageController(
    val context: Context,
) : StorageController {
    private val idCounter = AtomicInt(0)

    override val activeStorage: StateFlow<Storage>
        field = MutableStateFlow(createStorage())

    fun notifyUserUnlocked() {
        activeStorage.value = createStorage()
    }

    private fun createStorage(): AndroidStorage {
        val id = idCounter.fetchAndIncrement()
        val isUserUnlocked = UserManagerCompat.isUserUnlocked(context)
        return AndroidStorage(id, context, isUserUnlocked)
    }
}
