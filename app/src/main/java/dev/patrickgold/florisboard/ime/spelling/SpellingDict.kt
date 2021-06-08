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

import dev.patrickgold.florisboard.common.NativeInstanceWrapper
import dev.patrickgold.florisboard.common.NativePtr
import dev.patrickgold.florisboard.ime.nlp.Word

@JvmInline
value class SpellingDict private constructor(
    private val _nativePtr: NativePtr
) : NativeInstanceWrapper {
    companion object {
        fun new(path: String): SpellingDict {
            val nativePtr = nativeInitialize(path)
            return SpellingDict(nativePtr)
        }

        external fun nativeInitialize(path: String): NativePtr
        external fun nativeDispose(nativePtr: NativePtr)

        external fun nativeSpell(nativePtr: NativePtr, word: Word): Boolean
        external fun nativeSuggest(nativePtr: NativePtr, word: Word): Array<out String>
    }


    override fun nativePtr(): NativePtr {
        return _nativePtr
    }

    override fun dispose() {
        nativeDispose(_nativePtr)
    }

    fun spell(word: Word): Boolean {
        return nativeSpell(_nativePtr, word)
    }

    fun suggest(word: Word): Array<out String> {
        return nativeSuggest(_nativePtr, word)
    }
}
