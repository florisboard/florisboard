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

@file:OptIn(ExperimentalContracts::class)

package dev.patrickgold.florisboard.ime.spelling

import dev.patrickgold.florisboard.common.NativeInstanceWrapper
import dev.patrickgold.florisboard.common.NativePtr
import dev.patrickgold.florisboard.ime.core.LocaleSerializer
import dev.patrickgold.florisboard.res.AssetRef
import dev.patrickgold.florisboard.ime.nlp.Word
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.*
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

@JvmInline
value class SpellingDict private constructor(
    private val _nativePtr: NativePtr
) : NativeInstanceWrapper {
    companion object {
        const val LICENSE_FILE_NAME = "LICENSE.txt"
        const val README_FILE_NAME = "README.txt"

        fun new(ref: AssetRef, meta: Meta): SpellingDict {
            val nativePtr = nativeInitialize(ref.path, meta.affFileName, meta.dicFileName)
            return SpellingDict(nativePtr)
        }

        external fun nativeInitialize(baseBath: String, affFileName: String, dicFileName: String): NativePtr
        external fun nativeDispose(nativePtr: NativePtr)

        external fun nativeSpell(nativePtr: NativePtr, word: Word): Boolean
        external fun nativeSuggest(nativePtr: NativePtr, word: Word): Array<out String>

        inline fun <R> metaBuilder(block: MetaBuilder.() -> R): R {
            contract {
                callsInPlace(block, InvocationKind.EXACTLY_ONCE)
            }
            return block(MetaBuilder())
        }
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

    @Serializable
    data class Meta(
        @SerialName("language")
        @Serializable(with = LocaleSerializer::class)
        val locale: Locale,
        @SerialName("original")
        val originalSourceId: String,
        @SerialName("aff")
        val affFileName: String,
        @SerialName("dic")
        val dicFileName: String,
        @SerialName("hyph")
        val hyphFileName: String? = null,
        @SerialName("readme")
        val readmeFileName: String? = null,
        @SerialName("license")
        val licenseFileName: String? = null
    )

    data class MetaBuilder(
        var locale: Locale? = null,
        var originalSourceId: String? = null,
        var affFileName: String? = null,
        var dicFileName: String? = null,
        var hyphFileName: String? = null,
        var readmeFileName: String? = null,
        var licenseFileName: String? = null
    ) {
        fun build(): Result<Meta> {
            return when {
                locale == null -> Result.failure(
                    NullPointerException("Property 'locale' is null!")
                )
                originalSourceId == null -> Result.failure(
                    NullPointerException("Property 'originalSourceId' is null!")
                )
                affFileName == null -> Result.failure(
                    NullPointerException("Property 'affFileName' is null!")
                )
                dicFileName == null -> Result.failure(
                    NullPointerException("Property 'dicFileName' is null!")
                )
                else -> Result.success(
                    Meta(
                        locale!!,
                        originalSourceId!!,
                        affFileName!!,
                        dicFileName!!,
                        hyphFileName,
                        readmeFileName,
                        licenseFileName
                    )
                )
            }
        }
    }
}
