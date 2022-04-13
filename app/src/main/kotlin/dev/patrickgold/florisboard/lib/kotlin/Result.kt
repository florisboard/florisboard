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

@file:Suppress("NOTHING_TO_INLINE")

package dev.patrickgold.florisboard.lib.kotlin

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async

typealias DeferredResult<T> = Deferred<Result<T>>

inline fun <T> CoroutineScope.runCatchingAsync(
    crossinline block: suspend CoroutineScope.() -> T,
): DeferredResult<T> {
    return this.async {
        runCatching { block() }
    }
}

inline fun resultOk(): Result<Unit> {
    return Result.success(Unit)
}

inline fun <T> resultOk(value: T): Result<T> {
    return Result.success(value)
}

inline fun <T> resultErr(error: Throwable): Result<T> {
    return Result.failure(error)
}

inline fun <T> resultErrStr(error: String): Result<T> {
    return Result.failure(Exception(error))
}

inline fun Result<*>.throwOnFailure() {
    getOrThrow()
}
