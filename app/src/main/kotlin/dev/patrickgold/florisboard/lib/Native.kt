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

package dev.patrickgold.florisboard.lib

import java.nio.ByteBuffer

/**
 * Type alias for a native pointer.
 */
typealias NativePtr = Long

/**
 * Constant value for a native null pointer.
 */
const val NATIVE_NULLPTR: NativePtr = 0L

/**
 * Type alias for a native string in standard UTF-8 encoding.
 */
typealias NativeStr = ByteBuffer

/**
 * Converts a native string to a Java string.
 */
fun NativeStr.toJavaString(): String {
    val bytes: ByteArray
    if (this.hasArray()) {
        bytes = this.array()
    } else {
        bytes = ByteArray(this.remaining())
        this.get(bytes)
    }
    return String(bytes, Charsets.UTF_8)
}

/**
 * Converts a Java string to a native string.
 */
fun String.toNativeStr(): NativeStr {
    val bytes = this.toByteArray(Charsets.UTF_8)
    val buffer = ByteBuffer.allocateDirect(bytes.size)
    buffer.put(bytes)
    buffer.rewind()
    return buffer
}

/**
 * Generic interface for a native instance object. Defines the basic
 * methods which each native instance wrapper should define and be able
 * to handle to.
 */
interface NativeInstanceWrapper {
    /**
     * Returns the native pointer of this instance. The returned pointer
     * is only valid if [dispose] has not been previously called.
     *
     * @return The native null pointer for this instance.
     */
    fun nativePtr(): NativePtr

    /**
     * Deletes the native object and frees allocated resources. After
     * invoking this method one MUST NOT touch this instance ever again.
     */
    fun dispose()
}
