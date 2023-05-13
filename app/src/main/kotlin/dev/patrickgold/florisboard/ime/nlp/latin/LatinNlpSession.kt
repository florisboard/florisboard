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

package dev.patrickgold.florisboard.ime.nlp.latin

import dev.patrickgold.florisboard.ime.keyboard.KeyProximityChecker
import dev.patrickgold.florisboard.lib.FlorisLocale
import dev.patrickgold.florisboard.lib.io.FsFile
import dev.patrickgold.florisboard.native.NativeInstanceWrapper
import dev.patrickgold.florisboard.native.NativePtr
import dev.patrickgold.florisboard.native.NativeStr
import dev.patrickgold.florisboard.native.toNativeStr
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LatinNlpSessionConfig(
    val primaryLocale: FlorisLocale,
    val secondaryLocales: List<FlorisLocale>,
    @SerialName("baseDictionaries")
    val baseDictionaryPaths: List<String>,
    @SerialName("userDictionary")
    val userDictionaryPath: String,
    val predictionWeights: LatinPredictionWeights,
    val keyProximityChecker: KeyProximityChecker,
)

@JvmInline
value class LatinNlpSession(private val _nativePtr: NativePtr = nativeInit()) : NativeInstanceWrapper {
    fun loadFromConfigFile(configFile: FsFile) {
        nativeLoadFromConfigFile(_nativePtr, configFile.absolutePath.toNativeStr())
    }

    override fun nativePtr(): NativePtr {
        return _nativePtr
    }

    override fun dispose() {
        nativeDispose(_nativePtr)
    }

    companion object CXX {
        external fun nativeInit(): NativePtr
        external fun nativeDispose(nativePtr: NativePtr)

        external fun nativeLoadFromConfigFile(nativePtr: NativePtr, configPath: NativeStr)
        //external fun nativeSpell(word: NativeStr, prevWords: List<NativeStr>, flags: Int): SpellingResult
        //external fun nativeSuggest(word: NativeStr, prevWords: List<NativeStr>, flags: Int)
        //external fun nativeTrain(sentence: List<NativeStr>, maxPrevWords: Int)
    }
}
