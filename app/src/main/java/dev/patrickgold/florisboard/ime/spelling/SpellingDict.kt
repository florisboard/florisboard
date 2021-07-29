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

import dev.patrickgold.florisboard.common.FlorisLocale
import dev.patrickgold.florisboard.common.NATIVE_NULLPTR
import dev.patrickgold.florisboard.common.NativeInstanceWrapper
import dev.patrickgold.florisboard.common.NativePtr
import dev.patrickgold.florisboard.common.NativeStr
import dev.patrickgold.florisboard.common.toJavaString
import dev.patrickgold.florisboard.common.toNativeStr
import dev.patrickgold.florisboard.ime.nlp.Word
import dev.patrickgold.florisboard.res.ext.ExtensionConfig
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

@JvmInline
value class SpellingDict private constructor(
    private val _nativePtr: NativePtr
) : NativeInstanceWrapper {
    companion object {
        const val LICENSE_FILE_NAME = "LICENSE.txt"
        const val README_FILE_NAME = "README.txt"

        fun new(path: String, meta: Meta): SpellingDict? {
            val baseName = meta.affFile.removeSuffix(".aff")
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
        return nativeSpell(_nativePtr, word.toNativeStr())
    }

    fun suggest(word: Word, limit: Int): Array<out String> {
        val nativeSuggestions = nativeSuggest(_nativePtr, word.toNativeStr(), limit)
        return Array(nativeSuggestions.size) { i -> nativeSuggestions[i].toJavaString() }
    }

    @Serializable
    data class Meta(
        override val id: String,
        override val version: String,
        override val title: String,
        override val description: String,
        override val authors: List<String>,
        override val license: String = ExtensionConfig.CUSTOM_LICENSE_IDENTIFIER,
        override val licenseFile: String,
        override val readmeFile: String,
        @SerialName("language")
        @Serializable(with = FlorisLocale.Serializer::class)
        val locale: FlorisLocale,
        val originalSourceId: String,
        val affFile: String,
        val dicFile: String,
    ) : ExtensionConfig

    data class MetaBuilder(
        var version: String? = null,
        var title: String? = null,
        var description: String? = null,
        var authors: List<String>? = null,
        var locale: FlorisLocale? = null,
        var originalSourceId: String? = null,
        var affFile: String? = null,
        var dicFile: String? = null,
        var readmeFile: String? = null,
        var licenseFile: String? = null
    ) {
        fun build(): Result<Meta> {
            return when {
                locale == null -> Result.failure(
                    NullPointerException("Property 'locale' is null!")
                )
                originalSourceId == null -> Result.failure(
                    NullPointerException("Property 'originalSourceId' is null!")
                )
                affFile == null -> Result.failure(
                    NullPointerException("Property 'affFile' is null!")
                )
                dicFile == null -> Result.failure(
                    NullPointerException("Property 'dicFile' is null!")
                )
                else -> Result.success(
                    Meta(
                        version = version ?: "v0.0.0 (imported)",
                        title = title ?: "Imported spell check dictionary",
                        description = description ?: "Imported spell check dictionary",
                        authors = authors ?: listOf(),
                        id = ExtensionConfig.createIdForImport("spelling", "$originalSourceId.${locale?.localeTag()}"),
                        locale = locale!!,
                        originalSourceId = originalSourceId!!,
                        affFile = affFile!!,
                        dicFile = dicFile!!,
                        readmeFile = readmeFile ?: "",
                        licenseFile = licenseFile ?: ""
                    )
                )
            }
        }
    }
}
