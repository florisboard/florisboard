/*
 * Copyright (C) 2024-2025 The FlorisBoard Contributors
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

package dev.patrickgold.florisboard.ime.media.emoji

import android.content.Context
import dev.patrickgold.florisboard.lib.FlorisLocale
import org.florisboard.lib.android.bufferedReader
import io.github.reactivecircus.cache4k.Cache
import java.util.*

private typealias EmojiDataByCategoryImpl = EnumMap<EmojiCategory, MutableList<EmojiSet>>
private typealias EmojiDataBySkinToneImpl = EnumMap<EmojiSkinTone, MutableList<Emoji>>
typealias EmojiDataByCategory = Map<EmojiCategory, List<EmojiSet>>
typealias EmojiDataBySkinTone = Map<EmojiSkinTone, List<Emoji>>

data class EmojiData(
    val byCategory: EmojiDataByCategory,
    val bySkinTone: EmojiDataBySkinTone,
) {
    companion object {
        private val cache = Cache.Builder().build<String, EmojiData>()
        val Fallback = empty()

        private fun newByCategory(): EmojiDataByCategoryImpl {
            return EmojiDataByCategoryImpl(EmojiCategory::class.java).also { map ->
                for (category in EmojiCategory.entries) {
                    map[category] = mutableListOf()
                }
            }
        }

        private fun newBySkinTone(): EmojiDataBySkinToneImpl {
            return EmojiDataBySkinToneImpl(EmojiSkinTone::class.java).also { map ->
                for (skinTone in EmojiSkinTone.entries) {
                    map[skinTone] = mutableListOf()
                }
            }
        }

        fun empty(): EmojiData {
            return EmojiData(newByCategory(), newBySkinTone())
        }

        suspend fun get(context: Context, path: String): EmojiData {
            return cache.get(path) {
                loadEmojiDataMap(context, path)
            }
        }

        suspend fun get(context: Context, locale: FlorisLocale): EmojiData {
            val path = resolveEmojiAssetPath(context, locale) ?: return empty()
            return get(context, path)
        }

        private fun loadEmojiDataMap(context: Context, path: String): EmojiData {
            val byCategory = newByCategory()
            val bySkinTone = newBySkinTone()

            var ec: EmojiCategory? = null
            var emojiEditorList: MutableList<Emoji>? = null

            fun commitEmojiEditorList() {
                emojiEditorList?.let { byCategory[ec]!!.add(EmojiSet(it)) }
                emojiEditorList = null
            }

            context.assets.bufferedReader(path).useLines { lines ->
                for (line in lines) {
                    if (line.startsWith("#")) {
                        // Comment line
                    } else if (line.startsWith("[")) {
                        commitEmojiEditorList()
                        ec = EmojiCategory.entries.find { it.id == line.slice(1 until (line.length - 1)) }
                    } else if (line.trim().isEmpty() || ec == null) {
                        // Empty line
                        continue
                    } else {
                        if (!line.startsWith("\t")) {
                            commitEmojiEditorList()
                        }
                        // Assume it is a data line
                        val data = line.split(";")
                        if (data.size == 3) {
                            val base = emojiEditorList?.first()
                            val emoji = Emoji(
                                value = data[0].trim(),
                                name = base?.name ?: data[1].trim(),
                                keywords = data[2].split("|").map { it.trim() },
                            )
                            if (emojiEditorList != null) {
                                emojiEditorList!!.add(emoji)
                            } else {
                                emojiEditorList = mutableListOf(emoji)
                            }
                        }
                    }
                }
                commitEmojiEditorList()
            }

            for (category in byCategory.keys) {
                for (emojiSet in byCategory[category]!!) {
                    if (emojiSet.emojis.size == 1) {
                        // No variations provided, we fallback to using the base for all skin tones
                        val base = emojiSet.emojis.first()
                        for (skinTone in EmojiSkinTone.entries) {
                            bySkinTone[skinTone]!!.add(base)
                        }
                        continue
                    }
                    for (emoji in emojiSet.emojis) {
                        bySkinTone[emoji.skinTone]!!.add(emoji)
                    }
                }
            }

            return EmojiData(byCategory, bySkinTone)
        }

        /**
         * Resolves the path to the emoji asset file based on the active keyboard subtype and active locale.
         *
         * This method prioritizes usage of cached emoji layout maps when available. If a cached map is found then return,
         * the parseRawEmojiSpecsFile will correctly return the cached data
         *
         * It then attempts to locate a matching emoji asset file within the "ime/media/emoji/" directory in the
         * application's assets based on locale priority:
         * - Primary locale of the active subtype
         * - Secondary locales of the active subtype
         * - Root path ("ime/media/emoji/root.txt") as a fallback
         *
         * For each locale, file matching follows this preference order:
         * - {language}_{country}_{variant}.txt
         * - {language}_{country}.txt
         * - {language}.txt
         *
         * @param context The context used to access application assets.
         * @param locale The locale to resolve for.
         *
         * @return The path to the emoji asset file, or the root path ("ime/media/emoji/root.txt") if no match is found.
         */
        private fun resolveEmojiAssetPath(context: Context, locale: FlorisLocale): String? {
            val emojiAssets = context.assets.list("ime/media/emoji/")!!.toList()
            val makePath = { file: String -> "ime/media/emoji/$file" }
            val language = locale.language.lowercase()
            val country = locale.country.takeIf { it.isNotBlank() }
            val variant = locale.variant.takeIf { it.isNotBlank() }
            if (variant != null && country != null) {
                "${language}_${country}_${variant}.txt".takeIf { emojiAssets.contains(it) }?.let {
                    return makePath(it)
                }
            }
            if (country != null) {
                "${language}_${country}.txt".takeIf { emojiAssets.contains(it) }?.let { return makePath(it) }
            }
            "${language}.txt".takeIf { emojiAssets.contains(it) }?.let {
                return makePath(it)
            }
            return null
        }
    }
}
