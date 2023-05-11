/*
 * Copyright (C) 2023 Patrick Goldinger
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

#ifndef _FLORISBOARD_JNI_EXCEPTION_H_
#define _FLORISBOARD_JNI_EXCEPTION_H_

#include <jni.h>

#include <functional>
#include <memory>
#include <stdexcept>
#include <type_traits>
#include <typeinfo>

#define CATCH_EXCEPTION(EXCEPTIONTYPE, EXCEPTIONNAME)                                                                  \
    catch (const EXCEPTIONTYPE& e) {                                                                                   \
        exception_class = env->FindClass("dev/patrickgold/florisboard/native/" EXCEPTIONNAME);                         \
        exception_message = e.what();                                                                                  \
    }

namespace fl::jni {

template<typename F>
auto run_in_exception_container(JNIEnv* env, F&& block) noexcept -> std::invoke_result_t<F> {
    jclass exception_class = nullptr;
    const char* exception_message = nullptr;

    try {
        return block();
    }
    // std::logic_error
    CATCH_EXCEPTION(std::logic_error, "NativeLogicError")
    CATCH_EXCEPTION(std::invalid_argument, "NativeInvalidArgument")
    CATCH_EXCEPTION(std::domain_error, "NativeDomainError")
    CATCH_EXCEPTION(std::length_error, "NativeLengthError")
    CATCH_EXCEPTION(std::out_of_range, "NativeOutOfRange")
    // std::runtime_error
    CATCH_EXCEPTION(std::range_error, "NativeRangeError")
    CATCH_EXCEPTION(std::overflow_error, "NativeOverflowError")
    CATCH_EXCEPTION(std::underflow_error, "NativeUnderflowError")
    CATCH_EXCEPTION(std::runtime_error, "NativeRuntimeError")
    // std::bad_*
    CATCH_EXCEPTION(std::bad_array_new_length, "NativeBadArrayNewLength")
    CATCH_EXCEPTION(std::bad_alloc, "NativeBadAlloc")
    CATCH_EXCEPTION(std::bad_cast, "NativeBadCast")
    CATCH_EXCEPTION(std::bad_typeid, "NativeBadTypeid")
    // std::exception
    CATCH_EXCEPTION(std::exception, "NativeException")

    if (exception_class == nullptr || exception_message == nullptr) {
        exception_class = env->FindClass("java/lang/RuntimeException");
        exception_message = "Unknown error occurred in native code";
    }

    env->ThrowNew(exception_class, exception_message);
    return std::invoke_result_t<F>();
}

} // namespace fl::jni

#endif // _FLORISBOARD_JNI_EXCEPTION_H_
