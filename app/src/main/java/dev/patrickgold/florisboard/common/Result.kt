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

import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

inline fun <T> resultOk(value: () -> T): Result<T> {
    contract {
        callsInPlace(value, InvocationKind.EXACTLY_ONCE)
    }
    return Result.success(value())
}

inline fun <T> resultErr(error: () -> Throwable): Result<T> {
    contract {
        callsInPlace(error, InvocationKind.EXACTLY_ONCE)
    }
    return Result.failure(error())
}

inline fun <T> resultErrStr(error: () -> String): Result<T> {
    contract {
        callsInPlace(error, InvocationKind.EXACTLY_ONCE)
    }
    return Result.failure(Exception(error()))
}
