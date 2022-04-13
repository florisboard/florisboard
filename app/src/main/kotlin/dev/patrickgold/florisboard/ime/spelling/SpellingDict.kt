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

package dev.patrickgold.florisboard.ime.spelling

import dev.patrickgold.florisboard.ime.nlp.Word
import dev.patrickgold.florisboard.lib.NATIVE_NULLPTR
import dev.patrickgold.florisboard.lib.NativeInstanceWrapper
import dev.patrickgold.florisboard.lib.NativePtr
import dev.patrickgold.florisboard.lib.NativeStr
import dev.patrickgold.florisboard.lib.toJavaString
import dev.patrickgold.florisboard.lib.toNativeStr

@JvmInline
value class SpellingDict private constructor(
    private val _nativePtr: NativePtr
) : NativeInstanceWrapper {

    companion object {
        const val LICENSE_FILE_NAME = "LICENSE.txt"
        const val README_FILE_NAME = "README.txt"

        fun new(path: String, ext: SpellingExtension): SpellingDict? {
            val baseName = ext.spelling.affFile.removeSuffix(".aff")
            val nativePtr = nativeInitialize("$path/$baseName".toNativeStr())
            return if (nativePtr == NATIVE_NULLPTR) {
                null
            } else {
                SpellingDict(nativePtr)
            }
        }

        external fun nativeInitialize(basePath: NativeStr): NativePtr
        external fun nativeDispose(nativePtr: NativePtr)

        external fun nativeSpell(nativePtr: NativePtr, word: NativeStr): Boolean
        external fun nativeSuggest(nativePtr: NativePtr, word: NativeStr, limit: Int): Array<NativeStr>
    }

    override fun nativePtr(): NativePtr {
        return _nativePtr
    }

    override fun dispose() {
        nativeDispose(_nativePtr)
    }

    fun spell(word: Word): Boolean {
        return nativeSpell(_nativePtr, word.toNativeStr())
    }

    fun suggest(word: Word, limit: Int): Array<out String> {
        val nativeSuggestions = nativeSuggest(_nativePtr, word.toNativeStr(), limit)
        return Array(nativeSuggestions.size) { i -> nativeSuggestions[i].toJavaString() }
    }
}
