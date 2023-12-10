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

@file:Suppress("unused")

package dev.patrickgold.florisboard.native

/**
 * Base exception class for all native exceptions.
 *
 * @param msg The error message describing the native exception.
 *
 * @see <a href="https://en.cppreference.com/w/cpp/error/exception">std::exception on cppreference.com</a>
 */
open class NativeException(msg: String) : Exception(msg)

/**
 * Exception class used to describe failed native allocations.
 *
 * @param msg The error message describing the failed native allocation.
 *
 * @see <a href="https://en.cppreference.com/w/cpp/memory/new/bad_alloc">std::bad_alloc on cppreference.com</a>
 */
open class NativeBadAlloc(msg: String) : NativeException(msg)

/**
 * Exception class used to describe failed array allocations to to incorrect lengths.
 *
 * @param msg The error message describing the failed native array allocation.
 *
 * @see <a href="https://en.cppreference.com/w/cpp/memory/new/bad_array_new_length">std::bad_array_new_length on
 *  cppreference.com</a>
 */
open class NativeBadArrayNewLength(msg: String) : NativeBadAlloc(msg)

/**
 * Exception class used to describe failed native casts.
 *
 * @param msg The error message describing the failed native cast.
 *
 * @see <a href="https://en.cppreference.com/w/cpp/types/bad_cast">std::bad_cast on cppreference.com</a>
 */
open class NativeBadCast(msg: String) : NativeException(msg)

/**
 * Exception class used to describe failed native type id operators.
 *
 * @param msg The error message describing the failed native type id operator.
 *
 * @see <a href="https://en.cppreference.com/w/cpp/types/bad_typeid">std::bad_typeid on cppreference.com</a>
 */
open class NativeBadTypeid(msg: String) : NativeException(msg)

/**
 * Exception class which indicates violations of logical preconditions or class invariants.
 *
 * @param msg The error message describing the logic error.
 *
 * @see <a href="https://en.cppreference.com/w/cpp/error/logic_error">std::logic_error on cppreference.com</a>
 */
open class NativeLogicError(msg: String) : NativeException(msg)

/**
 * Exception class used to report invalid arguments.
 *
 * @param msg The error message describing the invalid argument.
 *
 * @see <a href="https://en.cppreference.com/w/cpp/error/invalid_argument">std::invalid_argument on cppreference.com</a>
 */
open class NativeInvalidArgument(msg: String) : NativeLogicError(msg)

/**
 * Exception class used to report domain errors.
 *
 * @param msg The error message describing the domain error.
 *
 * @see <a href="https://en.cppreference.com/w/cpp/error/domain_error">std::domain_error on cppreference.com</a>
 */
open class NativeDomainError(msg: String) : NativeLogicError(msg)

/**
 * Exception class used to report attempts to exceed the maximum allowed size.
 *
 * @param msg The error message describing the length error.
 *
 * @see <a href="https://en.cppreference.com/w/cpp/error/length_error">std::length_error on cppreference.com</a>
 */
open class NativeLengthError(msg: String) : NativeLogicError(msg)

/**
 * Exception class used to report arguments outside of the expected range.
 *
 * @param msg The error message describing the out-of-range argument.
 *
 * @see <a href="https://en.cppreference.com/w/cpp/error/out_of_range">std::out_of_range on cppreference.com</a>
 */
open class NativeOutOfRange(msg: String) : NativeLogicError(msg)

/**
 * Exception class used to indicate conditions only detectable at run time.
 *
 * @param msg The error message describing the runtime error.
 *
 * @see <a href="https://en.cppreference.com/w/cpp/error/runtime_error">std::runtime_error on cppreference.com</a>
 */
open class NativeRuntimeError(msg: String) : NativeException(msg)

/**
 * Exception class used to report range errors in internal computations.
 *
 * @param msg The error message describing the range error.
 *
 * @see <a href="https://en.cppreference.com/w/cpp/error/range_error">std::range_error on cppreference.com</a>
 */
open class NativeRangeError(msg: String) : NativeRuntimeError(msg)

/**
 * Exception class used to report arithmetic overflows.
 *
 * @param msg The error message describing the overflow error.
 *
 * @see <a href="https://en.cppreference.com/w/cpp/error/overflow_error">std::overflow_error on cppreference.com</a>
 */
open class NativeOverflowError(msg: String) : NativeRuntimeError(msg)

/**
 * Exception class used to report arithmetic underflows.
 *
 * @param msg The error message describing the underflow error.
 *
 * @see <a href="https://en.cppreference.com/w/cpp/error/underflow_error">std::underflow_error on cppreference.com</a>
 */
open class NativeUnderflowError(msg: String) : NativeRuntimeError(msg)
