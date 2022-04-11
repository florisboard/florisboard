/*
 * Copyright (C) 2022 Patrick Goldinger
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

package dev.patrickgold.florisboard.lib.kotlin

import kotlinx.coroutines.sync.Mutex
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

class GuardedByLock<out T : Any>(@PublishedApi internal val wrapped: T) {
    @PublishedApi
    internal val lock = Mutex(locked = false)

    suspend inline fun <R> withLock(owner: Any? = null, action: (T) -> R): R {
        contract {
            callsInPlace(action, InvocationKind.EXACTLY_ONCE)
        }
        lock.lock(owner)
        try {
            return action(wrapped)
        } finally {
            lock.unlock(owner)
        }
    }
}

inline fun <T : Any> guardedByLock(initializer: () -> T): GuardedByLock<T> {
    contract {
        callsInPlace(initializer, InvocationKind.EXACTLY_ONCE)
    }
    return GuardedByLock(initializer())
}
